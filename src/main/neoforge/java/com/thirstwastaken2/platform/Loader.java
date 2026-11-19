package com.thirstwastaken2.platform;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.neoforge.ConfigEnabledCondition;
import com.thirstwastaken2.neoforge.ItemEnabledCondition;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Every call into the mod loader, for NeoForge.
 *
 * <p>Each loader has its own copy of this class, with the same name and the same signatures, in its
 * own source directory; the build compiles exactly one of them. Common code calls it without knowing
 * which one. See {@code platform/AGENTS.md}.
 *
 * <p>Registration listens on the mod's own event bus, everything else on {@link NeoForge#EVENT_BUS}.
 * The mod bus is looked up from {@link ModList} rather than handed over by the entrypoint: the
 * container exists before the mod is constructed, and a lookup keeps the entrypoint to one call, the
 * same as on Fabric.
 */
public final class Loader {
    /** Registrations waiting for their registry's {@link RegisterEvent}, by registry. */
    private static final Map<ResourceKey<? extends Registry<?>>, List<Runnable>> PENDING = new HashMap<>();
    /** Registries whose event has already fired; a registration for one of them would never run. */
    private static final Set<ResourceKey<? extends Registry<?>>> REGISTERED = new HashSet<>();
    private static boolean listening;

    private Loader() { }

    /** True under every development run task, false in the jar players install. */
    public static boolean isDevelopmentEnvironment() {
        // FML turned its environment fields into methods in NeoForge 21.9.
        //? if >=1.21.9 {
        return !FMLEnvironment.isProduction();
        //?} else {
        /*return !FMLEnvironment.production;
        *///?}
    }

    /** The directory config files are read from and written to. */
    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * The water a fluid of this loader's fluid API carries: {@code defaultQuality} when it records no
     * grade, {@code null} for anything but water, or for an object that is not such a fluid. Here a
     * {@code FluidStack}, or from 1.21.2 the transfer API's {@code FluidResource}. For integration mixins
     * on another mod's fluid wrapper, which hold it as {@code Object} so that a class both loaders
     * compile names neither.
     */
    public static com.thirstwastaken2.purity.WaterQuality waterIn(Object fluid) {
        if (fluid instanceof net.neoforged.neoforge.fluids.FluidStack stack) {
            return com.thirstwastaken2.neoforge.WaterFluids.isWater(stack) ? com.thirstwastaken2.neoforge.WaterFluids.quality(stack) : null;
        }
        //? if >=1.21.2 {
        if (fluid instanceof net.neoforged.neoforge.transfer.fluid.FluidResource resource) {
            return com.thirstwastaken2.neoforge.WaterFluidResources.isWater(resource)
                    ? com.thirstwastaken2.neoforge.WaterFluidResources.quality(resource) : null;
        }
        //?}
        return null;
    }

    /** The name a mod gives itself, or {@code modId} when no mod has that id. */
    public static String modName(String modId) {
        return ModList.get().getModContainerById(modId).map(mod -> mod.getModInfo().getDisplayName()).orElse(modId);
    }

    /**
     * Runs {@code registration} when {@code registry} accepts new entries.
     *
     * <p>NeoForge freezes the built-in registries before mods are constructed and opens them again
     * while it fires {@link RegisterEvent}, so the registration is queued and run from that event for
     * {@code registry}. NeoForge fires data component types ahead of items on purpose, which is the
     * order the mod needs.
     */
    public static void onRegister(ResourceKey<? extends Registry<?>> registry, Runnable registration) {
        if (REGISTERED.contains(registry)) {
            throw new IllegalStateException("Registration for " + registry.identifier() + " arrived after its event");
        }
        if (!listening) {
            modBus().addListener(Loader::register);
            listening = true;
        }
        PENDING.computeIfAbsent(registry, key -> new ArrayList<>()).add(registration);
    }

    /**
     * How long {@code fuel} burns in a furnace, in ticks; 0 for anything that is not fuel. NeoForge
     * 1.21.1 keeps fuel in a data map that only its own stack method reads; later NeoForge feeds that
     * data map into vanilla's fuel values, so vanilla's answer counts modded fuel there.
     */
    public static <T extends BlockEntity & Container> int burnTime(T entity, ItemStack fuel) {
        //? if >1.21.1 {
        return Vanilla.burnTime(entity, fuel);
        //?} else
        //return fuel.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
    }

    /** Whether {@code fuel} burns in a furnace, asked without one, as a slot on the client asks. */
    public static boolean isFuel(Level level, ItemStack fuel) {
        //? if >1.21.1 {
        return Vanilla.isFuel(level, fuel);
        //?} else
        //return fuel.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0;
    }

