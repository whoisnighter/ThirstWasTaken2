# src/gametest — automated in-game tests

Server-side tests that run in a real Minecraft server with real registries, real items and a real
player, headlessly and in a few seconds.

```bash
./gradlew ":26.3.x:runGametest"
```

```bash
./gradlew ":1.21.1-neoforge:runGametest"
```

CI runs this for every node, NeoForge included, on every push. A failing test fails the build.

Locally, run only the classes a change touches with `-Ptests=<class>,<class>` (with or without the
`GameTest` suffix, case ignored); a name that matches no class fails and lists them all:

```bash
./gradlew ":26.3.x:runGametest" -Ptests=WaterSickness,Canteen
```

**Read the result from the exit code and the report, not the console.** The console is no record:
`1.20.1-forge` runs a dedicated server whose log goes to `run/1.20.1-forge/gametest/logs/latest.log`,
not to Gradle's output, so a grep for `tests passed` finds nothing there, and piping the run into
`grep` hides its exit code. Every node writes `versions/<node>/build/gametest/report.xml`, one
`<testcase>` per test and a `<failure>` inside each failed one. Several nodes in a row:

```bash
for n in 26.3.x 1.20.1-forge; do ./gradlew ":$n:runGametest" -Ptests=Canteen --offline -q > "build/gametest-$n.log" 2>&1; echo "$n exit=$? tests=$(grep -o '<testcase' versions/$n/build/gametest/report.xml | wc -l) failures=$(grep -o '<failure' versions/$n/build/gametest/report.xml | wc -l)"; done
```

`exit=0` with `failures=0` is a pass. Otherwise the reason is in `build/gametest-<node>.log`, or for a
test that failed, in the report's `<failure>`.

The test server's heap is capped at 1536 MB, which keeps a run from pushing a 16 GB machine into
paging; a full run peaks under 1 GB. `-PgametestHeap=2g` raises it. Both are in
`gradle/shared.gradle.kts`.

## What this is

GameTest is Mojang's own framework, shipped inside Minecraft
(`net.minecraft.gametest.framework`), and `fabric-gametest-api-v1` is a thin wrapper that finds
annotated methods and reports results. The runner replaces the dedicated server entirely: it skips
the EULA prompt, never opens a window, places each test in its own patch of a scratch world, then
exits non-zero if any required test failed and writes a JUnit XML report to
`versions/<version>/build/gametest/report.xml`.

The runner would reopen its scratch world, `run/<node>/gametest/world` (`gametestserver` on NeoForge
from 1.21.11), rather than make it anew, so `runGametest` deletes it first, with the config, in
`gradle/shared.gradle.kts`. Kept, it grew by a player file per test on every run, and saving into tens
of thousands of them made shutting the server down slower than the tests.

A mock player stays on the server until something removes it, and every join and leave is sent to every
player online. `TestFixtures.mockPlayer` removes the players of finished tests before it makes another,
and a test that needs a player many times over reuses one rather than making a player apiece: 800 of
them once made one test take 26 seconds and the shutdown a minute.

This is an ordinary Gradle source set, not part of the mod. `thirstwastaken2_gametest` is its own
small mod declared in `src/gametest/resources/fabric.mod.json`, and on NeoForge and Forge also in
`src/gametest/neoforge/resources/META-INF/neoforge.mods.toml` and `src/gametest/forge/resources/META-INF/mods.toml`, so none of it can reach a published
jar. Verify that with `unzip -l build/libs/<jar> | grep gametest` after a release build. The id has an
underscore rather than a hyphen because NeoForge mod ids cannot contain one.

## Writing a test

- Test classes are listed under the `fabric-gametest` entrypoint. A class that is not listed is
  silently never run, on either loader: the NeoForge harness reads the same list.
- A test is a `public`, non-static method taking one `GameTestHelper`, annotated `@GameTest`, ending
  in `helper.succeed()`.
- `helper.assertTrue` takes a `Component` from 1.21.5 and a string before it. Use
  `TestFixtures.check`, which takes a plain string on every version.
- Positions passed to `helper` are relative to the test's own patch of world. Positions passed to
  mod or vanilla code are absolute. `helper.absolutePos` converts; mixing them up is the easiest
  mistake to make here.
- A test body with no sequence runs inside a single tick. That is why `TestFixtures.water` can place
  a water source and sample it immediately: fluid spreading is scheduled, not immediate.
- Drive interactions through `player.gameMode.useItem(...)` rather than calling `ItemStack.use`
  directly. Only the game-mode path applies the result back to the player, which is what the fill
  hooks depend on.
