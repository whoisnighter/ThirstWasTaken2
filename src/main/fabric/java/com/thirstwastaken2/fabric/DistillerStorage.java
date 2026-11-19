package com.thirstwastaken2.fabric;

import com.google.common.collect.MapMaker;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The copper distiller as Transfer API fluid storage, on either half, so any mod's pipe fills its boiler
 * and empties its basin: water of any grade goes in to the boiler, Pure water comes out of the basin.
 * The NeoForge handlers keep the same rules. Any amount moves, to the millibucket, 81 droplets; the
 * machine counts millibuckets. See {@code docs/dev/integration/storage/PIPEZ-INTEGRATION.md}.
 *
 * <p>One storage per machine: its snapshot is the tanks, and two storages on one machine would put back
 * each other's. The machine is saved and the client told only on the final commit.
 */
public final class DistillerStorage extends SnapshotParticipant<Long> implements Storage<FluidVariant> {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;
    // Weak both ways, since each storage holds its machine: the transaction holds a storage while it is in use.
    private static final Map<DistillerBlockEntity, DistillerStorage> STORAGES =
            new MapMaker().concurrencyLevel(1).weakKeys().weakValues().makeMap();

    private final DistillerBlockEntity machine;
    private final List<StorageView<FluidVariant>> views;

    private DistillerStorage(DistillerBlockEntity machine) {
        this.machine = machine;
        this.views = List.of(new Boiler(), new Basin());
    }

    /** Called once by {@link ThirstWasTaken2Fabric}, after the blocks are registered. */
    public static void register() {
        FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
            DistillerBlockEntity machine = DistillerBlock.machine(level, pos, state);
            return machine == null ? null : STORAGES.computeIfAbsent(machine, DistillerStorage::new);
        }, ThirstBlocks.COPPER_DISTILLER);
    }

    private static WaterQuality pure() {
        return WaterQuality.fresh(WaterPurity.MAX);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        WaterQuality quality = WaterContainerStorage.quality(resource);
        if (quality == null || machine.isRemoved()) return 0;
        int filled = (int) Math.min(maxAmount / DROPLETS_PER_MB, DistillerBlockEntity.tankMb() - machine.boilerMb());
        if (filled <= 0) return 0;
        updateSnapshots(transaction);
        return machine.fillBoiler(filled, quality) * DROPLETS_PER_MB;
    }

    /** Only Pure water comes out. */
    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (machine.isRemoved() || !pure().equals(WaterContainerStorage.quality(resource))) return 0;
        int drained = (int) Math.min(maxAmount / DROPLETS_PER_MB, machine.basinMb());
        if (drained <= 0) return 0;
        updateSnapshots(transaction);
        return machine.drainBasin(drained) * DROPLETS_PER_MB;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return views.iterator();
    }

    @Override
    protected Long createSnapshot() {
        return machine.tanks();
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        machine.restoreTanks(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        machine.fluidsChanged();
    }

    /** The boiler: shows what it holds, gives none of it. */
    private final class Boiler implements StorageView<FluidVariant> {
        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public boolean isResourceBlank() {
            return machine.boilerQuality() == null;
        }

        @Override
        public FluidVariant getResource() {
            WaterQuality held = machine.boilerQuality();
            return held == null ? FluidVariant.blank() : WaterContainerStorage.water(held);
        }

        @Override
        public long getAmount() {
            return machine.boilerMb() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return DistillerBlockEntity.tankMb() * DROPLETS_PER_MB;
        }
    }

    /** The basin: Pure water, out through the storage's own extract. */
    private final class Basin implements StorageView<FluidVariant> {
        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return DistillerStorage.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return machine.basinMb() == 0;
        }

        @Override
        public FluidVariant getResource() {
            return machine.basinMb() == 0 ? FluidVariant.blank() : WaterContainerStorage.water(pure());
        }

        @Override
        public long getAmount() {
            return machine.basinMb() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return DistillerBlockEntity.tankMb() * DROPLETS_PER_MB;
        }
    }
}
