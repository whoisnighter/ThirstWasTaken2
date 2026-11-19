# purity/

Everything about sampled water quality.

`WaterQuality` is a **sealed** interface with two cases: `Fresh(purity)`, graded `0..3` (dirty,
murky, clean, pure), and `Salt`. Sea water is a different kind of water, not a low grade of fresh
water - cooking cannot improve it, one salty serving spoils a whole batch, and it never hydrates.
Sealing it is the point: with only two cases, code asks `quality instanceof WaterQuality.Fresh fresh` and
its `else` is salt water, never "any other grade", which is what stops salt water from quietly inheriting
a grade's tooltip, sprite or effects. (A pattern `switch` would say so to the compiler, but it is Java 21,
and core stays on Java 17 for 1.20.1; see the root `AGENTS.md`.)

| File | Owns |
|---|---|
| `WaterQuality` | the sealed pair, `Fresh` or `Salt` |
| `WaterPurity` | environmental sampling, storage, sickness, sprites and container tests |
| `WaterInteractions` | the interaction callbacks that move quality between world, blocks and items |
| `SourceDrawing` | finite water: whether scooping from the world takes the source (`waterFillTakesSource`, the `waterSourceConversion` game rule), told to clients by its own payload so both sides cast the same ray; bottles left to Thirsty Bottles when it is installed. The client side is checked by `tools/agent/gameplay/finite-water.jsonl` |

## Where a quality can live

| Carrier | Storage | Read with |
|---|---|---|
| Item stack | `water_purity` for a grade, `water_salty` for sea water | `WaterPurity.quality(stack)` |
| Cauldron | one `purity` blockstate value | `WaterPurity.storedQuality(state)` |
| Copper hanging pot | the same property, unset while empty | `HangingPotBlock.quality(state)` |
| Water in the world | biome baseline plus small local modifiers | `WaterPurity.sampleAt(level, pos)` |
| A NeoForge `FluidStack` | one component: `water_purity`, or `water_salty` for sea water | `WaterFluids.quality(stack)`, in `src/main/neoforge` |
| A Moonlight `SoftFluidStack` | the same one component, in a Supplementaries jar, goblet or faucet | `SoftFluidQuality.quality(stack)`, in `src/main/supplementaries` |
| Anything unstamped | `ThirstConfig.defaultPurity`, fresh | falls out of `quality` |

The cauldron value is `0` for unset, `1..4` for the four grades (offset by one so that zero can mean
"nothing poured in yet") and `5` for salt water. It is one property rather than a grade plus a
boolean flag on purpose: **vanilla hands a freshly placed block the first value of every property it
carries, and for a boolean that value is `true`**, so a separate salinity flag makes every new
cauldron read as sea water. `WaterPurity.storedValue` and `storedQuality` are the only two places
that know the encoding.

## The carried containers and the bowls as fluid containers

On NeoForge the waterskin, copper canteen, iron flask, terracotta bowl and terracotta water bowl carry the item fluid
capability, so other mods' pipes, tanks and pumps can fill and empty them: a Create Spout or Item
Drain, a Sophisticated Tank or Pump upgrade. `WaterContainerFluids` in `src/main/neoforge` holds the
rules, and each of NeoForge's two fluid APIs gets a thin handler over them, chosen by the build:

| NeoForge | API | Directory |
|---|---|---|
| 21.1 (Minecraft 1.21.1) | `IFluidHandlerItem`, `Capabilities.FluidHandler.ITEM` | `src/main/neoforge-fluidhandler` |
| 21.11 and later | the transfer API, `ResourceHandler<FluidResource>`, `Capabilities.Fluid.ITEM` | `src/main/neoforge-transfer` |

- **Only whole servings of 250 mB move**, a bottle's worth. A request is rounded down, so 300 mB moves
  one serving and 100 mB moves nothing. The transfer API's base class would refuse a part serving
  outright, so its handler rounds before calling it; both behave the same.
- **A container that holds water only takes more of the same grade.** That is every tank's rule for two
  fluids with different components. Mixing grades stays what pouring by hand does.
- **Water with no grade**, from a mod that does not know about quality, fills an empty container as
  `defaultPurity`, like any unstamped container.
- The handlers answer with the stack as it is afterwards. The transfer handler swaps it through its
  `ItemAccess`, so a filled empty bowl becomes a water bowl in whatever slot the access allows.

On Fabric the same five items are Transfer API fluid storage (`FluidStorage.ITEM`), through
`WaterContainerStorage` in `src/main/fabric`. That is what a Create Fly Spout fills and an Item Drain
empties, through Create Fly's own bridge to the Transfer API. The rules are the same, and so is the
serving: 250 mB, 20250 droplets, not Fabric's 27000-droplet bottle, so both loaders move the same
water. The grade rides on the `FluidVariant` as one component, as on Create's fluid stacks;
`platform/FabricTransfer` alone reads and writes it.

