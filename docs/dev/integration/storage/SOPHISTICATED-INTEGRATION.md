# Sophisticated Backpacks integration plan

What ThirstWasTaken2 does and still has to do with
[Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) on NeoForge. Almost every
upgrade that matters lives in Sophisticated Core, which Sophisticated Storage shares, so the work
targets Core and covers both. How the finished parts work is in
[src/main/sophisticated/AGENTS.md](../../../../src/main/sophisticated/AGENTS.md); this file is the order of
work and what each step needs.

Written on 2026-09-19 from Sophisticated Core `1.21.1-1.5.1.2341` and Sophisticated Backpacks
`1.21.1-3.26.3.2158`, the versions `1.21.1-neoforge` compiles against. The newer nodes compile against
Core `1.21.11-1.5.0.2340`, `26.1.2-1.5.0.2334` and `26.2-1.5.0.2337`.

"Every NeoForge node" below means the four that existed on that date. `26.3.x-neoforge`, added on
2026-09-20, has no Sophisticated Core build to compile against, so items 1 to 5 are absent there; the
waterskin and bowl fluid containers of item 6 need no Sophisticated and do work on it.

## Status

| # | Item | Kind | Status |
|---|---|---|---|
| 3 | Tank upgrade keeps water quality | bug | **Done** on `1.21.1-neoforge` |
| 1 | Feeding upgrade restores thirst | bug | **Done** on `1.21.1-neoforge` |
| 2 | Alchemy upgrade drinks through the mod | bug | **Done** on `1.21.1-neoforge` |
| 4 | Pump upgrade keeps water quality | bug | **Done** on `1.21.1-neoforge` |
| 7 | Smoking recipes for purified water | data | **Done**, every version |
| 6 | Waterskin and bowl as fluid containers | feature | **Done**, every NeoForge version |
| 5 | Drinking upgrade | feature | **Done** on `1.21.1-neoforge` |
| 8 | Newer NeoForge nodes (1.21.11, 26.1, 26.2) | port | **Done**: items 1 to 5 on every NeoForge node |
| 9 | Changelog and player docs | docs | **Done** |
| 10 | Cooking upgrades keep a canteen or flask's servings (issue #53) | bug | **Done** on every node that builds Sophisticated; see `src/main/sophisticated/AGENTS.md`, "Cooking upgrades" |

The order is the one agreed on: the bugs first, the Tank before the others because it let any water
turn Clean, then the cheapest feature, then the rest. All four bugs are fixed, the Drinking upgrade is
in, on every NeoForge node, and the player docs describe it.

## Done

### 3. Tank upgrade

The Tank upgrade moved water through handlers that know nothing of quality, so everything came out as
plain water and read as `defaultPurity`: a dirty bucket went in and Clean bottles came out, and sea
water bottles were refused outright. `TankUpgradeWrapperMixin` wraps the one method the tank finds
container handlers through, and `WaterQualityFluidHandler` carries the grade across. `WaterFluids`
moved to `../../../../src/main/neoforge` so Create and Sophisticated share it.

Checked in a real client with `../../../../tools/agent/integrations/sophisticated/sophisticated-tank.jsonl`, and once with the mixin off to
confirm the bug. The first version duplicated water (the last sea-water bottle was poured in forever);
the salt case now watches for that.

### 1. Feeding upgrade

The Feeding upgrade finishes eating with `Item.finishUsingItem`, past the mod's hook on
`ItemStack.finishUsingItem`, so fed food never restored thirst. `FeedingUpgradeWrapperMixin` calls
`ThirstManager.drinkItem` on that one call. Checked with `../../../../tools/agent/integrations/sophisticated/sophisticated-feeding.jsonl`:
seven melon slices took thirst from 4 to 20, and with the mixin off it stayed at 4.

### 2. Alchemy upgrade

