# Version differences

Everything that differs between the supported Minecraft versions, in one place: first what a player
can see, then what differs underneath and where the code handles it. Unlike the plan files beside it,
this page is a standing reference. It stays for as long as more than one version is supported.

Supported nodes and their jars:

| Node | Jar suffix | Runs on | Java | Loader API |
|---|---|---|---|---|
| `26.3.x` | `+26.3` | 26.3 | 25 | Fabric API 0.166.2+26.3 |
| `26.2.x` | `+26.2` | 26.2 | 25 | Fabric API 0.161.0+26.2 |
| `26.1.x` | `+26.1.2` | 26.1, 26.1.1, 26.1.2 | 25 | Fabric API 0.155.3+26.1.2 |
| `1.21.11` | `+1.21.11` | 1.21.11 | 21 | Fabric API 0.141.6+1.21.11 |
| `1.21.1` | `+1.21.1` | 1.21, 1.21.1 | 21 | Fabric API 0.116.18+1.21.1 |
| `1.20.1` | `+1.20.1` | 1.20.1 | 17 | Fabric API 0.92.12+1.20.1 |
| `26.3.x-neoforge` | `+26.3-neoforge` | 26.3 | 25 | NeoForge 26.3.0.83 |
| `26.2.x-neoforge` | `+26.2-neoforge` | 26.2 | 25 | NeoForge 26.2.0.93 |
| `26.1.x-neoforge` | `+26.1.2-neoforge` | 26.1, 26.1.1, 26.1.2 | 25 | NeoForge 26.1.2.121 |
| `1.21.1-neoforge` | `+1.21.1-neoforge` | 1.21.1 | 21 | NeoForge 21.1.262 |
| `1.20.1-forge` | `+1.20.1-forge` | 1.20.1 | 17 | Forge 47.4.28 |

The NeoForge jars are built and tested on every node. A NeoForge node builds the same Minecraft
version as the Fabric node it sits under, so everything on this page applies to both. 1.21 is the
one exception: the Fabric 1.21.1 jar claims it, and NeoForge 21.0 is a generation of its own.

1.20.1 has MinecraftForge 47 in place of NeoForge, built by `build.forge.gradle.kts` with ModDevGradle
Legacy, and the same holds for it: everything under 1.20.5 and 1.21 below applies to both 1.20.1 jars.
The Forge jar asks for any Forge 47 build, not the one it is built against.

The Fabric 1.20.1 jar claims 1.20.1 only, not 1.20: Fabric API's last build for 1.20 (0.83.0) has no
attachment API, which the jar needs for player data. `fabric.mod.json` names
`fabric-data-attachment-api-v1`, so the loader refuses an older Fabric API on 1.20.1 rather than the
game crashing.

NeoForge has published only betas for 26.3, so `26.3.x-neoforge` is pinned to one and asks players for
at least that build. Two of its optional integrations have no 26.3 release yet either: Cloth Config,
which only AppleSkin's own settings screen needs in runClient, and Sophisticated Core, so that node
alone among the NeoForge ones builds without the Sophisticated upgrades.

**This page is the version axis only.** What differs between Fabric and NeoForge on the *same*
Minecraft version, and which seam hides it, is in
[src/main/java/com/thirstwastaken2/platform/AGENTS.md](../../src/main/java/com/thirstwastaken2/platform/AGENTS.md).
Where an older NeoForge differs from a newer one, that is a version difference and it is on this page,
under the release that changed it.

Checked against the code on 2026-09-20: 126 `//? if` blocks, 9 replacement rules with 27 replacements
in `stonecutter.gradle.kts`, and the version branches in `build.gradle.kts` and
`build.neoforge.gradle.kts`. `build.forge.gradle.kts` builds one version and has no branches.

## What a player can see

Gameplay is the same on every version: the same thirst rules, water grades, recipes, loot, commands
and config. The gametests hold every node to that. Only these differ:

| | 26.3 | 26.2 | 26.1.x | 1.21.11 | 1.21.1 |
|---|---|---|---|---|---|
| Droplets in item tooltips | no shadow | no shadow | no shadow | no shadow | **with a shadow** |
| Config screen section headings | vanilla heading | vanilla heading | vanilla heading | vanilla heading | **a centred text row** |
| The Sand Filter on Fabric, through Create Fly | **no** | yes | yes | **no** | **no** |
| The Sand Filter on NeoForge, through Create | **no** | **no** | **no** | **no** | yes |

