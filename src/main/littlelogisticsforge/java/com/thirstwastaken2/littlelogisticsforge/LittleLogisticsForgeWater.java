package com.thirstwastaken2.littlelogisticsforge;

import java.util.function.BooleanSupplier;

/**
 * Whether the server thread is inside a click on one of Little Logistics' fluid tanks with a container in
 * hand. Loaded only from the mixins, which only apply with the mod installed.
 */
public final class LittleLogisticsForgeWater {
    /**
     * How deep the thread is inside such a click. Only there does {@code ForgeFluidUtilMixin} wrap a
     * container's handler, so no other mod's use of Forge's {@code FluidUtil} changes.
     */
    private static final ThreadLocal<int[]> TRANSFER = ThreadLocal.withInitial(() -> new int[1]);

    private LittleLogisticsForgeWater() { }

    /** Runs {@code transfer} with containers' handlers carrying the grade; see {@link #TRANSFER}. */
    public static boolean graded(BooleanSupplier transfer) {
        int[] depth = TRANSFER.get();
        depth[0]++;
        try {
            return transfer.getAsBoolean();
        } finally {
            depth[0]--;
        }
    }

    public static boolean inTransfer() {
        return TRANSFER.get()[0] > 0;
    }
}
