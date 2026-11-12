# src/main/littlelogisticsforge — buckets on Little Logistics' tanks, on Forge 1.20.1

The same bucket mixins as [src/main/littlelogistics](../littlelogistics/AGENTS.md), for Little Logistics
1.20.1.1 on Forge 47, the `1.20.1-forge` node. Read that guide first; this one records only what differs.

- **Its own directory**, since the mixins name the loader's `IFluidHandler` and `FluidUtil`. Same deps
  key, `deps.little_logistics`, and [its own row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
- **The Fluid Hopper** (`FluidHopperTileEntity.use`) is clicked the same way as the car and barge, and is
  wrapped too. Only this build has it.
- **Buckets** go through `com.thirstwastaken2.forge.WaterQualityFluidHandler`, the Forge copy of the
  NeoForge one, where the grade is an NBT tag.
- **The jar is in SRG names**, so it is a `modCompileOnly` and remapped. The barge and hopper mixins are
  `remap = false`; the car's click is Minecraft's `interact`, so that mixin is remapped through the
  refmap and only its `@At` target, Forge's, is `remap = false`. Since `interact` is an SRG name in the
  jar, the plugin asks the car for its own `getFluidStack` instead.

```
littlelogisticsforge/java/com/thirstwastaken2/littlelogisticsforge/
  LittleLogisticsForgeMixinPlugin, LittleLogisticsForgeWater,
  mixin/FluidTankCarEntityMixin, mixin/FluidTankBargeEntityMixin, mixin/FluidHopperTileEntityMixin,
  mixin/ForgeFluidUtilMixin
littlelogisticsforge/resources/
  thirstwastaken2.littlelogisticsforge.mixins.json
```

## Checking it

[tools/agent/integrations/little-logistics-1.20.1.jsonl](../../../tools/agent/integrations/little-logistics-1.20.1.jsonl)
makes the same car checks as on NeoForge, in Forge's NBT, then the same two on a Fluid Barge floating on a
pool and on a Fluid Hopper. Uncomment Little Logistics' `runClientMod` line in `build.forge.gradle.kts`
first. The car passed on 2026-11-11, the barge and hopper on 2026-11-12. With the mixin list emptied, all
six checks fail and every bucket comes out plain, so the script does catch the bug.

The shipped reobf jar was also booted on a real Forge 47.4.28 dedicated server with Little Logistics
1.20.1.1 and `-Dmixin.debug.export=true`: all four targets were exported with their `thirst$` member, the
car's from its SRG `m_6096_` through the refmap, and a Fluid Hopper emptying Murky water into a car left
the car Murky. The same server without Little Logistics starts clean.
