# platform/

The two places where the mod is allowed to know what it is running on: `Vanilla` for the Minecraft
version, `Loader` for the mod loader. Everything outside this package and its client twin,
`com.thirstwastaken2.client.platform`, compiles unchanged on every node.

```
core                          no version knowledge, no loader knowledge
Vanilla, ClientVanilla        version axis    Stonecutter comments, in src/main/java and src/client/java
Loader, ClientLoader          loader axis     one copy per loader, in src/main/<loader> and src/client/<loader>
```

**The two axes never meet in one file.** A file is either version conditional and lives in
`src/main/java`, or loader specific and lives in `src/main/fabric`. A loader file may contain version
conditionals (`Loader.creativeTabBuilder` does). A `src/main/java` file may never name a loader.

## Vanilla

The mod is built for several Minecraft versions from one source tree (see the root `AGENTS.md`), so
a handful of vanilla calls have to be written twice. `Vanilla` collects them behind signatures that
are identical on every version.

- **Only vanilla-facing plumbing.** No thirst logic, no config reads, no caching. If a method here
  needs to know what the mod is doing, it is in the wrong package.
- **Same signature on every version.** A caller must never need to know which branch is live.
- **Add to `Vanilla` rather than to the caller.** A `//?` block outside a `platform/` package and
  `mixin/` is a signal the seam is missing, and `checkVersionSeam` fails the build on it in every
  hand-written root: `src/main/java`, `src/client/java` and every `src/main/<name>/java` and
  `src/client/<name>/java`, the loader, fluid API and integration directories included. That check is
  the exit ramp; the number of blocks in here is not.
- **An integration has a `platform/` of its own** for what only it sees: a difference in the other
  mod's API, or in the loader API it alone calls, such as `com.thirstwastaken2.sophisticated.platform`.
  A vanilla difference still goes here, in `Vanilla`, where every integration can use it
  (`getString`, `getInt` and `isDrinkAnimation` came from Sophisticated that way). An integration's
  `platform/` classes are loaded only after its gate, like the rest of it.
- Mixins are the documented exception: their `@Inject` signatures track the target method and cannot
  be abstracted away. Keep their bodies one line regardless.

### What 1.21.1 costs

1.21.1 is the one old version, and most of the conditionals here exist for it. What it lacks, and
what stands in, for this package. The full list for every version, including datagen, mixins and
tests, is [docs/dev/VERSION-DIFFERENCES.md](../../../../../../docs/dev/VERSION-DIFFERENCES.md); a new
seam gets a row there too.

| Missing on 1.21.1 | Seam | What 1.21.1 does instead |
|---|---|---|
| items and blocks knowing their id before construction | `registerItem`, `registerBlock`, `registerBlockItem` | registers the properties as they are |
| `updateShape` in its later parameter order | `SupportedBlock` | the older override, with ticks scheduled on the level |
| the consumable component | `DrinkItem` | overrides use, animation, duration and finishing itself |
| custom model data as float lists | `modelSelector(index, value)` | one integer; no item reads more than one index |
| the `item_model` component | `itemModelOf` | `null`; only a gametest asks, to check sea water leaves vanilla's model alone |
| styles without a shadow | `dropletFont` | the tooltip droplets are drawn with a shadow |
| a block's id inside its constructor | `isWaterCauldron` | `BlocksMixin` marks the water cauldron's construction |
| `hurtServer`, `level()` as `ServerLevel`, permission sets, environment attributes | `hurt`, `level`, `isGameMaster`, `isOwner`, `waterEvaporates` | the older call, same meaning |
| `Registry#get(id)` returning a holder | `mobEffect` | `getHolder(id)`, same meaning |
| `CompoundTag#getStringOr`, `getIntOr` (1.21.5) | `getString`, `getInt` | the older getter, when the tag holds that type |
| `ItemUseAnimation` (1.21.2) | `isDrinkAnimation` | `UseAnim`, same meaning |

Pure renames (`ResourceLocation`, `CONSUME`, `moveTo`, `CONFUSION` and the rest) are replacements in
`stonecutter.gradle.kts`, not branches. A threshold written `>1.21.1` rather than a release number
means the exact release a call changed in was not pinned down; with no node between 1.21.1 and
1.21.11 it makes no difference to any jar.