- **Water in the world comes from `TestFixtures.water`,** which makes its patch plains with `/fillbiome`
  first. Where a test lands depends on the seed and on how many tests run before it, so an unpinned
  fixture can stand in an ocean and get salt water: 1.20.1 on Forge failed four drinking and
  filling tests that way once tests were added. A test that needs another biome sets it the same way,
  5 blocks past the block on every axis: `getBiome` picks one of the eight 4x4x4 cells around a block
  by seeded noise, so a tighter box passes or fails by where the test lands (it once read cold ocean).
- Use survival mode for anything that fills a container. `ItemUtils.createFilledResult` behaves
  differently once the player has infinite materials.

## The 1.21.1 harness

Fabric API's own `@GameTest` arrived with 1.21.5. On 1.21.1 a test uses vanilla's annotation with
Fabric's empty structure as its template, and `stonecutter.gradle.kts` rewrites both the import and
the annotation, so no test file changes for it. Write `@GameTest` with no arguments, or that
replacement stops matching.

The same mod tests run on every node. Runners report one or two more because they also include
vanilla smoke tests such as `minecraft:always_pass`; those are not the mod's.

## The NeoForge harness

The `-neoforge` nodes run the same test methods, with no test body changed and no NeoForge-only
branch in any of them. What stands in for Fabric API lives in `src/gametest/neoforge`:

| | Fabric API | NeoForge node |
|---|---|---|
| Annotation | `net.fabricmc.fabric.api.gametest.v1.GameTest` | `com.thirstwastaken2.gametest.neoforge.GameTest`, no arguments. `stonecutter.gradle.kts` swaps the import on `-neoforge` nodes only |
| Discovery | the `fabric-gametest` entrypoints | `ThirstWasTaken2GameTests` reads the same entrypoint list out of this mod's `fabric.mod.json` as plain JSON |
| Registration | `TEST_FUNCTION` at init, test instances when the dynamic registries load | `RegisterEvent` for `TEST_FUNCTION`, `RegisterGameTestsEvent` for the instances |
| Ids | `thirstwastaken2_gametest:<class>_<method>`, snake case | the same rule, so the report names match |
| Defaults | an empty 8x8x8 structure, `minecraft:default`, 20 ticks, required, padding 1 | the same values. The empty structure is this mod's own `data/thirstwastaken2_gametest/structure/empty.nbt`, and the environment an empty one registered as `thirstwastaken2_gametest:default`, the same definition as vanilla's, which the event cannot look up |
| Runner | `-Dfabric-api.gametest` on the server run | ModDevGradle's `gameTestServer` run type |
| Report | `fabric-api.gametest.report-file` | vanilla's `--report`, to the same `versions/<node>/build/gametest/report.xml` |

On 1.21.1 (`1.21.1-neoforge`) there are no test registries, environments or padding yet. The harness
adds a vanilla `TestFunction` per method to `GameTestRegistry.getAllTestFunctions()` from
`RegisterGameTestsEvent`, in `defaultBatch`, with the same structure, ticks and id; it bypasses
NeoForge's own registration, which only accepts vanilla's annotation and takes the structure's
namespace from a NeoForge annotation on the test class. That server has no `--report` option either,
so the build passes the report path as `-Dthirstwastaken2.gametest.report` and the harness installs
vanilla's `JUnitLikeTestReporter` itself. That report has no `minecraft:always_pass`, which the 26.2
and 26.3 nodes' reports do carry.

On 1.21.11 `TestData` has no padding and `TestEnvironmentDefinition` takes no type parameter; the
harness leaves padding out before 26.1 and holds both values in `var`s.

Two things that differ underneath and have not mattered to any test so far:

- Test classes are instantiated on their first test, not while the mod loads: they keep the mod's
  items in static fields, and NeoForge constructs mods before anything may register.
- Fabric API makes the test server report itself as a dedicated server; NeoForge leaves vanilla's
  `false`. Commands are still registered for a dedicated server on both.

## The Forge harness

`1.20.1-forge` runs the same test methods through `src/gametest/forge`, which is the NeoForge 1.21.1
path above under Forge 47's names: its own `GameTest` annotation, swapped in by the same import rule, a
vanilla `TestFunction` per method added from `RegisterGameTestsEvent`, the report through
`-Dthirstwastaken2.gametest.report`, and the empty structure at the plural
`data/thirstwastaken2_gametest/structures/empty.nbt` 1.20.1 reads. Two more pieces are its own:

