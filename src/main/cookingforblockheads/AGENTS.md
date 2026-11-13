# src/main/cookingforblockheads — Cooking for Blockheads' Sink gives Murky water

[Cooking for Blockheads](https://modrinth.com/mod/cooking-for-blockheads) (mod id `cookingforblockheads`,
package `net.blay09.mods.cookingforblockheads`, source
[TwelveIterations/CookingForBlockheads](https://github.com/TwelveIterations/CookingForBlockheads)) is a
kitchen multiblock: a cooking table that crafts food from the fridges, counters and sinks around it, an
Oven, a Sink, a Milk Jar and a Cow in a Jar. It requires Balm. It is a small integration, so it has no
plan doc; what was looked at and decided is here.

**Built on every node**, all three loaders: the mod has a build for every Minecraft version and loader
this one supports (16.0.15 on 1.20.1, 21.1.24 on 1.21.1, 21.11.6 on 1.21.11, 26.1.2.6, 26.2.0.5 and
26.3.0.2), each pinned by Modrinth version id in `stonecutter.properties.toml`, beside the Balm it
requires (`deps.balm`, on `runClient` only).

## What it does

- **The Sink gives Murky water.** By default it is an endless tap; with its `sinkRequiresWater` setting
  it is a tank of 16 buckets that keeps only an amount of plain water. Either way it handed out plain
  water bottles and buckets, which read as `defaultQuality`, Clean, and lacked the `water_salty: false`
  every cooking recipe matches on. Now whatever leaves it is Murky (`SinkWater.TAP`), the grade
  Candlelight's and Hearth and Harvest's sinks give, whatever filled it. It stays an endless tap, as its
  author meant; its water wants boiling. With a full inventory the filled container is dropped at the
  player's feet (vanilla's `ItemUtils.createFilledResult`); `SinkWater.after` also grades plain water in
  an item entity beside the player that has not ticked yet, so that is no way round it.
- **The Sink refuses sea water** by hand, from any water container: the click is spent and the stack stays
  in the hand. In a tank it would have come out fresh; on the endless tap it was only thrown away, and
  refusing it there too keeps the rule one rule.

## What needed nothing

- **The Oven** cooks with Minecraft's campfire, smoking and smelting recipes (smelting only when the
  result is food), through `RecipeManager.getRecipeFor` and `assemble`. So the mod's smoking recipes
  boil a Dirty or Murky bottle, bowl or bucket to Clean in it, and their ingredients, which ask for a grade
  and `water_salty: false`, refuse sea water. Nothing to add; the agent script checks both.
- **The kitchen crafting** lists only recipes whose result is food (or in its `foods` or `ingredients`
  tags), consumes the real stacks from the inventories around it, components and all, and hands the
  result of `assemble` over. Nothing of this mod's is food, and a recipe's water is used up in it, so no
  grade is stripped from anything that is drunk. The Sink offers recipes the items in
  `cookingforblockheads:water` (a plain water bucket) as an ingredient; that water is used up too.
- **The Milk Jar and the Cow in a Jar** hold milk, not water. A milk bucket restores thirst through the
  mod's config like any other.
- **No drinks of its own**, so no `ThirstConfig` ids and no data files. Its 26.3 Salt Filter is a recipe
  source for its own salt and has nothing to do with sea water.

```
cookingforblockheads/java/com/thirstwastaken2/cookingforblockheads/
  CookingForBlockheadsMixinPlugin   the gate: the Sink class looked up as a resource, its click method read off the class file
  SinkWater                         TAP (Murky), the sea water test, and the inventory compared before and after a click
  mixin/SinkBlockMixin              SinkBlock.useItemOn (use on 1.20.1): refuse at HEAD, grade what was drawn at RETURN
cookingforblockheads/resources/
  thirstwastaken2.cookingforblockheads.mixins.json
```

## How it works