`DrinkItem` and `SupportedBlock` are classes rather than methods because what differs is an override.
`SupportedBlock` hands a block one `supportChanged` call when the block below it changes, and breaks the
block first when `canSurvive` no longer holds.

`SavedPositions` is one too: one small int per block position kept with a level, for a block with no
block entity of its own (Kaleidoscope Tavern's placed water bottle). Saved data's save is an override
before 1.21.5 and a codec from it, and its id an `Identifier` from 26.1.

`SavedBlockEntity` is a class for the same reason: a block entity's save and load are overrides, whose
shape changed in 1.20.5 and again in 1.21.5. It hands the mod's block entities one `save` and `load`
over a small `Output` and `Input` of named values and one list of items, and sends what it saves to the
client when it `syncsToClient`. Block entity and menu types are built and registered by
`Vanilla.registerBlockEntity` and `registerMenu`, from their registries' `Loader.onRegister`; their
constructors are private in vanilla, opened on Fabric by the mod's access widener and made public by
NeoForge's and Forge's access transformers (see VERSION-DIFFERENCES.md, "Differences in Fabric API").

`ItemWaterData` is the one class that knows how an item stack stores its water: servings, grade and salt,
as the `ThirstComponents` data components. `WaterPurity` and `WaterskinItem` decide what the values mean;
this only reads and writes them, and makes item properties and loot functions that preset them. A
version without data components changes this class and nothing else. The vanilla components core reads
(`POTION_CONTENTS`, `CUSTOM_MODEL_DATA`, `ITEM_MODEL`, `BLOCK_ENTITY_DATA`, `CONSUMABLE`) are behind
`Vanilla` (`holdsWaterPotion`, `waterBottle`, `setModelSelector`, `modelSelectorOf`, `itemModelOf`,
`putBlockEntityInt`) or `DrinkItem` for the same reason.

`DrinkItem` is a class rather than a method because an item's use and animation are overrides. Keep it
the one place that knows how drinking starts and finishes: `WaterskinItem` extends it and only says
whether it has anything to drink (`canDrink`) and what a drink removes.

## Loader

`Loader` does not live in this directory. Each loader has its own copy at
`src/main/<loader>/java/com/thirstwastaken2/platform/Loader.java`, with the same class name and the
same public signatures, and each node's buildscript (`build.gradle.kts` for Fabric,
`build.neoforge.gradle.kts` for NeoForge, `build.forge.gradle.kts` for Forge 47 on 1.20.1) compiles
exactly one of them. There is no interface
and no service lookup: a static call to a class that exists once per jar is the cheapest seam there
is, and the compiler checks every call site.

What does live here are the types those signatures need, because both copies have to share them:
`PlayerData`, `Clientbound`, `UseBlockHandler`, `UseItemHandler`, and on the client `StatusBarRenderer`.

What crosses the network is described in common code as a plain record with `write(FriendlyByteBuf)` and
`read(FriendlyByteBuf)` (`ThirstData`, `DrinkValuesPayload`), never as a `StreamCodec` or a
`CustomPacketPayload`, which 1.20.1 lacks. Each `Loader` wraps that pair in its own packet type.