What both loaders share, counted in servings, is `item/WaterContainers`. `ContainerFluidGameTest`
checks all three implementations against one set of assertions, in millibuckets.

## The copper distiller as a fluid block

The distiller answers each loader's block fluid capability, so another mod's pipes and pumps (Pipez,
issue #65) fill its boiler and empty its basin. Tank 0 is the boiler: water of any grade in, mixed to
the worse as a pour mixes, nothing out. Tank 1 is the basin: Pure water out, stamped Pure, nothing in.
Unlike the carried containers, **any amount moves**, since Pipez's slowest pipe moves 50 mB a tick: the
machine counts millibuckets (`boilerMb`, `basinMb`, 250 to a serving) and boils only whole servings.

| Loader | Handler | Halves |
|---|---|---|
| NeoForge 21.1 | `neoforge-fluidhandler/.../DistillerFluidHandler` | both |
| NeoForge 26.x | `neoforge-transfer/.../DistillerResourceHandler`, one per machine for its journal | both |
| Forge 47 | `forge-fluidhandler/.../DistillerFluidHandler`, on the block entity | boiler only |
| Fabric | `fabric/.../DistillerStorage`, one per machine for its snapshot | both |

`fillBoiler` and `drainBasin` neither save nor sync; each handler calls `fluidsChanged` once a move is
final, so a simulated or rolled-back move leaves nothing behind. `DistillerFluidGameTest` checks all
four. The plan is `docs/dev/integration/storage/PIPEZ-INTEGRATION.md`.

## Rules the code keeps

- **Salt water carries no grade.** `setQuality`, through `ItemWaterData.setSalty`, removes `water_purity` from a salty stack. That is
  what keeps the purification recipes, which all match on a grade, from matching sea water, and
  what stops `get` from inventing one. Anything asking "how clean is it" goes through `quality`.
- **A fresh container always writes `water_salty: false`,** even though false is the component's
  default. Every cooking recipe matches on it, so a container that leaves it out silently stops being
  cookable - which is exactly what went wrong with looted water bottles once, and what
  `PurificationGameTest` now watches.
- **Sprites are part of the contract, not decoration.** `syncModel` runs inside `setQuality`. The
  mod's bowl switches on custom model data index 1 (`0..3` grades, `4` salt). Vanilla's bottle and
  bucket keep vanilla's sprite whatever they hold, on every version, and only the tooltip says Salty:
  1.21.1 and 1.20.1 have no `minecraft:item_model` component to swap it with, so swapping it on the
  later versions only made the nodes look different.
- **Waterskin mixing is serving-weighted and rounds down**, so one clean mouthful cannot talk a batch
  up a grade. Salt is not averaged at all: a single salty serving turns the whole skin into sea
  water. Cauldrons cannot average, because their blockstate has room for one value, so they keep the
  worse of what they hold and what is poured in, through `WaterQuality.worse`, which the hanging pot
  shares.
- **Water that arrives on its own is graded where it lands.** Rain and pointed dripstones fill
  cauldrons with nobody pouring anything in, so `filledByRain` and `filledByDripstone` stamp
  `WaterPurity.rainwaterQuality()` (Clean by default) and `dripstoneQuality()` (Pure), both from the
  config, rather than letting the cauldron fall through to `defaultQuality`. With
  `enableRainCollection` off, `filledByRain` does nothing and pots ignore rain. Both keep the worse of what the cauldron held and what fell in, like pouring,
  and both check that the blockstate actually changed: the vanilla hooks run whether or not a layer
  was added.
- **Sampling is interaction-only and server-only.** The fixed 5x3x5 block inspection must never move
  into a tick or item tooltip path. Ocean and beach biomes return `Salt` before that scan runs. Bottle
  and bucket mixins skip sampling on the prediction client. There are two exceptions. The Jade overlay
  (`client/compat/JadeIntegration`) samples the single block under the crosshair on the client, at
  most every 10 ticks per block. It shows the same grade the server would stamp because `sampleAt`
  reads nothing the client lacks: blockstates, the biome, fluid tags and biome tags. A fluid in
  `thirstwastaken2:pure_water` (Spelunkery's Spring Water) is Pure before any of that. **Keep it that way** - a new
  input that only the server knows would make the overlay lie. A Create Fly pump or Hose Pulley
  collects water on a tick, so it reuses one sample per pump for 100 ticks
  (`src/main/createfly/.../SampledWater`), and so do Create on NeoForge and Sophisticated's Pump
  upgrade (`src/main/neoforge/.../SampledWater`).
- **Heat stops at Clean.** `WaterPurity.BOILED` is the cap, and `boil` (one go) and `boilStep` (+1, Cold
  Sweat's Boiler) are the only places that know it; every heat source calls one of them. Only the
  distiller and the Sand Filter reach Pure. Water the sky can't reach is capped at Murky as the last,
  costliest step of `sampleAt` (`openToTheSky`, through `Vanilla.skyAbove`), and only for world water.
- **Water bottles do not stack.** The rework first stacked them to three, then took it back
  (2026-10-06): a potion that stacks surprises other mods' brewing and storage code. Filled
  terracotta bowls stack instead, from `terracottaWaterBowlStackSize` on the item's properties.
- **The contamination score is never stored.** `sampleAt` scores a source, grades it, and keeps only
  the grade, so no container carries a hidden number that the player cannot see and the tooltip
  cannot explain.
- **`isWaterContainer` is per stack, not per item.** Water bottles are plain `minecraft:potion` stacks
  distinguished only by their `POTION_CONTENTS`, and an empty waterskin is not a container. The
  `INFO` cache only answers the per-`Item` half of the question.
- **`INFO` caches forever.** Only put facts in it that cannot change at runtime. Config-dependent
  purity is stored as the sentinel `PURITY_FROM_CONFIG` (`-1`) and resolved on each call.
- **Optional mod support is by registry id only.** `resolve` matches namespaces (currently
  `farmersdelight`, `cold_sweat`, Miner's Delight's two, `minersdelight` and `miners_delight`,
  `bountifulfares` and `meadow`) as strings - no class is ever referenced, so no such mod is a dependency. Add support by
  extending `resolve`, not by importing anything.
- **The sickness tables, by difficulty.** `applyEffects` hands fresh water to `effect/WaterSickness`
  and always returns true: every fresh drink quenches, the illness is the price. `WaterSickness` gives
  each effect the config's `sicknessEffects` lists for the difficulty and the grade. Lines with the same
  `group` share one roll per drink, the rest roll on their own; by default Upset Stomach and Poison
  share `illness` for Dirty and Murky water, Clean and Pure have no lines, and there is no taste
  Nausea. The difficulty is read at the drink. Drinking again while ill extends Upset Stomach by half
  the new time up to 1.5 times it, any other effect by the line's time up to twice it, while
  `extendSicknessEffects` is on. The original also applied Hunger; the
  defaults never have. Salt water never reaches the
  roll: it spends exhaustion, applies Nausea and Parched II (without particles) for the config's
  `seaWaterNauseaSeconds` and `seaWaterParchedSeconds`, and returns false. With `enableSeaWater` off,
  `sampleAt` grades ocean and beach water like any other.
  `quenched` cuts what a drink of water quenches by the config's `quenchedPercent` (Dirty none, Murky
  a quarter, Clean half by default); tooltips show the grade's cut. Upset Stomach no longer cuts it.
  The design is `../../../../../../docs/dev/mechanics/WATER-SICKNESS.md`, and where its code goes
  `../../../../../../docs/dev/mechanics/WATER-SICKNESS-IMPLEMENTATION.md`.
- **`purityKey` and `purityColor` own the lang key and the colour together**, and they are the only
  palette for water quality; the tooltip tiers in `src/main/java/com/thirstwastaken2/AGENTS.md` say
  where those colours sit among the other lines. Adding a grade means
  Adding a grade means touching both switches plus `thirst.purity.*` in all nine lang files. The
  grade colours run warm to cool so that all four stay apart on a dark tooltip, and salt's line sits
  off that ramp entirely, in the pale cream of dried salt. Its bowl sprite is turquoise instead, deep
  enough that a bowl of sea water is not mistaken for the light blue of a pure one.

## Why interactions are deferred

Vanilla resolves a cauldron fill or drain *after* our `UseBlockCallback` returns, so
`transferCauldronPurity` cannot read the result inline. It computes the value, returns `PASS` (so the
vanilla interaction still happens and the later callbacks still run), and queues a `Runnable` on
`END_OF_TICK`, drained by `WaterInteractions.tick` on the same server tick. The queue is an
`ArrayDeque` with no locking - **server thread only**.

The callback also runs before anyone knows whether the cauldron accepts the container, and many are
not accepted: a bottle on a full cauldron, a bucket on one that is not full, a terracotta water bowl on
any (it has no cauldron interaction at all), Miner's Delight's water cup on a full one. Two checks keep a
refused use from storing or stamping a grade. `transferCauldronPurity` queues nothing unless vanilla's
interaction maps, which mods add to, hold an interaction for the item (`Vanilla.cauldronHasInteraction`).
The queued transfer then runs only if the cauldron's blockstate or the stack in the hand changed by the
end of the tick (`interacted`). The first is needed because the hand alone lies: the water cup, refused,
goes on to place its water in the world in the same tick. A creative refill of a full cauldron that was
unset changes neither, so it is not graded; nothing else real is missed. `CauldronGameTest` drives these
through `gameMode.useItemOn`, so vanilla really runs.

Draining is messier than filling: the filled container does not have to end up in the interaction
hand (a stacked glass bottle sends the water bottle to the first free slot), so
`stampDrawnContainer` stamps the first freshly created container that is not stamped yet.

Waterskins are excluded from `transferCauldronPurity` on purpose - they draw through
`fillWaterskinFromCauldron`, and vanilla has no interaction that could pour one back, so a scheduled
transfer would be a phantom.