- **`platform/MockPlayers`.** Vanilla 1.20.1 joins its mock player on a connection with no Netty
  channel, and Forge 47 adds its packet filters to that channel as the player joins, which throws. The
  Forge copy builds the same player on an `EmbeddedChannel`; the Fabric and NeoForge copies call
  `makeMockServerPlayerInLevel`, and `TestFixtures.mockPlayer` calls whichever is compiled.
- **`PlayerSyncGameTest` is skipped**, as on Fabric: Forge reads a client's channels off the Netty
  channel's attributes, which only a real login fills in. `platform/CapturingConnection` says so.

The player save test looks for the thirst value under `ForgeCaps`, where Forge keeps capabilities.

After changing the harness, break the NeoForge `Loader` on purpose and watch the NeoForge nodes alone go red.
Skipping `onUseItem` there fails the bowl and waterskin scooping tests; skipping `onUseBlock` fails the
cauldron bottle draw.

## Rules that keep these tests worth having

- **Every test must be able to fail.** After writing one, break the code it covers on purpose and
  confirm the test goes red. A test that passes against broken code is worse than no test, because
  it reports safety that is not there.
- **Assert against a freshly computed expectation, not a hard-coded number**, wherever the value
  depends on the world. Water quality is sampled from the biome and surroundings, so
  `WaterPurity.sampleAt` is the reference, not a literal.
- **Pair a negative assertion with a positive control.** `dehydratedPlayerDoesNotRegenerate` only
  means something next to `hydratedPlayerStillRegenerates`; without it the first would pass even if
  regeneration never triggered.
- **Datapack behaviour needs a test, because the compiler has no opinion about it.** The
  purification recipes match on components from a JSON file; nothing fails to build when a container
  stops carrying what they look for. `PurificationGameTest` asks the real recipe manager instead.
- **Only assert what the config makes deterministic.** Clean and Pure have no sickness lines and are
  safe to assert; Dirty and Murky are dice rolls, so `WaterSicknessGameTest` forces the roll instead.

## What is covered

