# Water reference

Every way to get water, what each grade of water does when drunk, by difficulty, and how the ways of
cleaning it compare.

**It describes the game as the code stands today**, after the purification rework. The design of the
sickness itself is in [WATER-SICKNESS.md](WATER-SICKNESS.md).

Every number below is a default. Most are in the config (`ThirstConfig`); the ones that are not say so.

## Grades

Fresh water has four grades; sea water is a different kind of water, not a grade.

| Grade | Number | Quenched given (`quenchedPercent`) |
|---|---|---|
| Dirty | 0 | 0% |
| Murky | 1 | 25% |
| Clean | 2 | 50% |
| Pure | 3 | 100% |
| Sea water | salt | none, see [Sea water](#sea-water) |

Anything holding water with no grade stamped on it counts as `defaultQuality`, Clean.

## What a drink gives

A thirst bar is 20. Thirst past a full bar is lost, from every source, and quenched never passes thirst.

| Drink | Thirst | Quenched (before the grade's share) | Time |
|---|---|---|---|
| A serving of water: bottle, Terracotta Water Bowl, Waterskin, Copper Canteen, Iron Flask (`plainWaterValue`) | 6 | 4 | 32 ticks (`plainWaterDrinkTicks`; a bottle is always vanilla's 32) |
| A sip by hand from a water block (`canDrinkByHand`) | 3 | 2 (not in the config) | one click while sneaking |
| A drink by hand from a water cauldron (`canDrinkByHand`), one level | 6 | 4 | one click |
| Any other potion | 6 | 8 | vanilla |
| Prepared soups and non-alcoholic drinks, from any mod | 6 | 4 | |
| Milk bucket | 4 | 0 | |
| Solid food | 0 to 2 | 0 | |

A Clean or Pure serving can be drunk with a full thirst bar while quenched is below 20 (`canDrinkWater(player, stack)`);
Dirty and Murky cannot.

| Carried or stacked | Servings | Thirst when full |
|---|---|---|
| Terracotta Water Bowls | 3 a stack (`terracottaWaterBowlStackSize`); water bottles do not stack | 18 |
| Waterskin | 4 (`waterskinCapacity`) | 24 |
| Copper Canteen | 4 (`copperCanteenCapacity`) | 24 |
| Iron Flask | 6 (`ironFlaskCapacity`) | 36 |
| Diamond Flask | 10 (`diamondFlaskCapacity`) | 60 |
| Netherite Flask | 15 (`netheriteFlaskCapacity`) | 90 |

Every count goes from 1 to 64. A bucket is three servings wherever it is poured, the same rate a
cauldron uses, whatever the destination holds.

## Where water comes from

### Water in the world

`WaterPurity.sampleAt` scores the water block from 0 (clean) to 100 (filthy), then grades the score.
It is sampled only when water is collected, drunk or looked at with Jade.

| Biome | Base score |
|---|---|
| Swamp, Mangrove Swamp, and modded swamps such as Terralith's and No Man's Land's (`thirstwastaken2:stagnant_water`, checked before river) | 85 |
| Jungle, Savanna, Badlands | 70 |
| Anything else | 55 |
| River | 42 |
| Mountain (`#is_mountain`) | 28 |
| Ocean, Beach, and `thirstwastaken2:sea_water` (No Man's Land's Mud Beach) | sea water, not scored (`enableSeaWater`) |

A fluid in `thirstwastaken2:pure_water` is Pure wherever it lies, before the sea check and the score; the
tag ships empty, for data packs. A fluid in `thirstwastaken2:salt_water` is sea water wherever it lies,
while `enableSeaWater` is on, and scored like any other water when it is off: Spelunkery's Spring Water,
as optional entries. See [src/main/spelunkery/AGENTS.md](../../../src/main/spelunkery/AGENTS.md).

| Modifier | Score |
|---|---|
| Biome base temperature 1.5 or more | +10 |
| Biome base temperature 0.15 or less | −10 |
| Above y 100 | −5 |
| Flowing water, not a source | −5 |
| Mud or mangrove roots within 2 blocks | +15 |
| Farmland or a composter within 2 blocks | +10 |

| Score | Grade |
|---|---|
| 0 to 15 | Pure |
| 16 to 35 | Clean |
| 36 to 65 | Murky |
| 66 to 100 | Dirty |

Then, only when the score left something better than Murky: water the sky can't reach is capped at
Murky (`COVERED_MAX`). The column is walked up at most 16 blocks to its top and compared with the
`MOTION_BLOCKING` heightmap through `Vanilla.skyAbove`, so leaves count as cover. Stored water
(cauldrons, pots) and `pure_water` fluids are not capped.

What that gives in practice:

- **Pure:** only cold mountains high up, open to the sky: frozen peaks, jagged peaks or snowy slopes
  above y 100 (28 − 10 − 5 = 13).
- **Clean:** other mountains above y 100, cold rivers (42 − 10 = 32), open to the sky.
- **Murky:** most plains, forests and rivers, ordinary lakes, and every cave pool.
- **Dirty:** swamps, jungles, savannas, badlands, and water next to mud or a farm.

None of these are in the config; they are constants in `WaterPurity`.

### Water that fills a cauldron on its own

| Source | Grade | Config |
|---|---|---|
| Rain into a cauldron or a hanging pot | Clean | `rainwaterQuality`, off with `enableRainCollection` |
| A pointed dripstone dripping into a cauldron | Pure | `dripstoneQuality` |

Both keep the worse of what the cauldron held and what fell in. A cauldron always keeps the worse of
two waters mixed; a carried vessel averages them.

### Other mods' water blocks

| Block | Grade drawn | Notes |
|---|---|---|
| Timber Well (Farm & Charm) | the groundwater it pumps, sampled at the source block | rain only: `rainwaterQuality`; a beach well gives sea water |
| Water Trough (Farm & Charm) | Murky | whatever was poured in; sea water refused |
| Kitchen sink (Candlelight) | Murky | fills from nothing; sea water refused |

See [src/main/farmandcharm/AGENTS.md](../../../src/main/farmandcharm/AGENTS.md).

### Loot

A water bottle in a pool added to mineshaft, bastion, Nether fortress, shipwreck supply and dungeon
chests: one in two chests gives 1 to 3 bottles, Clean or Pure in equal odds. Piglin bartering can
give the same bottles, Clean twice as often as Pure. See `compat/LootIntegration`.

Desert pyramid chests and desert and savanna village house chests have a pool of their own: one in two
gives 1 to 3 bottles, Dirty or Murky in equal odds. The dry surface is where water is scarcest, so
water is there to find, but not water to drink without boiling it.

### Villager trades

| Villager | Level | Price | Gives | Uses before restock |
|---|---|---|---|---|
| Leatherworker | Novice | 3 emeralds | an empty Waterskin (none while `enableWaterskin` is off) | 12 |
| Cleric | Novice | 1 emerald | a Clean water bottle | 16 |

Each joins the two novice trades vanilla draws from its pool, so about half of all novice
leatherworkers, and two clerics in three, offer it. See `compat/TradeIntegration`.

## Cleaning water

Heat makes Dirty and Murky water Clean in one go and never goes further: `WaterPurity.boil` and, for
Cold Sweat's Boiler, `boilStep` (+1 a pass, to Clean). Only the Copper Distiller makes Pure water from
anything, and only it takes the salt out of sea water (`block/DistillerBlockEntity`).
Create's Sand Filter is the one other way to Pure. With Spelunkery, a furnace boils a sea water bucket
down to a salt bucket, and nothing to drink. Clean and Pure water have no recipe anywhere, so heat
never takes them.

| Method | Time | Servings | Result | Fuel | Notes |
|---|---|---|---|---|---|
| Furnace, bottle or bowl | 8 s | 1 | Clean | yes | `enableFurnaceBoiling`; a stack of bowls goes through one at a time |
| Furnace, bucket | 24 s | 3 | Clean | yes | |
| Smoker, bottle, bowl or bucket | 4 s / 12 s | 1 or 3 | Clean | yes | also Sophisticated's Smoking upgrades |
| Copper Canteen, furnace | 2 s a serving, 8 s full | 1 to 4 | Clean | yes | one recipe per grade, any fill; `FurnaceMixin` times it per serving |
| Iron Flask, furnace | 3 s a serving, 18 s full | 1 to 6 | Clean | yes | the same |
| Diamond / Netherite Flask, furnace | 2 s a serving, 20 s / 30 s full | 1 to 10 / 15 | Clean | yes | the same |
| Copper Canteen, held on a campfire | 2 s a serving, 8 s full | 4 | Clean | no | hold use the whole time; `copperCanteenBoilSeconds` |
| Iron Flask, held on a campfire | 3 s a serving, 18 s full | 6 | Clean | no | `ironFlaskBoilSeconds` |
| Diamond / Netherite Flask, held on a campfire | 2 s a serving, 20 s / 30 s full | 10 / 15 | Clean | no | `diamondFlaskBoilSeconds`, `netheriteFlaskBoilSeconds` |
| Copper Hanging Pot | 3 s a serving, 9 s full | 3 | Clean | no | over a lit campfire; `copperHangingPotBoilSeconds`, `copperHangingPotCapacity` |
| Iron Hanging Pot | 4 s a serving, 24 s full | 6 | Clean | no | `ironHangingPotBoilSeconds`, `ironHangingPotCapacity` |
| Copper Distiller | 8 s a serving, 72 s full | up to 9 | Pure, sea water included | furnace fuel | coal runs ten servings; the cooling tub filled once; a bucket of sea water leaves one salt when another mod has salt; `distillerServingSeconds`, `distillerTankServings` |
| Cooking Pot (Farmer's Delight) | 10 s | 1 | Clean | heat below | only with Farmer's Delight; Dirty and Murky only |
| Boiler (Cold Sweat) | 10 s a grade, 20 s Dirty to Clean | up to 27 | up one grade a pass, to Clean | yes | only with Cold Sweat |
| Sand Filter (Create, Create Fly) | 10 mB a tick, 1.25 s a serving | continuous | up one grade a pass, to Pure; sea water passes salty | none of its own; the pumps feeding it need rotation | only with Create (NeoForge 1.21.1, Forge 1.20.1) or Create Fly (Fabric 26.1.x, 26.2.x); filters in series for more than one grade |
| Cold Sweat's Waterskin, campfire | 60 s | 1 | Clean | no | Cold Sweat's own recipe; `CampfireWaterskinMixin` stamps the grade. Its furnace and smoker recipes were removed |
| Teapot (Kaleidoscope Cookery) | 12 s | 4 teacups | safe tea, not water | heat below, a tea bag | a teacup restores its fixed value whatever the grade; sea water refused |
| Cooking Pot (Farm & Charm, and Candlelight's) | 45 s | 1 jug of tea | safe tea, not water | heat below, a glass bottle | takes a water bucket of any grade; sea water refused |
| Tea Kettle (HerbalBrews) | 25 ticks a tea | 1 cup | safe tea, not water | a stove below, blaze powder for heat, a glass bottle | takes a water bucket or any bottle of any grade; sea water refused |

No water recipe is a campfire-slot recipe: vanilla would take any vessel held against a campfire into
its slots and swallow the in-hand boil. No water recipe gives experience.

The Waterskin cannot be boiled at all. Clean water gets into it only from something already clean: a
hanging pot, a cauldron, or bottles and buckets boiled elsewhere.

Moving water (Create, Sophisticated tanks and pumps, Supplementaries jars) keeps its grade and cleans
nothing.

### Input to output, by grade

| Method | Dirty | Murky | Clean | Pure | Sea water |
|---|---|---|---|---|---|
| Furnace, smoker; canteen and flask in a furnace or on a campfire; hanging pots; Farmer's Delight Cooking Pot; Cold Sweat's Waterskin on a campfire | Clean | Clean | kept, no recipe | kept, no recipe | refused (a salt bucket with Spelunkery, salt in a Cooking Pot with Hearth and Harvest or Expanded Delight) |
| Boiler (Cold Sweat) | Murky, then Clean | Clean | kept | kept | refused |
| Copper Distiller | Pure | Pure | Pure | Pure | **Pure**, and salt when another mod has it |
| Sand Filter (Create, Create Fly) | Murky (+1) | Clean (+1) | Pure (+1) | Pure | passes through salty |
| Teapot, Farm & Charm and Candlelight Cooking Pot, HerbalBrews Tea Kettle | tea, not water: a fixed value whatever the grade | | | | refused |

Mixing in a cauldron or a hanging pot keeps the worse grade; a carried vessel averages, rounded down.

### Where Pure water comes from

The Copper Distiller, the Sand Filter, cold mountain water above y 100 open to the sky, a dripstone
cauldron, and underground and Nether loot.

## Drinking bad water

Clean and Pure water give nothing on any difficulty. Peaceful has the mildest Upset Stomach and Poison. The default Upset Stomach and Poison lines share the group `illness`, so one roll decides both: Poison's
chance sits inside Upset Stomach's, and Poison never comes alone (`sanitize()` keeps a grouped Poison
chance at or under its Upset Stomach's). There is no taste Nausea. The tables are `sicknessEffects` in
the config; `SicknessEffect.defaults()` holds these.

Read as: chance, time, level.

| | Dirty | Murky |
|---|---|---|
| Peaceful, Upset Stomach | 35%, 20 s, I | 15%, 15 s, I |
| Peaceful, Poison | 5%, 8 s | 2%, 5 s |
| Easy, Upset Stomach | 50%, 30 s, I | 25%, 20 s, I |
| Easy, Poison | 8%, 10 s | 3%, 8 s |
| Normal, Upset Stomach | 75%, 45 s, **II** | 50%, 30 s, I |
| Normal, Poison | 20%, 20 s | 8%, 15 s |
| Hard, Upset Stomach | 90%, 60 s, **II** | 70%, 45 s, **II** |
| Hard, Poison | 30%, 30 s | 15%, 20 s |

- **Upset Stomach** always blocks natural healing, food's and quenched's, with no switch. It drains no
  thirst and cuts no saturation or quenched. It cramps for 1 magic damage after a random wait, drawn
  afresh each time (`effect/UpsetStomach.waitTicks`): Peaceful 10 to 15 s, Easy 6 to 15 s, Normal 3 to
  12 s, Hard 0.5 to 8 s, three quarters of that at II, never under 0.5 s. The first comes a whole wait
  after the effect starts. Like Poison it stops at 1 health and works on Peaceful too. Fixed, no
  config. Like any damage, a cramp wakes a sleeper. Milk cures it, as it cures Poison; honey cures only
  Poison.
- **Parched** drains 4 thirst a minute at I and 8 at II, as illness: outside climate and Nourishment.
- **Drinking again while ill** (`extendSicknessEffects`, on): Upset Stomach becomes
  `max(R, min(R + D / 2, 1.5 * D))` ticks for R left and D incoming; any other effect adds the line's
  time to what is left, up to twice the line's time. The higher level is kept. Off, vanilla's rule.
- **Every fresh drink still restores thirst**; the grade only cuts its quenched and brings the chance of
  illness.
- A line naming an effect no mod registers is skipped, so a pack can list another mod's effect.

### Sea water

Salt water never restores thirst. It adds 8 exhaustion to the thirst bar (not in the config), Nausea
for 8 s (`seaWaterNauseaSeconds`) and Parched II for 30 s (`seaWaterParchedSeconds`). The sickness
tables above do not apply to it.

## Decisions already made

These came from the hanging pot's design and still hold.

- **A copper hanging pot holds one bucket, three servings, and an iron one two.** It is smaller than a
  cauldron, so three buckets looked wrong next to one. Up to 64 from the config, through
  `HangingPotBlockEntity`; the blockstate only shows how full it looks.
- **A bucket is three servings, never nine.** Otherwise water would multiply through a cauldron: three
  bottles make a bucket, and that bucket would give nine bottles. That matters in the Nether.
- **Nothing can be poured into a pot where water evaporates**, as in the Nether. The water hisses away
  like a bucket emptied there, but the player keeps it.
- **Boiling is timed per serving**, as a furnace times each item. Pouring more in adds only the new
  servings' time; what has boiled is kept, and water already Clean or Pure counts as boiled. A pot's water is one
  batch with one grade, so no serving can be taken out Pure while the rest is still boiling.
- **Copper boils faster, iron holds more**, for the canteen and flask and for the pots alike. Copper
  oxidising and boiling slower as it weathers was set aside: three more textures and blocks.
