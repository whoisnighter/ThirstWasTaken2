package com.thirstwastaken2.dev.platform;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Every call into the mod loader the dev tools make and {@code com.thirstwastaken2.platform.Loader}
 * does not, for NeoForge. The counterpart of the Fabric copy, under the same rules.
 */
public final class DevLoader {
    /** Which loader this build compiled, for the agent's {@code probe} answer. */
    public static final String LOADER = "neoforge";

    private DevLoader() { }

    /** The run directory: {@code run/<node>} for a server or a client, one per node. */
    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    /** The version of the mod this is loaded beside, as the loader reports it. */
    public static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    /** Whether another mod is loaded, which the benchmark asks about Create Fly. */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Runs once the server is accepting commands: a dedicated one, or a client's integrated one. */
    public static void onServerStarted(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> handler.accept(event.getServer()));
    }

    /**
     * Pours {@code amount} mB of water of {@code quality} into the block at {@code pos} through this
     * loader's block fluid capability, from above, committed: the way a pipe fills a tank. Returns how much
     * went in, or -1 when the block offers no such capability. Server thread only.
     */
    public static int fillFluid(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                                com.thirstwastaken2.purity.WaterQuality quality, int amount) {
        net.neoforged.neoforge.fluids.FluidStack water = com.thirstwastaken2.neoforge.WaterFluids.stamp(
                new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount), quality);
        //? if >=1.21.2 {
        var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos,
                net.minecraft.core.Direction.UP);
        if (handler == null) return -1;
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int filled = handler.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(water), amount, transaction);
            transaction.commit();
            return filled;
        }
        //?} else {
        /*var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos,
                net.minecraft.core.Direction.UP);
        if (handler == null) return -1;
        return handler.fill(water, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        *///?}
    }

    /** Runs as the server shuts down, while the world is still there. */
    public static void onServerStopping(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> handler.accept(event.getServer()));
    }
}