Everything else a player sees, the thirst bar, the bowl and waterskin sprites, the Salty tooltip line,
the drinking animation and sound, is the same. On some versions it is produced differently, which is
the rest of this page.

### Why 1.21.1 looks different

- **The droplet shadow.** Later versions turn it off with `Style#withoutShadow`, which only exists
  from 1.21.4. On 1.21.1 the tooltip renderer decides, for all text at once.

The shadow has no per-text switch, so it stays.

Sea water in a bottle or bucket looks like ordinary water on **every** version, and only its tooltip
says Salty. Up to 1.4 the 1.21.2+ nodes swapped the sprite through the `minecraft:item_model`
component, which 1.21.1 and 1.20.1 lack, so the versions looked different; it was dropped to make them
the same. The bowl keeps its sea-water sprite everywhere, because it is the mod's own item, with its
own model and custom model data overrides.

### Why the Sand Filter is missing

Not a Minecraft difference: it follows which version of Create exists for each loader. The same Sand
Filter is built twice, on two different mods:

- **Fabric, through Create Fly.** Create Fly has no release for 26.3, 1.21.11 or 1.21.1. A node compiles
  `src/main/createfly` only when it sets `deps.create_fly`; see
  [src/main/createfly/AGENTS.md](../../src/main/createfly/AGENTS.md).
- **NeoForge, through Create.** Create 6 ships for NeoForge on 1.21.1 only, so `1.21.1-neoforge` is the
  one NeoForge node that sets `deps.create` and compiles `src/main/create`; see
  [src/main/create/AGENTS.md](../../src/main/create/AGENTS.md).

Farmer's Delight is different: nothing a player sees depends on it being there at build time, because
its recipes load or are skipped by their conditions. Only `1.21.1-neoforge` among the NeoForge nodes
puts it on the dev client, since the others have no build to test against yet.

## What differs underneath

Grouped by the release that introduced the newer form, newest first. A version is affected by every
section above its own line. The code column is where the difference is handled; callers elsewhere
never see it.

"Replacement" means a pure rename in `stonecutter.gradle.kts`, with no branch in any file.

### 26.3 (affects 26.2, 26.1.x, 1.21.11, 1.21.1)

| Difference | Code |
|---|---|
| Blocks lost their codec, and with it `simpleCodec` and the `codec()` override a block owed vanilla | `SupportedBlock`, which carried it for `HangingPotBlock` |
| Every `PushReaction` constant was renamed; `DESTROY` is `POPPED` | replacement |
| `LootPoolSingletonContainer` split into three classes, of which the entry builders are typed on `UniformContainerBase` | replacement |
| Loot number providers split into an int and a float family, each behind a `Holder`, so a pool's rolls and a count are built differently | `Vanilla.lootPool`, `Vanilla.setCount` |
| The block state loot condition is `MatchBlock`, taking a block lookup and writing `match_block`; it was `LootItemBlockStatePropertyCondition` | `ThirstBlockLootProvider.boilerHalf`, for the distiller's drop |
| `Inventory#placeItemBackInInventory` asks whether the client predicted the call | `Vanilla.placeItemBackInInventory` |
| Recipes became a registry: a recipe provider bootstraps them alongside their unlock advancements, and the criteria that name a recipe name a holder rather than a key | `ThirstRecipeProvider`, and `RecipeKeys` for the two providers outside that registry set |
| The advancement builder's `display` split in two, and only `rootDisplay` still takes the tab background | `ThirstAdvancementProvider`, at both call sites. Not a replacement: `display` is still the name a child calls on 26.3, so a rule rewriting it would be reversed onto those too |
| A saved block state is written under `id` and `properties` rather than `Name` and `Properties` | `CauldronGameTest` |
| The renderer's pipeline type moved from Blaze3D into Renderpearl | replacement |
| Opening a path in the file manager moved off `Util.OS` onto `Blaze3D` | `ClientVanilla.openPath` |
| The game moved from GLFW to SDL, so the window handle is an SDL one | the agent client's `ClientWindow` |
| SDL numbers the mouse buttons from one, so left is 1 and right 3 where GLFW had 0 and 1, and a widget only takes the new left | the agent client's `AgentClientVanilla.click`, which reads the numbers out of `InputConstants` so that `client.click` means the same button on every node |
| `MouseHandler#onMove` takes two more arguments with SDL | none: the agent client's `MouseMoveMixin` matches it by name and takes none of them |
| A gametest's `TestData` names the dimension it runs in | NeoForge `ThirstWasTaken2GameTests` |
| `ResourceManager#listResources` filters with a `ResourceManager.Selector` rather than a `Predicate` | none needed: `DataPackDrinks` lists its files through `FileToIdConverter#listMatchingResources`, the same on every version |
| Furnace fuel is the item's cooking fuel component, its burn time resolved in a loot context that names the block entity as the container at work; the level's fuel values are gone | `Vanilla.burnTime`, which takes a block entity that is a container for it; `Vanilla.isFuel` asks whether the item has the component |

