package com.thirstwastaken2;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.block.DistillerInteractions;
import com.thirstwastaken2.block.DistillerSalt;
import com.thirstwastaken2.block.HangingPotInteractions;
import com.thirstwastaken2.block.ThirstBlockEntities;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.block.ThirstMenus;
import com.thirstwastaken2.command.ThirstCommands;
import com.thirstwastaken2.compat.LootIntegration;
import com.thirstwastaken2.compat.TradeIntegration;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.DataPackDrinks;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.purity.SourceDrawing;
import com.thirstwastaken2.purity.WaterInteractions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThirstWasTaken2 {
    public static final String MOD_ID = "thirstwastaken2";
    /** The Minecraft version this jar was built against; substituted per version at build time. */
    public static final String MINECRAFT = /*$ minecraft*/"26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /** System property that forces {@link #DEV} on or off. */
    public static final String DEV_PROPERTY = "thirstwastaken2.dev";
    /**
     * Whether this is a development run rather than a published jar: true under every Loom run task
     * ({@code runServer}, {@code runClient}, {@code runGametest}, {@code runBenchmark}), false in the jar
     * players install. {@code -Dthirstwastaken2.dev=true|false} overrides the detection either way.
     *
     * <p>Dev-only tooling such as {@code /thirst benchmark} registers nothing unless this is set. It also
     * lives in the separate {@code dev} source set, so it cannot reach a published jar even when the flag
     * is forced on.
     */
    public static final boolean DEV = detectDev();

    private ThirstWasTaken2() { }

    /** Called once by the loader's entrypoint. Everything the mod registers and hooks starts here. */
    public static void initialize() {
        ThirstConfig.load();
        ThirstData.register();
        // Blocks and items take a registry holder when they are built, so a loader that freezes the
        // registries before mods start needs all of these deferred to its registration phase.
        Loader.onRegister(Registries.BLOCK, ThirstBlocks::register);
        Loader.onRegister(Registries.BLOCK_ENTITY_TYPE, ThirstBlockEntities::register);
        ItemWaterData.register();
        Loader.onRegister(Registries.ITEM, ThirstItems::register);
        Loader.onRegister(Registries.CREATIVE_MODE_TAB, ThirstItems::registerCreativeTab);
        Loader.onRegister(Registries.MOB_EFFECT, ThirstEffects::register);
        Loader.onRegister(Registries.MENU, ThirstMenus::register);
        LootIntegration.register();
        TradeIntegration.register();
        Loader.registerResourceConditions();

        Loader.onServerTickEnd(ThirstManager::tick);
        Loader.onServerTickEnd(WaterInteractions::tick);
        Loader.onServerTickEnd(SourceDrawing::tick);
        Loader.onUseBlock(ThirstManager::drinkByHand);
        Loader.onUseBlock(ThirstManager::drinkFromCauldron);
        Loader.onUseBlock(HangingPotInteractions::use);
        Loader.onUseBlock(DistillerInteractions::fillTub);
        Loader.onUseBlock(DistillerInteractions::useMachine);
        Loader.onUseBlock(WaterInteractions::emptyWaterskinOnBlock);
        Loader.onUseBlock(WaterInteractions::fillWaterskinFromCauldron);
        Loader.onUseBlock(WaterInteractions::transferCauldronPurity);
        Loader.onUseItem(WaterInteractions::fillFromWater);
        Loader.onRegisterCommands(ThirstCommands::register);
        Loader.onTagsLoaded(ThirstApi::clearCache);
        Loader.onTagsLoaded(DistillerSalt::clearCache);
        // Data pack thirst values: parsed by the server, handed to each client on join and after /reload.
        Loader.onServerDataReload(DataPackDrinks.RELOAD_ID, DataPackDrinks::reload);
        DataPackDrinks.registerPayload();
        Loader.onDataPackSync(DataPackDrinks::sync);
        // Whether scooping water takes the source, which the client cannot work out for itself.
        SourceDrawing.registerPayload();
        Loader.onDataPackSync(SourceDrawing::sync);

        LOGGER.info("ThirstWasTaken2 initialized for Minecraft {}{}", MINECRAFT, DEV ? " (dev)" : "");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static boolean detectDev() {
        String forced = System.getProperty(DEV_PROPERTY);
        return forced != null ? Boolean.parseBoolean(forced) : Loader.isDevelopmentEnvironment();
    }
}
