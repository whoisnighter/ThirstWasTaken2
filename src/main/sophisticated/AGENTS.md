# src/main/sophisticated — Sophisticated Core's upgrades

[Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) keeps its upgrades in
Sophisticated Core, which Sophisticated Storage shares, so everything here targets Core and works for
both. Today it fixes five of Sophisticated's upgrades, counting the cooking ones as one, and adds one
of its own:

- the **Tank upgrade** keeps water's grade: a dirty bucket poured in comes back out as dirty bottles,
  and sea water stays sea water. Without it the tank handed out plain water, which the mod reads as
  `defaultPurity`, so any water went in and Clean water came out;
- the **Feeding upgrade** restores thirst for what it feeds. Without it a melon fed from a backpack
  filled the hunger bar and left the thirst bar where it was;
- the **Alchemy upgrade** restores thirst for the potions and food it applies, and never drinks plain
  water;
- the **Pump upgrade** grades the water it collects from the world where it lies, keeps the grade of
  buckets it empties or fills in a player's hand, and can pump graded water back out at all;
- the **Smelting, Smoking and Blasting upgrades**, and their Auto versions, leave a canteen or flask
  as full as it went in. On older Cores it came out purified and empty;
- the **Drinking upgrade**, the mod's own, drinks from the backpack when the thirst bar is low, the
  cleanest water first, bottles and other drinks as well as water from a Tank upgrade.

What is still to do across Sophisticated's upgrades is in
[docs/dev/integration/storage/SOPHISTICATED-INTEGRATION.md](../../../docs/dev/integration/storage/SOPHISTICATED-INTEGRATION.md).

This directory is **only compiled by nodes that set `deps.sophisticated_core`** in
`stonecutter.properties.toml`: every NeoForge node. Sophisticated Core comes in two generations, the
same split as NeoForge's own fluid API. On 1.21.1 its tanks move fluid through `IFluidHandler`; from
1.21.11 through the transfer API (`ResourceHandler<FluidResource>`, `ItemAccess`, transactions). What
does not touch fluid is one copy for both, here. Where Minecraft itself changed it calls core
`platform/Vanilla`; where Core's API or FML changed, `sophisticated/platform` (`SophisticatedUpgradeItem`,
`ModFiles`). `checkVersionSeam` fails on a `//?` anywhere else. The tank and pump code is written once
per generation, in a directory of its own beside this one, and `build.neoforge.gradle.kts` adds the one
that fits the node. Both use the same class names, so
the mixin config is shared.

```
sophisticated/java/com/thirstwastaken2/sophisticated/     every NeoForge node
  SophisticatedPresence      the gate: FML's mod file for `sophisticatedcore`, and the generation's marker class inside it;
                             and Core's class files, read without loading them, for the plugin's checks
  SophisticatedMixinPlugin   applies the mixins only when the gate passes, each only where Core still has what it hooks
  SophisticatedEntrypoint    a second @Mod class that registers the Drinking upgrade after the gate
  mixin/FeedingUpgradeWrapperMixin  hands out thirst where the Feeding upgrade finishes eating
  mixin/AlchemyUpgradeWrapperMixin  the same for the Alchemy upgrade, and keeps plain water out of it
  mixin/CookingLogicMixin           a canteen or flask cooked by a Smelting, Smoking or Blasting upgrade keeps its servings
  drinking/DrinkingUpgrade           items, the two settings components, the settings containers
  drinking/DrinkingUpgradeItem       basic and advanced, told apart by filter size and `isAdvanced`
  drinking/DrinkingUpgradeWrapper    what to drink and when, on the upgrade tick
  drinking/DrinkingUpgradeContainer  the settings tab's server half
  drinking/DrinkAt                   how thirsty to be first: any, half a drink, a whole drink
sophisticated-fluidhandler/java/…/sophisticated/          1.21.1
  SophisticatedGeneration    the marker class the gate looks for, and what this generation's mixins need of Core
  (WaterQualityFluidHandler, a container's IFluidHandlerItem with the grade carried across it, is in
   core's src/main/neoforge-fluidhandler, shared with Extra Delight)
  StampedWaterSource         world water as an IFluidHandler that hands out its sampled grade
  mixin/TankContainerHandlerMixin   wraps the one method the Tank finds container handlers through, Core 1.5.35 and later
  mixin/TankUpgradeWrapperMixin     the same method where it was up to Core 1.5.33
  mixin/PumpUpgradeWrapperMixin     the Pump upgrade: world pickup, buckets in hand, pumping out
  mixin/FluidFilterLogicMixin       a pump filter set to water takes water of any grade
  drinking/DrinkingStorage          the storage's slots and tanks, as the Drinking upgrade reads them
sophisticated-transfer/java/…/sophisticated/              1.21.11 and later
  SophisticatedGeneration      the marker class the gate looks for
  WaterQualityResourceHandler  a container's fluid ResourceHandler with the grade carried across it
  UnstampedItemAccess          the container's ItemAccess, as the handler underneath sees it
  CollectedWaterStorage        the tanks while the Pump fills them from world water, stamping it
  mixin/…, drinking/DrinkingStorage   the same five classes as 1.21.1
sophisticated/resources/                                  every NeoForge node
  thirstwastaken2.sophisticated.mixins.json
  assets/…                  the two upgrade textures, models and item model definitions, the tab's button icons
  data/…                    both mods' upgrade tags
sophisticated-<generation>/resources/data/…               the recipes and their unlocks, in each generation's format
../../client/sophisticated/java/com/thirstwastaken2/client/sophisticated/
  SophisticatedClientEntrypoint  asks the gate, and nothing else
  DrinkingUpgradeTabs            registers the settings tabs, past the gate
  DrinkingUpgradeTab             the basic and advanced tabs
```

