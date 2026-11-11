# src/main/kaleidoscope — water quality in Kaleidoscope Cookery's stockpot and teapot

[Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) (mod id `kaleidoscope_cookery`)
keeps water in two blocks, the stockpot and the teapot, and both keep only a fluid id, so the grade a
bucket had is lost on the way in. This directory is where that gets fixed. The plan, the order of work
and what is still to do are in
[docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md](../../../docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md).

**Built on the eight nodes that set the key**: `1.21.1-neoforge`, `1.20.1-forge` and the six Fabric
nodes. What it does there:

- **the stockpot and the teapot keep the grade** of the water poured in, and hand it back on the
  bucket taken out;
- **a teapot picked up and placed again** keeps it, in the item's block entity data;
- **an empty teapot item dipped into water** samples it where it lies, as a bucket does;
- **dripstone** fills a teapot with `dripstonePurity` water, as it does a cauldron (the 1.20.1 and
  1.21.1 builds only: the others do not let dripstone fill a teapot at all);
- **sea water**: both blocks take it, the teapot from a bucket or from the world, and hand it back
  salty; the teapot **brews nothing from it**, since tea brewed from it would come out safe;
- and **Jade** names the grade under the crosshair.

The drink and soup values are not here. They are ids in `config/itemvalues/KaleidoscopeItemValues`, common code that names no class
of the mod, so they reach every node, including those that do not compile this directory and a player
on the unsupported official Fabric build.

## Which build

| Node | Mod | Modrinth project |
|---|---|---|
| `1.21.1-neoforge`, `1.20.1-forge` | the official mod | `kaleidoscope-cookery` |
| `1.20.1`, `1.21.1`, `1.21.11`, `26.1.x`, `26.2.x`, `26.3.x` | Refabricated, the Fabric port | `kaleidoscope-cookery-refabricated` |

The official mod has no NeoForge build past 1.21.1, so the other NeoForge nodes do not set the key and
do not compile this directory. The official Fabric build stopped at 1.0.1, before the teapot, and is not
supported. Refabricated has the same mod id and the same package, so one directory serves both, and
since it names no loader and no fluid API, **all three loaders compile it**, as with
[src/main/supplementaries](../supplementaries/AGENTS.md), and `checkLoaderSeam` keeps it that way.
Each key is pinned by Modrinth version id; the
Fabric uploads of different Minecraft versions share one version number. `1.21.11` takes only hotfixes upstream
(1.3.0.10 is one), which `update_mc_deps.py` picks up like any other build.

On `1.20.1`, `1.21.1` and `1.21.11` Refabricated requires Forge Config API Port, `deps.forge_config_api_port`, on
the `runClient` classpath only. Its Night Config is nested in its jar, which Loom does not unpack into a
run, so `nestedMods` in `build.gradle.kts` takes it out, as it does Moonlight's CodecUI, when the
classpath is resolved (the Loom that builds 1.20.1 refuses one resolved while the project configures). From 26.1 on it
is optional and no table names it.

```
kaleidoscope/java/com/thirstwastaken2/kaleidoscope/
  KaleidoscopePresence       the gate: a classpath probe for the teapot, then one per mixin target,
                             and a read of the class file for a method only some builds have
  KaleidoscopeMixinPlugin    applies each mixin only where the gate allows it
  BrewedWater                the mod's own interface on both block entities: the grade they hold now
  BrewedWaterQuality         the only place that reads a grade off what goes in, and stores it
  ReturnedWater              the grade on its way back out, per thread, for one remove call
  mixin/StockpotBlockEntityMixin   addSoupBase, removeSoupBase, save and load
  mixin/TeapotBlockEntityMixin     addTeaFluid (every overload), removeTeaFluid, getDrops, save and load, and tick (no tea from sea water)
  mixin/TeapotDripstoneMixin       receiveDripstoneFluid, which only the 1.21.1 builds have
  mixin/TeapotItemMixin            an empty teapot scooping world water
  mixin/ItemUtilsMixin             stamps the bucket both blocks hand back through ItemUtils
  mixin/ItemUtilsPlayerMixin       the same through giveItemToPlayer, which only Refabricated has
  mixin/InventoryMixin             the same where Refabricated's teapot fills a bucket in the slot
  mixin/TapCookeryMixin            Kaleidoscope Tavern's tap into either block, which only Kaleidoscope
                                   Chinese Food adds
kaleidoscope/resources/
  thirstwastaken2.kaleidoscope.mixins.json
../../client/kaleidoscope/java/com/thirstwastaken2/client/kaleidoscope/
  KaleidoscopeJade           adds a reader to the mod's own Jade plugin
```

## How a grade moves

