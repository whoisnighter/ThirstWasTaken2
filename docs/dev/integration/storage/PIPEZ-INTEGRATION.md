# Pipez: the copper distiller as a fluid block

Asked for in [issue #65](https://github.com/whoisnighter/ThirstWasTaken2/issues/65) on 26.2 NeoForge:
pipe Murky water from a Sophisticated Storage tank into the copper distiller, and its Pure water out
into another tank.

## What Pipez is

[Pipez](https://github.com/henkelmax/pipez) (henkelmax) has item, fluid, energy and universal pipes.
Builds: NeoForge 1.20.2 to 26.3, Forge up to 1.20.2, no Fabric. Its fluid pipe
(`FluidPipeType`) reads only the loader's own block capability, `Capabilities.Fluid.BLOCK` on 26.x
(`FluidHandler.BLOCK` on 21.1, `ForgeCapabilities.FLUID_HANDLER` on Forge), and moves through
`ResourceHandlerUtil.move`. `FluidPipeBlock.canConnectTo` connects, and draws its arm, to any
neighbour that answers that capability.

So there is nothing of Pipez's to call. The gap was ours: the distiller answered no fluid capability
at all, only the hopper's `WorldlyContainer`. Sophisticated's tanks already answer it, which is why
tank-to-tank piping worked.

## Decisions

- **No Pipez code, no deps key, no table row.** The distiller answers each loader's standard fluid
  capability. Any pipe, pump or tank that uses it works, Pipez among them: Mekanism's pipes, Create's,
  Sophisticated's pump, and on Fabric Create Fly's.
- **No model or texture of ours.** Pipez draws its own connection once the capability is there.
- **Water goes in to the boiler, Pure water comes out of the basin**, through either half where the
  loader allows it (NeoForge, Fabric). On Forge 1.20.1 a capability lives on a block entity, and only
  the boiler half has one, so only it is piped. Two tanks are listed: 0 the boiler, which takes water
  and gives none, 1 the basin, which gives Pure water and takes none.
- **Any amount moves, not whole servings.** Pipez's unupgraded fluid pipe moves 50 mB a tick, its
  Basic one 100; a 250 mB serving rule, as the carried containers keep, would block both. The tanks
  now count millibuckets underneath (a serving is still 250 mB), keep the part serving in the save
  (`boiler_partial`, `basin_partial`) and boil only whole servings. A bucket, bottle or skin still
  needs whole servings, as before.
- **Grades mix as poured by hand**: the boiler keeps the worse of what it holds and what comes in, salt
  stays salt. Water with no grade counts as `defaultQuality`. Anything that is not water is refused.
- **The basin gives Pure water stamped Pure**, the one component the other fluid integrations read.
  A request for water of another grade, or unstamped, gets nothing, the same exact comparison a tank
  makes.
- **The tub's coolant is not piped.** It is poured once and stays; a pipe gives it no reason to exist.
- **Transactions are respected.** NeoForge 26.x and Fabric snapshot the tanks per machine and save and
  sync only on the root commit, so a simulated or aborted move leaves nothing behind.

## Where the code is

| Loader | API | File |
|---|---|---|
| core | tanks in mB, `fillBoiler`, `drainBasin`, snapshot | `block/DistillerBlockEntity` |
| NeoForge 21.1 | `IFluidHandler`, `Capabilities.FluidHandler.BLOCK` | `src/main/neoforge-fluidhandler/.../DistillerFluidHandler` |
| NeoForge 26.x | `ResourceHandler<FluidResource>`, `Capabilities.Fluid.BLOCK` | `src/main/neoforge-transfer/.../DistillerResourceHandler` |
| Forge 47 | `IFluidHandler`, `ForgeCapabilities.FLUID_HANDLER` | `src/main/forge-fluidhandler/.../DistillerFluidHandler` |
| Fabric | `Storage<FluidVariant>`, `FluidStorage.SIDED` | `src/main/fabric/.../DistillerStorage` |

`DistillerFluidGameTest` drives every handler through its loader's capability lookup, the way a pipe
does, in Pipez's 50 mB steps.

## State

Built (2026-11-19). The gametests pass on every node. Not yet tried in game with Pipez itself; the
issue's reporter offered to test a build.