Result: 26.3 writes its recipe unlocks with a `recipes` key holding the recipe id, where earlier
versions write `recipe`. Nothing else in the generated files moved.

### 26.2 (affects 26.1.x, 1.21.11, 1.21.1)

| Difference | Code |
|---|---|
| Whether the HUD is hidden (F1) moved from the options into the HUD object | `ClientVanilla.isHudHidden`; the agent client's `AgentClientVanilla.toggleHud` flips the same state |
| Opening a screen moved from the client onto the GUI | `ClientVanilla.setScreen`, `AgentClientVanilla.screen` |
| The right-hand status bar stack heights moved from `Gui` to `Hud` | NeoForge `ClientLoader.addRightStatusBar` |
| The main render target moved from the client onto its game renderer | `AgentClientVanilla.mainTarget`, for `screenshot` and `readFrame` |
| Entity type constants moved from `EntityType` to `EntityTypes` | `TestFixtures.mountType`, `piglinType` |
| Advancement trigger classes moved into `triggers` | replacement |
| `StatePropertiesPredicate` moved into `predicates` (it was in `criterion`, `critereon` before 1.21.11) | `ThirstBlockLootProvider.boilerHalf`, one branch per package, since only datagen names it |
| Serene Seasons' API says whether a dimension has seasons (`SeasonHelper.hasSeasons`); its builds for older versions keep that only in their internal config, whose class extends GlitchCore's and Night Config's, so the loader scripts compile against those two as well | `SeasonsPlatform.hasSeasons` in Serene Seasons' `platform/` |

### 26.1 (affects 1.21.11, 1.21.1)

| Difference | Code |
|---|---|
| A saved data type's id is an `Identifier`, not a file name | `platform/SavedPositions.typeId` |
| An item's crafting remainder is a template, which may be missing | `Vanilla.craftingRemainder`, for the distiller's lava bucket |
| A container screen extracts its background in `extractBackground` and its tooltips in `extractTooltip`, takes its size in the constructor, and draws the hovered slot's tooltip itself; before, `renderBg`, two size fields, and `render` calling `renderTooltip` | `client/platform/MachineScreen`, which `DistillerScreen` extends |
| The cauldron interaction maps became `CauldronInteractions`' dispatchers, asked with the stack and answering a do-nothing default; before, `CauldronInteraction`'s maps by item (`InteractionMap.map()`, a plain map on 1.20.1) | `Vanilla.cauldronHasInteraction` |
| A player's action bar message has its own method, `sendOverlayMessage` | `Vanilla.sendOverlayMessage`, which the Kaleidoscope Cookery Teapot's sea water refusal calls |
| The HUD draw target was renamed `GuiGraphicsExtractor` | replacement |
| Its `renderFakeItem` became `fakeItem` | replacement; `ConfigTheme.item` |
| `EditBox` lost `setFilter` | none: `ItemValueRows.valueBox` undoes an invalid keystroke in its responder, on every version |
| A block's render layer follows its textures, so a cut-out model needs no registration | `ClientLoader.renderCutout`; before it Fabric registers the layer and NeoForge reads `render_type` from the model |
| Blockstate definitions became `BlockStateModelDispatcher` | `HangingPotModels` |
| A widget draws in `extractWidgetRenderState` rather than `renderWidget`, and `drawString` became `text` | `ClientVanilla.canvas`, `ClientVanilla.button`, `ClientVanilla.text` |
| Fabric's creative tab builder was renamed `FabricCreativeModeTab` | `Loader.creativeTabBuilder` |
| Fabric API renamed `ResourceLoader#registerReloader` to `registerReloadListener`, and its payload registries `playS2C` to `clientboundPlay` | Fabric `Loader.onServerDataReload`, `Loader.clientboundPayload` |
| Fabric's data generation output and tag provider were renamed | replacement |
| Recipe results became `ItemStackTemplate`, cooking recipes gained new constructors, and building a result no longer takes the registries | `ThirstRecipeProvider`, `FarmersDelightRecipeProvider`, `TestFixtures.assemble` |
| Model texture mappings take a `Material` | `ThirstModelProvider` |
| NeoForge stopped throwing when an attachment syncs to a connection that never negotiated the channel, and answers for a fake player's channelless connection rather than throwing | NeoForge `Loader.syncsTo` |
| Gametests gained padding between them, and `TestEnvironmentDefinition` a type parameter | NeoForge `ThirstWasTaken2GameTests` |
| Villager trades became data: `villager_trade` files in per-level tags that each profession's `trade_set` draws from. Fabric API dropped `TradeOfferHelper` and NeoForge `VillagerTradesEvent` | `ThirstVillagerTradeProvider` writes `compat/TradeIntegration.TRADES` from 26.1; `Loader.addVillagerTrade` adds them before it |