| Class | Covers |
|---|---|
| `WaterFillingGameTest` | bottle and bucket filling, the bottle from flowing water too, that each fill resamples the water, that an abandoned fill leaves nothing behind |
| `WaterSourceGameTest` | finite water: on, the waterskin, bowl and bottle take the source they fill from and refuse flowing water, a vessel draws three servings, a waterlogged slab is drained; off, the source stays; Auto following the waterSourceConversion game rule |
| `WaterEffectsGameTest` | salt water, that bad water still quenches, quenched cut by grade and left whole by Upset Stomach, Clean and Pure water, milk and honey, boiling not desalinating |
| `WaterSicknessGameTest` | the sickness tables, rolls forced: each line rolling on its own at its level and seconds, an unknown effect skipped, an empty grade giving nothing, each difficulty reading its own table, a drink reading the world's difficulty, grouped lines sharing one roll and ungrouped ones rolling apart, Upset Stomach extending by half up to 1.5 times and other effects up to twice (and not when switched off), the defaults (no Poison without Upset Stomach, Clean and Pure never anything, Hard's worst roll Upset Stomach II and Poison), and a hand-edited table clamped and filled in, a grouped Poison capped at its Upset Stomach |
| `UpsetStomachGameTest` | Upset Stomach draining no thirst, Nausea still draining on top of it, saturation left whole, and that it never hurts on its own |
| `HealthRegenGameTest` | food healing at half the thirst bar and not below, Upset Stomach, dehydration's switch leaving Upset Stomach's gate, the refund on both heal paths; quenched healing once saturation is spent, at saturation's speed for 6 exhaustion a heal, needing a full bar, half the food bar and no illness |
| `DrainGameTest` | a point a minute standing still, scaled by the global speed and hidden from `EXHAUSTION` listeners; Parched's 4 and 8 a minute whatever the climate; activity scaled by the climate, and the climate curve never falling as it warms |
| `RecoveryGameTest` | water told apart from potions per stack, every container restoring the same serving, each grade's quenched, thirst over a full bar lost, the quenched cap, only Clean and Pure topping up a full bar, and tooltip rows showing the item's value at a full bar |
| `CoveredWaterGameTest` | open water keeping its grade, a roof and leaves capping it at Murky, deep open water not covered by itself, stored water keeping its grade under a roof; and milk curing Poison and Upset Stomach, honey only Poison |
| `ConfigMigrationGameTest` | `defaultPurity`, `rainwaterPurity` and `dripstonePurity` read into the `*Quality` keys: legacy only, new only, both (the new wins), and saving only the new ones |
| `WaterskinGameTest` | mixing, salinity, capacity, emptying |
| `CanteenGameTest` | the copper canteen and iron flask: capacity, one sprite, boiling on a campfire through the real use path (complete, one step short, kept and restarted progress, soul campfire), salt, unlit and the waterskin not boiling, capacities from the config (1, 64, clamped, over a lowered capacity), both vessels in a furnace at 1, 4, 6 and 64 servings and their per-serving time, a real furnace ticked to the end handing both back still full (issue #55), no smoker or campfire recipe, the crafting recipes (canteen, flask, three clay bowls, four-leather waterskin, iron-centred distiller boiler) |
| `TooltipGameTest` | the lines the mod adds to a tooltip, droplet row arithmetic, that the rows need AppleSkin, and that cached lines are handed out as copies |
| `PlayerStateGameTest` | the sprint gate, exhaustion mirroring waiting for the tick, small exhaustion being carried until it crosses a sync step, the Hunger effect cancelling out, and that riding does not dehydrate |
| `CauldronGameTest` | the cauldron blockstate property (water cauldron only, old powder snow saves still load, a fresh cauldron is not sea water), the deferred quality transfer, and the grades rain and dripstone leave behind |
| `HangingPotGameTest` | the copper hanging pot through the real use path: filling, a full pot, mixing grades, drawing a bottle or a waterskin, the frame following the campfire, boiling per serving, topping up, rain and the Nether; the iron pot filling and boiling the same way, and each pot's boil time; a pot saved before its block entity keeping its water, capacities of 1 and 64, a pot over a lowered capacity, and the fill shown in thirds |
| `DistillerGameTest` | the copper distiller as a two-block structure (placing, a taken spot, mining either half, creative), building it in the world (a boiler merging into a firebox, the pipe joining a lined-up pair and refusing any other), what each piece drops, and the tub's coolant |
| `MachineSeamsGameTest` | the version seams a machine stands on: the distiller's block entity type registered for it, a block entity's values and items coming back from a save as the chunk loads one, a menu type building its menu, and fuel burning as long as in a furnace through `Loader.burnTime` |
| `DistillerMachineGameTest` | the distiller at work, ticked by hand: water poured from its slot (a bucket only when all three fit), a bucket of sea water distilled to three Pure servings and counted toward salt, no coolant or nothing to boil burning no fuel, a lit fire holding when the basin is full, the basin filling each container, each face's slots and a real hopper above the tub and below the boiler, the right-click pour and draw, breaking it spilling its slots, and the pipe starting the machine; the fire drawn and giving light only while it burns; a bucket of sea water leaving one salt, fresh water none, a full salt slot holding the sea water, and which item is the salt (the gametest mod tags sugar `thirstwastaken2:distiller_salt` for it, so the salt slot always shows here); tanks of 1 (no bucket) and 64, and the water slot taking one stacked bowl at a time |
| `PurificationGameTest` | which water the furnace accepts: looted bottles yes, salt water never |
| `WaterStackGameTest` | water bowls stacking to `terracottaWaterBowlStackSize` and only with the same water, water bottles not stacking, and drinking from a stack of bowls leaving the rest and one empty |
| `EnvironmentGameTest` | the datapack damage type and its tags, and the version-forked environment call |
| `CreativeTabGameTest` | the creative tab has the right icon and holds every item the mod adds |
| `AdvancementGameTest` | every advancement on the mod's tab loads and hangs off one root, its recipe advancements unlock recipes that exist, and the Cooking Pot files are skipped without Farmer's Delight |
| `ThirstDataGameTest` | the state record: drinking, the quenched cap, overflow into quenched, spending exhaustion, clamping, both codecs round-tripping, including a save from before `enabled` existed, and a whole player saved to NBT and loaded back, which is the attachment under the codec |
| `ThirstTickGameTest` | the tick spending quenched before thirst, peaceful with and without depletion, disabled and invulnerable players, Fire Resistance and Fire Protection slowing the drain, salt water charging at once, the full-bar rule |
| `DrinkingGameTest` | drinking a bowl and a waterskin through the real right-click path, the canteen and flask restoring what a waterskin does at any grade, what is left in the hand, the drink animation and duration, water refused on a full bar while honey is not, the advancements a drink earns, drinking by hand with every way it is refused, and drinking a cauldron level with an empty hand |
| `ThirstApiGameTest` | what items restore from the config, the blacklist, the `c:drinks` tag fallback (the gametest mod tags a nautilus shell for it) and the magic drinks it leaves out, keyword matching and its blacklist, the per-item cache dropping on commit, and `sanitize` clamping a hand-edited config |
| `IntegrationApiGameTest` | what another mod can use: data pack values loading, an unknown item skipped without losing its file, every step of the resolution order (config and blacklist over a data pack, a data pack over `c:drinks`, an entry of nothing over keywords), re-parsing the same packs, the sync payload round trip, the player and purity methods on `ThirstApi`, and `ThirstEvents` changing, cancelling and surviving a throwing listener. The gametest mod ships its data pack file and tags dried kelp `c:drinks` for it. Listeners cannot be unregistered, so each acts on its own test's players only |
| `CommandGameTest` | `/thirst set` and `/thirst enable` through the dispatcher, the argument range, and the permission requirement |
| `WaterInteractionsGameTest` | scooping with the bowl and the waterskin, the clay bowl holding nothing, filling the waterskin in one scoop, drawing it from a cauldron only as deep as the cauldron is, pouring it out, and a bottle drawn from a cauldron keeping its grade |
| `LootGameTest` | graded water in each seeded chest and in piglin bartering, no water anywhere else, and a table a data pack replaced still getting it |
| `ItemAppearanceGameTest` | the custom model data bowls and waterskins dispatch on, that sea water leaves vanilla's bottle and bucket sprite alone, and the waterskin bar's width and colour |
| `ContainerFluidGameTest` | the waterskin and the bowls through each loader's item fluid API: whole servings only, one grade per container, the grade carried both ways, sea water staying salty, water with no grade filling as `defaultPurity`, no lava. `platform/ContainerFluids` runs the same assertions through NeoForge 1.21.1's `IFluidHandlerItem`, NeoForge's transfer API from 1.21.11 and Fabric's Transfer API, in millibuckets |
| `PlayerSyncGameTest` | that a player is told their own thirst and no one else's, with three players in range of each other. NeoForge only: what it exists to catch is `Loader.syncsTo`, and Fabric's counterpart is a value handed to Fabric API rather than a function the mod writes. There the same check is two agent clients, as [.../dev/agent/AGENTS.md](../../dev/java/com/thirstwastaken2/dev/agent/AGENTS.md) describes |

`WaterskinGameTest` also covers pouring a bottle or bucket into a slotted waterskin from the cursor,
and `PurificationGameTest` the grade every boiling recipe produces and the crafted water bowl.

`PlayerSyncGameTest` is the one test whose fixture has to be proved before its assertions mean
anything, and it is worth knowing why. Both loaders build the sync list out of who is *tracking* the
player, and three simulated players standing still are tracked by nobody: the chunk map asks once
when a player is added, which is before a single chunk has been sent, and afterwards it only asks
again about an entity whose section has changed. So the sync list was `[self]` whatever the predicate
said, and the test passed with `syncsTo` opened wide. `SyncPlayer.settle` now does both halves once a
tick — it sends the chunks the tracking view is waiting on, and moves each player a section up and
back down, vertically so the chunk never changes while the section does — and two assertions run
before the real ones: that each player is watching the chunk, and that a broadcast about one of them
reaches all three. A run where nobody is watching anybody fails as a broken fixture rather than
passing on nothing.

What none of them can reach - the client, damage to a player, other dimensions and biomes, real
time - is in [docs/dev/MANUAL-TESTING.md](../../../docs/dev/MANUAL-TESTING.md), with a section per
version.

The test server enables every experiment, including the Villager Trade Rebalance data pack, which
replaces the mineshaft chest. `LootGameTest` relies on that to test a replaced table, and checks the
pack is enabled first so that a server that stops enabling it fails loudly instead of proving nothing.

## Known limits of the harness

A mock player reports `GameType.CREATIVE` and ignores `setGameMode`, and vanilla refuses to damage a
creative player. Anything whose outcome is vanilla applying damage cannot be asserted here; assert
that the mod registered the right thing instead. `EnvironmentGameTest` checks that the dehydration
damage type and its `bypasses_armor` entry loaded, and leaves losing health to a manual check.

Its abilities are survival-like even so, which is why the filling tests behave normally.

Client rendering is not covered, and neither is anything on a timer: dehydration damage, peaceful
regeneration and the faster Nether drain all stay manual. `fabric-client-gametest-api-v1` could cover
the rendering, but it needs a real window and screenshot baselines maintained per Minecraft version.

Much of what forks per version is client-side, so the manual pass that matters after a HUD change is
the thirst bar (a mixin of its own on 1.21.1), the tooltip glyphs, F1 and the config screen, on each
version.
