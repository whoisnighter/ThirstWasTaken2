# src/main/spelunkery — salt boiled from sea water in Spelunkery

[Spelunkery](https://modrinth.com/mod/spelunkery) (mod id `spelunkery`, package `com.ordana.spelunkery`)
is a cave overhaul. Two things in it touch water: Spring Water, a fluid in `#minecraft:water` that pools
in mountains, and `spelunkery:salt_from_boiling`, a furnace recipe that boils any water bucket into a
salt bucket. The plan and its decisions are
[docs/dev/integration/world/SPELUNKERY-INTEGRATION.md](../../../docs/dev/integration/world/SPELUNKERY-INTEGRATION.md).

**Built on `1.21.1` and `1.21.1-neoforge`**, the mod's only builds for a version this mod supports
(0.4.4). Its 1.20.1 line is no longer updated and is not built against. What it does:

- **Boiling into salt**: a cooking recipe whose result is `spelunkery:salt_bucket` matches only sea
  water. A fresh bucket matches only our purification recipes, so it is purified rather than turned
  into salt, and a Pure one stays in the furnace. A sea water bucket, which nothing of ours cooks,
  boils into salt.

Spring Water is not here. It is in the fluid tag `thirstwastaken2:salt_water`, written by
`ThirstFluidTagProvider` in `src/datagen` as optional entries, which `WaterPurity.sampleAt` grades salty
before the biome, while `enableSeaWater` is on. It was Pure until 2026-11-19; see the plan's decision 2. That is data and core code naming no class of the mod, and it ships on every node.

```
spelunkery/java/com/thirstwastaken2/spelunkery/
  SpelunkeryMixinPlugin              applies the mixin only where Spelunkery's main class is on the classpath, by resource lookup
  mixin/AbstractCookingRecipeMixin   AbstractCookingRecipe.matches: a salt bucket recipe refuses all but sea water
spelunkery/resources/
  thirstwastaken2.spelunkery.mixins.json
```

## How it works

- **Why not override the recipe file.** Our jar could ship its own
  `data/spelunkery/recipe/salt_from_boiling.json`, but on Fabric 1.21.1 nothing orders one mod's data
  over another's (`fabric:resource_load_order` came after Fabric API 0.116), so whose file wins is
  chance. A failed `matches` takes the recipe out of the furnace's choice wherever it comes from.
- **By result, not by id.** `matches` does not know its recipe's id, and the result is what makes it
  Spelunkery's. That also covers a data pack's smoker or campfire copy.
- **Worked out once per recipe.** Whether a recipe makes a salt bucket is stored in a field of the
  recipe on its first successful match: recipes never change, and a furnace asks every few ticks.
- **The cached last recipe goes through `matches` too**, so a furnace that last boiled sea water does
  not take a fresh bucket next.

## How it stays optional

1. **Build.** Only where `deps.spelunkery` is set do the loader scripts add this directory and append
   the mixin config to the built manifest (on NeoForge with `spelunkery` as an optional dependency),
   from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Nothing is compiled against Spelunkery. It and Moonlight Lib are on `runClient` only.
   `-PwithoutOptional=spelunkery` leaves Spelunkery out; `-PwithoutOptional=moonlight` leaves it and
   Supplementaries out.
2. **Runtime gate.** The mixin's target is Minecraft's, always there, so `SpelunkeryMixinPlugin` looks
   up `com/ordana/spelunkery/Spelunkery.class` as a resource instead, never loading it.

## Checking it

- `checkOptionalSeam` finds the plugin loaded without the mod, and passes.
- `runGametest` passes unchanged.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=spelunkery`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/spelunkery.jsonl](../../../tools/agent/integrations/spelunkery.jsonl).
