package com.thirstwastaken2.fabric;

import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** The Fabric {@code main} entrypoint. Everything it starts is loader independent. */
public final class ThirstWasTaken2Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ThirstWasTaken2.initialize();
        WaterContainerStorage.register();
        DistillerStorage.register();
        // The Sand Filter. Only the builds that compile it declare this entrypoint, so on every other
        // Minecraft version the list is empty. See src/main/createfly/AGENTS.md.
        FabricLoader.getInstance().getEntrypoints("thirstwastaken2:createfly", ModInitializer.class)
                .forEach(ModInitializer::onInitialize);
        // The integrations both loaders compile, which may not name ModInitializer; see IntegrationEntrypoint.
        FabricLoader.getInstance().getEntrypoints("thirstwastaken2:integration", Runnable.class)
                .forEach(Runnable::run);
    }
}