## How it stays optional

The same three layers as [src/main/create](../create/AGENTS.md).

1. **Build.** Only when `deps.sophisticated_core` is set, `build.neoforge.gradle.kts` adds this
   directory, the generation's directory and `src/client/sophisticated`, and appends the mixin config
   and an optional `sophisticatedcore` dependency to the built `neoforge.mods.toml`, as
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt) says.
2. **Runtime gate.** `SophisticatedPresence` looks inside Core's own jar for
   `SophisticatedGeneration.MARKER`, a class only the generation this node was written for has:
   `ITrackedContentsItemHandler` on 1.21.1, `MutableStackItemAccess` from 1.21.11. A Core of the other
   generation is skipped with a warning instead of crashing on a missing mixin target. The mixin plugin
   and both entrypoints ask it before anything that names a Sophisticated class is loaded.

   Asking is not enough on its own: the JVM verifies a class whole when it links it, before any code
   in it runs, so a method that never runs still makes the verifier load the types it returns. An
   entrypoint therefore names no Sophisticated type at all and calls into another class instead,
   `DrinkingUpgradeTabs` on the client and `DrinkingUpgrade` on both sides. Registering the tabs in
   `SophisticatedClientEntrypoint` itself crashed every client without Sophisticated Core, since
   verifying the tab factories loaded `DrinkingUpgradeTab` and with it its Sophisticated superclass.

   The plan once split this gate per upgrade, so the Feeding mixin could apply on a node that had no
   tank code yet. With both generations written there is no such node, and a split would only let a
   Core whose Feeding upgrade had moved crash the game, so it stays one gate.