- **Graded by what changed, not by where it came from.** Water leaves the Sink by different routes on
  different versions: a bottle the Sink builds itself, a bucket through Balm's `useFluidTank` hook (1.20.1
  to 26.2) or through its own `FluidInteractions` (26.3), and on the versions with the Balm hook the mod's
  own waterskins and bowls through the loader's fluid API, which grades ungraded water `defaultQuality`.
  Hooking each would mean a mixin per route and version, some of them in Balm. Instead the HEAD injector
  copies the player's inventory (41 stacks, once per click, server only), and the RETURN injector
  compares every slot: a water container new in a slot is stamped Murky; a stack of the same water that
  grew has its new ones split off and stamped; one of this mod's vessels that gained servings has them
  mixed in Murky through `WaterContainers.holding`, serving-weighted as pouring by hand is. A slot that
  lost water poured it into the Sink and keeps its grade: a skin half poured out is not regraded.
- **Creative mode** goes through the same comparison: `ItemUtils.createFilledResult` leaves the empty
  container in a creative hand and adds the filled one to the inventory, where it is found.
- **Only the signature forks.** The click is `use` on 1.20.1, returns an `ItemInteractionResult` on
  1.21.1 and an `InteractionResult` from 1.21.2. Everything else is in `SinkWater`, which compiles
  unchanged on every node.
- **Compiled against the mod**, `modCompileOnly` on Fabric and Forge (remapped) and `compileOnly` on
  NeoForge, so `@Mixin(SinkBlock.class)` and `useItemOn` are remapped to intermediary and SRG with the
  rest. Balm is not compiled against: the Sink's class hierarchy is the mod's and Minecraft's.

## Not covered

- **Water piped into a `sinkRequiresWater` Sink.** Balm's tank keeps a fluid and an amount, so the grade
  of piped water is lost on the way in, as it is poured in by hand; it comes out Murky. Sea water piped in
  is not refused, and comes out Murky too. It needs a non-default setting and a pipe of sea water.
- **Salty containers in the kitchen.** The cooking table never asks a recipe whether it matches; it finds
  each ingredient on its own and calls `assemble`. So Croptopia's and Bountiful Fares' refusals of sea
  water, which sit in `matches`, do not stop a bucket of sea water kept in a fridge from crafting their
  water bottles or teas. The item providers differ by loader and version (1.20.1 has a different kitchen
  altogether), so this was left out of a small integration.

## How it stays optional

1. **Build.** Only where `deps.cooking_for_blockheads` is set (every node) do the loader scripts add this
   directory and append the mixin config to the built manifest, with `cookingforblockheads` as an optional
   dependency on NeoForge and Forge, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=cooking-for-blockheads` leaves it and Balm out of `runClient`.
2. **Mixin plugin.** `shouldApplyMixin` looks the Sink's class up as a resource and reads its methods off
   the class file, never loading it, and logs a warning when the click method is gone. Both injectors are
   `require = 0`.
3. **Nothing else names the mod.** `SinkWater` names only Minecraft and core types and is loaded only
   from the mixin.

## Checking it

- The gametests run without the mod. Every node builds, and `checkOptionalSeam`, `checkDataConditions`
  and `checkLang` pass on all of them; `runServer`, which never has the mod, comes up clean on `26.3.x`
  and `1.21.1-neoforge`.
- [tools/agent/integrations/cooking-for-blockheads.jsonl](../../../tools/agent/integrations/cooking-for-blockheads.jsonl)
  checks the Sink and the Oven in a real client, from 1.21.1 up; its header says how to run and verify it.
  It passed whole on 2026-11-13 on `26.3.x` (Fabric) and `1.21.1-neoforge`. On 1.21.1 NeoForge the Sink
  filled an empty waterskin through Balm's hook, 4 of 4 Murky; on 26.3 the Sink no longer goes through
  Balm and leaves a waterskin empty. Not run on 1.20.1, whose commands have no item components.