The Alchemy upgrade drinks and eats what its filters name, on a condition, and finished through
`Item.finishUsingItem` the same way, so a potion drunk from a backpack gave no thirst.
`AlchemyUpgradeWrapperMixin` calls `ThirstManager.drinkItem` where the upgrade finishes an item, for
drinking and eating only, since a splash potion is thrown.

Decided: **the Alchemy upgrade never drinks plain water.** Water stays the player's choice, and the
Drinking upgrade (item 5) is where drinking from a backpack belongs. The first version of this plan
said a water bottle in its filter was drunk for free; that was wrong. Sophisticated's own potion
definition skips any potion without effects, water included. The mixin refuses plain water at the
condition check anyway, so a definition another mod adds cannot drink it either.

Checked with `../../../../tools/agent/integrations/sophisticated/sophisticated-alchemy.jsonl`: a Fire Resistance potion took thirst from 4 to
10, and dirty water was left alone. With the mixin off the potion was drunk and thirst stayed at 4.

### 4. Pump upgrade

Three problems, one of them caused by item 3:

- water collected from the world carried no grade, so a pump by a swamp filled the tank with water
  that read Clean, and one by the sea collected fresh water;
- buckets in a player's hand lost their grade going in, because the pump looks up their handler
  itself rather than through the Tank upgrade;
- once the Tank fix stamped the water in the tanks, the pump could not pump it out at all: it asks the
  backpack for a stack with no components, and the backpack only hands out matching ones.

`PumpUpgradeWrapperMixin` samples world water at the source (through `SampledWater`, moved to
`../../../../src/main/neoforge` and shared with Create, and only once the tanks have room), wraps buckets in hand,
and builds the pump-out request from the water in the tank. A pump filter set to water now takes water
of any grade (`FluidFilterLogicMixin`), since a filter made from a plain bucket refused graded water.

Checked with `../../../../tools/agent/integrations/sophisticated/sophisticated-pump.jsonl`: a plains pool gave the same grade as a bottle
filled from it by hand, an ocean pool gave salt water, a filtered pump still collected, and buckets
went in and out with their grades. Without the mixins every case lost its grade or, pumping out,
moved nothing. The neighbouring-block path, through a Create Fluid Tank next to the player, was run
afterwards and keeps the grade both ways too.

### 7. Smoking recipes

Water was purified by `smelting` and `campfire_cooking` recipes only, so the Smoking upgrades, and a
vanilla Smoker, could not purify it. The datagen now writes nine `smoking` recipes per version next to
the smelting ones, at 100 ticks, half a furnace, as a smoker is for food. They count for the
`boil_water` advancement, since a smoker credits the player the same way a furnace does, and
`PurificationGameTest` checks them, salt water included. This is plain data, so it covers every
version and both loaders, not just the nodes with Sophisticated.

Checked with `../../../../tools/agent/integrations/sophisticated/sophisticated-cooking.jsonl`: a purity-0 bottle came out of both the
Smoking and the Smelting upgrade with `water_purity: 2`.

### 6. Waterskin and bowl as fluid containers

The waterskin (three servings) and the terracotta bowls (one) now carry NeoForge's item fluid
capability, a serving being 250 mB, so the Tank and Pump upgrades, Create and any other fluid mod can
fill and empty them. It needs no Sophisticated, and it covers every NeoForge version: 1.21.1 through
`IFluidHandlerItem`, 1.21.11 and later through the transfer API, each a thin handler in a source
directory of its own over shared rules (`WaterContainerFluids`). Whole servings only, and a container
takes more water only of the grade it holds; the details are in `purity/AGENTS.md`. The Sophisticated
wrapper leaves these items alone, since their handler already carries the grade.

`ContainerFluidGameTest` runs one set of assertions against both APIs on all four NeoForge nodes, and
caught two bugs in the transfer handler on the way. In a client on 1.21.1: waterskins and bowls went
through the Tank upgrade with their grades, the Pump filled a waterskin in hand, and Create's Spout
filled a waterskin on a Depot once (750 mB, not twice) while an Item Drain emptied a bowl.

