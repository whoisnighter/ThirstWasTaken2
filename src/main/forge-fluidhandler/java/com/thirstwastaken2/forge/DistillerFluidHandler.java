package com.thirstwastaken2.forge;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * The copper distiller as Forge's fluid capability, so a Pipez pipe, or any other mod's, fills its boiler
 * and empties its basin. Tank 0 is the boiler, which takes water of any grade and gives none; tank 1 the
 * basin, which gives Pure water and takes none. Any amount moves; the machine counts millibuckets. The
 * NeoForge handlers keep the same rules. See {@code docs/dev/integration/storage/PIPEZ-INTEGRATION.md}.
 *
 * <p>Forge hangs a capability on a block entity, and only the boiler half has one, so only it is piped;
 * on NeoForge and Fabric either half is.
 */
final class DistillerFluidHandler implements IFluidHandler {
    private static final Identifier ID = ThirstWasTaken2.id("distiller_fluids");
    private static final int BOILER = 0;

    private final DistillerBlockEntity machine;

    private DistillerFluidHandler(DistillerBlockEntity machine) {
        this.machine = machine;
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, DistillerFluidHandler::attach);
    }

    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof DistillerBlockEntity machine)) return;
        LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> new DistillerFluidHandler(machine));
        event.addCapability(ID, new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
                return ForgeCapabilities.FLUID_HANDLER.orEmpty(capability, handler);
            }
        });
        event.addListener(handler::invalidate);
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
        if (resource.isEmpty() || !resource.isFluidEqual(water(pure(), 1))) return FluidStack.EMPTY;
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