Both blocks keep a fluid id and nothing else, and both build the bucket they hand back from nothing,
so the grade lives in a `@Unique` field on the block entity, saved as one int under
`thirstwastaken2:water_quality` (`WaterPurity.storedValue`: 1 to 4 for the grades, 5 for sea water).

- **Read before the call, never after.** `addSoupBase` shrinks the bucket and `addTeaFluid` empties
  it, so the grade is read off the stack first and kept only when the call returns true.
- **The field is only trusted while the block holds water**: the stockpot in `PUT_INGREDIENT` with
  water as its soup base, the teapot in `PUT_INGREDIENT` with water as its tea fluid.
  `thirst$heldWater` answers null otherwise, which is what is saved, what Jade shows and what a remove
  call stamps. Every way into that state writes the field again, so a stale value is never read and
  nothing needs clearing when the soup is served.
  **A tap writes it too.** With Kaleidoscope Tavern and Kaleidoscope Chinese Food installed, a tap fills
  either block through the addon's own accessors, in `kcf$fillCookery`, a method the addon's mixin merges
  into Tavern's `TapBlock`, past `addSoupBase` and `addTeaFluid`. `TapCookeryMixin` hands the block the
  grade at that method's head, before the addon saves and syncs it: a water cauldron's own grade, or the
  world's water behind a waterlogged block, sampled where it lies (lava, nothing). It names Tavern's
  tap by string and none of Tavern's classes, so it lives here. It is applied only where
  `KaleidoscopePresence.hasChineseFoodTap` finds the addon's mixin still declaring that method, at a
  priority above the addon's so the method is merged first, with `require = 0` so a renamed one skips
  it rather than fail the game. Checked in game by
  `tools/agent/integrations/kaleidoscope-chinese-food-tap.jsonl`, with the addon's `runClientMod` line
  uncommented.
- **Out wherever the bucket lands while a remove call runs.** `ReturnedWater.during` holds the grade
  for the length of the call and restores what was there however it ends. It is a `ThreadLocal`
  because both blocks run their calls on the client too, and in single player the two threads run them
  at once. The stockpot hands its bucket over through `ItemUtils.getItemToLivingEntity` on both
  loaders, and so does the teapot on NeoForge, through `FluidUtils.fillItem`. Refabricated's `fillItem`
  goes through the Fabric Transfer API instead, which swaps the empty bucket for a full one **in the
  player's inventory slot**, through `Inventory.setItem`, and names `ItemUtils` only for a creative
  player's extra bucket (`giveItemToPlayer`). So three mixins stamp: `ItemUtilsMixin`,
  `ItemUtilsPlayerMixin` and `InventoryMixin`. A second stamp of the same grade changes nothing, and no
  other water enters an inventory during a remove call.
- **The Transfer API keeps an empty bucket's components.** On Fabric a Dirty bucket poured into a
  teapot leaves an empty bucket that still says Dirty, and filling that same bucket again brings the
  grade back with no help from this mod. A test has to draw with a fresh bucket, or it passes for the
  wrong reason.
- **Stamped through `WaterPurity.setQuality`**, so fresh water gets `water_salty: false` and sea water
  its sprite. A block filled before the integration existed has no grade and hands back a plain
  bucket, which reads as `defaultPurity`.

## How it stays optional

The same three layers as Supplementaries.

1. **Build.** Only when `deps.kaleidoscope_cookery` is set and the loader is in its row, the loader script adds this directory and
   append the mixin config to the built manifest (on NeoForge with `kaleidoscope_cookery` as an optional
   dependency), from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt). Only NeoForge compiles it for
   now; once Fabric does too, `checkLoaderSeam` keeps it free of either loader's API. Fabric needs it remapped, since it is mixed into, so it
   is `modCompileOnly`; NeoForge takes it as `compileOnly`. Both put it on `runClient` only, so
   `runServer` and the gametests run without it, and `-PwithoutOptional=kaleidoscope_cookery` (or
   `kaleidoscope-cookery`, `kaleidoscope-cookery-refabricated`) leaves it out of `runClient` as well.
2. **Runtime gate.** `KaleidoscopePresence` answers every question with a resource lookup, which never
   loads a class, and names no class of the mod's, no Minecraft class and no loader. The teapot's class
   is the version check: the official Fabric 1.0.1 has the same mod id and package but no teapot, so
   without it the whole integration is off and one warning says to install Refabricated. Past that,
   each target is probed by name, and a missing one logs once and is skipped. A mixin on a method only
   some builds have (`receiveDripstoneFluid`, `giveItemToPlayer`) is applied only where `hasMethod`
   finds it in the target's class file, read with ASM, so a build without it skips that mixin rather
   than fails it.