| `Loader` | What it hides |
|---|---|
| `isDevelopmentEnvironment`, `configDir`, `isModLoaded` | the loader's own environment |
| `onRegister` | when a registry accepts entries: at once on Fabric, from the registration event on a loader that freezes registries early |
| `playerData` | the attachment system that saves a value on a player and syncs it to its owner, in the form its `write` and `read` agree on |
| `creativeTabBuilder` | a tab builder that places itself in the tab list |
| `onServerTickEnd`, `onUseBlock`, `onUseItem`, `onRegisterCommands`, `onTagsLoaded` | the event bus |
| `onLootTable` | loot table modification, on every table whoever wrote it |
| `addVillagerTrade` | a trade added to a profession's pool at a level, up to 1.21.11; from 26.1 trades are data and it does nothing |
| `onServerDataReload` | a server data reload listener, run at startup and on `/reload` |
| `registerResourceConditions` | the `thirstwastaken2:item_enabled` load condition the mod's recipes carry |
| `onDataPackSync`, `clientboundPayload` | telling each client what a data pack decided: when to, and the payload's registration, which returns the `Clientbound` that sends it only to a client that can take it |
| `ClientLoader.addRightStatusBar` | HUD layer registration and the right-hand status bar height |
| `ClientLoader.appleSkinShowsExhaustionUnderlay` | AppleSkin's own setting, which it keeps in a different class shape on each loader |
| `ClientLoader.renderCutout` | drawing a block with its transparent pixels cut out |
| `burnTime`, `isFuel` | how long an item burns as furnace fuel, counting the fuel a loader adds of its own; whether it burns at all, asked without a furnace, as a slot on the client asks |
| `ClientLoader.registerScreen` | the screen a menu opens on the client |

### How each loader answers

| Seam | Fabric | NeoForge |
|---|---|---|
| `isDevelopmentEnvironment`, `configDir`, `isModLoaded` | `FabricLoader` | `FMLEnvironment.isProduction`, `FMLPaths.CONFIGDIR`, `ModList` |
| `onRegister` | runs at once | queued, run from `RegisterEvent` for that registry on the mod bus. NeoForge constructs mods while the built-in registries are frozen, so anything that registers, items included, has to wait for the event. It fires data component types before items on purpose. A registration for a registry whose event already fired throws |
| `playerData` | a data attachment, `AttachmentSyncPredicate.targetOnly()` | an `AttachmentType` with `serialize(codec.fieldOf("value"))` and `sync((holder, to) -> holder == to, ...)`, registered through `onRegister`. Saved under `neoforge:attachments` rather than `fabric:attachments`, so a world moved between loaders starts every player at full thirst |
| `creativeTabBuilder` | `FabricCreativeModeTab.builder()`, `FabricItemGroup` before 26.1 | `CreativeModeTab.builder()` |
| `onServerTickEnd` | `ServerTickEvents.END_SERVER_TICK` | `ServerTickEvent.Post` |
| `onUseBlock`, `onUseItem` | `UseBlockCallback`, `UseItemCallback`: a non-`PASS` result stops the chain | `PlayerInteractEvent.RightClickBlock`, `RightClickItem`: on a non-`PASS` result, `setCancellationResult` and `setCanceled(true)`, which also skips later listeners |
| `onTagsLoaded` | `CommonLifecycleEvents.TAGS_LOADED` | `TagsUpdatedEvent` |
| `onRegisterCommands` | `CommandRegistrationCallback` | `RegisterCommandsEvent` |
| `onLootTable` | `LootTableEvents.MODIFY` | `LootTableLoadEvent`, `getTable().addPool` |
| `addVillagerTrade` | `TradeOfferHelper.registerVillagerOffers`, by `ResourceKey` from 1.21.5 and with the trade rebalance pool from 1.21; nothing from 26.1 | `VillagerTradesEvent`, matching `getType()`; nothing from 26.1 |
| `onServerDataReload` | `ResourceLoader.get(SERVER_DATA).registerReloadListener`, a `ResourceManagerReloadListener` | `AddServerReloadListenersEvent.addListener(id, ...)` |
| `onDataPackSync` | `ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS` | `OnDatapackSyncEvent.getRelevantPlayers()` |
| `registerResourceConditions` | `ResourceConditions.register` of `platform/ItemEnabledCondition`, whose `test` takes a `RegistryInfoLookup` from 1.21.2 and a `HolderLookup.Provider` before it, hence its place in `platform/` | `neoforge/ItemEnabledCondition`'s `MapCodec` into `CONDITION_SERIALIZERS` through `onRegister`. The recipe files name it under `condition`, Fabric's key; the build moves it to `type` (`neoForgeConditions`) |
| `clientboundPayload` | the record wrapped in a private `Wrapped` payload, its codec built with `StreamCodec.ofMember`, then `PayloadTypeRegistry.clientboundPlay()`; the handler waits in `fabric/ClientboundPayloads` until the client entrypoint hands it to `ClientPlayNetworking`, which common code cannot see | the same wrapping, then `RegisterPayloadHandlersEvent`, `registrar("1").optional().playToClient` on the mod bus |
| sending, through the returned `Clientbound` | `ServerPlayNetworking.canSend`, then `send` | `hasChannel`, then `PacketDistributor.sendToPlayer`; never to a fake player, for the reason `syncsTo` gives |
| `ClientLoader.addRightStatusBar` | `HudElementRegistry.attachElementAfter(FOOD_BAR)` plus `HudStatusBarHeightRegistry.addRight`; `GuiMixin` on 1.21.1 | a layer `registerAbove(VanillaGuiLayers.FOOD_LEVEL)` that draws at `guiHeight() - hud.rightHeight` and advances `Hud.rightHeight` only when it drew, and only when the player can be hurt, which is when vanilla draws the food bar |
| `ClientLoader.renderCutout` | `BlockRenderLayerMap` before 26.1, nothing from 26.1, where the game reads the layer off the textures | nothing: the model's `render_type` before 26.1, the textures from 26.1. It runs during mod construction, so it never asks for the block |
| `burnTime`, `isFuel` | `Vanilla.burnTime`, `Vanilla.isFuel`: Fabric's fuel registry feeds vanilla's table and fuel values | `Vanilla.burnTime`, NeoForge feeding its fuel data map into the fuel values; on 1.21.1 `ItemStack#getBurnTime`, the only reader of that data map there |
| `ClientLoader.registerScreen` | `MenuScreens.register`, opened by the mod's access widener, at client initialization | `RegisterMenuScreensEvent` on the mod bus |
| `ClientLoader.appleSkinShowsExhaustionUnderlay` | `ModConfig.INSTANCE.showFoodExhaustionHudUnderlay` | `ModConfig.SPEC.isLoaded() && ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get()`; reading a NeoForge config value before FML loads it throws |

