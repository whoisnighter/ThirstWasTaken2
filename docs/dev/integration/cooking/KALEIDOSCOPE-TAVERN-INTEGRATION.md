# Kaleidoscope Tavern integration plan

What ThirstWasTaken2 should do with Kaleidoscope Tavern (mod id `kaleidoscope_tavern`, package
`com.github.ysbbbbbb.kaleidoscopetavern`): the official
[Kaleidoscope Tavern](https://modrinth.com/mod/kaleidoscopetavern) on **NeoForge 1.21.1** and **Forge
1.20.1**, and the unofficial
[Kaleidoscope Tavern Refabricated](https://modrinth.com/mod/kaleidoscope-tavern-refabricated) on the
**Fabric** nodes. It is Kaleidoscope Cookery's sibling, by the same team, ported to Fabric by the same
person. This file sets the order of work, what each step needs and how each one is checked. Once the
work is built, how it works goes in `src/main/kaleidoscopetavern/AGENTS.md`.

Written on 2026-10-07 from:

- the official repository [KaleidoscopeMods/KaleidoscopeTavern](https://github.com/KaleidoscopeMods/KaleidoscopeTavern),
  branches `1.21.1` (`1.2.0-neoforge+mc1.21.1`), `main` (`1.2.0-forge+mc1.20.1`) and `26.1.2`
  (`1.1.2-neoforge+mc26.1.2`, **never published**);
- the port [NightEpiphany/KaleidoscopeTavern-Refabricated](https://github.com/NightEpiphany/KaleidoscopeTavern-Refabricated),
  branches `1.20.1-fabric`, `1.21.1-fabric`, `1.21.11-fabric`, `26.1.2-fabric`, `26.2-fabric` and
  `26.3-fabric` (head `68af326`, 2026-10-06);
- the Modrinth projects `r9RZvhiJ` (official) and `UMblNdlF` (Refabricated), and the CurseForge project
  `1475175`, which has the same two official files and nothing newer.

## Which build for which node

The same shape as Kaleidoscope Cookery: the official mod on the two old modded loaders, Refabricated on
every Fabric node, nothing on NeoForge past 1.21.1.

| Node | Mod | Version | Modrinth id |
|---|---|---|---|
| `1.21.1-neoforge` | official | `1.2.0-neoforge+mc1.21.1` (2026-07-01) | `W9ILsQt7` |
| `1.20.1-forge` | official | `1.2.0-forge+mc1.20.1` (2026-07-01) | `mUnU2u9e` |
| `1.20.1` | Refabricated | `1.2.0.13-fabric+mc1.20.1` | `iU6XwfBv` |
| `1.21.1` | Refabricated | `1.2.0.13-fabric+mc1.21.1` | `xLKaK5UA` |
| `1.21.11` | Refabricated, hotfixes only | `1.2.0.6-fabric+mc1.21.11` | `rcsRCqe1` |
| `26.1.x` | Refabricated | `1.2.0.13-fabric+mc26.1.2` | `LONHwOG0` |
| `26.2.x` | Refabricated | `1.2.0.13-fabric+mc26.2` | `o1wBLTt3` |
| `26.3.x` | Refabricated | `1.2.0.13-fabric+mc26.3` | `f7tIhCOi` |
| `26.x-neoforge` | none | — | — |

The official `26.1.2` branch builds a NeoForge jar, but nothing is published from it, and it lacks
`WaterloggedBehavior` and `VanillaBottlePlaceEvent`, so it is an older line, not a newer one. When it
ships, it gets checked like any new build; until then `26.1.x-neoforge` sets no key.

**Why one directory can serve all eight.** `BarrelBlockEntity`, `WaterCauldronTapBehavior`,
`WaterloggedBehavior`, `TapBehaviorManager` and `VanillaBottlePlaceEvent` are on every published branch
under the same names. What differs is below the methods we touch:

- **The barrel's tank.** Official: a NeoForge or Forge `FluidTank`, filled through the mod's own
  `util/FluidUtils` over the loader's fluid capability. Refabricated: its own `CustomFluidTank` over the
  Fabric Transfer API, filled through `util/fluids/FluidUtils`. Both expose `addFluid(LivingEntity,
  ItemStack)` and `removeFluid(LivingEntity, ItemStack)` on the block entity, and both hand the result
  over through the mod's `ItemUtils.getItemToLivingEntity`. Hooking there names neither loader's API, so
  the row gets all three loaders, as Kaleidoscope Cookery's does.
- **`VanillaBottlePlaceEvent`** is a NeoForge `@SubscribeEvent` on the official mod and a
  `UseBlockCallback` on Refabricated. The bottle *block* it places, and that block's drop, are the same.
- **Jade**: the official plugin is `compat/jade/ModPlugin`, Refabricated's is `compat/jade/ModJadePlugin`
  (not on `1.21.1-fabric`, which has none at that path). Step 7 checks each.

**Runtime requirements on `runClient`.** Refabricated requires Forge Config API Port on `1.20.1` and
`1.21.1` (already `deps.forge_config_api_port`, for Kaleidoscope Cookery) and, on `1.20.1` only,
Reach Entity Attributes `>=2.4.0`; from 26.x on, Forge Config API Port is only suggested. Check each
node's `fabric.mod.json` at the pinned version before adding anything.

## Where water lives in Kaleidoscope Tavern

| Place | What it does with water | What happens to the grade today |
|---|---|---|
| **Barrel** (`BarrelBlockEntity`, a 3×3×3 multiblock) | holds four buckets (`MAX_FLUID_AMOUNT`) of any fluid. With water plus sugar cane, potato or wheat it ferments rum, vodka or whiskey; any fluid and ingredients no recipe matches ferment into 16 vinegar. Water can be **taken back out** with a bucket while the lid is open and it is not brewing | **lost both ways.** The tank keeps a fluid and nothing else. Refabricated's `fillItem` even hands over `fluid.getBucket().getDefaultInstance()`, a fresh bucket |
| **Tap on a water cauldron** (`WaterCauldronTapBehavior`) | fills an empty or part-full cauldron below to **full**, or a placed empty bottle below into a placed water bottle. The source cauldron is **not drained** | the new cauldron is `WATER_CAULDRON.defaultBlockState()`, so its `purity` value is 0, unset, read as `defaultQuality` (Clean). A Dirty cauldron makes Clean ones |
| **Tap on any waterlogged block** (`WaterloggedBehavior`, the fallback when no behaviour is registered for the block) | the same two outcomes, from a waterlogged slab, stairs, fence and so on | the water is never sampled: a waterlogged block in the sea or a swamp gives an unset, Clean cauldron |
| **Placed water bottle** (`VanillaBottlePlaceEvent`, shift-use with a water bottle; config `WATER_BOTTLE_PLACEMENT`) | turns the bottle into the `water_bottle` block, a plain `BottleBlock` with no block entity. Picked up by hand or broken, its loot table drops `Potions.WATER` | **lost.** The bottle that comes back has no `water_purity` and no `water_salty`, so a Dirty or sea-water bottle comes back Clean and fresh |
| **Shaker** | a cocktail is three tagged ingredients; `#cocktail_ingredient_white` holds vodka, whiskey, rum and **`minecraft:potion`**, so any water bottle counts | no water is kept: the bottle is an ingredient, the cocktail its own item |
| **Pressing tub** | grapes and berries into six juice fluids, each with its own bucket | no water involved |

Nothing else in the mod holds water: the cabinets, racks and holders store bottles as items, and the
drink blocks hold drinks.

## The drinks

Every drink is an item with a drinking animation and its own `finishUsingItem`; none has
`FoodProperties`. So `ThirstConfig` ids are the whole job, and the keyword fallback must not be left to
guess: it would match `wine` and `juice` and miss the spirits and cocktails, and `bucket` is
blacklisted.

| Group | Items |
|---|---|
| Wines | `wine`, `champagne`, `carignan`, `sakura_wine`, `plum_wine`, `ice_wine`, `polaris_sweet_white`, `red_queen`, `riesling_dry_white`, `sunset_glow`, `madame_shexiang`, `sweet_berry_wine`, `sherry`, `mother_snow`, `luminous_bride`, `glowflower_brew`, `sauvignon_blanc_dry_white`, `miners_star` |
| Spirits | `vodka`, `whiskey`, `rum`, `brandy` |
| Mead | `honey_wine` |
| Cocktails | `white_lady`, `emerald`, `brass_heart`, `godfather`, `grasshopper`, `screwdriver`, `mojito`, `allium_garden`, `depth_charge`, `nether_special`, `bloody_mary`, `sculk_special`, `signature_cocktail`, `mystery_cocktail` |
| Other bottles | `watermelon_juice`, `vinegar` |
| Juice buckets (drinkable) | `grape_bucket`, `ice_grape_bucket`, `gold_grape_bucket`, `green_grape_bucket`, `sweet_berries_bucket`, `glow_berries_bucket` |
| Not a drink | `molotov` (thrown), `grape` and the other grapes (food, see below) |

Re-list the ids from `ModItems` on each pinned branch before writing them; Refabricated sometimes trails
the official mod by an item.

## Status

| Step | State |
|---|---|
| 1. Build dependency and gate | done, eight nodes build; `1.21.11` since Refabricated 1.2.0.6 fixed its crash |
| 2. Thirst values | done; spirits at 0, 0 |
| 3. Investigation: what happens to a grade | done, from the sources and in game; see below |
| 4. The barrel keeps the grade | done |
| 5. The tap keeps or samples the grade | done |
| 5b. The tap into the stockpot and teapot (Kaleidoscope Chinese Food) | done, in `src/main/kaleidoscope`; run in game with the addon on `1.21.1-neoforge` and `1.20.1-forge` |
| 6. Decision: the placed water bottle | **decided: (a)**, built |
| 7. Decision: sea water in the barrel and the shaker | **decided: (a)**, built |
| 8. Jade line on the barrel | done, from any of its blocks |
| 9. Docs | done |
| 10. Optional seam | done: `checkOptionalSeam` passes, boot without the mod, agent script on all seven nodes |

How it was built is in [src/main/kaleidoscopetavern/AGENTS.md](../../../../src/main/kaleidoscopetavern/AGENTS.md).

### What was found, correcting the plan below

- **The barrel is three blocks a side** (27 blocks, `LAYER` floor, wall, ceiling, `INDEX` 0 to 8), one
  block entity at the bottom centre, the lid at the top centre. It holds **four buckets**
  (`MAX_FLUID_AMOUNT = 4000`) on every branch, not one.
- **Refabricated does eat our containers** (step 3.2), and worse than the plan feared: `emptyItem` hands
  back `onConsumed(result)`, an empty bucket for any fluid container that is not a bucket, and `fillItem`
  hands back `resource.getFluid().getBucket()`, a full bucket, whatever was held out, so an empty canteen
  drew a whole bucket for one serving. The barrel refused this mod's containers on Fabric until upstream
  fixed it (#29, 1.2.0.12); it still does on an older Refabricated build.
- **The barrel's tank names a loader on both builds** (NeoForge or Forge `FluidTank`, Fabric
  `SingleVariantStorage`). Both build a `BarrelRecipeContainer` from it, whose `getFluid()` is a plain
  `Fluid`, so the mixin reads the tank that way and names neither.
- **A refused container has to use the click up.** The barrel tries `removeFluid` when `addFluid` fails,
  then passes the click on, and a bucket would empty itself over the lid.
- **`1.21.1-fabric` does have the Jade plugin** at `compat/jade/ModPlugin`.
- **Refabricated 26.x needs no Forge Config API Port**; `1.20.1`, `1.21.1` and `1.21.11` require it, and
  `1.20.1` also Reach Entity Attributes, nested in its jar.
- **Refabricated's `1.21.11` build (1.2.0.5) crashes on launch** with current Fabric API: its
  `PlayerMixin` registers an entity data serializer the old way, which Fabric API refuses. CI's
  `runDatagen` on `1.21.11` crashed on it. **Decided 2026-10-09: `1.21.11` is left out** (no
  `deps.kaleidoscope_tavern` there) until a fixed build is out; code that cannot run is not built.
  1.2.0.6 fixed it on 2026-10-10, and the node sets the key again. See "Reported upstream" below.
- **The tap can be opened by redstone** (`neighborChanged`), which is how the agent script drives it.
- **Kaleidoscope Chinese Food's fill** (step 5b) needs no class of Tavern's: the source and the
  destination are block states, and the tap's facing gives the source position. So it lives in
  Cookery's directory, which owns the blocks it writes to, and names Tavern's tap by string.
- **A waterlogged slab spills water** out of its open sides, and `getBiome` blurs biome cells near
  their edges; the agent script walls the slab in and paints a wide ocean.

### In game

`tools/agent/integrations/kaleidoscope-tavern.jsonl` on 2026-10-09: every check passed on `1.21.1` and
`26.3.x`, and on `1.21.1-neoforge` every one but `canteenRefused`, which only Fabric refuses. Its
`-1.20.1` copy: every check on `1.20.1`, and on `1.20.1-forge` every one but `canteenRefused`. The Jade
capture reads Dirty from a side block of the barrel, and Salty on the cauldron the tap filled from the
sea. `smoke/boot.jsonl` with `-PwithoutOptional=kaleidoscope_tavern` came up on `1.21.1-neoforge`. Every
check passed on `26.1.x` and `26.2.x` too, on 2026-10-09.

On 2026-10-15, with Refabricated 1.2.0.12 and `canteenRefused` turned into `canteenPoured`: every check
passed on `1.21.1`. A Dirty canteen with two servings goes into the barrel and stays a canteen.

Step 5b: `tools/agent/integrations/kaleidoscope-chinese-food-tap.jsonl` on `1.21.1-neoforge` and
`1.20.1-forge` with Kaleidoscope Chinese Food 1.1.14, 2026-10-09: every check passed on both. A tap on a Dirty cauldron fills the
stockpot below with Dirty water, and one on a waterlogged slab in the ocean fills the teapot below with
sea water, which keeps its tea bag and brews nothing.

### Reported upstream

To [NightEpiphany/KaleidoscopeTavern-Refabricated](https://github.com/NightEpiphany/KaleidoscopeTavern-Refabricated/issues):

1. **1.21.11 crashes on launch** (`1.21.11-fabric` at `c52f6a0`, 1.2.0.5). `mixin/PlayerMixin` calls
   `EntityDataSerializers.registerSerializer` from a static initializer; Fabric API's object builder module
   refuses that and asks for `FabricTrackedDataRegistry.register`. The `26.x` branches avoid it with
   `OPTIONAL_UNSIGNED_INT`. When a fixed build is out: set `deps.kaleidoscope_tavern` on `[fabric."1.21.11"]`,
   run `runDatagen` and the agent script there.
2. **The barrel swaps containers for buckets** (every Fabric branch, `util/fluids/FluidUtils`):
   `emptyItem` hands back `onConsumed(result)`, an empty bucket for any fluid container that is not a
   bucket, and `fillItem` hands back `resource.getFluid().getBucket()`, a full bucket, for whatever was held
   out, so one serving drawn into a canteen becomes 1000 mB. While it stands, the integration refuses this
   mod's containers on Fabric (`TavernWater.refusesContainer`); drop that once fixed.

Filed 2026-10-09 as [#28](https://github.com/NightEpiphany/KaleidoscopeTavern-Refabricated/issues/28) (the
1.21.11 crash) and [#29](https://github.com/NightEpiphany/KaleidoscopeTavern-Refabricated/issues/29) (the
containers). The repository has no issue template; titles follow its `[Bug]` convention.

**Both fixed and closed on 2026-10-10.** 1.2.0.6-fabric+mc1.21.11 fixes the crash; 1.2.0.12 on every other
branch, and 1.2.0.6 on 1.21.11, fix the containers: `emptyItem` and `fillItem` now work on a mutable
item context (`FluidUtils.mutableContext`) and hand back what the transfer left, and `onConsumed` is
deprecated. `1.21.11` sets `deps.kaleidoscope_tavern` again, and `TavernWater.refusesContainer` refuses
only on a build without the fix, read off the `mutableContext` name in the class file
(`KaleidoscopeTavernPresence.swapsContainers`), since players may still run an older one.

## 1. Build dependency and gate

- **Row** in [the integration table](../../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt):
  `dir = "kaleidoscopetavern"`, `depsKey = "deps.kaleidoscope_tavern"`, all three loaders, mixin config
  `thirstwastaken2.kaleidoscopetavern.mixins.json`, `neoForgeDependencies = listOf("kaleidoscope_tavern")`,
  and a Jade entrypoint if step 8 is built. Its own directory, not `src/main/kaleidoscope`: the two mods
  are installed separately, so each needs its own key and gate.
- **Keys** in `stonecutter.properties.toml`, by Modrinth version id, on the eight nodes of the table
  above. Fabric `modCompileOnly` (remapped, since it is mixed into) and NeoForge/Forge `compileOnly`,
  each plus `runClientMod` with the names `kaleidoscope-tavern`, `kaleidoscope-tavern-refabricated`,
  `kaleidoscope_tavern`, so `-PwithoutOptional` takes any of them.
- **`update_mc_deps.py`**: a `ModrinthDep("kaleidoscope_tavern", "kaleidoscope-tavern-refabricated",
  by_id=True, neoforge_project="kaleidoscopetavern", ...)` like Kaleidoscope Cookery's, with this file in
  its mirrors. `1.21.11` is not frozen today; leave it unfrozen unless the port says so.
- **Gate** `KaleidoscopeTavernPresence`: a resource lookup for each mixin target, as
  `KaleidoscopePresence` does, naming no class of the mod, no Minecraft class and no loader. A missing
  target logs once and only its own mixins are skipped.
- **Names on Fabric**: as with Kaleidoscope Cookery, the mod's own methods (`addFluid`, `removeFluid`,
  `onEndExtract`) are matched by name with `remap = false`, and any Minecraft method the mod overrides
  (`saveAdditional`, `loadAdditional`) says `remap = true`. Check the built `1.21.1` and `1.20.1` jars
  with `javap -v`: Refabricated's 1.21.x jars were in intermediary for Kaleidoscope Cookery.

**Check:** `./gradlew ":<node>:build"` on all eight; `runServer` on one Fabric and one NeoForge node
prints the init line.

## 2. Thirst values (data only, no class referenced)

`kaleidoscopeTavernDrinks(drinks)` in `config/itemvalues/KaleidoscopeItemValues`, called from both `DefaultItemValues.drinks()` and `bringUpToDate`, with
`putMissing` like every other mod's. Common code, so it reaches every node, including those that do not
compile the integration directory. Proposed values, following the precedents already in the file:

| Group | Thirst, quenched | Precedent |
|---|---|---|
| Wines | 3, 1 | Brewin' and Chewin' and Hearth and Harvest wines |
| `honey_wine` | 5, 2 | mead in Brewin' and Chewin', Vinery, Cultural Delights |
| Spirits (`vodka`, `whiskey`, `rum`, `brandy`) | 0, 0 | Cultural Delights' spirits; Brewin' and Chewin' gives its rums 2, 1 |
| Cocktails | 3, 1 | mixed with juice and ice; Brewin' and Chewin' gives Bloody Mary 4, 2 |
| `mystery_cocktail` | 2, 0 | a failed mix, like Kaleidoscope Cookery's `mystery_tea` (3, 1) but worse |
| `watermelon_juice` | 6, 4 | Farmer's Delight `melon_juice` |
| `vinegar` | 0, 0 | Cultural Delights' vinegar |
| Juice buckets | 6, 4 | a juice; milk gets the same value in a bucket as in a bottle |

Spirits at 0, 0 versus 2, 1 is the one real choice here: the barrel ferments them from water, which
argues for 2, 1 like Brewin' and Chewin's rum. Settle it when writing the method and say why in its
Javadoc. The brew level (one to five stars) changes the mod's effects, not our value.

Grapes are food with `FoodProperties`; add them to `kaleidoscopeTavernFoods` at 2, 0 only if a sweet
berry or Kaleidoscope Cookery's tomato already gets that, otherwise leave them out.

**Check:** a gametest in the `ThirstConfig` defaults test that the ids resolve to their values without
the mod (the same as other id-only mods), and the agent script drinks one of each group.

## 3. Investigation: what happens to a grade

Before writing any mixin, run each path in a real client on `1.21.1-neoforge` and `1.21.1` with
`/data get` and the tooltip, and write what was found here, as Brewin' and Chewin's step 3 did:

1. a Dirty water bucket into the barrel, taken back out with a fresh empty bucket (not the same one: on
   Fabric the Transfer API keeps an empty bucket's components, see `src/main/kaleidoscope/AGENTS.md`);
2. a Dirty **canteen** and **waterskin** into the barrel. Refabricated's `emptyItem` passes the emptied
   container through `onConsumed`, which turns **any fluid container that is not a bucket into an empty
   bucket**. If our canteen comes back as a bucket, that is their bug: report it upstream with the line,
   and refuse our containers on Fabric until it is fixed rather than lose the player's canteen;
3. on NeoForge, whether the `FluidTank` keeps a canteen's fluid components, and whether it refuses to
   mix two water stacks with different components (Brewin' and Chewin's keg did);
4. a tap on a Dirty cauldron, on a waterlogged slab in the sea, and over a placed empty bottle;
5. a Dirty and a sea-water bottle shift-placed and picked up again;
6. a Dirty bottle and a sea-water bottle shaken into a Mojito.

## 4. The barrel keeps the grade (bug)

The pattern is Kaleidoscope Cookery's stockpot, step for step:

- an interface `BarrelWater` on `BarrelBlockEntity` through a mixin, with a `@Unique` int saved under
  `thirstwastaken2:water_quality` (`WaterPurity.storedValue`);
- `addFluid`: read the grade off the stack **before** the call (the call empties it), keep it only when
  the call returns true and the tank now holds water. If water is added to water already in the tank,
  keep the **worse** of the two, as a cauldron does;
- `removeFluid`: hold the grade in a `ReturnedWater`-style `ThreadLocal` for the length of the call and
  stamp the water bucket handed over through `ItemUtils.getItemToLivingEntity`. Reuse Kaleidoscope
  Cookery's `ReturnedWater` shape; do not share the class, since the two directories are gated apart;
- trust the field only while the tank holds water and the barrel is not brewing; once a recipe starts,
  `clearItemsAndFluid` empties the tank and the field is meaningless;
- save and load with the same version forks as `StockpotBlockEntityMixin` (`ValueOutput` from 1.21.6,
  `CompoundTag` before 1.20.5).

The fermented result is its own item and carries no grade (step 7 says why that is safe).

**Check:** the agent script pours a Dirty bucket in and draws a Dirty bucket out, on one node of each
loader; `/data get block` shows the saved value.

## 5. The tap keeps or samples the grade (bug)

- **`WaterCauldronTapBehavior.onEndExtract`**: after its `setBlockAndUpdate`, stamp the cauldron below
  with the source cauldron's grade (`sourceState` is a parameter), the worse of it and what the
  destination already held. One `@Inject` at `TAIL`, body one line into a `TavernTap` helper.
- **`WaterloggedBehavior.onEndExtract`**: the source is world water, so sample it as a bucket would at
  the waterlogged block's position (`tapPos.relative(facing.getOpposite())`), through the same
  `WaterPurity` call `WaterInteractions.fillFromWater` uses. A waterlogged block in the sea gives a
  sea-water cauldron (stored value 5), in a swamp a Murky or Dirty one.
- **Into a placed empty bottle**: the result is the `water_bottle` block, whose grade is step 6's
  problem. Whatever step 6 decides, the tap writes through the same place.

The tap does not drain its source. That is the mod's design (an endless tap on a cauldron) and not ours
to change; with this step it at least copies the right water.

**Check:** a tap on a Dirty cauldron fills a Dirty cauldron; a tap on a waterlogged slab in the ocean
fills a salty one that refuses to be drunk by hand.

### 5b. The tap into Kaleidoscope Cookery's stockpot and teapot (bug, from Kaleidoscope Chinese Food)

Found 2026-10-07 reading the jars of
[Kaleidoscope Chinese Food](https://modrinth.com/mod/kaleidoscopechinesefood) 1.1.14 (mod id
`kaleidoscope_chinesefood`, NeoForge 1.21.1 and Forge 1.20.1, no source published, CC BY-NC-ND 4.0).
Tavern alone does not do this; the addon adds it, and only when Tavern is present (its
`TavernMixinConfigPlugin` probes for Tavern).

- **What the addon does**: `com.bmt.kaleidoscope_chinesefood.mixins.tavern.TapBlockMixin` wraps
  `ITapBehavior.isMatch` and `onStartExtract` in Tavern's `TapBlock.tryOpen`, so a tap whose source is a
  water cauldron, a waterlogged block or a lava cauldron also fills an empty, lidless stockpot or an
  empty teapot below. Its private static `kcf$fillCookery(Level, BlockPos, BlockState, BlockState)` sets
  the fluid through two accessors of its own, `StockpotBlockEntityAccessor.setSoupBaseId` and
  `TeapotBlockEntityAccessor.setTeaFluidId`, then `setStatus` and `refresh`. Water is
  `ModSoupBases.WATER` and `minecraft:water`.
- **Why it is our bug**: [src/main/kaleidoscope](../../../../src/main/kaleidoscope/AGENTS.md) writes the
  grade only in `addSoupBase` and `addTeaFluid`, so water that comes in this way carries no grade. It
  comes out as an unstamped bucket, read as Clean, even from a Dirty cauldron or a waterlogged block in
  the sea, and the teapot brews tea from sea water.
- **Fix, with step 5**: one `@Inject` at the `TAIL` of `kcf$fillCookery`, applied only when the addon is
  present (a classpath probe for `TapBlockMixin`, in `KaleidoscopePresence` or the Tavern directory's
  gate). It stamps the stockpot or teapot below through `BrewedWaterQuality` with the grade step 5
  takes off the source: the cauldron's grade, or the world sample at the waterlogged block. Targeting a
  method another mod's mixin merges into Tavern's class is fragile: the name `kcf$fillCookery` can change
  in any release without notice. Probe it the way `TeapotDripstoneMixin` probes a method only some
  builds have, and let a missing method skip the mixin rather than fail the load.
- Lives in the Tavern directory, since it only exists with Tavern installed, and needs the addon as a
  `compileOnly` dependency on `1.21.1-neoforge` and `1.20.1-forge` only.

**Check:** with Cookery, Tavern and the addon, a tap on a Dirty cauldron over an empty stockpot gives
back a Dirty bucket; a tap on a waterlogged slab in the ocean over an empty teapot gives a salty bucket
and brews no tea.

## 6. Decision: the placed water bottle

The `water_bottle` block has no block entity, so there is nowhere on the block to keep a grade, and its
loot table drops a plain `Potions.WATER`. Today that turns any bottle Clean and fresh: a Dirty bottle
placed and picked up again is Clean, and a sea-water bottle becomes drinkable. Options:

- **(a) Keep the grade beside the block**, in a per-level `SavedData` map from position to stored value,
  written when the event places a stamped bottle (and when the tap fills one, step 5), read and removed
  when the block drops its bottle (a mixin on the drop path of `BottleBlock` for the `water_bottle`
  block only). Exact, but it is a second store with its own cleanup: pistons, explosions, `/setblock`
  and a block replaced without dropping must all clear their entry, or an old grade is stamped on a new
  bottle.
- **(b) Refuse to place a bottle whose grade is not the default**, with an action bar message, by
  cancelling the placement before the mod's handler runs (NeoForge: a higher-priority
  `RightClickBlock`; Fabric: an earlier `UseBlockCallback`; Forge the same as NeoForge). Simple and
  never wrong, but a player with Pure water can no longer decorate with it, and every bottle filled from
  the world is stamped, so in practice it disables the feature.
- **(c) Stamp the dropped bottle as the worst it could have been**: Dirty, salty never. Simple and never
  launders, but punishes a Pure bottle put on a shelf.
- **(d) Accept the loss** and say so in the docs.

**Decided: (a)**, scoped to the one block, in `platform/SavedPositions`. Read and removed in
`BlockBehaviour.getDrops`, which every drop goes through (breaking, explosions, pistons, picking up by
hand); a bottle removed without a drop (creative, an arrow, `/setblock`) leaves its entry, which only a
command-placed bottle in the same place can meet, since shift-use and the tap write the entry afresh.
**Recommended: (a)**, scoped to the one block. It is the only option that keeps what the player put
down, and the tap needs somewhere to write to anyway. If it proves fragile in step 3's testing, fall
back to (c) for sea water only (a placed sea-water bottle comes back salty) and accept the fresh-water
loss.

## 7. Decision: sea water in the barrel and the shaker

Every other brewing block refuses sea water or brews nothing from it: Brewin' and Chewin's keg refuses
it, Kaleidoscope Cookery's teapot brews nothing. Here the barrel would ferment sea water and sugar cane
into perfectly safe rum.

- **(a) The barrel refuses sea water**, from a bucket or a canteen, with the action bar message the keg
  uses. In `addFluid`, before the call, since we read the stack there anyway. **Recommended**, for
  consistency. **Decided and built**, with the shaker refusing sea water too.
- **(b) Sea water ferments into vinegar**, the mod's own result for anything no recipe matches, by
  making the recipe lookup miss when the held grade is salty. Fits the mod, but it hides the reason from
  the player.

For the **shaker**, refuse a sea-water bottle as an ingredient (the mod's `ShakerItem` stores three
items; refuse it where it takes one), and accept fresh water of any grade.

**Fermented drinks and cocktails are safe whatever fresh water went in**, as Brewin' and Chewin's are:
the drink is its own item with its own value, alcohol and weeks in a barrel stand in for boiling, and
tracking a grade through a brew would need a component on every drink the mod makes.

## 8. Jade line on the barrel (optional)

Both builds have a Jade plugin with a `BarrelComponentProvider`. Add our own reader as
`KaleidoscopeJade` does for the teapot: the grade under the crosshair while the barrel holds water, from
`BarrelWater`. Client source set, `src/client/kaleidoscopetavern`, registered through the row's `jade`
entrypoint on Fabric and the mod's own plugin hook on NeoForge and Forge. Skip it on `1.21.1` Fabric if
Refabricated has no Jade plugin there.

## 9. Docs

- `src/main/kaleidoscopetavern/AGENTS.md`, in the shape of `src/main/kaleidoscope/AGENTS.md`;
- a row in the root `AGENTS.md` integration table, its mixin config in the "own configs" sentence, and
  this file's row under "Where to look" and in [README](../README.md) moved to `done`;
- `docs/docs/integrations/kaleidoscope-tavern.md` for players, through the `write-docs` skill: which
  versions, what restores how much, that the barrel and the tap keep the water's grade, and what
  happens with sea water. Other versions ignore Kaleidoscope Tavern, which has no build for them;
- the CHANGELOG entry;
- the Modrinth and CurseForge pages: Kaleidoscope Tavern is of the Kaleidoscope family, so it goes on
  them, as Kaleidoscope Cookery is;
- `docs/dev/MANUAL-TESTING.md`: one line per behaviour of steps 4 to 7.

## 10. Optional seam

- `checkOptionalSeam` finds the gate, the mixin plugin and the Jade class loaded without the mod, and
  passes.
- `runGametest` passes unchanged on every node: the mod is never on its classpath.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl` with the mod, with
  `-PwithoutOptional=kaleidoscope_tavern`, and with `kaleidoscope_tavern,jade`, on `1.21.1-neoforge`,
  `1.20.1-forge` and one Fabric node, comes up and stays up.
- `tools/agent/integrations/kaleidoscope-tavern.jsonl` (and a `-1.20.1` copy in that version's NBT, as
  Kaleidoscope Cookery has), every `execute` line asserting its own "Test passed": steps 2, 4, 5, 6 and 7
  in order, plus two Jade screenshots. Make its world with `tools/agent/new_world.py`.

## Risks

- **Refabricated's `onConsumed`** may eat our canteens and waterskins on Fabric (step 3.2). If it does,
  that is a loss of the player's item, not just a grade, and has to be refused before anything else in
  this file ships on Fabric.
- **The multiblock.** The barrel is eight blocks with one block entity; the mixin must stamp through the
  one `BarrelBlock.getBarrelEntity` returns, never through the block clicked.
- **`1.21.11` trails** the other Fabric builds (`1.2.0.6` against `1.2.0.12`); its branch takes hotfixes
  only. Check each target there separately before assuming it matches.
- **Step 6 (a)** is a new kind of store in this mod. If it lands, its cleanup paths get a gametest each.

## Not doing

- **Intoxication and thirst** (drunk effects raising the drain, a hangover): as in Brewin' and Chewin',
  it changes balance for every player of both mods. Not planned.
- **The pressing tub and juice fluids**: no water goes in; the juice buckets are drinks, covered by step 2.
- **The tap's other sources** (beehive, dragon head, lava cauldron, watermelon, barrel): no water.
- **Kaleidoscope Tavern Vanilla Enhanced**, **Kaleidoscope World Liquor** and other addons: separate
  mods, each its own decision when someone asks.
- **NeoForge 26.x**, until the official mod publishes one.