Result: 1.21.11 writes shorter recipe files, because a live `ItemStack` omits components the item
already has by default. The stack the furnace hands out is the same; see
[src/datagen/java/AGENTS.md](../../src/datagen/java/AGENTS.md).

### 1.21.11 (affects 1.21.1)

| Difference | Code |
|---|---|
| `ResourceLocation` became `Identifier`, `ResourceKey#location` became `#identifier`, `Util` moved package | replacement |
| The advancement criterion package lost its `critereon` spelling | replacement, chosen inside the 26.2 rule because replacements do not chain |
| Command permission levels became permission sets | `Vanilla.isGameMaster`, `Vanilla.isOwner` |
| Game rules became typed values in their own package, `naturalRegeneration` renamed `NATURAL_HEALTH_REGENERATION` | `Vanilla.naturalRegeneration`, and `Vanilla.waterSourceConversion` for `RULE_WATER_SOURCE_CONVERSION` |
| The window handle accessor was renamed from `getWindow` to `handle` | `AgentClientVanilla.windowHandle` (written `>1.21.1`) |
| A connection's send listener became Netty's own | NeoForge `CapturingConnection` (written `>1.21.1`) |
| Villagers and their trades moved to `entity.npc.villager` | `TestFixtures.noviceOffers` (written `>=1.21.11`) |
| A screenshot is read back from the GPU asynchronously and handed to a callback, takes a downscale factor, and `NativeImage` answers ARGB rather than ABGR | `AgentClientVanilla.screenshot`, `AgentClientVanilla.readFrame` (written `>1.21.1`, since no node sits between) |

### 1.21.9 (affects 1.21.1)

| Difference | Code |
|---|---|
| A block item's block entity data became `TypedEntityData`, naming its type rather than keeping it under `id` | `Vanilla.putBlockEntityInt`, which a Kaleidoscope Cookery teapot item scooping water calls |
| A screen's mouse handlers take one `MouseButtonEvent` rather than a position and a button | the agent client's `AgentClientVanilla.press`, `release` and `drag` (written `>1.21.1`) |
| Fabric API's v1 resource loader replaced `ResourceManagerHelper`, which is gone by 1.21.11, and a reload listener no longer names itself | Fabric `Loader.onServerDataReload`, written `>=1.21.11` since no node sits between |
| Fonts are named through `FontDescription` | `Vanilla.dropletFont` |
| The chain became the iron chain, item and texture | `ThirstRecipeProvider`, `HangingPotModels` |
| "Water evaporates here" moved from the dimension type to environment attributes | `Vanilla.waterEvaporates` |
| FML turned its environment fields into methods, and hands a mod's manifest out as a stream rather than a path | NeoForge `Loader.isDevelopmentEnvironment`, `ThirstWasTaken2GameTests.openManifest` |

### 1.21.6 (affects 1.21.1)

| Difference | Code |
|---|---|
| A block entity saves and loads through `ValueOutput` / `ValueInput` | `StockpotBlockEntityMixin`, `TeapotBlockEntityMixin` (Kaleidoscope Cookery) |
| Fabric API gained the HUD element and status bar height registries | `ClientLoader.addRightStatusBar`; on 1.21.1 `GuiMixin` draws the bar after the food bar and moves the air bubbles up |
| Fabric's block render layer map moved into its rendering module and takes a chunk section layer | Fabric `ClientLoader.renderCutout` |
| A tooltip is queued for the end of the frame (`setComponentTooltipForNextFrame`) where it was drawn on the spot | `ClientVanilla.tooltip` |
| Saving and loading an entity take a `ValueOutput` / `ValueInput` rather than a `CompoundTag` | `TestFixtures.savePlayer`, `loadPlayer` |