### How Forge 47 answers

Forge 47, the `1.20.1-forge` node's loader, is NeoForge's parent one generation back, and answers most
seams the way the NeoForge column does, under `net.minecraftforge` names: `onRegister` queues for
`RegisterEvent` the same way, and `onUseBlock`, `onUseItem`, `onTagsLoaded`, `onRegisterCommands` and
`onLootTable` and `addVillagerTrade` use the events of the same names. Where it differs:

| Seam | Forge 47 |
|---|---|
| `isDevelopmentEnvironment` | the static field `FMLEnvironment.production` |
| `playerData` | no attachments: one capability, `thirstwastaken2:player`, attached to every player in `AttachCapabilitiesEvent<Entity>`, holding each value under its id and saving it through its codec under `ForgeCaps`. Copied in `PlayerEvent.Clone` unless the player died, as an attachment without `copyOnDeath` is. Sent on every set and again on `PlayerLoggedInEvent`, `PlayerRespawnEvent` and `PlayerChangedDimensionEvent`. All in `ForgeNetworking` |
| `clientboundPayload` | a `SimpleChannel` per payload, named by its id, accepting a missing mod on either side (`acceptMissingOr`), so a vanilla client may join. The client handler sets the value on `ClientPlayer.get()`, a class in `src/client/forge` a dedicated server never loads |
| sending, through the returned `Clientbound` | never to a `FakePlayer`, whose connection has no Netty channel; otherwise `isRemotePresent`, then `send(PacketDistributor.PLAYER...)`. Forge negotiates channels at login, so nothing waits the way it does on Fabric 1.20.1 |
| `onServerTickEnd` | `TickEvent.ServerTickEvent` at `Phase.END` |
| `onServerDataReload` | `AddReloadListenerEvent`, without an id |
| `onDataPackSync` | `OnDatapackSyncEvent.getPlayer()`, or every player in the list when it names none, on `/reload` |
| `registerResourceConditions` | `CraftingHelper.register` of a serializer (`platform/ItemEnabledCondition` in `src/main/forge`). The build moves the condition under `conditions` and `type` (`forgeConditions`) |
| `ClientLoader.addRightStatusBar` | `RegisterGuiOverlaysEvent.registerAbove(VanillaGuiOverlay.FOOD_LEVEL)`, drawing at `screenHeight - ForgeGui.rightHeight` and advancing it when it drew, only when `shouldDrawSurvivalElements` |
| `ClientLoader.renderCutout` | `ItemBlockRenderTypes.setRenderLayer(..., cutout())` from `FMLClientSetupEvent`, once the block exists |
| `burnTime`, `isFuel` | `ForgeHooks.getBurnTime`, which asks the item before vanilla's table |
| `ClientLoader.registerScreen` | `MenuScreens.register`, public under Forge's access transformer, from `FMLClientSetupEvent` on the main thread |
| `ClientLoader.appleSkinShowsExhaustionUnderlay` | the same as NeoForge |

