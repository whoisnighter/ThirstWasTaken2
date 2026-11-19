package com.thirstwastaken2.gametest.platform;

import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The copper distiller through NeoForge's block fluid capability, the way a Pipez pipe reaches it:
 * {@code IFluidHandler} on 1.21.1 and the transfer API from 1.21.11. A move that is not committed is
 * simulated on 1.21.1 and rolled back on the transfer API.
 */
public final class DistillerFluids {
    private DistillerFluids() { }

    /** Whether both halves are piped; on Forge only the boiler half is. */
    public static boolean bothHalves() {
        return true;
    }

    /** Pours {@code amount} mB of water of {@code quality}, or with no grade when null. -1 with no capability. */
    public static int fillWater(Level level, BlockPos pos, WaterQuality quality, int amount, boolean commit) {
        FluidStack water = new FluidStack(Fluids.WATER, amount);
        return fill(level, pos, quality == null ? water : WaterFluids.stamp(water, quality), commit);
    }

    public static int fillLava(Level level, BlockPos pos, int amount) {
        return fill(level, pos, new FluidStack(Fluids.LAVA, amount), true);
    }

    //? if >=1.21.2 {
    public static boolean has(Level level, BlockPos pos) {
        return handler(level, pos) != null;
    }

    /** Draws up to {@code amount} mB of whatever the handler offers, as a pipe does. -1 with no capability. */
    public static FluidMove drain(Level level, BlockPos pos, int amount, boolean commit) {
        var handler = handler(level, pos);
        if (handler == null) return new FluidMove(-1, null, null);
        for (int index = 0; index < handler.size(); index++) {
            var held = handler.getResource(index);
            if (held.isEmpty()) continue;
            int drained;
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                drained = handler.extract(held, amount, transaction);
                if (commit) transaction.commit();
            }
            if (drained > 0) return new FluidMove(drained, WaterFluids.quality(held.toStack(1)), null);
        }
        return new FluidMove(0, null, null);
    }

    private static int fill(Level level, BlockPos pos, FluidStack fluid, boolean commit) {
        var handler = handler(level, pos);
        if (handler == null) return -1;
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int filled = handler.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(fluid), fluid.getAmount(),
                    transaction);
            if (commit) transaction.commit();
            return filled;
        }
    }

    private static net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler(
            Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP);
    }
    //?} else {
    /*public static boolean has(Level level, BlockPos pos) {
        return handler(level, pos) != null;
    }

    public static FluidMove drain(Level level, BlockPos pos, int amount, boolean commit) {
        var handler = handler(level, pos);
        if (handler == null) return new FluidMove(-1, null, null);
        FluidStack drained = handler.drain(amount, commit
                ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        return new FluidMove(drained.getAmount(), drained.isEmpty() ? null : WaterFluids.quality(drained), null);
    }

    private static int fill(Level level, BlockPos pos, FluidStack fluid, boolean commit) {
        var handler = handler(level, pos);
        if (handler == null) return -1;
        return handler.fill(fluid, commit
                ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
    }

    private static net.neoforged.neoforge.fluids.capability.IFluidHandler handler(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
    }
    *///?}
}
