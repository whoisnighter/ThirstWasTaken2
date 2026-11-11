# Extra Delight integration plan

What ThirstWasTaken2 should do with [Extra Delight](https://modrinth.com/mod/extradelight) (mod id
`extradelight`, package `com.lance5057.extradelight`), the large Farmer's Delight addon: juices, ades,
milkshakes, coffee, tea and soups, and a kitchen full of blocks that hold water (a Tap, a Sink Cabinet,
Jars, Kegs, a Vat, a Mixing Bowl, a Chiller, an Evaporator, a Funnel). This file sets the order of work,
what each step needs and how each one is checked. Once the work is built, how it works goes in
`src/main/extradelight/AGENTS.md`.

Written on 2026-10-06 from:

- the repository [Lance5057/ExtraDelight](https://github.com/Lance5057/ExtraDelight), branch `1.21` at
  `effd768c` (2026-07-28), `mod_version=2.6.6`, the released build;
- the Modrinth project `yRrY3XII` (`extradelight`), and the separate unofficial Forge 1.20.1 backport
  `extra-delight-unofficial-port` (one release, `1.0.1`, 2026-07-11).

## Which build for which node

| Node | Build | Modrinth version id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `2.7` (2026-11-08; written against `2.6.6`, `1SSgrmDa`) | `zUBFUbzl` | Farmer's Delight, which the node already runs |
| every other node | none | none | none |

The official mod is **NeoForge 1.21.1 only**, and still updated. The Forge 1.20.1 build is an unofficial
backport by someone else with a single release, so it is left out: its item ids are not checked, and if
they match, the thirst values (common config) reach it anyway. Its blocks get nothing.

## What Extra Delight already does for thirst

Its datagen writes Tough As Nails tags (`toughasnails:thirst/<n>_thirst_drinks`, `toughasnails:drinks`)
for 27 drinks, and `c:drinks/milk`. Neither is read by this mod: `c:drinks` itself is empty, so the
fallback in `ThirstApi` matches nothing. Today **every Extra Delight drink restores nothing**. Its Tough
As Nails numbers are a useful ranking (lemonade 8, juices and ades 6, tea 4, milkshakes and coffee 2),
not a scale to copy.

## Where water lives in Extra Delight (2.6.6)

Every bottle goes through one class, `util/BottleFluidRegistry`: `getFluidFromBottle(ItemStack)` turns
a bottle into its fluid, `getBottleFromFluid(FluidStack)` a fluid into its bottle. The water bottle is
registered there as `Potions.WATER` and `SizedFluidIngredient.of(Fluids.WATER, 250)`. Both directions
drop every component: a Dirty or salty bottle goes in as plain water and comes back as a plain bottle,
which reads `defaultPurity` (Clean).

Buckets and other fluid containers go through NeoForge's `FluidUtil` and the item's fluid handler, and
NeoForge's bucket wrapper takes and gives plain water too.

The tanks themselves keep components. `FancyTank` (Vat, Mixing Bowl, Chiller) keys its fluids by
`FluidKey`, which compares `isSameFluidSameComponents`; the Jar, Keg, Evaporator and the rest use
NeoForge's `FluidTank`, which does the same; the Jar keeps its fluid on the item when picked up. So
stamped water stays stamped inside, and two grades never share a tank. The loss is only at the edges.

| Block | Takes water from | Gives water to | What goes wrong |
|---|---|---|---|
| Tap | nothing: `WellFluidCapability(WATER)` is endless | bottles, buckets, any fluid handler, pipes | Clean water from nothing, forever |
| Sink Cabinet | the same endless capability | the same | the same |
| Jar, Keg | bottles (`BottleFluidRegistry`), buckets (`FluidUtil.interactWithFluidHandler`) | the same | grade and salt lost both ways |
| Vat, Mixing Bowl, Chiller | their fluid input slot (`IFancyTankHandler.fillInternal`: bottles, fluid handlers) | their fluid output slot (`drainInternal`) | the same |
| Evaporator | fluid handlers only (`FluidUtil.interactWithFluidHandler`) | nothing, it makes salt | fresh water boils down to salt |
| Chiller drip tray | 1 mB of plain water a tick while ice chills | buckets, fluid handlers, glass bottles | meltwater reads Clean |
| Melting Pot | ice melts into 1000 mB of plain water | bottles, fluid handlers | the same |
| Funnel | a water source above it, picked up and removed, or a fluid handler above | the handler in front, or placed as a block | world water picked up unsampled |

Recipes that take water, all matched by fluid alone (`SizedFluidIngredient.of(Fluids.WATER, n)`), so
**sea water matches too**:

| Block | Recipes |
|---|---|
| Mixing Bowl | lemonade, limeade, orangeade, dalgona coffee, gummies, marshmallow, nougat, wheat dough |
| Vat | fish sauce, kimchi, miso paste, naem moo, natto, sauerkraut, soaked soybeans, soy sauce, vinegar, yeast |
| Evaporator | `evaporate_water`: 1000 mB of water to 1 to 2 salt, about 8 minutes |
| Crafting | `vinegar_pot`, `yeast_pot`: a water bottle or bucket in a shapeless recipe |

Its salt (`extradelight:salt`) is tagged `c:dusts/salt`, so the Copper Distiller already picks it as the
salt it leaves behind, through `#c:dusts/salt` in `thirstwastaken2:distiller_salt`.

## The drinks and foods

Proposed values (thirst, quenched), against what the other mods already have:

| Items | Value | Like |
|---|---|---|
| `lemonade`, `limeade`, `orangeade`, `punch`, `glow_berry_juice`, `sweet_berry_juice`, `tomato_juice`, `cactus_juice`, `orange_juice`, `grapefruit_juice` | 8, 13 | Farmer's Delight's melon juice |
| `lemon_juice`, `lime_juice` (neat, they make the drinker pucker) | 4, 5 | half a juice |
| the 10 `*milkshake` | 8, 12 | Fruits Delight's mango milkshake |
| `gourmet_hot_chocolate` | 8, 13 | Farmer's Delight's hot cocoa |
| `chocolate_milk`, `eggnog`, `horchata`, `soy_milk`, `xocolati` | 6, 8 | a bottle of milk |
| `ginger_beer` (a soft drink here) | 6, 8 | a bottle of milk |
| `tea` (corn silk tea) | 6, 9 | Kaleidoscope Cookery's teas |
| `coffee`, `dalgona_coffee` | 5, 8 | Croptopia's coffee |
| `apple_popsicle`, `glow_berry_popsicle`, `sweet_berry_popsicle`, `honey_popsicle` | 7, 9 | Farmer's Delight's melon popsicle |
| `caramel_popsicle`, `cinnamon_popsicle`, `fudge_popsicle` | 5, 7 | creamier, less water |
| `melon_gazpacho` | 6, 8 | a cold soup of melon |
| `borscht`, `cactus_soup`, `carrot_soup`, `corn_chowder`, `fish_soup`, `hazelnut_soup`, `miso_soup`, `mulligatawny_soup`, `onion_soup`, `oxtail_soup`, `potato_soup`, `sauerkraut_soup`, `tomato_soup`, `gazpacho`, `congee`, `chicken_stew`, `lamb_stew`, `pork_stew` | 4, 5 | every other soup and stew |
| `curry`, `chili_con_carne`, `white_chili` | 3, 4 | thick, like noodles |
| the 11 `*ice_cream`, `ice_cream_sundae`, `affogato`, the 7 `*custard` | 2, 3 | Croptopia's ice cream, Farmer's Delight's custard |

Left out as solid food: the `*_rice` dishes, pies, cakes, puddings, jellies (`jelly_*`), `stewed_apples`,
and every feast block (it serves one of the bowls above). `pickle_juice`, the sauces, syrups, oils and
`*_fluid_bucket`s are ingredients, not drinks.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath, and the gate | build | `1.21.1-neoforge` | **Done** (2026-10-06) |
| 2 | Thirst values for the drinks and foods | data | all (config) | **Done** (2026-10-06), values as proposed |
| 3 | Bottles keep the grade (`BottleFluidRegistry`) | code | `1.21.1-neoforge` | **Done** (2026-10-06) |
| 4 | Buckets and fluid containers keep the grade (the `FluidUtil` call sites) | code | `1.21.1-neoforge` | **Done** (2026-10-06) |
| 5 | Water from nothing or from the world: Tap, Sink Cabinet, Funnel, meltwater | decision, then code | `1.21.1-neoforge` | **Done** (2026-10-06), as proposed |
| 6 | Sea water refused in the Vat, the Mixing Bowl and the two pot recipes | code | `1.21.1-neoforge` | **Done** (2026-10-06) |
| 7 | The Evaporator makes salt from sea water only | decision, then code | `1.21.1-neoforge` | **Done** (2026-10-06), as proposed |
| 8 | Nothing crashes without the mod | test | all | **Done** (2026-10-06) |
| 9 | Changelog, player docs, store pages (a Farmer's Delight addon, so it gets a row) | docs | none | **Done** (2026-10-06) |

How it was built is in [src/main/extradelight/AGENTS.md](../../../../src/main/extradelight/AGENTS.md).

Order: 1 and 2 first, as in every plan, then 3 and 4, which every later step stands on, then 5 to 7.

## 1. The mod on the `runClient` classpath

`deps.extra_delight = "1SSgrmDa"` in `[neoforge."1.21.1"]`, a `compileOnly` and `runClientMod` line in
`build.neoforge.gradle.kts` (with Farmer's Delight, as Hearth and Harvest has), a row in the integration
table (directory `extradelight`, NeoForge only, its mixin config), and `ModrinthDep("extra_delight",
"extradelight", by_id=True)` in `update_mc_deps.py`. The gate reads each target off its class file, as
Hearth and Harvest's does, so a renamed method disables one mixin rather than crashing the game.

**Checked with** `runServer` on `1.21.1-neoforge` with and without the mod, and `checkOptionalSeam`.

## 2. Thirst values

Ids in `ThirstConfig.extraDelightDrinks` / `extraDelightFoods`, merged with `putIfAbsent` like every
other mod's, from the table above. Teas, coffees and juices are their own items, so whatever water went
into them they are safe, as everywhere else.

**Checked with** a gametest that the defaults list them, that the dry dishes are not listed, and that a
player's own value survives the merge; then one drink of each kind in the dev client.

## 3. Bottles keep the grade

Two return-value mixins on `BottleFluidRegistry`:

- `getFluidFromBottle(ItemStack)`: when the stack is a water container, return a **copy** of the fluid
  stamped with `WaterPurity.quality(stack)`. A copy, because the method hands out the first stack of a
  `SizedFluidIngredient`, which caches it; stamping it in place would stamp every later call;
- `getBottleFromFluid(FluidStack)`: when the fluid is water and the result a water container, stamp the
  result with `WaterFluids.quality(fluid)`. Unstamped water stays `defaultPurity`, as in every tank.

That covers the Jar, the Keg, the Vat, Mixing Bowl and Chiller slots, and any later block that goes
through the registry. A Dirty bottle poured into a Jar comes back Dirty; a sea water bottle comes back
salty, and a Jar of fresh water refuses it, by the tank's own component rule.

## 4. Buckets and fluid containers keep the grade

NeoForge's bucket wrapper drops the grade both ways. The fix is the one Sophisticated's Tank upgrade
uses, `WaterQualityFluidHandler`: look the container's handler up on its unstamped copy, stamp water
leaving it with the container's grade, and stamp the container it turns into with the water entering it.
That class lives in `src/main/sophisticated`, which Extra Delight's directory cannot reach, so it moves
to `src/main/neoforge-fluidhandler` (NeoForge 21.1, the only API Extra Delight is built against), and
both integrations use it from there.

The call sites, each a one-line `@WrapOperation` handing the handler through it:

| Class | Call |
|---|---|
| `JarBlockEntity.use`, `KegBlockEntity.use` | `FluidUtil.interactWithFluidHandler(player, hand, tank)` |
| `EvaporatorBlock.useItemOn`, `JuicerBlock`, `MortarBlock` | the same |
| `IFancyTankHandler.fillInternal` / `drainInternal` | `getCapability(Capabilities.FluidHandler.ITEM)` on the slot stack |
| `TapBlock.useItemOn`, `SinkCabinetBlock.useItemOn` | `FluidUtil.getFluidHandler(stack)`, `tryFillContainer`, `tryEmptyContainer` |
| `ChillerBlockEntity.drainDripTray` | the bucket branch builds `getBucket().getDefaultInstance()` itself; stamp it |

`drainInternal`'s bucket branch also builds the bucket from the fluid type; it gets the same stamp.

**Checked with** an agent script: a Dirty bucket into a Jar and back, a sea water bucket into a Keg and
back, a Murky bucket through the Mixing Bowl's slots, and the Jar picked up and placed again.

## 5. Water from nothing or from the world (decided: as proposed)

- **Tap and Sink Cabinet: Murky**, the grade every other endless sink gives (Candlelight's kitchen sink,
  Hearth and Harvest's Sink, Farm & Charm's trough). They stay endless. One mixin on
  `WellFluidCapability`, whose only users are these two with water: what it hands out (`getFluid`,
  `drain`) is stamped Murky. Their bottle path builds `new FluidStack(WATER)` itself, so the
  `getBottleFromFluid` argument there is stamped Murky too. Water poured into them is thrown away, as
  now, sea water included; nothing comes back from it.
- **Funnel: sampled.** A source block it picks up is water collected from the world, so it gets
  `WaterPurity.sampleAt` at that position, like a bucket there. Water drained from a handler above keeps
  its own stamp already.
- **Meltwater: `rainwaterPurity`** (Clean by default), for the Chiller's drip tray and the Melting Pot's
  ice. Ice is frozen fallen water, and nothing else in the game melts to a better grade. The alternative,
  Pure like dripstone, would make a Chiller and a stack of ice a purifier.

## 6. Sea water refused in recipes

The Vat and the Mixing Bowl match their fluid ingredients by fluid alone. A mixin at the head of each
recipe's `matches` answers false while the block's tank holds salty water, as Cultural Delights' vat and
Brewin' and Chewin's keg do. The tank keeps it and hands it back salty. Fresh water of any grade still
works, and the grade goes: lemonade and kimchi are their own items.

`vinegar_pot` and `yeast_pot` are vanilla shapeless recipes. A `ShapelessRecipe.matches` mixin like
Croptopia's (a salty bucket or bottle in the grid of a recipe whose result is the mod's) refuses them.
Croptopia's own cannot be reused: it only loads with Croptopia.

## 7. The Evaporator (decided: as proposed)

Evaporating fresh water into salt makes salt out of nothing, and every other salt this mod allows comes
from sea water (Hearth and Harvest's pot, Spelunkery's furnace, the Copper Distiller). Proposed:
`EvaporatorRecipe.matches` answers false for `evaporate_water` unless the tank's water is salty, so the
Evaporator turns sea water into salt and leaves fresh water standing in it. Recipes for other fluids are
untouched. The other choice is to leave it as it is, which costs nothing and only matters to a player
who wants salt.

## 8 and 9

As in every plan: `-PwithoutOptional=extradelight` on `boot.jsonl`, `checkOptionalSeam`; then the
CHANGELOG, a page under `docs/integrations/farmers-delight/`, the installation table, and a row in the
Farmer's Delight family on both store pages.

## What was found in game

Checked on 2026-10-06 with `tools/agent/integrations/extra-delight.jsonl` on `1.21.1-neoforge`, every
check passing, and `runGametest` (255 tests, none failing). Its header says how to run it.

- **The Tap and the Oak Sink** hand out Murky water by glass bottle and by bucket.
- **A Jar** takes two Dirty bottles, refuses a Clean one, and hands a Dirty bottle back; a Dirty bucket
  poured into a fresh Jar is Dirty water in it. **A Keg** takes a sea water bucket and hands it back salty.
- **The Evaporator** starts drying sea water (`display` turns to the salt block, `cookTime` counts up) and
  never starts on Murky water, which stays in it.
- **The Mixing Bowl** finds wheat dough's recipe with Murky water and a spoon stirs it; with sea water it
  finds none, and the click opens its screen.
- **A Funnel** under a water source takes it up stamped with the grade `sampleAt` gives there.
- **Lemonade** restores 8 and **coffee** 5, as configured.
- Without the mod (`-PwithoutOptional=extradelight`), `boot.jsonl` comes up clean and the mixin config
  applies nothing.

Three things of Extra Delight's own, which the agent script works around and this integration leaves
alone, reported upstream as [Lance5057/ExtraDelight#288](https://github.com/Lance5057/ExtraDelight/issues/288):

- **A Jar answers a click with `PASS`**, so after it has emptied or filled the bucket in hand, vanilla's
  bucket acts on the world too: a full bucket is poured out beside the Jar and its water can wash small
  blocks away, a Mixing Bowl nearby included. The script checks the Jar's tank rather than the hand, and
  dries the floor after each block.
- **A Jar hands nothing back for its last 250 mB**: `use` looks the bottle up after draining, from a tank
  that is then empty. The script pours two bottles before drawing one.
- **The Mixing Bowl looks its recipe up only when its inventory changes**, so water and items written
  with `/data merge` find nothing until a hopper adds the last ingredient.

Small blocks need aiming: from 0.5 200 0.5, pitch 37 hits a Jar on 0 200 2 and pitch 41 a Mixing Bowl; at
30 the click passes over them and lands on the floor.