The fluid capability is `IFluidHandlerItem`, attached through `AttachCapabilitiesEvent<ItemStack>` once
the mod's items are registered (`src/main/forge-fluidhandler`); water on a Forge `FluidStack` carries
the same `thirstwastaken2` compound an item does (`forge/WaterFluids`). Forge 47 has one mod class per
mod, not one per side, so `ThirstWasTaken2Forge` starts the client half itself, through a static call a
dedicated server never makes, and registers the config screen with `ConfigScreenHandler`.

### What older NeoForge versions change

Every NeoForge node answers every seam the way the table says. Where an older NeoForge differs, the
difference is a version conditional inside the NeoForge loader directories:

| Seam | Differs on | What it does there |
|---|---|---|
| `isDevelopmentEnvironment` | 1.21.1 (NeoForge 21.1) | the static field `FMLEnvironment.production`; the method arrived with 21.9 |
| `playerData` saving | 1.21.1 | `serialize(codec)` rather than a map codec under `value`, so the saved shape differs from later versions |
| `playerData` syncing | before 26.1 (NeoForge 21.1 and 21.11) | the predicate, `Loader.syncsTo`, turns away a fake player and then asks `connection.hasChannel(SyncAttachmentsPayload.TYPE)`: those versions throw when they send the payload to a connection that never negotiated it, which is what a gametest mock player has, and a vanilla client too, and throw again from `hasChannel` itself for a fake player, whose connection has no channel to read the negotiation off |
| `ClientLoader.addRightStatusBar` | before 26.2 | `Gui.rightHeight`; 26.2 moved it to `Hud` |
| `onServerDataReload` | 1.21.1 | `AddReloadListenerEvent`, which takes a listener without an id |