3. **Mixin plugin.** `KaleidoscopeMixinPlugin.shouldApplyMixin` asks the gate for the mixin's own
   target, so a class renamed upstream only takes its own mixins down, and `NEEDS_METHOD` for the two
   that need a method. `onLoad` asks too, so the warning about an unsupported build is logged at
   startup whatever the config lists.

## Names on Fabric

The mod's own methods and fields are matched **by name alone, with no descriptor**, and the mixins say
`remap = false`, as in Supplementaries: the Fabric jars keep the mod's own names. But Refabricated's
1.21.1 and 1.21.11 jars are in intermediary for Minecraft's names, so a Minecraft method the mod
overrides is `method_11007` there, not `saveAdditional`. Every injection on a Minecraft name says
`remap = true` on itself: `saveAdditional`, `loadAdditional`, `TeapotItem.use`, the `setBlockEntityData`
and `pickupBlock` calls, and `InventoryMixin`. Loom rewrites those strings in the built jar; NeoForge
and 26.x have nothing to rewrite. `fillFluid` is the mod's own, so its `@At` says `remap = false` and
has no descriptor. Check a change here in the built `1.21.1` jar with `javap -v`, not only in a dev
run, which uses Mojang names throughout.

## Version forks

Only in the mixins, each body one line:

| Difference | Where |
|---|---|
| `saveAdditional` / `loadAdditional` take a `ValueOutput` / `ValueInput` from 1.21.6 | both block entity mixins; `BrewedWaterQuality.save` / `load` take `putInt` / `getIntOr`, so each branch makes the same call |
| `getDrops` hands `setBlockEntityData` a `TagValueOutput` after 1.21.1 | `TeapotBlockEntityMixin` |
| `pickupBlock` takes any `LivingEntity` after 1.21.1, and no entity before 1.20.2 | `TeapotItemMixin`, whose body is `BrewedWaterQuality.scoop`; on 1.20.1 the player comes from `use`'s arguments |
| Before 1.20.5 the block entities save through `saveAdditional(CompoundTag)` and load through `load(CompoundTag)`, with no registries | both block entity mixins |
| The 1.20.1 Forge build fills a scooping teapot through a fluid capability and never calls `fillFluid` | `TeapotItemMixin` stamps the teapot `use` returns before 1.20.5, on both loaders, rather than wrapping `fillFluid` |

Vanilla's own differences go through `Vanilla`: the item's block entity data (`putBlockEntityInt`,
`TypedEntityData` from 1.21.9) and the action bar (`sendOverlayMessage`, from 26.1).

## Checking it

- `checkOptionalSeam` finds the plugin, the gate and `KaleidoscopeJade` as classes loaded without the
  mod, and passes.
- `runGametest` passes unchanged on every node: the mod is never on its classpath.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl`, with the mod and with
  `-PwithoutOptional=kaleidoscope_cookery` (Jade still there, the 1.0.9 shape) and
  `kaleidoscope_cookery,jade`, comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/kaleidoscope-cookery.jsonl](../../../tools/agent/integrations/kaleidoscope-cookery.jsonl), whose
  header says how to run and verify it. Every `execute` line asserts its own "Test passed". It covers
  a Dirty bucket through each block, sea water through both blocks, a teapot of sea water brewing
  nothing beside one of Clean water that brews, a
  teapot of Murky water picked up, placed and emptied, a teapot item dipped in a swamp and in the sea,
  and dripstone, and leaves two Jade screenshots. It passed whole on `1.21.1-neoforge` and `1.21.1`
  on 2026-09-23, and on `1.21.11`, `26.1.x`, `26.2.x` and `26.3.x` with only the two dripstone lines
  failing, as they must there. On 2026-10-09 `1.21.11` also failed `freshBrews` and `seaKeepsBag`, with
  1.3.0.9 and 1.3.0.10 alike, so not from the bump: neither teapot set up from block data on magma
  brews or keeps its bag there. Not yet looked into. Run again on the four Fabric nodes after the pins moved to
  Refabricated 1.5.1, with the same result. Make its world with `tools/agent/new_world.py`, which
  also copies the `data/minecraft` files 26.x keeps its world generation settings in.
- [tools/agent/integrations/kaleidoscope-cookery-1.20.1.jsonl](../../../tools/agent/integrations/kaleidoscope-cookery-1.20.1.jsonl)
  is the same script in 1.20.1's NBT. Every one of its 26 checks passed on `1.20.1` (Refabricated
  1.5.1.1) and `1.20.1-forge` (the original 1.5.1) on 2026-09-28, dripstone included, and the Forge Jade
  capture reads Salty under the teapot of sea water.