### 1.21.5 (affects 1.21.1)

| Difference | Code |
|---|---|
| Item tooltips gained `addDetailsToTooltip` | `ItemStackMixin`; on 1.21.1 it wraps the hover-text call in `getTooltipLines` |
| The Confusion effect was renamed Nausea, `Entity#moveTo` became `snapTo` | replacement |
| Fabric API gained its own `@GameTest` annotation | replacement; on 1.21.1 tests use vanilla's with Fabric's empty structure |
| `GameTestHelper#assertTrue` takes a `Component` | `TestFixtures.check` |
| Blockstate generators hand over a parsed definition rather than JSON | `HangingPotModels` |
| Tests register through the test function registry, and the server writes its own JUnit report with `--report` | NeoForge `ThirstWasTaken2GameTests` and `build.neoforge.gradle.kts`; before it the harness registers and reports itself |
| A `CompoundTag`'s getters answer with an `Optional` or a fallback | `Vanilla.getString`, `getInt`, which `DrinkingUpgradeContainer.handlePacket` (Sophisticated) calls |
| A block entity saves through `ValueOutput` and loads through `ValueInput`, and `ContainerHelper` takes those | `platform/SavedBlockEntity` |
| Saved data is a `SavedDataType` with a codec and no `save` method | `platform/SavedPositions`, the placed water bottles of Kaleidoscope Tavern |
| A block entity that is a container spills its contents itself when removed; before, the block did it in `onRemove` | `SupportedBlock`, which does it before 1.21.5 for any such block entity |
| A villager profession is a registry key, and a trade's `getOffer` takes the level | `Loader.addVillagerTrade`, `TestFixtures.noviceOffers` |

### 1.21.4 (affects 1.21.1)

| Difference | Code |
|---|---|
| Custom model data became float lists | `Vanilla.modelSelector` |
| Item model definitions (`assets/…/items/`) replaced model overrides | `ThirstModelProvider`, `ThirstItemModelDefinitionProvider` (not built on 1.21.1) |
| Styles can turn off the text shadow | `Vanilla.dropletFont` (visible, see above) |
| The data generator's model classes, and Fabric's model provider, moved to client packages | replacement |

### 1.21.2 (affects 1.21.1)