The resource translation (`neoForgeJson` in build-logic's `ResourceTranslation.kt`) also differs on 1.21.1: the ingredient type
goes under vanilla's `type` key, and the components ingredient's `base`, a whole ingredient there, is
turned into the `items` holder set. From 1.21.11 on it is `neoforge:ingredient_type` and the holder set.

### The NeoForge nodes' scope

`26.3.x-neoforge`, `26.2.x-neoforge`, `26.1.x-neoforge` and `1.21.1-neoforge` are the NeoForge nodes;
`1.21.11-neoforge` was dropped. What they leave out, and why:

- **Create Fly.** A Fabric port; `src/main/createfly` never compiles here, because the node does not
  set `deps.create_fly`.
- **Farmer's Delight at runtime, except on `1.21.1-neoforge`.** That node sets `deps.farmersdelight` to
  vectorwing's original, which puts it on runClient; the mod itself names no class of it on any node.
  The other NeoForge nodes have no build to test against yet. Its recipes load or are skipped
  correctly everywhere, through the translated `neoforge:conditions`.
- **Cloth Config, on `26.3.x-neoforge` only.** It has no 26.3 NeoForge build, and only AppleSkin's own
  settings screen needs it in runClient; `build.neoforge.gradle.kts` treats it as optional because of it.
- **`src/dev` and `src/datagen`.** Fabric only; each node reads the files the Fabric node on its
  Minecraft version generates.
- **Minecraft 1.21.** The Fabric 1.21.1 jar covers it; NeoForge 21.0 is a generation of its own.
- **Carrying a world between loaders.** Fabric saves the thirst attachment under `fabric:attachments`,
  NeoForge under `neoforge:attachments`, so a moved world starts every player at full thirst. There is
  no migration, on purpose.

The jars are `ThirstWasTaken2-<version>+<minecraft>-neoforge.jar`. Every Minecraft version has a
NeoForge node and the manual passes in
[docs/dev/MANUAL-TESTING.md](../../../../../../docs/dev/MANUAL-TESTING.md) are done, apart from 26.3's,
so NeoForge ships marked beta. `26.3.x-neoforge` is pinned to a NeoForge beta as well, since 26.3 has
no release build.

Two mixins reach methods NeoForge patches: `ItemStack#addDetailsToTooltip`, where the mod's rows land
after NeoForge's own tooltip hook, and `CauldronBlock#receiveStalactiteDrip`, whose `RETURN` injection
also fires on NeoForge's early return for modded fluids. Both are harmless as written.

Rules:

- **Plumbing only, the same as `Vanilla`.** A method takes the mod's handler and hands it to the
  loader; it never decides anything. What the mod hooks, and in what order, stays in
  `ThirstWasTaken2.initialize`, which is loader independent.
- **Shape a signature after what the mod needs, not after the loader at hand.** Fabric's callbacks
  are one way to deliver a block interaction; NeoForge's cancellable events are another. The
  signature has to be implementable by both.
- **Every copy changes together.** Adding a method to one `Loader` means adding it to all of them;
  the node that lacks it is the one that fails to compile.
- **Entrypoints are loader code.** `ThirstWasTaken2Fabric`, `ThirstWasTaken2FabricClient`, the NeoForge
  `@Mod` classes `ThirstWasTaken2NeoForge` and `ThirstWasTaken2NeoForgeClient`, and the Forge `@Mod`
  class `ThirstWasTaken2Forge` with its `ThirstWasTaken2ForgeClient` do one thing: call
  `ThirstWasTaken2.initialize` and `ThirstWasTaken2Client.initialize`. The NeoForge client class also
  registers the config screen with the mods list, which Mod Menu's entrypoint does on Fabric. The
  NeoForge `Loader` finds the mod event bus itself, through `ModList`, so the mod class passes nothing
  in. So are the manifests (`fabric.mod.json`, `neoforge.mods.toml`) and anything written against a
  loader-only mod, such as `ModMenuIntegration`. After `initialize`, both main entrypoints run the integrations both loaders compile:
  Fabric the `thirstwastaken2:integration` entrypoints (`Runnable`), NeoForge and Forge every class
  their scan data finds marked `@IntegrationEntrypoint`. The annotation is common code in this package, so an
  integration names neither loader to be started.
- **`checkLoaderSeam` fails on a loader import in `src/main/java` or `src/client/java`.** It reads
  imports, so it cannot see the methods Fabric API injects into vanilla classes
  (`getAttachedOrCreate`, `FabricItemStack` and friends). Those compile on Fabric and only fail on the
  next loader. Do not call them outside `src/main/fabric`. Nor can it see what Fabric API's transitive
  access wideners open: `ThirstDamageTypes` reads `DamageSources.source` and `damageTypes`, which Forge 47
  opens through `src/main/forge/resources/META-INF/accesstransformer.cfg`, in SRG names.

`src/dev` and `src/datagen` are Fabric only, each its own small Fabric mod. They sit outside the seam
on purpose: neither ships, and datagen output is shared by every loader on a Minecraft version.
Datagen writes Fabric's spellings once, and the NeoForge and Forge nodes translate the Fabric-only JSON
shapes as they copy resources (build-logic's `ResourceTranslation.kt`, applied by
`gradle/moddev.gradle.kts`), rather than datagen writing a second copy; see [src/main/resources/AGENTS.md](../../../../resources/AGENTS.md).

`src/gametest` runs on every loader. Its test classes are shared; the NeoForge and Forge nodes swap one
import and add a harness of their own in `src/gametest/neoforge` and `src/gametest/forge`, which is test
code rather than a seam. See
[src/gametest/java/AGENTS.md](../../../../../gametest/java/AGENTS.md).