3. **Each mixin fails soft.** A Core that passes the gate can still have moved a method a mixin
   hooks: Core 1.21.1-1.5.35 moved the Tank's container code out of `TankUpgradeWrapper`, and the
   `getFluidHandler` hook, then `require = 1`, crashed every game at loading (issue #61). Now every
   injector is `require = 0`, and `SophisticatedMixinPlugin` reads each target's class file before it is
   transformed: a method a mixin names (injected into or shadowed) that is gone skips that mixin, and a
   call a hook sits on that is gone leaves that hook doing nothing. Both are logged as "not a version of
   Sophisticated Core ThirstWasTaken2 supports" with what the player loses, so a change upstream turns
   that one feature off instead of stopping the game. What each mixin needs is listed beside it:
   `COMMON` in the plugin for Feeding and Cooking, `SophisticatedGeneration.NEEDS` for the rest, whose
   methods and calls differ by generation. A new hook gets its methods and calls added there.
4. **Data.** Each basic recipe needs its own mod's `upgrade_base` (`neoforge:mod_loaded`) and every
   recipe needs the upgrade to exist, so a Core the gate refused leaves no broken recipe behind. That
   condition is `neoforge:item_exists` on 1.21.1 and `neoforge:registered` from 1.21.11, which is one
   reason the recipes live in the generation directories; the other is that 1.21.2 changed how a recipe
   writes its ingredients. The tag entries are `required: false` for the same reason.

**Nothing outside this directory may reference a class in it.**

## The Tank upgrade

`getFluidHandler(ItemStack)` is the only way the tank reaches a container, from its input and output
slots and from the cursor, so one `@WrapMethod` covers every transfer. Up to Core 1.21.1-1.5.33,
26.1.2-1.5.34 and 26.2-1.5.32 it is a method of `TankUpgradeWrapper` (`TankUpgradeWrapperMixin`); from
1.5.35, 1.5.36 and 1.5.34 respectively it is in `TankContainerHandler`, an abstract class of its own that
the Tank upgrade and Sophisticated Storage's tank block (Storage 1.6.18 on 1.21.1) both extend
(`TankContainerHandlerMixin`), with the same signature and the same body. The plugin applies the first
only when `TankContainerHandler` is absent and the second only when it is there, so one integration
covers both shapes of Core. The handlers
it returns know nothing of quality: NeoForge's bucket wrapper takes and gives plain water, and Core's
own bottle handler only matches a water bottle with no components at all. `WaterQualityFluidHandler`:

- looks a stamped container up as its `WaterPurity.unstamped` copy, so Core recognises it. The mod's
  own waterskin and bowls are the exception: their capability already carries the grade (see
  `purity/AGENTS.md`), so they are looked up as they are and never wrapped;
- stamps water leaving the container with the grade it held, and passes water entering it down plain;
- hands out a **stamped copy** from `getContainer`, never the delegate's own stack. Core's bottle
  handler drains only while its container still equals a plain bottle, and the tank ignores a drain
  that returns nothing after it has already filled itself, so stamping in place poured the last bottle
  in forever. That is what the salt case of the agent check below watches.

The fluid in the tank keeps the one-component rule of `WaterFluids` (shared with Create, in
`src/main/neoforge`), so water of two grades never shares a tank: Core compares with
`isSameFluidSameComponents`. A container whose water does not match the tank is refused, and Core
moves it on to the result slot, as it does with lava offered to a water tank. Water with no quality at
all, from a creative tank or another mod, still counts as `defaultPurity` but is not equal to water
stamped with that grade, the same as in Create.

A backpack's tank can also be filled or drained through the backpack's own fluid capability, by a pipe
for instance. Those transfers carry `FluidStack`s, which keep their components on their own.

**From 1.21.11** the method is `getFluidHandler(ItemStack, ItemAccess)` and the handler works on the
container through that `ItemAccess`, swapping in the new container inside a transaction. The same three
rules hold, in `WaterQualityResourceHandler`, with the container side moved into `UnstampedItemAccess`:
the handler underneath is found on the unstamped copy and sees the container unstamped, and whatever
it swaps in (a filled bucket, a filled bottle) is stamped with the grade of the water moving into it.
The access writes straight through to the tank's own, so rolling the transaction back undoes it, and
the grade is always read from the real container rather than kept, so nothing stale survives a
rollback. Core's bottle handler no longer checks its container on the way out, so the pour-in-forever
bug has no counterpart here. A container the tank refuses stayed in the input slot on the Cores before
`TankContainerHandler`, which is Core's own change; on 26.3-1.5.36 it moves on to the result slot again.

**Core 26.1.2-1.5.36, 26.2-1.5.34 and 26.3-1.5.36 dupe containers in the Tank, with or without this
mod.** `TankContainerHandler.drainStack`/`fillStack` put the used container in the result slot through
the tank's `ItemAccessItemHandler` and then clear the input slot with a direct write to the upgrade
stack, inside one transaction; NeoForge's `StackItemAccess` copies its own copy of the upgrade back over
it on commit, so the input slot keeps its container and the result slot gains one every cooldown. Not
ours and not guarded; see item 12 of the plan. A Tank script on those Cores shows it as a full bucket
left in slot 0 beside a stack of empty ones, whatever the grade, so read the grades and ignore the
counts until Core is fixed.

## The Feeding upgrade

The mod hands out thirst for everything eaten or drunk at the head of `ItemStack.finishUsingItem`
(`ItemStackMixin`). `FeedingUpgradeWrapper.tryFeedingStack` calls `Item.finishUsingItem` directly, past
that hook, so `FeedingUpgradeWrapperMixin` wraps that one call and runs `ThirstManager.drinkItem` just
before it, on the server, which is the same point in the same order. Nothing else hands out thirst for
eating, NeoForge's `LivingEntityUseItemEvent.Finish` included, so nothing is counted twice.

The upgrade still decides *when* to feed by the hunger bar alone. Drinking because the player is
thirsty is the Drinking upgrade's job, below.

## The Alchemy upgrade

The Alchemy upgrade drinks or eats what its filters name, on a condition (always, on fire, hurt and so
on). On 1.21.1 it finishes through `Item.finishUsingItem` like the Feeding upgrade; from 1.21.11 through
`ItemStack.finishUsingItem`, where the mod's own hook already hands out thirst. Its item definitions do
that inside static lambdas, whose generated names are not something to target, so
`AlchemyUpgradeWrapperMixin` hooks the two named methods around them:

- **`tick`**, 1.21.1 only, makes the one call that finishes whatever is being applied,
  `FinishUsing.apply`. The mixin runs `ThirstManager.drinkItem` first, for a player and only for an
  item used by drinking or eating: a splash potion is thrown, not drunk. From 1.21.11 this hook would
  count every potion twice, so a Stonecutter branch leaves it out.
- **`applyTo`** tests each filter's condition before it takes anything out of the backpack. The mixin
  answers false for a filter holding plain water (`WaterPurity.isPlainWaterDrink`), so water stays the
  player's own choice, never drunk at a full bar or without a look at its grade. Sophisticated's potion
  definition already skips a potion with no effects, water included, so this only matters for a
  definition another mod adds through `AlchemyUpgradeWrapper.addItemDefinition`. Refusing at the
  condition rather than at the end matters: a refused finish would leave the upgrade starting to drink
  again every check, taking the bottle out and putting it back.

The filter slot still accepts a water bottle; it just never fires.

## The Pump upgrade

The pump moves fluid between the backpack's tanks and three things: water in the world, buckets in
the hands of nearby players, and the fluid handlers of neighbouring blocks. `PumpUpgradeWrapperMixin`
covers each place the grade was lost:

- **Collecting from the world.** `fillFromBlock` hands the tanks a `BucketPickup` (or the block's own
  capability), which gives plain water. The mixin wraps it in `StampedWaterSource` with the grade from
  `SampledWater` (in `src/main/neoforge`, shared with Create), sampled at the source block. The pump
  runs on a tick and searches every source in range until one transfer works, so it only samples once
  the tanks have room for water; a full backpack by a lake would otherwise sample the lake every two
  seconds. `SampledWater` then keeps the answer for 100 ticks per source position.
- **Buckets in hand.** `fillFromHand` and `fillContainerInHand` get the bucket's handler straight from
  its capability, not through the Tank upgrade's lookup, so both are wrapped with
  `WaterQualityFluidHandler.of`. That factory takes a handler already found on the stamped stack,
  which is fine for NeoForge's bucket wrapper but not for Core's own bottle handler; bottles have no
  capability, so the pump never sees them anyway.
- **Pumping out.** `fillFluidHandler` asks the backpack for `new FluidStack(fluid, amount)`, a request
  with no components, and the backpack's handler only drains fluid whose components match. Once the
  Tank fix stamped the water in the tanks, the pump could no longer fill anything from them, a bucket
  or a neighbouring tank. The mixin builds the request from the water actually in the tank instead.
- **Placing water** in the world uses the tank's own stack and then becomes world water, graded where
  it lies like any poured bucket. Nothing to do.

A pump's fluid filter compares components too, and a filter is set from whatever container the player
clicked, so a filter made from a plain bucket refused every graded stack. `FluidFilterLogicMixin` makes
a water filter match water of any grade.

**From 1.21.11** the pump works on `ResourceHandler`s:

- **Collecting from the world.** The tanks are wrapped instead of the source, in
  `CollectedWaterStorage`, at the head of `fillFromBlock`: water going in is stamped with the sampled
  grade. That covers a `BucketPickup` and a block's capability alike, since both hand out plain water.
  The same guards as 1.21.1: a sample only at a source block, and only while the tanks have room.
- **Buckets in hand.** `handleFluidContainerInHand` looks the handler up on the container's `ItemAccess`
  through `CapabilityHelper.getFromFluidHandler`; the mixin hands that lookup the unstamped view and
  wraps what it finds, as the Tank does.
- **Pumping out** needs nothing: the pump asks the tanks for the resource they hold, components and all.
- **The filter** is asked about both resources and stacks, so the mixin wraps both comparisons.

## The Drinking upgrade

The mod's own upgrade, the thirst version of the Feeding upgrade, in a basic and an Advanced tier. It is
the first part of the integration that registers content, so it has `SophisticatedEntrypoint`, a second
`@Mod` class like Create's, and a client one in `src/client/sophisticated` for the settings tab.

**What it drinks.** `DrinkingUpgradeWrapper.canFilter` and `isDrink`: an item with a thirst value that is
drunk (the drink use animation, or one of the mod's plain water drinks), minus what a player would not want
drunk for them: potions other than water, which are the Alchemy upgrade's, milk (`c:drinks/milk`, it
clears every effect) and ominous bottles. The filter slots take the same items, by item, so an empty
waterskin can be set as a filter. Water from a Tank upgrade in the same storage counts too, 250 mB at a
time, drunk as a water bottle of its grade; the item filter does not apply to it.

**Which first.** Fresh water at or above the lowest grade, the cleanest first; salt water never. At an
equal grade the tank goes before items, since it leaves no empty bottle to find room for. Drinks that
are not water at all come last.

**When.** The Feeding upgrade's timings: 100 ticks between checks, 10 while the player is still
thirsty, players within 3 blocks of a placed storage. Only while thirst applies to the player at all
(`canDrinkWater`, enabled, not invulnerable) and the bar is not full, and only once the drink fits by
the `DrinkAt` setting: any, half of it, or all of it.

**How.** An item is finished through `ItemStack.finishUsingItem`, where the mod's `ItemStackMixin`
hands out thirst, sickness and advancements, so it counts exactly as drinking by hand; the container it
leaves goes back into the storage, or to the player. Tank water goes through `ThirstManager.drinkItem`.

**Settings** live on the upgrade stack as `thirstwastaken2:drink_at` and `thirstwastaken2:drink_min_purity`.
Only the Advanced tier reads them or shows their buttons, like Advanced Feeding; the basic one drinks at
half a drink, Clean or better. The grade button goes up on a left click and down on a right click.

**Storage.** `DrinkingStorage`, one per generation, is the only part that reads the storage's slots and
tanks; the wrapper is shared.

**Data.** Recipes mirror Feeding's: a waterskin, two glass bottles and an ender pearl around an upgrade
base, one recipe per mod's base, and `sophisticatedcore:upgrade_next_tier` with a diamond, two gold and
three redstone for the Advanced tier, which keeps the filter. Both items are in `sophisticatedbackpacks:upgrade`
and `sophisticatedstorage:upgrade`. The textures are the mod's own; Sophisticated's are not reused.
The transfer generation's unlock advancements have no `has_the_recipe` criterion, only the item one:
26.3 renamed `recipe_unlocked`'s `recipe` key to `recipes`, every older node reads only `recipe`, and
one copy of the files serves 1.21.11 to 26.3. Either spelling made the other nodes refuse to load a
world. Picking up the item still unlocks the recipe, so nothing a player sees changed.

## Testing

The gametests run without Sophisticated and prove the node still loads without it. The upgrades are
checked in a real client, from backpack templates in `tools/agent/integrations/sophisticated/sophisticated-pack`. Both scripts
say how to set the world up.

The tables below were first run on 1.21.1. On 2026-09-19 every script was run on 1.21.11 and 26.2
and again on 1.21.1, and every case matched, except the ones that need a mod only `1.21.1-neoforge`
has on its runClient classpath (Create's Fluid Tank, Farmer's Delight's cider) and the refused bottle
in the Tank's "no mix" case, which stays in the input slot from 1.21.11. 26.1 was not run in a client.

On 2026-11-13 the Tank, Pump, Drinking, Feeding, Alchemy and Cooking scripts were run again on
`1.21.1-neoforge` with Core 1.5.33 and Backpacks 3.26.21, without Create on the classpath, so all six
mixins apply there. Every grade came out as before. Three expectations had moved with the mod or Core,
not with the integration, and the scripts now say so: a waterskin holds four servings
(`waterskinCapacity`), so the Pump's skin case leaves the tank empty and the Tank's skin case leaves the
filled skin, not yet full, in its input slot; Core 1.5.33's tank result slot took all four bottles of a
bucket at once; and honey (2) and apple cider (6) bring the Drinking "other" case to 12, not 16. Feeding
went from thirst 4 to 18, Alchemy's potion to 10 and its water stayed at 4.

Later that day, for issue #61, on `1.21.1-neoforge`: with Core 1.5.35 and the old code the client
stopped at loading on `TankUpgradeWrapperMixin` (the reported crash). With the fix, the Tank, Pump and
Cooking scripts on Core 1.5.35 gave every result the 1.5.33 run above records (the Pump's Create cases
aside, Create being off the classpath), with no "not a version" warning in the log; and the Tank script
on Core 1.5.33, compiled against 1.5.35 with only the runtime jar swapped, gave the same, through
`TankUpgradeWrapperMixin`. The 26.x nodes, whose newest Cores moved the method the same way, were built
but not run in a client.

What the scripts and the world need on the newer versions, all already in the files:

- **Templates in both shapes.** Every template carries the 1.21.1 keys (`backpackItemRegistryName`,
  `backpackContents`) and the 1.21.11 ones (`itemRegistryName`, `contents`, Core's `ContainerContents`
  with positional `stacks`); each version ignores the other's. The legacy shape the newer Core still
  accepts cannot be used: it needs the registries, which do not exist yet while data packs load, so
  every item came out empty. A potion stacks to one, and 26.2 reads a larger stack in a Tank slot as
  empty, so no template puts more than one in a slot.
- **`/sophisticatedbackpacks`**, the one command name both versions share (`/sbp` became `/sb`).
- **`/reload` first.** On 26.2 Sophisticated reads data-pack templates before item components exist,
  so every item in them fails until a reload.
- **One template per case.** From 1.21.11 a template given twice comes with what the first backpack
  from it ended up holding, so the ocean pump case has its own `pump_world_ocean`.
- **`template create <name> true`.** Created templates live in the world's saved data, and without the
  override a second run exports the first run's result.
- **Into the main hand from slot 1.** A world whose player has another hotbar slot selected gets the
  backpack moved to the main hand after each give.
- **The off hand** is read from `equipment.offhand` from 1.21.5 as well as `Inventory`.
- **A 26.2 world** keeps its generation settings beside `level.dat`, which
  `tools/agent/new_world.py` copies. The driven client passes NeoForge's loading warnings screen by
  itself when none of the warnings is this mod's.

### Tank

[tools/agent/integrations/sophisticated/sophisticated-tank.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-tank.jsonl) builds four
backpacks from the templates in `tools/agent/integrations/sophisticated/sophisticated-pack`, lets each tank run in the main
hand, and exports what is left as SNBT. The file says how to set up the world and what each export has
to show. Checked on 2026-09-19, and once with the mixin disabled for comparison:

| Case | With the mixin | Without |
|---|---|---|
| purity-0 bucket, bottled | tank and bottle `water_purity: 0` | plain water, so the bottle reads Clean |
| four sea-water bottles, one bucket (since 2026-09-19: one bottle into 750 mB of sea water) | a bucket with `water_salty: true`, the glass bottles, empty tank | the bottles are refused |
| dirty bottle into a Pure tank | refused, the tank keeps 250 mB of Pure | refused |
| unstamped bucket, bottled | tank and bottle `water_purity: 2` | plain water |
| a full murky waterskin poured in, an empty one filled | the second holds three servings of `water_purity: 1` | before the capability, neither moved |
| a dirty water bowl poured in, an empty bowl filled | a water bowl of `water_purity: 0` | before the capability, neither moved |

Not checked yet: the cursor path (clicking a container onto the tank in the GUI), which reaches the
same method, and Sophisticated Storage.

### Feeding

[tools/agent/integrations/sophisticated/sophisticated-feeding.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-feeding.jsonl) starves a
player to food 6, sets thirst to 4, and hands them a backpack with a Feeding upgrade and 16 melon
slices. Checked on 2026-09-19: seven slices were eaten, food went to 20 and thirst to 20. With the
mixin left out of the config, food went to 20 and thirst stayed at 4.

### Alchemy

[tools/agent/integrations/sophisticated/sophisticated-alchemy.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-alchemy.jsonl) hands out two
backpacks with three bottles each and an Alchemy upgrade set to Always on that bottle. Checked on
2026-09-19: with Fire Resistance, one potion was drunk and thirst went from 4 to 10, quenched to 8; with
dirty water, nothing was drunk and all three bottles stayed. With the mixin left out of the config the
potion was still drunk but thirst stayed at 4, and the water was refused as well, which is
Sophisticated's own rule rather than the mixin's.

### Pump

[tools/agent/integrations/sophisticated/sophisticated-pump.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-pump.jsonl) builds a one-block
pool on a stone platform and sets its biome with `/fillbiome`, so the grade does not depend on where the
world spawned. Checked on 2026-09-19, then with the Pump and filter mixins left out of the config, then
with only the filter mixin left out:

| Case | With the mixins | Without them |
|---|---|---|
| plains pool | `water_purity: 1`, the same grade as a bottle filled from that pool by hand | plain water |
| ocean pool | `water_salty: true` | plain water |
| plains pool, pump filter set to water | `water_purity: 1` | with only the filter mixin out: nothing collected |
| purity-0 bucket in the off hand, poured in | tank `water_purity: 0`, an empty bucket back | plain water |
| 1000 mB of purity 3, pumped into an empty bucket in the off hand | a bucket with `water_purity: 3`, empty tank | the bucket stays empty |
| 1000 mB of purity 3, pumped into a Create Fluid Tank next to the player | the Create tank holds 1000 mB of `water_purity: 3` | not run |
| a Create Fluid Tank of purity-0 water next to the player, pumped in | tank `water_purity: 0`, the Create tank empty | not run |
| 1000 mB of purity 3, pumped into an empty waterskin in the off hand | four servings of `water_purity: 3`, the tank empty (three and 250 mB left before `waterskinCapacity` went to 4) | not run |

The client ignores the facing part of `tp ... facing`; turn the player with an explicit yaw and pitch.
Holding `use` needs more than four ticks to register as a click.

The two Create Fluid Tank cases were added and run afterwards, with every mixin in, on the same day. A
block entity is not readable from `server.command`, which runs off the server thread, so they read the
Create tank with `client.command` and the answer is the `[CHAT]` line in the client's log.

### Drinking

Three scripts, each starting from thirst 4 unless it says otherwise. Checked on 2026-09-19.

[tools/agent/integrations/sophisticated/sophisticated-drinking.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-drinking.jsonl), what it
drinks:

| Case | Thirst after | Left in the backpack |
|---|---|---|
| one dirty and two Clean bottles, basic upgrade | 16 | the dirty bottle, two glass bottles |
| 1000 mB of Pure in a Tank and one Clean bottle | 20 | 250 mB of Pure, the Clean bottle untouched |
| 1000 mB of sea water in a Tank and one Murky bottle | 4 | everything |
| two Clean and one Pure bottle, Advanced set to Pure and a whole drink | 10 | the two Clean bottles, one glass bottle |
| honey, Farmer's Delight apple cider, milk, an awkward potion, an ominous bottle | 12 (16 before honey and cider went to 2 and 6) | milk, the awkward potion, the ominous bottle, two glass bottles |
| honey before a Clean bottle, from thirst 14 | 20 | the honey: water goes first |

[tools/agent/integrations/sophisticated/sophisticated-drinking-craft.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-drinking-craft.jsonl)
crafts at a real crafting table, moving every ingredient with the slot clicks a screen sends: the basic
upgrade from each mod's upgrade base, and the Advanced one from a basic upgrade set to Dirty, which
keeps `drink_min_purity: 0`. It then places a Sophisticated Storage chest (`deps.sophisticated_storage`
puts Storage on the runClient classpath) and shift-clicks the upgrade in from the inventory:
Sophisticated only puts it in an upgrade slot the tag lets it into, and it went there, not into the
chest. Standing beside the closed chest, thirst went from 4 to 16 and the chest held two glass bottles.

[tools/agent/integrations/sophisticated/sophisticated-drinking-tab.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-drinking-tab.jsonl)
opens the Advanced tab by setting `sophisticatedcore:open_tab_id` on the backpack and clicks its
buttons with `client.click`: one click moved "whole drink" to "any", and one left and two right clicks
took Pure round to Clean, which the export shows as `drink_at: "any"`, `drink_min_purity: 2`. Then it
captures both tabs in every language the mod ships and measures their titles with `client.textWidth`.

**Tab titles have little room.** The label is the tab's width less 26: 37 GUI pixels on the basic tab
and 55 on the advanced one. Measured with the game's font, `Drinki...` (33) fits and `Drinking` (39)
does not, `Adv. Drink...` (55) fits and `Adv. Drinking` (63) does not. Every title fits but one: the
Russian advanced title, `Продв. питьё` (67), is kept anyway and shows cropped, as Sophisticated's
own `Продв. корм.` does, because no shorter form reads naturally. Check a changed title with the tab
script.

### Cooking upgrades

The Smelting and Smoking upgrades cook with the vanilla recipe types, and the mod ships smoking
recipes for water alongside the smelting ones, so bottles need nothing here. Canteens and flasks do:
their recipe matches any fill, and core's `CookingRecipeMixin` copies the servings over when the recipe
is assembled. Core's `CookingLogic` long skipped `assemble` and took the recipe's result as it is
(`getResultItem` on 1.21.1 up to Core 1.5.18, the `result` field on 26.x up to at least 1.5.19), so the
vessel came out purified and empty (issue #53). `CookingLogicMixin` gives the result the input's
servings where `smelt` hands it to `setCookOutput`, which every Core version checked does with a copy of
its own. On a Core that assembles it finds the servings already there and does nothing. The upgrade still
cooks a vessel for the recipe's own time, the default fill's, not per serving as a furnace does.

[tools/agent/integrations/sophisticated/sophisticated-cooking.jsonl](../../../tools/agent/integrations/sophisticated/sophisticated-cooking.jsonl) gives each
upgrade a purity-0 bottle and a piece of coal. Checked on 2026-09-19: both came out with
`water_purity: 2`, through `purify_water_bottle_0_smoking` and `_smelting`. Its later cases smelt
vessels: the mod has smelting recipes for canteens and flasks but no smoking or blasting ones, so the
Smoking and Blasting cases are controls that must leave the flask untouched in the input slot.

Checked on 2026-11-13, `1.21.1-neoforge`, Core 1.5.33 (the PR #58 pins), every case as the file says:

| Case | Result |
|---|---|
| Smoking, purity-0 bottle | slot 2, `water_purity: 2` (`purify_water_bottle_0_smoking`) |
| Smelting, purity-0 bottle | slot 2, `water_purity: 2` |
| Smelting, iron flask, 3 servings of purity 0 | slot 2, `water_servings: 3`, `water_purity: 2` |
| Smelting, copper canteen, 4 servings of purity 0 | slot 2, `water_servings: 4`, `water_purity: 2` |
| Auto-Smelting, iron flask, 3 servings of purity 1 | pushed into the backpack, `water_servings: 3`, `water_purity: 2` |
| Smoking, iron flask (control) | untouched in slot 0, coal unburnt |
| Blasting, iron flask of purity 1 (control) | untouched in slot 0, coal unburnt |

The smelting flask case alone on 26.x, same day, with the current pins and then with an old Core whose
`smelt` reads the recipe's `result` field, with and without `CookingLogicMixin` in the config:

| Node | Core (Backpacks) | Mixin | Flask in slot 2 |
|---|---|---|---|
| `26.1.x-neoforge` | 1.5.34 (3.26.20) | in | `water_servings: 3`, `water_purity: 2` |
| `26.1.x-neoforge` | 1.5.19 (3.26.15) | in | `water_servings: 3`, `water_purity: 2` |
| `26.1.x-neoforge` | 1.5.19 (3.26.15) | out | `water_purity: 2`, no servings: empty, issue #53 |
| `26.2.x-neoforge` | 1.5.32 (3.26.19) | in | `water_servings: 3`, `water_purity: 2` |
| `26.2.x-neoforge` | 1.5.18 (3.26.16) | in | `water_servings: 3`, `water_purity: 2` |
| `26.2.x-neoforge` | 1.5.18 (3.26.16) | out | `water_purity: 2`, no servings: empty, issue #53 |

An old Core needs a Backpacks whose `sophisticatedcore` range accepts it: 26.1.2's 3.26.17 asks for
1.5.20 or later, 3.26.15 for 1.5.15; 26.2's 3.26.16 asks for 1.5.18. Sophisticated Storage's cooking
upgrades go through the same `CookingLogic`, but Storage is off the runClient classpath by default and
was not run.

### Effects on the test player

A world whose `level.dat` was copied by hand from the Farmer's Delight check carried Nourishment on its
player, which cancels all exhaustion, so the Feeding and Alchemy scripts clear the player's effects
first. A world made by `tools/agent/new_world.py` has no player of its own, so that is only a guard now.
