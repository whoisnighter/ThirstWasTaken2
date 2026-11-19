package com.thirstwastaken2.platform;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.thirstwastaken2.ThirstWasTaken2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Every call into the mod loader, for MinecraftForge 47 (Minecraft 1.20.1).
 *
 * <p>Each loader has its own copy of this class, with the same name and the same signatures, in its
 * own source directory; the build compiles exactly one of them. Common code calls it without knowing
 * which one. See {@code platform/AGENTS.md}.
 *
 * <p>The same arrangement as the NeoForge copy: registration listens on the mod's own event bus,
 * everything else on {@link MinecraftForge#EVENT_BUS}. Player data and payloads, which Forge 47 has no
 * attachment or payload API for, are in {@link ForgeNetworking}.
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
        return !FMLEnvironment.production;
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
     * {@code FluidStack}. For integration mixins on another mod's fluid wrapper, which hold it as
     * {@code Object} so that a class both loaders compile names neither.
     */
    public static com.thirstwastaken2.purity.WaterQuality waterIn(Object fluid) {
        return fluid instanceof net.minecraftforge.fluids.FluidStack stack && com.thirstwastaken2.forge.WaterFluids.isWater(stack)
                ? com.thirstwastaken2.forge.WaterFluids.quality(stack) : null;
    }

    /** The name a mod gives itself, or {@code modId} when no mod has that id. */
    public static String modName(String modId) {
        return ModList.get().getModContainerById(modId).map(mod -> mod.getModInfo().getDisplayName()).orElse(modId);
    }

    /**
     * Runs {@code registration} when {@code registry} accepts new entries: Forge, like NeoForge, opens the
     * built-in registries only while it fires {@link RegisterEvent} for each, so it is queued until then.
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
     * How long {@code fuel} burns in a furnace, in ticks; 0 for anything that is not fuel. Forge lets an
     * item say its own burn time, which vanilla's table never sees, so Forge's hook is asked.
     */
    public static <T extends BlockEntity & Container> int burnTime(T entity, ItemStack fuel) {
        return net.minecraftforge.common.ForgeHooks.getBurnTime(fuel, net.minecraft.world.item.crafting.RecipeType.SMELTING);
    }

    /** Whether {@code fuel} burns in a furnace, asked without one, as a slot on the client asks. */
    public static boolean isFuel(Level level, ItemStack fuel) {
        return net.minecraftforge.common.ForgeHooks.getBurnTime(fuel, net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0;
    }

    /**
     * Registers the {@code thirstwastaken2:item_enabled} and {@code thirstwastaken2:config_enabled} load
     * conditions the mod's recipes and advancements carry. Forge keeps its condition serializers in a map
     * of its own rather than a registry.
     */
    public static void registerResourceConditions() {
        CraftingHelper.register(ItemEnabledCondition.SERIALIZER);
        CraftingHelper.register(ConfigEnabledCondition.SERIALIZER);
    }

    /**
     * Registers a per-player value, saved with {@code codec} and synced to its owner in the form
     * {@code write} and {@code read} agree on.
     */
    public static <T> PlayerData<T> playerData(Identifier id, Supplier<T> initial, Codec<T> codec,
                                               BiConsumer<T, FriendlyByteBuf> write,
                                               Function<FriendlyByteBuf, T> read) {
        return ForgeNetworking.playerData(id, initial, codec, write, read);
    }

    /** Builder for a creative tab that finds its own place in the tab list. */
    public static CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    /** Runs at the end of every server tick. */
    public static void onServerTickEnd(Consumer<MinecraftServer> handler) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) handler.accept(event.getServer());
        });
    }

    /** See the NeoForge copy: cancelling with the handler's result both ends the chain and becomes the result. */
    public static void onUseBlock(UseBlockHandler handler) {
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            InteractionResult result = handler.use(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    /** The same contract as {@link #onUseBlock}. */
    public static void onUseItem(UseItemHandler handler) {
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
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
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> handler.run());
    }

    /** Runs whenever the server builds its command tree, including on {@code /reload}. */
    public static void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> handler) {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> handler.accept(event.getDispatcher()));
    }

    /**
     * Runs for every loot table as it loads, handing over the table's id and a way to append a pool,
     * whoever wrote the table: vanilla, a mod, or a data pack that replaced it.
     */
    public static void onLootTable(BiConsumer<Identifier, Consumer<LootPool.Builder>> handler) {
        MinecraftForge.EVENT_BUS.addListener((LootTableLoadEvent event) ->
                handler.accept(event.getName(), pool -> event.getTable().addPool(pool.build())));
    }

    /**
     * Adds a trade to the pool {@code profession} draws from at {@code level}, 1 (novice) to 5 (master).
     * {@code offer} is asked each time a villager draws the trade and may return {@code null} to offer
     * nothing.
     */
    public static void addVillagerTrade(Identifier profession, int level,
                                        Supplier<net.minecraft.world.item.trading.MerchantOffer> offer) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.village.VillagerTradesEvent event) -> {
            if (profession.equals(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType()))) {
                event.getTrades().get(level).add((entity, random) -> offer.get());
            }
        });
    }

    /**
     * Runs {@code handler} on every server data load, at startup and on {@code /reload}, with the
     * resource manager of the packs being loaded. Forge 47 takes listeners without a name.
     */
    public static void onServerDataReload(Identifier id, Consumer<ResourceManager> handler) {
        ResourceManagerReloadListener listener = handler::accept;
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(listener));
    }

    /**
     * Runs for each player the server sends its data pack contents to: one player as they join, and
     * every player after {@code /reload}, where Forge 47 names no player and means all of them.
     */
    public static void onDataPackSync(Consumer<ServerPlayer> handler) {
        MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() != null) handler.accept(event.getPlayer());
            else event.getPlayerList().getPlayers().forEach(handler);
        });
    }

    /**
     * Declares a payload the server sends to clients, and what a client does with one. The handler
     * runs on the client's main thread. Call it during {@code initialize}, on both sides.
     *
     * <p>A channel of its own that accepts a missing mod on the other side, so a client without the mod
     * may still join; {@link Clientbound#send} skips it.
     */
    public static <T> Clientbound<T> clientboundPayload(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                                        Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        return ForgeNetworking.clientboundPayload(id, write, read, handler);
    }

    /** The mod's own event bus. The container exists before the mod is constructed, as on NeoForge. */
    static IEventBus modBus() {
        return ((FMLModContainer) ModList.get().getModContainerById(ThirstWasTaken2.MOD_ID).orElseThrow()).getEventBus();
    }

    private static void register(RegisterEvent event) {
        REGISTERED.add(event.getRegistryKey());
        List<Runnable> registrations = PENDING.remove(event.getRegistryKey());
        if (registrations != null) registrations.forEach(Runnable::run);
    }
}