Not done: Fabric's Transfer API. No Fabric integration needs it yet.

### 5. Drinking upgrade

A new upgrade, the thirst version of the Feeding upgrade, in a basic and an Advanced tier. It drinks
from the backpack when the thirst bar is low: bottles, waterskins, bowls and other drinks with a thirst
value, and water from a Tank upgrade in the same backpack, 250 mB at a time. The cleanest water goes
first, nothing below the lowest grade (Clean by default) and never salt water. Everything goes through
the same code as drinking by hand. The Advanced tier adds the "drink at" and lowest-grade buttons and a
larger filter, as Advanced Feeding does.

Decided: the recipe mirrors Feeding's (a waterskin, two glass bottles and an ender pearl around an
upgrade base; the Advanced tier from the basic with a diamond, two gold and three redstone); a Tank of
water counts, held to the same lowest grade, so a Pump by an untreated lake does not make a free
canteen; and the Feeding upgrade's cooldowns, 100 ticks and 10 while still thirsty. Potions other than
water, milk and ominous bottles are never drunk.

Checked with three agent scripts: `sophisticated-drinking.jsonl` (clean bottles drunk and the dirty one
left, a Pure tank before a Clean bottle, sea water refused, the Advanced settings obeyed, honey and cider
drunk but not milk or potions, water before honey), `sophisticated-drinking-craft.jsonl` (all three
recipes in a crafting table, the upgrade in a Sophisticated Storage chest) and
`sophisticated-drinking-tab.jsonl` (the buttons clicked, the tab in all nine languages). The agent client
gained `client.click`, `client.slot(s)`, `client.language` and `client.textWidth` for them. The details
are in `../../../../src/main/sophisticated/AGENTS.md`.

### 8. Newer NeoForge nodes

From 1.21.11 Sophisticated Core's tanks move fluid through NeoForge's transfer API
(`ResourceHandler<FluidResource>`, `ItemAccess`, transactions) instead of `IFluidHandler`. All three
newer nodes now set `deps.sophisticated_core` and `deps.sophisticated_backpacks`, and
`deps.sophisticated_storage` for runClient.

- **Split by generation, not by upgrade.** `../../../../src/main/sophisticated` holds what does not touch fluid:
  the gate, the Feeding and Alchemy mixins and the Drinking upgrade. The tank and pump code lives in
  `src/main/sophisticated-fluidhandler` (1.21.1) and `src/main/sophisticated-transfer` (1.21.11 and
  later), with the same class names, so the mixin config is shared. `build.neoforge.gradle.kts`
  picks one, the same way it picks `neoforge-fluidhandler` or `neoforge-transfer`.
- **One gate, per generation.** `SophisticatedPresence` looks for `SophisticatedGeneration.MARKER`,
  a class only the generation the node was written for has. The plan said to split the gate per
  upgrade so the Feeding mixin could apply where the Tank one could not; with both generations
  written there is no such node, and a split would only let a Core whose Feeding upgrade moved crash
  the game.
- **Tank.** `WaterQualityResourceHandler` wraps the handler `getFluidHandler(ItemStack, ItemAccess)`
  finds, and `UnstampedItemAccess` shows it the container unstamped and stamps whatever it swaps in.
  The transfer API builds the new container from scratch the same way `IFluidHandler` did, so the
  grade is carried across as before. The quality is read from the real container every time, never
  kept, so a rolled-back transaction leaves nothing stale.
- **Pump.** Only two of 1.21.1's three fixes were needed. World water is stamped as it goes into the
  tanks (`CollectedWaterStorage`), which covers a `BucketPickup` and a block's own capability alike.
  Buckets in hand get the unstamped view. Pumping out needs nothing: this generation asks the tanks
  for the resource they hold, components and all.