| Difference | Code |
|---|---|
| Items and blocks are given their id before construction, and a block item names itself after the block only when asked | `Vanilla.registerItem`, `registerBlock`, `registerBlockItem`; `SophisticatedUpgradeItem` (Sophisticated's `platform/`) |
| `Block#updateShape` reordered its parameters and schedules ticks through its own argument | `SupportedBlock` |
| Drinking became the consumable component | `DrinkItem`; on 1.21.1 it overrides use, animation, duration and finishing |
| The `item_model` component | `Vanilla.itemModelOf`, `null` before 1.21.2; `ItemAppearanceGameTest` asserts sea water sets none on any version |
| `Item#use` returns a result without the resulting stack; `CONSUME` became `SUCCESS_SERVER` | `ItemStackMixin`, `Loader.onUseItem`; the constant is a replacement |
| Server-side damage became `hurtServer` | `Vanilla.hurt` |
| `FoodData#tick` takes a `ServerPlayer` | `FoodDataMixin` |
| `Registry#get` became `getValue` | replacement |
| `Registry#get(id)` returns a holder, where `getHolder` did | `Vanilla.mobEffect` |
| Every `Ingredient` became non-empty, so the codec lost its `CODEC_NONEMPTY` twin | `FarmersDelightRecipeProvider` |
| A components ingredient's `base` became a holder set rather than a whole ingredient | `neoForgeJson` in build-logic's `ResourceTranslation.kt`, translating Fabric's JSON as it is copied |
| GUI draw calls take a render pipeline | the dev-only `GuiDrawMixin`, which records vanilla's food and air sprite rectangles |
| A potion's crafting remainder is a glass bottle, so the Cooking Pot serves boiled water into one | `FarmersDelightRecipeProvider` names no container either way (visible in game only) |
| Recipes are registry entries with keys, built by a separate recipe provider | `ThirstRecipeProvider`, `ThirstAdvancementProvider`, `AdvancementGameTest` |
| The shapeless recipe builder can give its result components | `ThirstRecipeProvider`; on 1.21.1 the filled-bowl recipe is written out by hand |
| Use animations became `ItemUseAnimation` | `Vanilla.isDrinkAnimation`, which `DrinkingUpgradeWrapper.canFilter` calls; `AlchemyUpgradeWrapperMixin` (Sophisticated, NeoForge only) |
| A recipe names an ingredient by id or `#tag` rather than as an object | the Drinking upgrade's recipes, one copy per generation in `src/main/sophisticated-fluidhandler` and `-transfer` |
| A block entity type is built by a public constructor where a builder did | `Vanilla.blockEntityType`; the constructor and its supplier are widened on Fabric (below) |
| Furnace fuel is the level's fuel values, where it was a static table | `Vanilla.burnTime`, `Vanilla.isFuel` |
| An item's crafting remainder is a stack, where it was an item | `Vanilla.craftingRemainder` |

### 1.20.5 and 1.21 (affect 1.20.1)

Both 1.20.1 nodes, `1.20.1` on Fabric and `1.20.1-forge`. They are nodes of the one tree rather than a
separate copy so that a change to core reaches 1.20.1 in the same commit; the
[version policy](../../AGENTS.md#version-policy) applies, so a feature that needs a core-code fork
for 1.20.1 retires 1.20.1.
Most of these are written `>=1.20.5` or `<1.20.5` though the
change came earlier, in 1.20.2 or 1.20.3: with no node in between, the boundary only has to fall
between 1.20.1 and 1.21.1. Gametests, datagen and the dev tools fork in place.

| Difference | Code |
|---|---|
| No data components: an item's water is a `thirstwastaken2` compound in its tag, `{servings, purity, salty}` | `platform/ItemWaterData`; `platform/ThirstComponents` is empty there. A fluid variant carries the same compound: `FabricTransfer` |
| No default components: a new stack is given its item's default tag (the filled bowl's grade 3 and its model) | `platform/DefaultData`, filled by `ItemWaterData.freshByDefault` and `Vanilla.modelSelectorByDefault`, applied by `ItemStackMixin` in `ItemStack`'s constructor |
| A vanilla recipe result cannot carry a tag (a cooking result is a bare item id) | `platform/NbtRecipes`: `thirstwastaken2:smelting`, `smoking`, `campfire_cooking` and `crafting_shapeless`, whose `result` takes `nbt`. They build vanilla's recipes, so a client is sent vanilla's |
| Custom model data is the `CustomModelData` tag; potions are the `Potion` tag | `Vanilla.modelSelector` and its neighbours, `Vanilla.waterBottle`, `Vanilla.holdsWaterPotion` |
| Effects are keyed by `MobEffect`, not `Holder<MobEffect>` | `Vanilla.getEffect`, `hasEffect`, `effectInstance`, `poison` |
| `ResourceLocation` has public constructors instead of `fromNamespaceAndPath`, `withDefaultNamespace` and `parse` | `replacements` in `stonecutter.gradle.kts` (below 1.21) |
| No `AdvancementHolder`; no loot table registry, so a table is known by its id | `Vanilla.awardAdvancement`, `Vanilla.lootTableId`; `Loader.onLootTable` hands every loader an id |
| Block methods such as `getShape` and `tick` are public, and `isPathfindable` takes a level and position; no block codec | `HangingPotBlock` overrides them as public on every version; `SupportedBlock` |
| A container screen does not dim the world behind itself; `renderBackground` is asked to | `client/platform/MachineScreen` |
| `BucketPickup#pickupBlock` takes no entity (1.20.2 added the player) | `Vanilla.pickupFluid`; the constant `PICKUP_BLOCK` in `BucketItemMixin` |
| `playerWillDestroy` returns nothing (from 1.20.2 it returns the state) | `SupportedBlock.beforePlayerBreaks`, which `DistillerBlock` uses |
| A block entity saves to a tag alone and loads through `load`, and its update tag takes no registries | `platform/SavedBlockEntity`; `MachineSeamsGameTest` saves and loads one the way the chunk does |
| Saved data is made by a loader function handed to `computeIfAbsent` and saved to a tag alone; from 1.20.5 a `SavedData.Factory` and a save that takes registries | `platform/SavedPositions` |
| `getUseDuration` takes no entity; no `hasInfiniteMaterials`, `blockInteractionRange` or white smoke | `DrinkItem`, `Vanilla.hasInfiniteMaterials`, `Vanilla.blockReach`, `Vanilla.steamParticle` (a cloud) |
| Hover text is handed the level; `FoodData` adds food through `eat(int, float)`; the cauldron is told its weather by a predicate | `ItemStackMixin`, `FoodDataMixin`, `BlocksMixin` |
| No GUI sprite atlas: vanilla's HUD icons are regions of `textures/gui/icons.png` | `ClientVanilla.blitSprite` knows the food icons the config preview draws; the dev `GuiDrawMixin` records food and air from `blit` |
| The mouse wheel has no horizontal amount; widgets have no `setHeight`; `Util.OS.openPath` took a `File` | `client/platform/ScrollingScreen`, `ClientVanilla.setHeight`, `ClientVanilla.openPath` |
| Enchantments are registered objects, not data (1.21) | `Vanilla.damageProtection` |
| Data directories are plural: `recipes`, `advancements`, `loot_tables`, `tags/items` (1.21) | datagen `DataDirectories`; `processGametestResources` renames the gametests' own data |
| Recipes and advancements are built through `FinishedRecipe` and `Consumer<Advancement>` | `src/datagen/legacy`, which `build.gradle.kts` compiles in place of `ThirstRecipeProvider`, `ThirstAdvancementProvider` and `FarmersDelightRecipeProvider` |
| Fabric's convention tags name the material first (`c:copper_ingots`) and have no iron nuggets | `LegacyRecipeProvider` |
| No `no_knockback` damage tag, ominous bottle or trade rebalance pack | `ThirstDamageTypeTagProvider`; the gametests that need them are left out |
| Bucket pickup takes no entity at all (1.20.2 added the player) | `BucketItemMixin` |
| The gametest mock player joins on a connection with no Netty channel, which Forge 47 cannot take | the Forge `MockPlayers` in `src/gametest/forge`; the other loaders' copies call vanilla |
| Runs on Java 17 | `requiredJava` in `build.gradle.kts` and `build.forge.gradle.kts` |

### Somewhere between 1.21.1 and 1.21.11

These are written `>1.21.1` because the exact release was not pinned down. With no node in between,
it makes no difference to any jar.

| Difference | Code |
|---|---|
| `BlockItem.setBlockEntityData` takes a `TagValueOutput` | `TeapotBlockEntityMixin` (Kaleidoscope Cookery) |
| A block's description id is known inside its constructor (on 1.21.1 asking caches a wrong name) | `Vanilla.isWaterCauldron`; on 1.21.1 `BlocksMixin` marks the water cauldron's construction |
| GUI blits take a render pipeline and a tint | `ClientVanilla.blit`, `ClientVanilla.blitSprite`; on 1.21.1 both are render state, set before the draw and reset after it |
| A button draws its contents through `renderContents`, and presses take the input that caused them | `ClientVanilla.button` |
| `ServerPlayer#level` returns a `ServerLevel` | `Vanilla.level` |
| The drinking sound became a registry holder | `Vanilla.drinkSound` |
| Bucket pickup takes any living entity | `BucketItemMixin`, `TeapotItemMixin` (Kaleidoscope Cookery) |
| The food check a sprint asks moved from the client's `LocalPlayer` onto `Player` | `PlayerMixin`; on 1.21.1 `LocalPlayerMixin` hooks the client's own copy, and `TestFixtures.canSprint` answers from vanilla's rule there, because a server test cannot reach it |
| A NeoForge attachment saves through a map codec, so the value goes under a field | NeoForge `Loader.playerData`; a world carried from one to the other starts at full thirst |
| NeoForge's `AddReloadListenerEvent` became `AddServerReloadListenersEvent`, which takes the listener's id | NeoForge `Loader.onServerDataReload` (written `>=1.21.11`) |
| Advancement backgrounds are named by texture id | `ThirstAdvancementProvider` |
| Fabric's tag builder was renamed `builder` | `ThirstBiomeTagProvider`, `ThirstDamageTypeTagProvider` |
| NeoForge reads a custom ingredient's type from `neoforge:ingredient_type` rather than vanilla's `type` | `build.neoforge.gradle.kts` |
| NeoForge's fluid API became the transfer API (`ResourceHandler`, `ItemAccess`, transactions), and Sophisticated Core followed it | `src/main/neoforge-fluidhandler` / `-transfer` and `src/main/sophisticated-fluidhandler` / `-transfer`, chosen in `build.neoforge.gradle.kts` (written `>=1.21.2`) |
| NeoForge's `item_exists` recipe condition became `registered` | the Drinking upgrade's recipes, per generation as above |
| FML hands a mod file's contents out through `getContents`, where `findResource` gave a path | `ModFiles.contains` in Sophisticated's `platform/`, for `SophisticatedPresence` (written `>=1.21.2`) |
| `Entity#startRiding` gained a second flag | `PlayerStateGameTest` |
| Levels expose their highest buildable y | `BenchmarkWorld` |

## Differences in Fabric API rather than Minecraft

- **Payload types and attachment sync** do not exist on 1.20.1. Its attachments save themselves but
  do not sync, and payloads are plain channels. `src/main/fabric-legacypayload` and
  `src/client/fabric-legacypayload` hold its `FabricNetworking` and `ClientboundReceivers`, which send
  a player's value when it is set and again on joining, respawning and changing dimension, holding
  what a client has not yet said it takes until it does. `src/main/fabric-payload` is every later
  version's; `build.gradle.kts` picks one.
- **Resource conditions** on 1.20.1 are a predicate over the JSON rather than typed conditions with a
  codec. `ItemEnabledCondition` forks for it, and implements `ConditionJsonProvider` there for datagen.
  Loot tables are modified through `loot.v2`: `Loader.onLootTable`.

- **Attachment sync** exists on every version, including 1.21.1, where Fabric API backported it.
  `Loader.playerData` is the same on all four Fabric nodes.
- **Loot tables from vanilla's experiment packs** are reported as built in by Fabric API on 1.21.1 and
  as a data pack's on later versions. The mod adds its loot to every table regardless of source, so
  this has no effect; it is recorded because it is why the mod stopped filtering by source.
- **A resource condition's `test`** takes a `RegistryOps.RegistryInfoLookup` from 1.21.2 and a
  `HolderLookup.Provider` on 1.21.1. `platform/ItemEnabledCondition` in `src/main/fabric` forks for it
  (written `>=1.21.2`); it reads neither. `FabricRecipeProvider.withConditions` is the same on every node.
- **What Fabric API opens to itself only**: the menu type's constructor and supplier, the block entity
  type's supplier and, from 1.21.2, its constructor, and `MenuScreens.register` with its constructor
  interface. NeoForge's and Forge's access transformers make all of them public, so common code calls
  them as they are. On Fabric the mod's own access widener opens them, written for each node by
  build-logic's `AccessWidener.kt`: an access widener in `named` names before 26.1, a class tweaker in
  `official` names from it, and the block entity type's constructor only where it takes two arguments.

## Differences in Forge 47 rather than Minecraft

Forge 47 is 1.20.1's loader in place of NeoForge. What differs between it and the other two loaders on
the same Minecraft version is a loader difference, in
[platform/AGENTS.md](../../src/main/java/com/thirstwastaken2/platform/AGENTS.md); what belongs here is
what the Forge node needs that no other node does. Why Forge at all: nearly every 1.20.1 modpack
runs it, and NeoForge 47.1, the 1.20.1 fork, is no longer maintained. It is built with ModDevGradle
Legacy rather than ForgeGradle, the same plugin family and DSL as `build.neoforge.gradle.kts`, and
player data is a Capability rather than Cardinal Components, which would be a required dependency:

- **The game runs under SRG names** outside development. The jar that ships is `reobfJar`, the mixins
  carry a refmap, `META-INF/accesstransformer.cfg` is written in SRG names, and mod dependencies are
  remapped the other way through the `mod*` configurations. All in `build.forge.gradle.kts`.
- **Mixin configs are named in the jar manifest** (`MixinConfigs`), not in `mods.toml`, and on the run
  command lines in development.
- **MixinExtras is not shipped by Forge 47.** The jar nests `mixinextras-forge` (`deps.mixinextras`).
- **Convention tags are `forge:`**, so `src/main/forge` fills the two `c:` tags the 1.20.1 recipes use
  from `#forge:ingots/copper` and `#forge:ingots/iron`.

## Keeping this page true

- A new `//? if` block or replacement gets a row under the release that introduced the newer form. If
  a player can see it, it also gets a row in the first table and a line on the
  [installation page](../docs/installation.md).
- A difference between the loaders is not a version difference: it belongs in
  [platform/AGENTS.md](../../src/main/java/com/thirstwastaken2/platform/AGENTS.md). A `//? if` inside
  `src/main/neoforge`, `src/client/neoforge` or `src/gametest/neoforge` is both, and belongs here too.
- Retiring a node deletes the sections that only affect it. For 1.21.1 that is every section from
  1.21.9 down, plus the unpinned one.
- `grep -rn "//? if" src --include=*.java`, `stonecutter.gradle.kts` and the `sc.current.parsed`
  branches in both buildscripts are the source of truth; this page is their index.
