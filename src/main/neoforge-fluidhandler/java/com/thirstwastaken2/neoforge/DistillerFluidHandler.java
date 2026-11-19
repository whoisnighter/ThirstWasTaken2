package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The copper distiller as NeoForge 1.21.1's block fluid capability, so a Pipez pipe, or any other mod's,
 * fills its boiler and empties its basin. Tank 0 is the boiler, which takes water of any grade and gives
 * none; tank 1 the basin, which gives Pure water and takes none. Any amount moves; the machine counts
 * millibuckets. The transfer API's handler, for the later versions, keeps the same rules. See
 * {@code docs/dev/integration/storage/PIPEZ-INTEGRATION.md}.
 */
final class DistillerFluidHandler implements IFluidHandler {
    private static final int BOILER = 0;

    private final DistillerBlockEntity machine;

    private DistillerFluidHandler(DistillerBlockEntity machine) {
        this.machine = machine;
    }

    /** On either half of a whole distiller; an unpiped boiler has no machine and answers nothing. */
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, side) -> {
            DistillerBlockEntity machine = DistillerBlock.machine(level, pos, state);
            return machine == null ? null : new DistillerFluidHandler(machine);
        }, ThirstBlocks.COPPER_DISTILLER);
    }

    private static FluidStack water(WaterQuality quality, int amount) {
        return WaterFluids.stamp(new FluidStack(Fluids.WATER, amount), quality);
    }

    private static WaterQuality pure() {
        return WaterQuality.fresh(WaterPurity.MAX);
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank == BOILER) {
            WaterQuality held = machine.boilerQuality();
            return held == null ? FluidStack.EMPTY : water(held, machine.boilerMb());
        }
        return machine.basinMb() == 0 ? FluidStack.EMPTY : water(pure(), machine.basinMb());
    }

    @Override
    public int getTankCapacity(int tank) {
        return DistillerBlockEntity.tankMb();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (!WaterFluids.isWater(stack)) return false;
        return tank == BOILER || WaterFluids.quality(stack).equals(pure());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (machine.isRemoved() || !WaterFluids.isWater(resource)) return 0;
        int filled = Math.min(resource.getAmount(), DistillerBlockEntity.tankMb() - machine.boilerMb());
        if (filled <= 0) return 0;
        if (action.execute()) {
            machine.fillBoiler(filled, WaterFluids.quality(resource));
            machine.fluidsChanged();
        }
        return filled;
    }

    /** Only Pure water comes out, and only to a request for exactly that, as a tank compares. */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, water(pure(), 1))) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (machine.isRemoved()) return FluidStack.EMPTY;
        int drained = Math.min(maxDrain, machine.basinMb());
        if (drained <= 0) return FluidStack.EMPTY;
        if (action.execute()) {
            machine.drainBasin(drained);
            machine.fluidsChanged();
        }
        return water(pure(), drained);
    }
}