- **Alchemy.** From 1.21.11 Core finishes through `ItemStack.finishUsingItem`, where the mod's own
  hook already hands out thirst, so the `tick` hook is 1.21.1 only; with it a potion counted twice
  (4 to 16 instead of 4 to 10). The plain-water refusal stays on every node. Feeding still calls
  `Item.finishUsingItem` on every version, so its mixin is unchanged.
- **Drinking upgrade.** `DrinkingStorage`, one per generation, reads the storage's slots and tanks;
  everything else is shared, with Stonecutter branches for `ItemUseAnimation` (1.21.2), an item's id
  in its properties (1.21.2) and `CompoundTag`'s getters (1.21.5).
- **Recipes and unlocks** moved into the generation directories: 1.21.2 writes ingredients as ids,
  and NeoForge's `item_exists` condition became `registered`. The item model definitions
  (`assets/…/items/`) are shared; 1.21.1 ignores them.

Checked in a real client on 1.21.11 and 26.2, with the same agent scripts as 1.21.1, which was run
again afterwards to check nothing moved there. Every case gave the result 1.21.1 records, except:

- the Create cases of the Pump script, since Create is on the runClient classpath of `1.21.1-neoforge`
  only;
- Farmer's Delight's cider in the Drinking script, for the same reason (thirst 8 rather than 16 in
  that case);
- a refused container in the Tank's input slot stays there from 1.21.11, where 1.21.1 moves it on to
  the result slot. That is Core's own behaviour, not the integration's.

26.1 compiles and passes its gametests, and its Core is the same code as 26.2's for everything the
integration touches, but it was not run in a client.

The scripts needed changes to run on the newer versions; `../../../../src/main/sophisticated/AGENTS.md` lists them.
Two are Sophisticated's own quirks: from 1.21.11 a data-pack template given twice comes with what the
first backpack from it ended up holding, and on 26.2 templates only load their items after a
`/reload`.

### 9. Changelog and player docs

- `../../../../CHANGELOG.md`, under `[Unreleased]`: the Drinking upgrade, the four upgrades that keep water's grade
  or restore thirst, smokers purifying water, and waterskins and bowls as fluid containers, with a note
  that it is every NeoForge version and no Fabric one.
- A new page, `../../../docs/integrations/sophisticated-backpacks.md`, in the Features sidebar, with the
  Drinking upgrade's tab (`sophisticated-drinking-upgrade.png`) and its recipe
  (`drinking-upgrade-recipe.png`). Only the basic recipe is shot; the page names the Advanced one's
  ingredients.
- A row in the installation page's NeoForge table, listing Core's versions, since that is what the mod
  is built against; the overview's list of supported mods; and a row in the NeoForge table of
  `../../../MODRINTH.md` and `docs/CURSEFORGE.md`.

The item tooltips were shortened for it, in all nine languages: "Auto-drinks when thirsty", and "Choose
when and what to drink" on the Advanced tier. The recipe was shot on 26.2 with the grid filled through
the agent queue and the pointer moved by computer use. A tooltip always covers the hovered sprite, so
the image is two captures, one unhovered and one hovered, with the tooltip moved beside the slot.

Nothing is left to do.

## Testing, for every item

- The gametests run without Sophisticated. They prove the node still loads without it, and must keep
  passing on every NeoForge node: `./gradlew ":<node>-neoforge:runGametest"`.
- Everything that needs Sophisticated is checked in a real client with an agent script and a backpack
  template from `../../../../tools/agent/integrations/sophisticated/sophisticated-pack`. `/sophisticatedbackpacks template give` builds the
  backpack (the short `/sbp` is `/sb` from 1.21.11), opening it once unpacks the template, and
  `template create <name> true` plus `export` write what is left as SNBT under the world's
  `datapacks/`, so the result is read as text.
- Run each check once with the new mixin left out of the config as well, to show the bug was real and
  that the check can tell the two apart.
- Make the world with `tools/agent/new_world.py`: a `level.dat` copied by hand keeps the old player,
  Nourishment included, which cancels exhaustion. Clearing effects at the start of a script guards it.
