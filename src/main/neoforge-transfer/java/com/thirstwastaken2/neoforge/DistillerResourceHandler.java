package com.thirstwastaken2.neoforge;

import com.google.common.collect.MapMaker;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Map;
import java.util.Objects;

/**
 * The copper distiller as a fluid {@code ResourceHandler}, NeoForge's transfer API from 1.21.11, so a
 * Pipez pipe, or any other mod's, fills its boiler and empties its basin. Tank 0 is the boiler, which
 * takes water of any grade and gives none; tank 1 the basin, which gives Pure water and takes none.
 * Any amount moves; the machine counts millibuckets. See
 * {@code docs/dev/integration/storage/PIPEZ-INTEGRATION.md}.
 *
 * <p>One handler per machine, as NeoForge's cauldron wrapper keeps one per cauldron: its journal holds
 * the tanks for a transaction to put back, and two handlers on one machine would put back each other's.
 * The machine is saved and the client told only on the root commit.
 */
final class DistillerResourceHandler extends SnapshotJournal<Long> implements ResourceHandler<FluidResource> {
    private static final int BOILER = 0;
    private static final int BASIN = 1;
    private static final Map<DistillerBlockEntity, DistillerResourceHandler> HANDLERS =
            new MapMaker().concurrencyLevel(1).weakKeys().weakValues().makeMap();
    /** Water of each grade, then salt, built once: pipes ask every tick. */
    private static FluidResource[] waters;

    private final DistillerBlockEntity machine;

    private DistillerResourceHandler(DistillerBlockEntity machine) {
        this.machine = machine;
    }

    /** On either half of a whole distiller; an unpiped boiler has no machine and answers nothing. */
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) -> {
            DistillerBlockEntity machine = DistillerBlock.machine(level, pos, state);
            return machine == null ? null : HANDLERS.computeIfAbsent(machine, DistillerResourceHandler::new);
        }, ThirstBlocks.COPPER_DISTILLER);
    }

    private static FluidResource water(WaterQuality quality) {
        FluidResource[] built = waters;
        if (built == null) {
            built = new FluidResource[WaterPurity.MAX + 2];
            for (int grade = 0; grade <= WaterPurity.MAX; grade++) built[grade] = of(WaterQuality.fresh(grade));
            built[WaterPurity.MAX + 1] = of(WaterQuality.SALT);
            waters = built;
        }
        return built[quality instanceof WaterQuality.Fresh fresh ? fresh.purity() : WaterPurity.MAX + 1];
    }

    private static FluidResource of(WaterQuality quality) {
        return FluidResource.of(WaterFluids.stamp(new FluidStack(Fluids.WATER, 1), quality));
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        if (index == BOILER) {
            WaterQuality held = machine.boilerQuality();
            return held == null ? FluidResource.EMPTY : water(held);
        }
        return machine.basinMb() == 0 ? FluidResource.EMPTY : water(WaterQuality.fresh(WaterPurity.MAX));
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return index == BOILER ? machine.boilerMb() : machine.basinMb();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return resource.isEmpty() || isValid(index, resource) ? DistillerBlockEntity.tankMb() : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        if (resource.isEmpty()) return false;
        FluidStack stack = resource.toStack(1);
        if (!WaterFluids.isWater(stack)) return false;
        return index == BOILER || WaterFluids.quality(stack).equals(WaterQuality.fresh(WaterPurity.MAX));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index != BOILER || machine.isRemoved()) return 0;
        FluidStack stack = resource.toStack(1);
        if (!WaterFluids.isWater(stack)) return 0;
        int filled = Math.min(amount, DistillerBlockEntity.tankMb() - machine.boilerMb());
        if (filled <= 0) return 0;
        updateSnapshots(transaction);
        return machine.fillBoiler(filled, WaterFluids.quality(stack));
    }

    /** Only Pure water comes out, and only to a request for exactly that, as a tank compares. */
    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index != BASIN || machine.isRemoved() || !resource.equals(water(WaterQuality.fresh(WaterPurity.MAX)))) {
            return 0;
        }
        int drained = Math.min(amount, machine.basinMb());
        if (drained <= 0) return 0;
        updateSnapshots(transaction);
        return machine.drainBasin(drained);
    }

    @Override
    protected Long createSnapshot() {
        return machine.tanks();
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        machine.restoreTanks(snapshot);
    }

    @Override
    protected void onRootCommit(Long original) {
        if (machine.tanks() != original) machine.fluidsChanged();
    }
}
