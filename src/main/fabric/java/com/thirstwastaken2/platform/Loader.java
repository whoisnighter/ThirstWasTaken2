package com.thirstwastaken2.platform;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.LootPool;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Every call into the mod loader, for Fabric.
 *
 * <p>Each loader has its own copy of this class, with the same name and the same signatures, in its
 * own source directory; the build compiles exactly one of them. Common code calls it without knowing
 * which one. See {@code platform/AGENTS.md}.
 */
public final class Loader {
    private Loader() { }

    /** True under every development run task, false in the jar players install. */
    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /** The directory config files are read from and written to. */
    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /**
     * The water a fluid of this loader's fluid API carries: {@code defaultQuality} when it records no
     * grade, {@code null} for anything but water, or for an object that is not such a fluid. Here a
     * {@code FluidVariant}. For integration mixins on another mod's fluid wrapper, which hold it as
     * {@code Object} so that a class both loaders compile names neither.
     */
    public static com.thirstwastaken2.purity.WaterQuality waterIn(Object fluid) {
        return fluid instanceof net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant
                ? com.thirstwastaken2.fabric.WaterContainerStorage.quality(variant) : null;
    }

    /** The name a mod gives itself, or {@code modId} when no mod has that id. */
    public static String modName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(mod -> mod.getMetadata().getName()).orElse(modId);
    }

    /**
     * Runs {@code registration} when {@code registry} accepts new entries.
     *
     * <p>Fabric leaves the built-in registries open while mods initialize, so this runs it straight
     * away and {@code registry} only matters to loaders that freeze them first.
     */
    public static void onRegister(ResourceKey<? extends Registry<?>> registry, Runnable registration) {
        registration.run();
    }

    /**
     * How long {@code fuel} burns in a furnace, in ticks; 0 for anything that is not fuel. Fabric's
     * fuel registry feeds vanilla's own table, so vanilla's answer counts modded fuel too.
     */
    public static <T extends BlockEntity & Container> int burnTime(T entity, ItemStack fuel) {
        return Vanilla.burnTime(entity, fuel);
    }

    /** Whether {@code fuel} burns in a furnace, asked without one, as a slot on the client asks. */
    public static boolean isFuel(Level level, ItemStack fuel) {
        return Vanilla.isFuel(level, fuel);
    }

    /**
     * Registers the {@code thirstwastaken2:item_enabled} and {@code thirstwastaken2:config_enabled} load
     * conditions the mod's recipes carry, so a recipe for an item or a feature the config switches off is
     * skipped as it loads. See {@link ItemEnabledCondition} and {@link ConfigEnabledCondition}.
     */
    public static void registerResourceConditions() {
        ItemEnabledCondition.register();
        ConfigEnabledCondition.register();
    }

    /**
     * Registers a per-player value, saved with {@code codec} and synced to its owner in the form
     * {@code write} and {@code read} agree on.
     */
    public static <T> PlayerData<T> playerData(Identifier id, Supplier<T> initial, Codec<T> codec,
                                               BiConsumer<T, FriendlyByteBuf> write,
                                               Function<FriendlyByteBuf, T> read) {
        return FabricNetworking.playerData(id, initial, codec, write, read);
    }

    /**
     * Builder for a creative tab that finds its own place in the tab list. Fabric API renamed this
     * entrypoint from {@code FabricItemGroup} to {@code FabricCreativeModeTab} in 26.1.
     */
    public static CreativeModeTab.Builder creativeTabBuilder() {
        //? if >=26.1 {
        return net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder();
        //?} else {
        /*return net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder();
        *///?}
    }

    /** Runs at the end of every server tick. */
    public static void onServerTickEnd(Consumer<MinecraftServer> handler) {
        ServerTickEvents.END_SERVER_TICK.register(handler::accept);
    }

    public static void onUseBlock(UseBlockHandler handler) {
        UseBlockCallback.EVENT.register(handler::use);
    }

    public static void onUseItem(UseItemHandler handler) {
        //? if >=1.21.2 {
        UseItemCallback.EVENT.register(handler::use);
        //?} else {
        /*// Before 1.21.2 the callback also returns the stack left in the hand, which is whatever the
        // handler put there.
        UseItemCallback.EVENT.register((player, level, hand) -> new net.minecraft.world.InteractionResultHolder<>(
                handler.use(player, level, hand), player.getItemInHand(hand)));
        *///?}
    }

    /**
     * Runs after tags are bound to their registries: on the server at startup and on {@code /reload},
     * and on a client each time it joins a server and receives that server's tags.
     */
    public static void onTagsLoaded(Runnable handler) {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> handler.run());
    }

    /** Runs whenever the server builds its command tree, including on {@code /reload}. */
    public static void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> handler) {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> handler.accept(dispatcher));
    }

    /**
     * Runs for every loot table as it loads, handing over the table's id and a way to append a pool,
     * whoever wrote the table: vanilla, a mod, or a data pack that replaced it.
     *
     * <p>Fabric API does say where a table came from, but not which pack, so vanilla's own experiment
     * packs cannot be told apart from a player's data pack, and its answer for them changed between
     * versions. Taking every table is the one rule that holds on every version and every loader.
     */
    public static void onLootTable(BiConsumer<Identifier, Consumer<LootPool.Builder>> handler) {
        //? if >=1.20.5 {
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) ->
                handler.accept(key.identifier(), table::withPool));
        //?} else {
        /*net.fabricmc.fabric.api.loot.v2.LootTableEvents.MODIFY.register((resources, loot, id, table, source) ->
                handler.accept(id, table::withPool));
        *///?}
    }

    /**
     * Adds a trade to the pool {@code profession} draws from at {@code level}, 1 (novice) to 5 (master).
     * {@code offer} is asked each time a villager draws the trade and may return {@code null} to offer
     * nothing.
     *
     * <p>From 26.1 villager trades are data, which datagen writes, and Fabric API has no trade helper,
     * so there this does nothing.
     */
    public static void addVillagerTrade(Identifier profession, int level,
                                        Supplier<net.minecraft.world.item.trading.MerchantOffer> offer) {
        // The adder is called for the trade rebalance experiment's pools as well as vanilla's, so the
        // trade is offered whether the experiment is on or not.
        //? if >=1.21.5 <26.1 {
        /*net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerVillagerOffers(
                ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION, profession), level,
                (listings, rebalanced) -> listings.add((world, entity, random) -> offer.get()));
        *///?} elif >=1.21 <1.21.5 {
        /*net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerVillagerOffers(
                net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.get(profession), level,
                (listings, rebalanced) -> listings.add((entity, random) -> offer.get()));
        *///?} elif <1.21 {
        /*net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerVillagerOffers(
                net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.get(profession), level,
                listings -> listings.add((entity, random) -> offer.get()));
        *///?}
    }

    /**
     * Runs {@code handler} on every server data load, at startup and on {@code /reload}, with the
     * resource manager of the packs being loaded. It runs on the server thread, after vanilla's own
     * listeners have been handed the same packs; tags are bound only after it.
     */
    public static void onServerDataReload(Identifier id, Consumer<ResourceManager> handler) {
        ResourceManagerReloadListener listener = handler::accept;
        //? if >=26.1 {
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener);
        //?} elif >=1.21.11 {
        /*net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.SERVER_DATA).registerReloader(id, listener);
        *///?} else {
        /*// The v1 resource loader arrived with 1.21.9; before it a listener names itself.
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return id;
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        listener.onResourceManagerReload(manager);
                    }
                });
        *///?}
    }

    /**
     * Runs for each player the server sends its data pack contents to: one player as they join, and
     * every player after {@code /reload}. It is the point to send what a data pack decided.
     */
    public static void onDataPackSync(Consumer<ServerPlayer> handler) {
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> handler.accept(player));
    }

    /**
     * Declares a payload the server sends to clients, and what a client does with one. The handler
     * runs on the client's main thread. Call it during {@code initialize}, on both sides.
     *
     * <p>Fabric API keeps the client receiver in its client module, which common code cannot see, so the
     * handler waits in {@link FabricNetworking} until the client entrypoint registers it.
     */
    public static <T> Clientbound<T> clientboundPayload(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                                        Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        return FabricNetworking.clientboundPayload(id, write, read, handler);
    }
}
