# src/main/farmandcharm — water in Let's Do: Farm & Charm, and Candlelight's kitchen sinks

[Let's Do: Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm) (mod id `farm_and_charm`, package
`net.satisfy.farm_and_charm`) has three blocks that hold water and three that cook with it. Its addon
[Let's Do: Candlelight](https://modrinth.com/mod/lets-do-candlelight-farmcharm-compat) (mod id
`candlelight`) has no water code of its own: its eleven kitchen sinks are Farm & Charm's `SinkBlock` (Foundation's from 1.1.28, the library nested in Farm & Charm's jar, which both loader scripts unpack onto the compile classpath), its
stoves extend Farm & Charm's, and its Large Cooking Pot runs Farm & Charm's recipes. So this one
directory serves both, gated on Farm & Charm, and Candlelight has no row in the integration table. The
plans and their decisions are
[docs/dev/integration/lets-do/FARM-AND-CHARM-INTEGRATION.md](../../../docs/dev/integration/lets-do/FARM-AND-CHARM-INTEGRATION.md)
and [CANDLELIGHT-INTEGRATION.md](../../../docs/dev/integration/lets-do/CANDLELIGHT-INTEGRATION.md).

**Built on `1.21.1` and `1.21.1-neoforge`**, both mods' only builds for a version this mod supports
(Farm & Charm 1.1.28, Candlelight 2.1.15). Their 1.20.1 line is no longer updated and is not built
against. What it does:

- **Timber Well**: a bucket drawn from it carries the grade of the groundwater the well pumps, sampled
  where that source block lies, so a swamp well gives Dirty water and a beach well sea water. With no
  groundwater below, the well was filled by rain and gives `rainwaterPurity` (Clean by default); with
  rain collection off, the bucket is left unstamped.
- **Water Trough**: refuses a bucket of sea water, and what is drawn from it is Murky whatever went in.
- **Kitchen sink** (Candlelight): the same as the trough. It still fills from nothing, as its authors
  meant, but its water wants boiling.
- **Cooking Pot, Stove, Crafting Bowl** (Farm & Charm) **and Large Cooking Pot** (Candlelight): a salty
  stack matches no ingredient, so sea water cooks nothing. From Candlelight 2.1.14 the Large Cooking Pot
  extends Farm & Charm's Cooking Pot, so the Cooking Pot's target covers it; its own target is only for
  2.1.13 and older, which had a matcher of their own. Fresh water of any grade still cooks; what
  comes out is its own item, as tea is.

The drink and food values are not here. They are ids in `config/itemvalues/LetsDoItemValues` (`farmAndCharmDrinks`,
`farmAndCharmFoods`, `candlelightFoods`), common code that names no class of either mod.

```
farmandcharm/java/com/thirstwastaken2/farmandcharm/
  FarmAndCharmPresence      the gate: each target class looked up as a resource, the mods' own methods read off the class file
  FarmAndCharmMixinPlugin   applies each mixin on the targets that are there, Candlelight's pot only while it has its own matcher
  StandingWater             the grades: Murky for standing water, the well's groundwater sample, the salty bucket test
  mixin/TimberWellMixin     TimberWellBlock.useItemOn: stamps the filled bucket before ItemUtils.createFilledResult
  mixin/WaterTroughMixin    WaterTroughBlock.useItemOn: refuses a salty bucket; Murky through setItemInHand
  mixin/SinkMixin           Foundation's SinkBlock.useItemOn: refuses a salty bucket or bottle; Murky through Inventory.add or Player.drop
  mixin/SeaWaterIngredientMixin  every Ingredient.test in the four matchers: false for a salty stack
farmandcharm/resources/
  thirstwastaken2.farmandcharm.mixins.json
```

## How it works

- **One grade for standing water.** `StandingWater.STANDING` is Murky, and the trough and the sink share
  it so the two mods agree. Neither block can keep a grade (a level, a filled flag), so the grade is
  decided as the water comes out, from what the block is rather than what was poured in: nothing gets
  better by going through. Sea water is refused on the way in, since it would come out fresh.
- **The well samples on the draw.** `StandingWater.wellWater` scans the box `TimberWellBlock.hasGroundwater`
  scans, in its order, and hands the first source to `WaterPurity.sampleAt`. That is one sample per
  bucket, on the server, never on a tick, as `purity/AGENTS.md` asks. If rain and groundwater both
  filled it, the groundwater decides.
- **The recipes are matched per block.** None of the four goes through `GeneralUtil.matchesRecipe`: each
  has a private matcher (`matchesInventory`, `findBestMatchingSlot` and `countMatchingSlots`,
  `matchExact`) testing every ingredient against every slot. `SeaWaterIngredientMixin` wraps
  `Ingredient.test` in all of them. Only water containers carry salt, and `#c:water_bottles` holds only
  the water bucket, so a salty stack matching nothing is sea water not counting as water. Candlelight's
  stoves extend Farm & Charm's, so its stove target covers them.
- **Bottles and canteens are not accepted** by these recipes, as before: that is the tag Farm & Charm
  chose.

## Names on Fabric

The Fabric jars are in intermediary. `useItemOn` and every Minecraft call the mixins target are
remapped by Loom (`method_55765`, `Ingredient.test` as `method_8093`), which is why Farm & Charm is a
`modCompileOnly`. `SeaWaterIngredientMixin` says `remap = false` for the mods' own method names and
`remap = true` on its `@At`, and names Candlelight's pot by string, since Candlelight is not compiled
against. The plugin checks only the mods' own method names: `useItemOn` is a different name in each
loader's jar, so a block's mixin asks only that its class is there.

Loom writes a target named by string into the Fabric jar with slashes
(`net/satisfy/candlelight/...`), and Mixin hands it to `shouldApplyMixin` that way, while a target named
by class comes dotted. The plugin turns slashes into dots before it looks a target up. It did not up to
1.10.1, so the Large Cooking Pot was applied unchecked on Fabric and crashed every client with
Candlelight 2.1.14 or later, whose pot has no `matchesInventory` (issue #56; NeoForge, dotted, was
fine). `SeaWaterIngredientMixin` also has `require = 0`, so a matcher that changes shape upstream turns
it off for that block rather than stopping the game.

## How it stays optional

1. **Build.** Only where `deps.farm_and_charm` is set do the loader scripts add this directory and
   append the mixin config to the built manifest (on NeoForge with `farm_and_charm` as an optional
   dependency), from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Farm & Charm, Candlelight and Architectury API are on `runClient` only, so `runServer` and the
   gametests run without them. `-PwithoutOptional=farm-and-charm` leaves both mods out and
   keeps Architectury, which HerbalBrews and Beachparty need too (`-PwithoutOptional=architectury` leaves
   every Let's Do mod out); `-PwithoutOptional=candlelight`
   leaves Candlelight out and keeps Farm & Charm.
2. **Runtime gate.** `FarmAndCharmPresence` answers with resource lookups and class file reads, never
   loading a class, and names no class of either mod, no Minecraft class and no loader.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate for each target, so without Candlelight the
   ingredient mixin applies to Farm & Charm's three blocks and skips the Large Cooking Pot.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mods, and passes.
- `runGametest` passes unchanged: the mods are never on its classpath.
  `farmAndCharmDrinksAreMergedIntoAnOlderConfig` checks the config values of both.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=farm-and-charm`
  comes up and stays up, and so does `-PwithoutOptional=candlelight`.
- What it does is checked in a real client with
  [tools/agent/integrations/farm-and-charm.jsonl](../../../tools/agent/integrations/farm-and-charm.jsonl),
  whose header says how to run and verify it.
