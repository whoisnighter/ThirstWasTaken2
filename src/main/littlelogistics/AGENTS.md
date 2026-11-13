# src/main/littlelogistics — buckets on Little Logistics' tanks, on NeoForge

[Little Logistics](https://modrinth.com/mod/little-logistics) (mod id `littlelogistics`, package
`dev.murad.shipping`) carries fluids in its Fluid Tank Car and Fluid Barge, each a NeoForge `FluidTank`.
Asked for in [#52](https://github.com/whoisnighter/ThirstWasTaken2/issues/52).

**Built on `1.21.1-neoforge` only**, its newest build (1.21.1.1-beta.2, NeoForge only). Its 1.20.1 build
for Forge 47 is in [src/main/littlelogisticsforge](../littlelogisticsforge/AGENTS.md).

## What it does

A right click on the car (`FluidTankCarEntity.interact`) or the barge (`FluidTankBargeEntity.doInteract`)
hands the held container to NeoForge's `FluidUtil.interactWithFluidHandler`. NeoForge's bucket wrapper
gives and takes plain water, so a bucket drawn from a car of Murky water came out Clean, and a Murky
bucket poured in went in plain. Each click is wrapped in `LittleLogisticsWater.graded`, and inside it,
and only there, `NeoForgeFluidUtilMixin` hands back core's `WaterQualityFluidHandler` for the container,
as Immersive Engineering and Extra Delight do.

Nothing else needs code: the tanks compare components, so grades never merge, and the Docking Station
and pipes pass stacks through unchanged. The 1.21.1 build has no Fluid Hopper block.

```
littlelogistics/java/com/thirstwastaken2/littlelogistics/
  LittleLogisticsMixinPlugin   applies each mixin only with the mod there and its target method still declared
  LittleLogisticsWater         the graded() click scope
  mixin/FluidTankCarEntityMixin, mixin/FluidTankBargeEntityMixin   the click, inside graded()
  mixin/NeoForgeFluidUtilMixin FluidUtil.getFluidHandler(ItemStack): graded handlers, inside graded() only
littlelogistics/resources/
  thirstwastaken2.littlelogistics.mixins.json
```

## How it stays optional

1. **Build.** Only where `deps.little_logistics` is set does the NeoForge script add this directory and
   append the mixin config and an optional `littlelogistics` dependency to the built manifest, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   It is a `compileOnly`; on `runClient` it is off by default. Uncomment its `runClientMod` line in
   `build.neoforge.gradle.kts` to work on it.
2. **Runtime gate.** `LittleLogisticsMixinPlugin` answers from class files, never loading a class, and
   skips a mixin whose target lost its method, with a warning.

## Checking it

[tools/agent/integrations/little-logistics.jsonl](../../../tools/agent/integrations/little-logistics.jsonl)
fills a Fluid Tank Car with Murky water by NBT, draws a bucket from it and pours that into an empty car,
then does the same with Fluid Barges floating on a pool. The car passed on 2026-11-11, the barge on
2026-11-12. Its header says what each line should read.

The control, with this directory's mixin list emptied, was run on 2026-11-13 on `1.21.1-neoforge` (and
earlier on Forge, see [littlelogisticsforge](../littlelogisticsforge/AGENTS.md)): all four checks
said "Test failed". The car and barge still held 10000 and 4000 mB of `water_purity: 1`, but each bucket
drawn from them was a plain `minecraft:water_bucket`, read as Clean, and the car and barge it was poured
into held 1000 mB of plain water.
