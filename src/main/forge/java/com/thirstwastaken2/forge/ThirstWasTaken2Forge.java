package com.thirstwastaken2.forge;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.client.forge.ThirstWasTaken2ForgeClient;
import com.thirstwastaken2.platform.IntegrationEntrypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.common.Mod;
import org.objectweb.asm.Type;

/**
 * The Forge mod class. Everything it starts is loader independent, apart from the fluid capability
 * that lets other mods fill and empty the water containers.
 *
 * <p>Forge 47 has one mod class per mod, not one per side as NeoForge does, so the client half is
 * started from here, through a static call that a dedicated server never makes and so never loads.
 */
@Mod(ThirstWasTaken2.MOD_ID)
public final class ThirstWasTaken2Forge {
    public ThirstWasTaken2Forge() {
        ThirstWasTaken2.initialize();
        WaterContainerCapabilities.register();
        DistillerFluidHandler.register();
        if (FMLEnvironment.dist == Dist.CLIENT) ThirstWasTaken2ForgeClient.initialize();
        runIntegrationEntrypoints();
    }

    /**
     * The integrations both loaders compile, which may not carry {@code @Mod}; see the NeoForge mod class.
     * Only the nodes that compile an integration have its class in the scan data.
     */
    private static void runIntegrationEntrypoints() {
        Type annotation = Type.getType(IntegrationEntrypoint.class);
        ModList.get().getModFileById(ThirstWasTaken2.MOD_ID).getFile().getScanResult().getAnnotations().stream()
                .filter(data -> annotation.equals(data.annotationType()))
                .map(data -> data.clazz().getClassName())
                .sorted()
                .forEach(ThirstWasTaken2Forge::run);
    }

    private static void run(String className) {
        try {
            Class<?> type = Class.forName(className, true, ThirstWasTaken2Forge.class.getClassLoader());
            ((Runnable) type.getDeclaredConstructor().newInstance()).run();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not run the integration entrypoint " + className, e);
        }
    }
}