    /**
     * Registers the {@code thirstwastaken2:item_enabled} and {@code thirstwastaken2:config_enabled} load
     * conditions the mod's recipes carry, so a recipe for an item or a feature the config switches off is
     * skipped as it loads. See {@link ItemEnabledCondition} and {@link ConfigEnabledCondition}.
     */
    public static void registerResourceConditions() {
        onRegister(NeoForgeRegistries.Keys.CONDITION_CODECS, () -> {
            Registry.register(NeoForgeRegistries.CONDITION_SERIALIZERS, ThirstWasTaken2.id("item_enabled"),
                    ItemEnabledCondition.CODEC);
            Registry.register(NeoForgeRegistries.CONDITION_SERIALIZERS, ThirstWasTaken2.id("config_enabled"),
                    ConfigEnabledCondition.CODEC);
        });
    }

    /**
     * Registers a per-player value, saved with {@code codec} and synced to its owner in the form
     * {@code write} and {@code read} agree on.
     */
    public static <T> PlayerData<T> playerData(Identifier id, Supplier<T> initial, Codec<T> codec,
                                               BiConsumer<T, FriendlyByteBuf> write,
                                               Function<FriendlyByteBuf, T> read) {
        StreamCodec<FriendlyByteBuf, T> streamCodec = streamCodec(write, read);
        // An attachment type takes no registry holder, so it can be built now and registered later.
        // NeoForge 21.1 saves an attachment through a plain codec; later versions take a map codec, so
        // the value goes under a field there. A world carried from one to the other starts at full thirst.
        //? if >1.21.1 {
        AttachmentType<T> type = AttachmentType.builder(initial)
                .serialize(codec.fieldOf("value"))
                .sync(Loader::syncsTo, streamCodec)
                .build();
        //?} else {
        /*AttachmentType<T> type = AttachmentType.builder(initial)
                .serialize(codec)
                .sync(Loader::syncsTo, streamCodec)
                .build();
        *///?}
        onRegister(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
                () -> Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, id, type));
        return new AttachmentPlayerData<>(type);
    }

    /**
     * Whether a player's value is synced to {@code to}: only to the player it belongs to. Before 26.1
     * NeoForge sends the packet to whoever this accepts and throws when that connection never negotiated
     * the channel, which a gametest mock player and a vanilla client have not, so it asks first there.
     *
     * <p>Asking is itself unsafe for a fake player, the kind {@code FakePlayerFactory} hands out and the
     * benchmark's simulated players are: NeoForge reads the negotiated channels off the connection's
     * Netty channel, and a fake player's connection has none, so {@code hasChannel} throws instead of
     * answering false. From 26.1 on NeoForge answers false there itself. Nothing is synced to a player
     * with no client either way, and a fake player reaches this whenever one drinks, through
     * {@code ItemStackMixin}, so it is turned away before the channel is asked about.
     */
    private static boolean syncsTo(Object holder, Object to) {
        //? if >=26.1 {
        return holder == to;
        //?} else {
        /*if (holder != to) return false;
        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) to;
        return !player.isFakePlayer()
                && player.connection.hasChannel(net.neoforged.neoforge.network.payload.SyncAttachmentsPayload.TYPE);
        *///?}
    }

    /** Builder for a creative tab that finds its own place in the tab list. */
    public static CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    /** Runs at the end of every server tick. */
    public static void onServerTickEnd(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> handler.accept(event.getServer()));
    }

    /**
     * On Fabric a non-{@code PASS} result stops the callback chain and becomes the interaction's result.
     * Cancelling with that result does both here: listeners added this way skip a cancelled event, and
     * the game returns the cancellation result instead of using the block.
     */
    public static void onUseBlock(UseBlockHandler handler) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            InteractionResult result = handler.use(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    /** The same contract as {@link #onUseBlock}. */
    public static void onUseItem(UseItemHandler handler) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
            InteractionResult result = handler.use(event.getEntity(), event.getLevel(), event.getHand());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    /**
     * Runs after tags are bound to their registries: on the server at startup and on {@code /reload},
     * and on a client each time it joins a server and receives that server's tags.
     */
    public static void onTagsLoaded(Runnable handler) {
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> handler.run());
    }

    /** Runs whenever the server builds its command tree, including on {@code /reload}. */
    public static void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> handler) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> handler.accept(event.getDispatcher()));
    }

    /**
     * Runs for every loot table as it loads, handing over the table's id and a way to append a pool,
     * whoever wrote the table: vanilla, a mod, or a data pack that replaced it.
     */
    public static void onLootTable(BiConsumer<Identifier, Consumer<LootPool.Builder>> handler) {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) ->
                handler.accept(event.getKey().identifier(), pool -> event.getTable().addPool(pool.build())));
    }

    /**
     * Adds a trade to the pool {@code profession} draws from at {@code level}, 1 (novice) to 5 (master).
     * {@code offer} is asked each time a villager draws the trade and may return {@code null} to offer
     * nothing.
     *
     * <p>From 26.1 villager trades are data, which datagen writes, and NeoForge has no trade event, so
     * there this does nothing.
     */
    public static void addVillagerTrade(Identifier profession, int level,
                                        Supplier<net.minecraft.world.item.trading.MerchantOffer> offer) {
        //? if >=1.21.5 <26.1 {
        /*NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.village.VillagerTradesEvent event) -> {
            if (event.getType().identifier().equals(profession)) {
                event.getTrades().get(level).add((world, entity, random) -> offer.get());
            }
        });
        *///?} elif <1.21.5 {
        /*NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.village.VillagerTradesEvent event) -> {
            if (profession.equals(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType()))) {
                event.getTrades().get(level).add((entity, random) -> offer.get());
            }
        });
        *///?}
    }

    /**
     * Runs {@code handler} on every server data load, at startup and on {@code /reload}, with the
     * resource manager of the packs being loaded. It runs on the server thread, after vanilla's own
     * listeners have been handed the same packs; tags are bound only after it.
     */
    public static void onServerDataReload(Identifier id, Consumer<ResourceManager> handler) {
        ResourceManagerReloadListener listener = handler::accept;
        //? if >=1.21.11 {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddServerReloadListenersEvent event) ->
                event.addListener(id, listener));
        //?} else {
        /*// NeoForge 21.1 takes listeners without a name; the id only matters to later versions' ordering.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent event) ->
                event.addListener(listener));
        *///?}
    }

    /**
     * Runs for each player the server sends its data pack contents to: one player as they join, and
     * every player after {@code /reload}. It is the point to send what a data pack decided.
     */
    public static void onDataPackSync(Consumer<ServerPlayer> handler) {
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> event.getRelevantPlayers().forEach(handler));
    }

    /**
     * Declares a payload the server sends to clients, and what a client does with one. The handler
     * runs on the client's main thread. Call it during {@code initialize}, on both sides.
     *
     * <p>Registered as optional, so a client without the mod may still join; {@link #send} skips it.
     * The handler is common code, so it is safe to name on a dedicated server, where it never runs.
     */
    public static <T> Clientbound<T> clientboundPayload(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                                        Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        CustomPacketPayload.Type<Wrapped<T>> type = new CustomPacketPayload.Type<>(id);
        StreamCodec<FriendlyByteBuf, Wrapped<T>> codec = wrappedCodec(type, write, read);
        modBus().addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional()
                .playToClient(type, codec, (payload, context) -> handler.accept(payload.value())));
        // A fake player is turned away first, for the reason syncsTo gives.
        return (player, value) -> {
            if (!player.isFakePlayer() && player.connection.hasChannel(type)) {
                PacketDistributor.sendToPlayer(player, new Wrapped<>(type, value));
            }
        };
    }

    /** A common-code payload inside the loader's own packet type. */
    private record Wrapped<T>(CustomPacketPayload.Type<Wrapped<T>> type, T value) implements CustomPacketPayload { }

    private static <T> StreamCodec<FriendlyByteBuf, T> streamCodec(BiConsumer<T, FriendlyByteBuf> write,
                                                                   Function<FriendlyByteBuf, T> read) {
        return StreamCodec.ofMember(write::accept, read::apply);
    }

    private static <T> StreamCodec<FriendlyByteBuf, Wrapped<T>> wrappedCodec(
            CustomPacketPayload.Type<Wrapped<T>> type, BiConsumer<T, FriendlyByteBuf> write,
            Function<FriendlyByteBuf, T> read) {
        return StreamCodec.ofMember((payload, buffer) -> write.accept(payload.value(), buffer),
                buffer -> new Wrapped<>(type, read.apply(buffer)));
    }

    private static IEventBus modBus() {
        return ModList.get().getModContainerById(ThirstWasTaken2.MOD_ID).orElseThrow().getEventBus();
    }

    private static void register(RegisterEvent event) {
        REGISTERED.add(event.getRegistryKey());
        List<Runnable> registrations = PENDING.remove(event.getRegistryKey());
        if (registrations != null) registrations.forEach(Runnable::run);
    }

    private record AttachmentPlayerData<T>(AttachmentType<T> type) implements PlayerData<T> {
        @Override
        public T get(Player player) {
            return player.getData(type);
        }

        @Override
        public void set(Player player, T value) {
            player.setData(type, value);
        }
    }
}
