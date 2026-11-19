package com.thirstwastaken2.gametest.platform;

import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * The copper distiller through Forge's fluid capability, the way a Pipez pipe reaches it. Forge hangs it
 * on a block entity, so only the boiler half answers. A move that is not committed is simulated.
 */
public final class DistillerFluids {
    private DistillerFluids() { }

    public static boolean bothHalves() {
        return false;
    }

    public static boolean has(Level level, BlockPos pos) {
        return handler(level, pos) != null;
    }

    public static int fillWater(Level level, BlockPos pos, WaterQuality quality, int amount, boolean commit) {
        FluidStack water = new FluidStack(Fluids.WATER, amount);
        return fill(level, pos, quality == null ? water : WaterFluids.stamp(water, quality), commit);
    }

    public static int fillLava(Level level, BlockPos pos, int amount) {
        return fill(level, pos, new FluidStack(Fluids.LAVA, amount), true);
    }

    public static FluidMove drain(Level level, BlockPos pos, int amount, boolean commit) {
        IFluidHandler handler = handler(level, pos);
        if (handler == null) return new FluidMove(-1, null, null);
        FluidStack drained = handler.drain(amount, action(commit));
        return new FluidMove(drained.getAmount(), drained.isEmpty() ? null : WaterFluids.quality(drained), null);
    }

    private static int fill(Level level, BlockPos pos, FluidStack fluid, boolean commit) {
        IFluidHandler handler = handler(level, pos);
        return handler == null ? -1 : handler.fill(fluid, action(commit));
    }

    private static IFluidHandler.FluidAction action(boolean commit) {
        return commit ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
    }

    private static IFluidHandler handler(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? null : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
    }
}
