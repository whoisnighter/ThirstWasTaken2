package com.thirstwastaken2.gametest.platform;

import com.thirstwastaken2.fabric.WaterContainerStorage;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/**
 * The copper distiller through the Transfer API's block fluid storage, the way any Fabric pipe reaches
 * it, in millibuckets: 81 droplets each. A move that is not committed is rolled back.
 */
public final class DistillerFluids {
    private static final long MB = FluidConstants.BUCKET / 1000;

    private DistillerFluids() { }

    public static boolean bothHalves() {
        return true;
    }

    public static boolean has(Level level, BlockPos pos) {
        return storage(level, pos) != null;
    }

    public static int fillWater(Level level, BlockPos pos, WaterQuality quality, int amount, boolean commit) {
        return fill(level, pos, quality == null ? FluidVariant.of(Fluids.WATER) : WaterContainerStorage.water(quality),
                amount, commit);
    }

    public static int fillLava(Level level, BlockPos pos, int amount) {
        return fill(level, pos, FluidVariant.of(Fluids.LAVA), amount, true);
    }

    /** Draws up to {@code amount} mB of whatever the storage offers, as a pipe does. */
    public static FluidMove drain(Level level, BlockPos pos, int amount, boolean commit) {
        Storage<FluidVariant> storage = storage(level, pos);
        if (storage == null) return new FluidMove(-1, null, null);
        for (StorageView<FluidVariant> view : storage) {
            if (view.isResourceBlank()) continue;
            FluidVariant held = view.getResource();
            long drained;
            try (Transaction transaction = Transaction.openOuter()) {
                drained = storage.extract(held, amount * MB, transaction);
                if (commit) transaction.commit();
            }
            if (drained > 0) return new FluidMove((int) (drained / MB), WaterContainerStorage.quality(held), null);
        }
        return new FluidMove(0, null, null);
    }

    private static int fill(Level level, BlockPos pos, FluidVariant fluid, int amount, boolean commit) {
        Storage<FluidVariant> storage = storage(level, pos);
        if (storage == null) return -1;
        try (Transaction transaction = Transaction.openOuter()) {
            long filled = storage.insert(fluid, amount * MB, transaction);
            if (commit) transaction.commit();
            return (int) (filled / MB);
        }
    }

    private static Storage<FluidVariant> storage(Level level, BlockPos pos) {
        return FluidStorage.SIDED.find(level, pos, Direction.UP);
    }
}
