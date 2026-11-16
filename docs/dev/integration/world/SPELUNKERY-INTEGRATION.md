# Spelunkery integration plan

What ThirstWasTaken2 should do with [Spelunkery](https://modrinth.com/mod/spelunkery) (mod id
`spelunkery`, package `com.ordana.spelunkery`), a cave overhaul: new ores and tools, rock salt, sluices
and channels, and Spring Water, a glowing fluid that pools in mountains. This file sets the order of
work, what each step needs and how each one is checked. Once the work is built, how it works goes in
[src/main/spelunkery/AGENTS.md](../../../../src/main/spelunkery/AGENTS.md).

Written on 2026-10-03 from:

- the repository [Silversmith-Mods/Spelunkery](https://github.com/Silversmith-Mods/Spelunkery), branch
  `1.21.1` at `105af13`, and the released `1.21.1-0.4.4` jars;
- the Modrinth project `spelunkery`.

## Which build for which node

**Only the two 1.21.1 nodes.** The 1.20.1 line stopped at `0.3.16` (2025-01-25) and is not updated any
more, and there is no build newer than 1.21.1.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `1.21.1-0.4.5` Fabric | `EoItgBRY` | Moonlight Lib, already pinned for Supplementaries |
| `1.21.1-neoforge` | `1.21.1-0.4.5` NeoForge | `A9ac6DN7` | Moonlight Lib, already pinned |
| every other node | none | — | — |

## What Spelunkery already does for thirst

Nothing. Its jar names no thirst mod. It has no drinks and no food that quenches.

## Where water lives in Spelunkery

| Block / path | Water in | Out | What happens to the grade today |
|---|---|---|---|
| **Spring Water** (`spelunkery:spring_water`, `flowing_spring_water`) | worldgen: `spring_water_pool` (lakes rimmed with smooth basalt) and `spring_water_spring`, in `#minecraft:is_mountain`, from y 100 up | — | it is in `#minecraft:water`, so a bottle, bowl, waterskin and a sip by hand take it as water. `sampleAt` grades it by biome like any mountain water: usually Clean, sometimes Murky |
| Spring Water with a bucket | the fluid | `spring_water_bucket` | the bucket holds the fluid itself, not our water: it carries no grade and is not drunk, so nothing to do |
| **Boiling a water bucket in a furnace** (`spelunkery:salt_from_boiling`, `minecraft:smelting`, ingredient `minecraft:water_bucket` with no components) | any water bucket | `salt_bucket`, which places 8 salt and then becomes an empty bucket | **clashes with our purification.** A fresh bucket below Pure matches both this recipe and our `purify_water_bucket_<grade>_smelting`. Which one the furnace takes depends on recipe order, so a player boiling water may get salt. A Pure fresh bucket, which nothing of ours matches, always turns to salt |
| Salt in a water cauldron over fire (`WaterCauldronBlockMixin`) | a cauldron layer | rock salt, one layer less | uses water up and keeps nothing. Fine |
| Slime block in a full water cauldron | the cauldron | a slime, an empty cauldron | uses water up. Fine |
| Sluice and channels | water flowing over them | loot | water is not collected. Fine |

Salt has other sources: rock salt is mined, and 8 salt plus a bucket craft a salt bucket. So boiling is
not the only way to get it.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | both 1.21.1 | **Done** (2026-10-03) |
| 2 | Spring Water's grade | decision, then data and core | every node (the tag) | **Decided** (2026-10-03): (a), and **done** |
| 3 | Boiling water into salt | decision, then code | both 1.21.1 | **Decided** (2026-10-03): (a), and **done** |
| 4 | What happens, per path, in game | investigation | both 1.21.1 | **Done** (2026-10-03), every case as expected |
| 5 | Nothing crashes without the mod | test | both 1.21.1 | **Done** (2026-10-03) |
| 6 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-03) |

## 1. The mod on the `runClient` classpath

