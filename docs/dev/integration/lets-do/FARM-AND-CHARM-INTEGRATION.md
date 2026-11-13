# Let's Do: Farm & Charm integration plan

What ThirstWasTaken2 should do with [Let's Do: Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm)
(mod id `farm_and_charm`, package `net.satisfy.farm_and_charm`), the farming mod of the Let's Do
collection: crops, a Cooking Pot, a Stove, a Timber Well, Water Troughs and herbal teas. This file sets
the order of work, what each step needs and how each one is checked. Once the work is built, how it
works goes where the steps below say.

Written on 2026-10-01 from:

- the repository [Let-s-Do-Collection/FarmAndCharm](https://github.com/Let-s-Do-Collection/FarmAndCharm),
  branch `1.21.1` at `8c0b772` (2026-09-24), which is the released `1.1.26`;
- the Modrinth project `HJetCzWo`.

## Which build for which node

**Only the two 1.21.1 nodes.** The 1.20.1 branch stopped at `1.0.14` (2025-10-09, Fabric only) and is
not updated any more, so `1.20.1` and `1.20.1-forge` are left out on purpose, and there is no build
newer than 1.21.1.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `1.1.26` Fabric | `isj5qmqa` | Architectury API (built against `13.0.8`; newest `13.0.11+fabric`, `Pzc2FP5K`), Cloth Config (we already pin `15.0.140+fabric`) |
| `1.21.1-neoforge` | `1.1.26` NeoForge | `Y8Uh3mlO` | Architectury API (`13.0.11+neoforge`, `1IiqEQGl`) |
| every other node | none | — | — |

The mod is written once in Architectury's `common` module, so the classes the integration touches
(`TimberWellBlock`, `WaterTroughBlock`, `SinkBlock`, the cooking block entities) have the same names and
package on both loaders. One integration directory serves both, like Kaleidoscope Cookery.

## What Farm & Charm already does for thirst

Nothing. Its jar names no thirst mod, and none of its items is in `c:drinks`. Today every one of its
teas and soups restores nothing with us; only keyword matching would reach them, and that is off by
default.

## Where water lives in Farm & Charm

| Block / path | How water gets in | How it gets out | What happens to the grade today |
|---|---|---|---|
| **Timber Well** (`timber_well`, 3 blocks, `LEVEL 0..3`) | right-click with an empty hand pumps one level, **only if a water source block lies within 6 blocks below** the well's footprint (3x4 area); rain adds a level, 1 in 6 random ticks | an empty bucket takes one level → `minecraft:water_bucket` (`ItemUtils.createFilledResult`) | the bucket is unstamped, so it reads as `defaultPurity` (Clean). The groundwater source is **not consumed**: one source block under a well is endless water. Sea water under a beach well comes out Clean |
| **Water Trough** (`water_trough`, connects in a line, `LEVEL 0..3`) | a water bucket fills the whole connected line; rain adds a level, 1 in 18 random ticks | an empty bucket → `water_bucket`. In a line of 3 or more, **every trough but the two ends is an endless source** (`isInfiniteSource`) | the trough keeps only a level, so **any bucket poured in comes back out Clean**: a Dirty or salty bucket is laundered to Clean by one round trip |
| **Cooking Pot** (`cooking_pot`, needs heat under it) | recipes take `#farm_and_charm:water_bottles` → `#c:water_bottles`, which Farm & Charm fills with **`minecraft:water_bucket` only**; the bucket comes back as the remainder | the teas (a jug, in a glass bottle container), `barley_soup`, `yeast` | the bucket is matched by item, so **any grade and sea water** make tea. The tea itself carries no grade |
| **Stove** (needs fuel) | `farmers_bread`: dough plus `#farm_and_charm:water_bottles` | bread | as the pot; bread is not a drink, so only salt water matters, and barely |
| **Crafting Bowl** (no heat) | `dough`: flour, yeast, `#farm_and_charm:water_bottles` | dough | as the pot; dough is not a drink and is baked afterwards |
| Tea Jug (placed) | — | a glass bottle takes a cup, two cups per jug | no water: the jug already is tea |
| Water Sprinkler, Pet Bowl, Feeding Trough | — | — | for crops and animals; nothing a player drinks |

`SinkBlock` exists in the source but **Farm & Charm does not register it**. Its addon
[Candlelight](CANDLELIGHT-INTEGRATION.md) does, as eleven kitchen sinks, and it is the worst case of all:
an empty hand fills it from nothing and a glass bottle draws plain `Potions.WATER`. The sink mixins
are Farm & Charm's classes and so live in `src/main/farmandcharm`, but they are planned, decided and
checked in the Candlelight plan, since only Candlelight puts a sink in the game. Candlelight's Large
Cooking Pot and stoves also run Farm & Charm's recipes; step 6 below covers them.

Because `#c:water_bottles` holds only the bucket, **a vanilla water bottle, our waterskin, canteen,
flask and bowls are not accepted** by any of these recipes. That is Farm & Charm's choice and stays
(see Not planned).

## The drinks and foods

In `ThirstConfig` by id, like Fruits Delight and Brewin' and Chewin': a `farmAndCharmDrinks` /
`farmAndCharmFoods` pair merged with `putMissing`. That reaches every node, matches nothing where the
mod is absent, and needs no code of the mod's. Proposed (thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Tea, a cup (glass bottle back) | `strawberry_tea_cup`, `nettle_tea_cup`, `ribwort_tea_cup` | 6, 9, as Kaleidoscope Cookery's teas |
| Tea, the jug drunk from the hand | `strawberry_tea`, `nettle_tea`, `ribwort_tea` | 6, 9: one serving. Placed, the same jug pours two cups, so drinking it whole is the wasteful way, and is not rewarded for it |
| Soups, a bowl | `barley_soup`, `onion_soup`, `potato_soup`, `simple_tomato_soup`, `goulash` | 4, 5, as Farmer's Delight's soups and stews |
| Porridge, a bowl | `corn_grits` | 3, 4 |
| Salad, a bowl | `farmer_salad` | 4, 5, as Farmer's Delight's mixed salad |
| Fruit and vegetables | `tomato` | 2, 3, as Farmer's Delight's tomato |
| | `lettuce` | 1, 2, as Farmer's Delight's cabbage leaf |
| | `strawberry` | 1, 2, as sweet berries |
| Left out | `oatmeal_with_strawberries` (dry oats, no water in the recipe), `corn`, `onion`, `oat_pancake`, the roasts, patties, breads, cakes, pasta, `cooked_salmon`, `cooked_cod`, the pet and animal foods, `rotten_tomato` | — |

The ids come from the mod's `en_us.json` and `ObjectRegistry`. Step 3 confirms each one resolves.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | both 1.21.1 | **Done** (2026-10-01) |
| 2 | Thirst values for the drinks and foods | data | all (config) | **Done** (2026-10-01), values as proposed |
| 3 | What happens to a grade, per path, in game | investigation | both 1.21.1 | **Done** (2026-10-01) |
| 4 | Timber Well water gets a grade | decision, then code | both 1.21.1 | **Decided** (2026-10-01): (a), and **done** |
| 5 | Water Trough stops laundering water | decision, then code | both 1.21.1 | **Decided** (2026-10-01): (c), and **done** |
| 6 | Sea water refused in the Cooking Pot | code | both 1.21.1 | **Done** (2026-10-01), Stove and Crafting Bowl too |
| 7 | Nothing crashes without the mod | test | both 1.21.1 | **Done** (2026-10-01) |
| 8 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-01) |
| 9 | No upstream change can stop the game | code | both 1.21.1 | **Done** (2026-11-13), after #56 |

How it works now is in [src/main/farmandcharm/AGENTS.md](../../../../src/main/farmandcharm/AGENTS.md); what
follows is what each step did and found.

## 1. The mod on the `runClient` classpath (done)

`deps.farm_and_charm`, `deps.candlelight` and `deps.architectury` in `[fabric."1.21.1"]` and
`[neoforge."1.21.1"]`, pinned by Modrinth id, and all three in `MODRINTH_DEPS`. A row in the integration
table (`farmandcharm`, both loaders). Farm & Charm is `modCompileOnly` on Fabric, since it is mixed into,
and `compileOnly` on NeoForge; Candlelight and Architectury are on `runClient` only. Neither jar nests
anything, and Farm & Charm's Fabric manifest asks for Architectury alone, not Cloth Config.
`-PwithoutOptional=farm-and-charm` leaves all three out; `-PwithoutOptional=candlelight` leaves only
Candlelight out. A first cut tied Architectury to Candlelight's names, so leaving Candlelight out
crashed NeoForge on Farm & Charm's missing dependency; the boot smoke found it.

## 2. Thirst values (done)

`farmAndCharmDrinks` and `farmAndCharmFoods` in `config/itemvalues/LetsDoItemValues`, the table above, merged with
`putMissing`. `farmAndCharmDrinksAreMergedIntoAnOlderConfig` checks them and Candlelight's together.
`runGametest` passes (196) on `1.21.1` and `1.21.1-neoforge`.

## 3. Investigation (done)

[tools/agent/integrations/farm-and-charm.jsonl](../../../../tools/agent/integrations/farm-and-charm.jsonl)
passed whole on `1.21.1` and `1.21.1-neoforge` (Cold Sweat left out) on 2026-10-01, after steps 4 to 6,
so its cases are written for the finished behaviour:

| Case | Found |
|---|---|
| Every id in step 2, and the blocks | all exist; `farm_and_charm:sink` does not |
| A well over a source, swamp / plains / beach | Dirty / Murky / sea water, the level going down by one each draw |
| A well with no source below | Clean, rain's grade |
| A trough: salty bucket, then Pure bucket, then draw | the salty one refused and kept; the Pure one fills it; the draw is Murky |
| The cooking blocks with a plain and a salty bucket | the plain ones make nettle tea and farmers bread, the salty ones nothing, the bucket left in its slot |
| Drinking and eating from thirst 4 | the tea cup and the jug 10, barley soup 8, tomato 6 (barley soup 10 since the values were rebalanced on 2026-10-21: soups 6, 4) |

What the code survey had right: the tag holds only the water bucket, so bottles and canteens are not
accepted, and the trough and the well never kept a grade. What it had wrong: the cooking blocks do
**not** match through `GeneralUtil.matchesRecipe`. Each has its own matcher, which is where step 6 went.

## 4. Decision: Timber Well water (done, (a))

`TimberWellMixin` wraps the `ItemUtils.createFilledResult` call in `useItemOn` and stamps the bucket with
`StandingWater.wellWater`: the first groundwater source in the box `hasGroundwater` scans, sampled with
`WaterPurity.sampleAt`, or `rainwaterPurity` when there is none. With rain collection off a rain-only
well leaves the bucket unstamped. The other options, kept for the record: (b) a fixed Clean, which would
have let a well skip purifying anywhere; (c) nothing.

## 5. Decision: the Water Trough (done, (c))

`WaterTroughMixin`: a salty water bucket is refused at the head of `useItemOn`, and what goes through
`setItemInHand` is stamped Murky (`StandingWater.STANDING`) if it is water. The empty bucket left after
pouring is no water container and is untouched. The grade is a constant, not a config field. Rejected:
(a) a `purity` property on the trough, five injection points into another mod's block for a block
meant for animals; (b) refusing everything worse than Clean, which tells a player no without saying
why.

## 6. Sea water in the cooking blocks (done)

`SeaWaterIngredientMixin` wraps every `Ingredient.test` in the matchers of the Cooking Pot
(`matchesInventory`), the Stove (`findBestMatchingSlot`, `countMatchingSlots`; Candlelight's stoves
extend it) and the Crafting Bowl (`matchExact`), and in Candlelight's Large Cooking Pot, named by
string. A salty stack matches nothing. The plugin checks each target's own method names; with
Candlelight absent its target is skipped and the rest apply.

## 7. Optional seam (done)

`checkOptionalSeam`, `checkLoaderSeam`, `checkVersionSeam`, `checkLang` and `checkDataConditions` pass on
both nodes. `tools/agent/smoke/boot.jsonl` comes up and stays up on both nodes with
`-PwithoutOptional=farm-and-charm` and with `-PwithoutOptional=candlelight`.

## 8. Docs (done)

`CHANGELOG.md` (Unreleased), the site's page
[integrations/farm-and-charm](../../../docs/integrations/farm-and-charm/index.md) and its addon page [candlelight](../../../docs/integrations/farm-and-charm/candlelight.md), with four pictures (two scenes, and two first-person shots of a bucket and a bottle just drawn, their grade in the tooltip)
taken on a lakeshore in `ShotsAgent` (Fabric 1.21.1), its sidebar entry through `.vitepress/mods.ts`,
rows in `docs/docs/installation.md`, a row with the well picture in the Modrinth and CurseForge main
tables and a line in their versions tables, the root `AGENTS.md`, `WATER-REFERENCE.md` and
`SALT-WATER-REFUSALS.md`.

## 9. Failing soft (done, 2026-11-13)

Issue #56 crashed Fabric at launch because an injector with the config's `defaultRequire` 1 met a method
Candlelight had removed. The Timber Well, Water Trough and sink mixins were the same shape, guarded only
by their class being there. Now every injector is `require = 0`, and `FarmAndCharmMixinPlugin` reads each
target's class file before Mixin transforms it: a block with no `useItemOn` (nor `method_55765`, its
intermediary name in a Fabric player's game) is skipped, and a call a hook sits on that is gone from it
(`createFilledResult`, `setItemInHand`, `Inventory.add`, `Player.drop`, `Ingredient.test`) is logged,
the rest of the mixin still applying. Both warnings say "not a version ThirstWasTaken2 supports". A
first cut looked for the handlers in `postApply` instead, and warned wrongly for every
`@WrapOperation`, which MixinExtras applies after it.

Checked on a production Fabric 1.21.1 server with the built jar, Farm & Charm 1.1.29 and Candlelight
2.1.15 and `-Dmixin.debug.export=true`: no warning, and every hook in the exported classes. With a
Farm & Charm jar edited so that Foundation's sink has no `method_55765` and the well no longer calls
`createFilledResult`: one warning each, the sink left untransformed, and the server starts.

## Not planned

- **1.20.1, Forge, and any version past 1.21.1.** The 1.20.1 build is no longer updated, and there is
  nothing newer.
- **Our containers in `#c:water_bottles`.** Adding `minecraft:potion` would let every potion count as
  water; adding the waterskin and canteen would consume them whole in the pot. Farm & Charm chose the
  bucket, and it stays.
- **A grade on the teas or soups.** They are heated and are their own items; like Farmer's Delight's
  cider, they quench at a fixed value.
- **Animals drinking from troughs and wells** (`TimberWellBlock.drink`). Not ours.
- **The Sink here.** Farm & Charm does not register it; Candlelight does, and
  [its plan](CANDLELIGHT-INTEGRATION.md) handles it.