`deps.spelunkery` in `[fabric."1.21.1"]` and `[neoforge."1.21.1"]`, pinned by Modrinth id, and in
`MODRINTH_DEPS`. A row in the integration table (`spelunkery`, both loaders, its mixin config,
`spelunkery` as an optional NeoForge dependency). Nothing is compiled against Spelunkery: the one mixin
targets Minecraft's class and finds the salt bucket by id, so the mod is on `runClient` only.
Moonlight's `runClientMod` lines move out of Supplementaries' block, so either mod brings it.
`-PwithoutOptional=spelunkery` leaves Spelunkery out.

## 2. Decision: Spring Water's grade

- **(a) Always Pure.** A fluid tag, `thirstwastaken2:pure_water`, with Spelunkery's two fluids as optional
  entries, written by a `ThirstFluidTagProvider` in `src/datagen`. `sampleAt` returns Pure for a fluid in
  it, before the sea check and the biome scoring, so a bottle, a bowl, a waterskin, a sip by hand and the
  Jade overlay all agree. Spring Water is rare, deep in mountains, and gives Regeneration: it reads as
  mineral spring water, like the dripstone water that is Pure. A tag, not a class, so a data pack can add
  another mod's fluid. Recommended.
- (b) Always Clean: the same tag, one grade lower.
- (c) Nothing: graded by biome like other mountain water.

The tag is data with optional entries, so it ships on every node, like Terralith's entries in
`stagnant_water`. It does nothing where Spelunkery is absent.

## 3. Decision: boiling water into salt

- **(a) Only sea water boils into salt.** A mixin on `AbstractCookingRecipe.matches`: a cooking recipe
  whose result is `spelunkery:salt_bucket` does not match a stack that is not sea water
  (`WaterPurity.isSalty`). Fresh buckets then match only our purification, and a sea water bucket, which
  nothing of ours takes, boils into salt: evaporating sea water leaves salt behind. With `enableSeaWater`
  off no bucket is salty, so salt comes only from rock salt. Recommended.
- (b) Sea water and Pure water: fresh water below Pure purifies first.
- (c) Nothing: which recipe a fresh bucket hits stays down to recipe order.

Overriding `data/spelunkery/recipe/salt_from_boiling.json` from our jar was ruled out: Fabric API's
resource loader on 1.21.1 (`fabric-resource-loader-v0` 1.3.1) has no load order between mods
(`fabric:resource_load_order` arrived later), so whose file wins is not ours to decide.

The mixin is matched by result, not by recipe id, so a data pack's smoker or campfire copy of the recipe
is covered too.

## 4. Investigation

A script `tools/agent/integrations/spelunkery.jsonl`, written for the finished behaviour:

| Case | Expected |
|---|---|
| `spring_water_bucket`, `salt_bucket`, `salt` exist | all exist |
| A bottle filled from a Spring Water source placed in a plains biome | Pure |
| A Dirty, a Murky and a Clean fresh bucket in a furnace | each one grade up, never a salt bucket |
| A Pure fresh bucket in a furnace | stays in the input slot |
| A sea water bucket in a furnace | a salt bucket |

Run on both nodes on 2026-10-03, every case as expected: the Spring Water bottle came out Pure in
plains; Dirty went to Clean, Murky and Clean to Pure, the Pure bucket stayed in its input slot with no
output, and only the sea water bucket became a salt bucket.

## 5. Optional seam

`tools/agent/smoke/boot.jsonl` comes up and stays up with `-PwithoutOptional=spelunkery`.
`checkOptionalSeam`, `checkDataConditions` and the other checks pass on both nodes, and so does
`runGametest` there and on `26.3.x`, since the fluid tag is read in core code.

## 6. Docs

`CHANGELOG.md` (Unreleased), a site page, its sidebar entry, rows in `docs/docs/installation.md`, the
Modrinth and CurseForge pages, the root `AGENTS.md`, `SALT-WATER-REFUSALS.md` and `WATER-REFERENCE.md`.

## Not planned

- **1.20.1, Forge, and any version past 1.21.1.** The 1.20.1 line is no longer updated. The fluid tag
  ships there anyway, as data.
- **Grading the Spring Water bucket.** It holds Spelunkery's fluid, not water a player drinks.
- **Water from the salt cauldron.** Boiling salt into rock salt uses water up and makes nothing to drink.
