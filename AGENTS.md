# ThirstWasTaken2

A thirst mod for **Minecraft 26.3, 26.2, 26.1.x, 1.21.11 and 1.21.1** on **Fabric** and **NeoForge** (1.21.11 on Fabric only): a
thirst bar, water purity and drinking. It began as a port of
[Thirst Was Taken](https://github.com/ghen-git/Thirst-Mod) (Forge, 1.19.2) and has diverged, so
upstream is a reference, not a spec. Its source is expected at `../Thirst-Mod` when comparing.

- Mod id and resource namespace: `thirstwastaken2`. Java package: `com.thirstwastaken2`.
- Published on [Modrinth](https://modrinth.com/mod/thirst-was-taken-2) and
  [CurseForge](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2).
- Every area below has its own `AGENTS.md`; read it before changing that area (see
  [Where to look](#where-to-look)).

## Build and run

One source tree, one jar per node. Nodes are the Gradle subprojects in `settings.gradle.kts`:
`26.3.x`, `26.2.x`, `26.1.x`, `1.21.11`, `1.21.1` on Fabric, and the same but `1.21.11` with `-neoforge`
(`build.neoforge.gradle.kts`; `1.21.11-neoforge` was dropped). The Fabric `1.21.1` jar also covers 1.21; the NeoForge one does not.
1.20.1 is `1.20.1` on Fabric and `1.20.1-forge` on MinecraftForge 47 (`build.forge.gradle.kts`, whose
shipped jar is the SRG-remapped `reobfJar`); see [docs/dev/VERSION-DIFFERENCES.md](docs/dev/VERSION-DIFFERENCES.md).

| Command | What it does |
|---|---|
| `./gradlew buildAndCollect` | Build every node, jars in `build/libs/` |
| `./gradlew ":<node>:build"` | Build one node |
| `./gradlew ":<node>:runServer"` | Fastest smoke test: applies every mixin, loads registries, idles. Clean run prints `ThirstWasTaken2 initialized for Minecraft <version>` |
| `./gradlew ":<node>:runClient"` | Dev client |
| `./gradlew ":<node>:runGametest"` | Automated in-game tests, headless, seconds. **The check that proves behaviour.** CI runs it on every node. `-Ptests=<Class,...>` runs only those classes. See [src/gametest/java/AGENTS.md](src/gametest/java/AGENTS.md) |
| `./gradlew ":<node>:runDatagen"` | Regenerate recipes, advancements, tags, damage type and models into `src/main/generated/<mc version>/` (Fabric nodes only) |
| `./gradlew ":<node>:checkDatagen"` | Fails if generated output differs from what is committed. CI runs it |
| `./gradlew ":<node>:runBenchmark"` | Server cost in time and memory. See [benchmark/AGENTS.md](src/dev/java/com/thirstwastaken2/dev/benchmark/AGENTS.md) |
| `./gradlew ":<node>:runServer" -Pagent=<file>.jsonl` | Script a running game and read numbers back. See [agent/AGENTS.md](src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md) |
| `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=<name,...>\|all` | A dev client without those optional mods comes up and stays up. The check 1.0.9 lacked |
| `./gradlew ":<node>:checkOptionalSeam"` | Fails when a class loaded without an optional mod names that mod. CI runs it |
| `./gradlew ":<node>:checkLang"` | Fails when one of the nine lang files lacks a key `en_us` has, or has one it lacks. CI runs it |
| `./gradlew ":<node>:checkDataConditions"` | Fails when a shipped data file names another mod's id without a `mod_loaded` condition for it. CI runs it |
| `./gradlew ":<node>:checkApiSurface"` | Fails when a public signature in `com.thirstwastaken2.api` names an internal type. CI runs it |
| `python tools/release/publish.py --dry-run` | Release to Modrinth, then `publish_curseforge.py --no-build`. Checklist and flags in the scripts' docstrings |

- **Never hand-edit `src/main/generated/`.** Change the generator in `src/datagen`. NeoForge nodes
  reuse the Fabric output and translate its keys; `checkNeoForgeResources` fails if one survives.
- Each node runs in its own `run/<node>/` (worlds are not portable across versions). A new one needs
  its own `eula.txt`.
- Unqualified `./gradlew build` acts on the **active** node (`26.3.x`). `./gradlew "Set active project
  to <node>"` rewrites the versioned comments in `src/` for the IDE. **Run
  `./gradlew "Reset active project"` before committing.**
- The first run needs network for `maven.modrinth`; after that `--offline` works.

## Stack and constraints

- **Java**: 26.1+ runs on Java 25, 1.21.x on Java 21. Anything a 1.20.1 node will compile (core, the
  Fabric loader directories, datagen, gametests, dev tools) uses nothing newer than Java 17: no pattern
  `switch`, no `Math.clamp`, no `getFirst`. Integration directories no 1.20.1 node builds may use Java 21.
  `release = 17` on the 1.20.1 nodes enforces it.
- **Multi-version via [Stonecutter](https://stonecutter.kikugie.dev)**. Per-node values (dependency
  versions, compat ranges) live only in `stonecutter.properties.toml`; there is no version catalog.
  `stonecutter.gradle.kts` is the controller, `build.gradle.kts` is the Fabric script (Loom),
  `build.neoforge.gradle.kts` the NeoForge one (ModDevGradle), `build.forge.gradle.kts` the Forge one
  (ModDevGradle Legacy), `gradle/moddev.gradle.kts` what the two ModDevGradle scripts share (source
  sets, mods and runs, the resource translation), `gradle/shared.gradle.kts` the tasks all three share.
  `build-logic` is an included build of plain Kotlin the scripts call: the integration table,
  `-PwithoutOptional`, the JFR arguments, the Fabric-to-NeoForge and Fabric-to-Forge JSON translation.
  It is data and pure functions only and never depends on Loom, ModDevGradle or Stonecutter; the
  scripts make every `sourceSets`, `loom`, `neoForge` and `legacyForge` call themselves, and hand
  `gradle/moddev.gradle.kts` what it needs from build-logic as extra properties. `./gradlew -p build-logic test` runs its tests.
- **Split source sets.** Anything touching `net.minecraft.client` goes in `src/client/java`, never
  `src/main/java`. NeoForge compiles both together, so only the Fabric nodes catch a mistake.
- **Common code never names a mod loader.** `src/main/java` and `src/client/java` call
  `platform/Loader` and `client/platform/ClientLoader`, one copy per loader in `src/*/<loader>`.
  `checkLoaderSeam` enforces it. See [platform/AGENTS.md](src/main/java/com/thirstwastaken2/platform/AGENTS.md).
- **Optional integrations are soft.** No hard dependency, ever. Gate on `Loader.isModLoaded` (plus a
  marker-class probe when extending a foreign class) and keep integration classes off the load path.
  Asking the gate is not enough on its own: the JVM verifies a class whole before any of it runs, so
  an entrypoint, a `@Mod` class, a Jade plugin or a mixin plugin names no type of that mod anywhere,
  lambdas included, and hands over through a static call into another class. `checkOptionalSeam`
  enforces it; 1.0.9 crashed every NeoForge client without Sophisticated Core for want of it.
- **Mixins**: in `com.thirstwastaken2.mixin`, package-private, `abstract`, every injected member
  prefixed `thirst$`, listed in `thirstwastaken2.mixins.json` or they silently do nothing. Client,
  Fabric-client, dev, Farmer's Delight (Fabric), Create (NeoForge and Forge), Create Fly, Sophisticated, Supplementaries, Kaleidoscope Cookery, Kaleidoscope Tavern, Brewin' and Chewin', Cold Sweat (NeoForge and Forge), Cultural Delights, Fruits Delight, Youkais' Feasts, Expanded Delight, Miner's Delight, Farm & Charm, HerbalBrews, Beachparty, Meadow, Spelunkery, Hearth and Harvest (NeoForge and Forge), No Man's Land, Mekanism, Immersive Engineering, Little Logistics, Croptopia and Bountiful Fares mixins
  have their own configs next to their sources. A new core config goes in both loader manifests; an
  integration's goes in its row of the integration table.
- **Player state** is the immutable record `ThirstData`. Derive a new one and write through
  `ThirstManager.set` only when it changed; every write is a sync packet.
- **Config** is the Gson POJO `ThirstConfig`. A new field: add it, clamp it in `sanitize()`, and if
  user-facing add a widget and reset line in `client/config/ConfigCategory`, in the page and tab of its
  topic, plus lang keys in all nine lang files (`checkLang` fails otherwise). Pages and tabs are cut by
  topic, not by length; see "Grouping settings" in [client/AGENTS.md](src/client/java/com/thirstwastaken2/client/AGENTS.md).
- **`com.thirstwastaken2.api` is public API** for other mods: `ThirstApi`, `ThirstEvents`. A signature
  there changes only after a deprecation, its public signatures name only Minecraft, JDK and `api` types
  (`checkApiSurface`), and it holds no `//?`. An addition bumps `ThirstApi.API_VERSION`. See
  [docs/docs/developers/](docs/docs/developers/java-api.md).
- **Per-item lookups are cached** by `Item` identity (`ThirstApi.CACHE`, `WaterPurity.INFO`). No
  string building or regex on a per-call path; tooltips call these every frame.
- **`ThirstWasTaken2.DEV`** is true under Loom run tasks, false in the published jar. Dev-only tooling
  lives in the `dev` source set, which `main` and `client` never reference.
- Intentional divergence from upstream gets a comment at the divergence.

## Supporting several Minecraft versions

Version differences live in two places only:

- **`platform/`**: `Vanilla` (common) and `ClientVanilla` (client) wrap every vanilla call whose shape
  changed, with one signature on every version.
- **`mixin/`**: an `@Inject` signature tracks its target, so a mixin may fork. Its body stays one line.

Everything else must compile unchanged on every version; `checkVersionSeam` fails CI otherwise. A
branch is a Stonecutter comment, the disabled side commented out:

```java
//? if >=26.2 {
return minecraft.gui.hud.isHidden();
//?} else {
/*return minecraft.options.hideGui;
*///?}
```

A pure rename needs no branch: add it to `replacements` in `stonecutter.gradle.kts`.

- **Replacements do not chain.** Each applies to the original text. When two differences meet, give
  each result a rule of its own (see `critereon` in `stonecutter.gradle.kts`).
- **Replacements run backwards too**, on every node whose condition is false and on "Reset active
  project". What a rule writes must be the same on every node it covers and must not occur in the
  sources for any other reason; the comment above `replacements` in `stonecutter.gradle.kts` says how.
- **No block comments inside a `//?` block.** The disabled branch is itself one `/* */`. Put Javadoc
  outside or use line comments.
- **A one-line branch** (`//?} else` with no brace) is disabled as `//line`, not `/*line*/`; that is
  the form Stonecutter writes back.
- **Set and Reset active project must round-trip**: after `"Set active project to <node>"` then
  `"Reset active project"`, `git diff` shows nothing new under `src/`.

Every difference between versions is listed in [docs/dev/VERSION-DIFFERENCES.md](docs/dev/VERSION-DIFFERENCES.md).

### Version policy

- No fixed limit on Minecraft versions or loaders, and adding one does not retire another. Every
  node still costs a CI job and a fork in `platform/` wherever vanilla differs, so a node is
  retired only when it is no longer worth that.
- 1.21.1 is kept for modpacks. If a feature needs a core-code fork for it, retire 1.21.1 instead.

### Adding a Minecraft version

1. Add `<version>` and `<version>-neoforge` in `settings.gradle.kts`, and three tables in
   `stonecutter.properties.toml`: `["<version>"]` (shared), `[fabric."<version>"]`,
   `[neoforge."<version>"]`. A key lives in exactly one table a node reads. AppleSkin is pinned by
   Modrinth version id, since its Fabric and NeoForge uploads share a version number.
2. CI needs nothing: `.github/workflows/build.yml` makes one job per loader table.
3. `./gradlew ":<version>:build"` and fix errors by extending `platform/`, not by branching at the call site.
4. Smoke-test with `runServer`.

### Dependency updates

Dependabot covers Gradle plugins, the wrapper, Actions and `docs/`, but not
`stonecutter.properties.toml`. `.github/scripts/update_mc_deps.py` does that daily through
`.github/workflows/update-mc-deps.yml`, into the `automation/minecraft-deps` PR. Test with `--dry-run`.
A new dependency must be added to `MODRINTH_DEPS` in the script.

The loaders (`deps.fabric_loader`, `deps.neoforge`, `deps.forge`) are the minimum the manifests ask
players for, and the build uses that same version. The script never moves them: it moves
`deps.*_latest` beside each and prints the loader's changelog from the minimum up to it in the PR.
Raise a minimum by hand, copying its `_latest` over it, only when an integration needs a newer
loader or a fix does; players asked not to update their loader with every release.

## Architecture

`ThirstWasTaken2.initialize` is the one loader-independent entry point, called by
`ThirstWasTaken2Fabric` and `ThirstWasTaken2NeoForge`. It loads config, registers player data, blocks,
components, items and loot, then hooks server tick, block/item use, commands and tag reload through
`Loader`. Init order, the drain chain and the invariants are in
[src/main/java/com/thirstwastaken2/AGENTS.md](src/main/java/com/thirstwastaken2/AGENTS.md).

Water is `WaterQuality`, sealed: `Fresh(grade 0-3)` or `Salt`. It is sampled from the world only when
water is collected, drunk or looked at with Jade, never on a tick or tooltip path. See
[purity/AGENTS.md](src/main/java/com/thirstwastaken2/purity/AGENTS.md).

### Source roots

| Path | Compiled |
|---|---|
| `src/main/java`, `src/client/java` | Everywhere. Loader-independent |
| `src/main/fabric`, `src/client/fabric` | Fabric nodes |
| `src/main/neoforge`, `src/client/neoforge` | NeoForge nodes (client compiled into main) |
| `src/main/neoforge-fluidhandler` / `neoforge-transfer` | NeoForge 1.21.1 / 26.1+, the fluid container API |
| `src/main/forge`, `src/client/forge`, `src/main/forge-fluidhandler` | The Forge node (client compiled into main), and its fluid container API |
| `src/main/<integration>`, `src/client/<integration>`, `src/dev/<integration>` | Where the integration's deps key is set, on the loaders its row allows. Wired from [the integration table](build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt); which nodes, below |
| `src/main/sophisticated-fluidhandler` / `sophisticated-transfer` | Sophisticated's nodes, 1.21.1 / 26.1+: its tank and pump code |
| `src/main/resources` | Hand-written assets and lang, all nodes |
| `src/main/generated/<mc version>` | Datagen output, never hand-edited |
| `src/datagen`, `src/gametest`, `src/dev` | Separate mods, never packaged |
| `src/watchdog` | NeoForge `-Pagent` runs: a JDK-only `-javaagent` that stops the run when FML fails before any mod loads |
| `tools/agent`, `tools/benchmark`, `tools/release` | Scripts for the agent client (by folder, see [tools/agent/AGENTS.md](tools/agent/AGENTS.md)), benchmark sets, publishing |

### Optional integrations

| Mod | Nodes | Where |
|---|---|---|
| AppleSkin, Jade, Mod Menu, Farmer's Delight, loot | all | [compat/AGENTS.md](src/main/java/com/thirstwastaken2/compat/AGENTS.md) |
| Drinks from other mods | all | their own data pack files (`data/<ns>/thirstwastaken2/drinks/`), the `c:drinks` tag, and registry ids in `config/itemvalues`; no class references. See [docs/docs/developers/data-packs.md](docs/docs/developers/data-packs.md) |
| Terralith | all | two optional entries in the `stagnant_water` biome tag, from `ThirstBiomeTagProvider` in `src/datagen`; no class references, no deps key
| Create Fly | `deps.create_fly`: Fabric 26.1.x, 26.2.x (no 26.3 build) | [src/main/createfly/AGENTS.md](src/main/createfly/AGENTS.md) |
| Farmer's Delight Refabricated's Cooking Pot | `deps.farmersdelight`, Fabric nodes | [src/main/farmersdelight/AGENTS.md](src/main/farmersdelight/AGENTS.md) |
| Create, and the Create Pipes n Physics addon with no deps key of its own | `deps.create`: `1.21.1-neoforge`, and `1.20.1-forge` from its own directory (the addon: `1.21.1-neoforge` only) | [src/main/create/AGENTS.md](src/main/create/AGENTS.md), [src/main/createforge/AGENTS.md](src/main/createforge/AGENTS.md) |
| Sophisticated Backpacks and Storage | `deps.sophisticated_core`: every NeoForge node | [src/main/sophisticated/AGENTS.md](src/main/sophisticated/AGENTS.md) |
| Supplementaries and Moonlight Lib | `deps.supplementaries`: both 1.21.1 nodes | [src/main/supplementaries/AGENTS.md](src/main/supplementaries/AGENTS.md) |
| Kaleidoscope Cookery | `deps.kaleidoscope_cookery`: `1.21.1-neoforge`, `1.20.1-forge` and every Fabric node (Refabricated) | [src/main/kaleidoscope/AGENTS.md](src/main/kaleidoscope/AGENTS.md) |
| Kaleidoscope Tavern | `deps.kaleidoscope_tavern`: `1.21.1-neoforge`, `1.20.1-forge` and every Fabric node (Refabricated) | [src/main/kaleidoscopetavern/AGENTS.md](src/main/kaleidoscopetavern/AGENTS.md) |
| Brewin' and Chewin' | `deps.brewin_and_chewin`: both 1.21.1 nodes | [src/main/brewinandchewin/AGENTS.md](src/main/brewinandchewin/AGENTS.md) |
| Cold Sweat | `deps.cold_sweat`: `1.21.1-neoforge`, and `1.20.1-forge` from its own directory | [src/main/coldsweat/AGENTS.md](src/main/coldsweat/AGENTS.md), [src/main/coldsweatforge/AGENTS.md](src/main/coldsweatforge/AGENTS.md) |
| Fruits Delight | `deps.fruits_delight`: `1.21.1-neoforge`, `1.20.1-forge` | [src/main/fruitsdelight/AGENTS.md](src/main/fruitsdelight/AGENTS.md) |
| Gensokyo Delight ~ Youkais' Feasts | `deps.youkais_feasts`: `1.21.1-neoforge`, `1.20.1-forge` (mod id `youkaisfeasts`, package `dev.xkmc.youkaishomecoming`) | [src/main/youkaisfeasts/AGENTS.md](src/main/youkaisfeasts/AGENTS.md) |
| Expanded Delight | `deps.expanded_delight`: `1.21.1-neoforge` | [src/main/expandeddelight/AGENTS.md](src/main/expandeddelight/AGENTS.md) |
| Miner's Delight | `deps.miners_delight`: `1.21.1-neoforge`, `1.20.1-forge` (mod id `minersdelight` and `miners_delight`). Its water cup and copper cup are graded by id in `WaterPurity`; a Spout recipe fill is stamped in the Create directories | [src/main/minersdelight/AGENTS.md](src/main/minersdelight/AGENTS.md) |
| Cultural Delights | `deps.cultural_delights`: `1.21.1-neoforge` (the Fabric port stopped at 0.17 and is not built against) | [src/main/culturaldelights/AGENTS.md](src/main/culturaldelights/AGENTS.md) |
| Serene Seasons | `deps.serene_seasons`: every node, every loader | [src/main/sereneseasons/AGENTS.md](src/main/sereneseasons/AGENTS.md) |
| Let's Do: Farm & Charm, and Candlelight, its addon, with no code of its own | `deps.farm_and_charm`: both 1.21.1 nodes (the 1.20.1 line is no longer updated) | [src/main/farmandcharm/AGENTS.md](src/main/farmandcharm/AGENTS.md) |
| Let's Do: HerbalBrews | `deps.herbalbrews`: both 1.21.1 nodes (the 1.20.1 line is no longer updated) | [src/main/herbalbrews/AGENTS.md](src/main/herbalbrews/AGENTS.md) |
| Let's Do: Beachparty | `deps.beachparty`: both 1.21.1 nodes (the 1.20.1 line is no longer updated) | [src/main/beachparty/AGENTS.md](src/main/beachparty/AGENTS.md) |
| Let's Do: Meadow | `deps.meadow`: both 1.21.1 nodes (the 1.20.1 line is no longer updated). Its wooden water bucket and wooden bucket are graded by id in `WaterPurity` | [src/main/meadow/AGENTS.md](src/main/meadow/AGENTS.md) |
| Spelunkery | `deps.spelunkery`: both 1.21.1 nodes (the 1.20.1 line is no longer updated). Its Spring Water is in the `pure_water` fluid tag, on every node | [src/main/spelunkery/AGENTS.md](src/main/spelunkery/AGENTS.md) |
| Hearth and Harvest | `deps.hearth_and_harvest`: `1.21.1-neoforge`, and `1.20.1-forge` from its own directory | [src/main/hearthandharvest/AGENTS.md](src/main/hearthandharvest/AGENTS.md), [src/main/hearthandharvestforge/AGENTS.md](src/main/hearthandharvestforge/AGENTS.md) |
| No Man's Land | `deps.nomansland`: `1.21.1-neoforge`. Its swamps are in `stagnant_water` and its Mud Beach in `sea_water`, biome tags on every node | [src/main/nomansland/AGENTS.md](src/main/nomansland/AGENTS.md) |
| Mekanism | `deps.mekanism`: `1.21.1-neoforge`, and `1.20.1-forge` from its own directory | [src/main/mekanism/AGENTS.md](src/main/mekanism/AGENTS.md), [src/main/mekanismforge/AGENTS.md](src/main/mekanismforge/AGENTS.md) |
| Immersive Engineering | `deps.immersive_engineering`: `1.21.1-neoforge`, and `1.20.1-forge` from its own directory | [src/main/immersiveengineering/AGENTS.md](src/main/immersiveengineering/AGENTS.md), [src/main/immersiveengineeringforge/AGENTS.md](src/main/immersiveengineeringforge/AGENTS.md) |
| Little Logistics | `deps.little_logistics`: `1.21.1-neoforge`, and `1.20.1-forge` from its own directory. A bucket on its fluid tanks keeps the grade | [src/main/littlelogistics/AGENTS.md](src/main/littlelogistics/AGENTS.md), [src/main/littlelogisticsforge/AGENTS.md](src/main/littlelogisticsforge/AGENTS.md) |
| Bountiful Fares | `deps.bountiful_fares`: both 1.21.1 nodes (the 1.20.x lines stopped in 2024). Its cup and water cup are graded by id in `WaterPurity` | [src/main/bountifulfares/AGENTS.md](src/main/bountifulfares/AGENTS.md) |
| Croptopia | `deps.croptopia`: 26.2, 26.1.x and 1.21.1 on both loaders, `1.20.1` and `1.20.1-forge` (no 26.3 or 1.21.11 build). On CurseForge only, resolved through CurseMaven by file id and bumped by hand. Its drinks are `ThirstConfig` ids | [src/main/croptopia/AGENTS.md](src/main/croptopia/AGENTS.md) |

### Adding an integration

1. Add a row to [the integration table](build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt): its directory, deps key,
   loaders, and what the manifest names. Both loader scripts wire its directories and patch the built
   manifest from that row; neither needs a block of its own unless the mod needs something the row
   cannot say.
2. Add the deps key to the nodes that build it in `stonecutter.properties.toml`, and the mod to
   `MODRINTH_DEPS` in `.github/scripts/update_mc_deps.py`.
3. Add its `compileOnly` and `runClientMod` lines to the loader scripts it builds on.
4. Put its version differences in `com.thirstwastaken2.<integration>.platform`, or in core
   `platform/Vanilla` when the difference is vanilla's. `checkVersionSeam` fails on a `//?` anywhere else.
5. Gate it at runtime and keep it off the load path. `checkOptionalSeam` checks that, and that core
   code never names its package, which it reads from the table. An integration both loaders compile that has to run
   code at init can carry neither `@Mod` nor `ModInitializer`: make it a `Runnable` marked
   `platform/IntegrationEntrypoint` and list it under `thirstwastaken2:integration` in its row's
   `fabricEntrypoints` (Serene Seasons does).

## Where to look

| Task / Area | Location |
|---|---|
| Common code: init order, state invariants, caching, tooltip lines | [src/main/java/com/thirstwastaken2/AGENTS.md](src/main/java/com/thirstwastaken2/AGENTS.md) |
| Vanilla hooks and fragile injections | [mixin/AGENTS.md](src/main/java/com/thirstwastaken2/mixin/AGENTS.md) |
| Water purity, sampling, cauldrons, fluid containers | [purity/AGENTS.md](src/main/java/com/thirstwastaken2/purity/AGENTS.md) |
| Version and loader differences | [platform/AGENTS.md](src/main/java/com/thirstwastaken2/platform/AGENTS.md) |
| HUD and config screen | [src/client/java/com/thirstwastaken2/client/AGENTS.md](src/client/java/com/thirstwastaken2/client/AGENTS.md) |
| Manifests, textures, fonts, lang | [src/main/resources/AGENTS.md](src/main/resources/AGENTS.md) |
| Datagen providers | [src/datagen/java/AGENTS.md](src/datagen/java/AGENTS.md) |
| Automated in-game tests | [src/gametest/java/AGENTS.md](src/gametest/java/AGENTS.md) |
| Dev-only tooling (agent client, benchmark) | [src/dev/java/AGENTS.md](src/dev/java/AGENTS.md) |
| Agent client scripts, which folder holds what | [tools/agent/AGENTS.md](tools/agent/AGENTS.md) |
| Benchmark baseline per node | [docs/dev/benchmark/BENCHMARK-BASELINE.md](docs/dev/benchmark/BENCHMARK-BASELINE.md) |
| Manual checks before a release | [docs/dev/MANUAL-TESTING.md](docs/dev/MANUAL-TESTING.md) |
| The API and data pack format other mods use | the site's developer pages, [docs/docs/developers/](docs/docs/developers/java-api.md) |
| Water reference: sources, grades, sickness chances by difficulty, purification methods | [docs/dev/mechanics/WATER-REFERENCE.md](docs/dev/mechanics/WATER-REFERENCE.md) |
| Where sea water is refused, kept salty, or mixed in (hanging pot) | [docs/dev/mechanics/SALT-WATER-REFUSALS.md](docs/dev/mechanics/SALT-WATER-REFUSALS.md) |
| Every integration plan, by folder (`lets-do/`, `cooking/`, `storage/`, `climate/`), with its state | [docs/dev/integration/README.md](docs/dev/integration/README.md) |
| Sophisticated upgrades still to do | [docs/dev/integration/storage/SOPHISTICATED-INTEGRATION.md](docs/dev/integration/storage/SOPHISTICATED-INTEGRATION.md) |
| Create Pipes n Physics: the plan, and what was found in game (`1.21.1-neoforge`) | [docs/dev/integration/storage/PIPES-N-PHYSICS-INTEGRATION.md](docs/dev/integration/storage/PIPES-N-PHYSICS-INTEGRATION.md) |
| Supplementaries work still to do | [docs/dev/integration/storage/SUPPLEMENTARIES-INTEGRATION.md](docs/dev/integration/storage/SUPPLEMENTARIES-INTEGRATION.md) |
| Kaleidoscope Cookery work still to do | [docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md](docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md) |
| Kaleidoscope Tavern: the plan, its decisions and what was found in game (`1.21.1-neoforge`, `1.20.1-forge`, every Fabric node) | [docs/dev/integration/cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md](docs/dev/integration/cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md) |
| Brewin' and Chewin' work still to do | [docs/dev/integration/cooking/BREWIN-AND-CHEWIN-INTEGRATION.md](docs/dev/integration/cooking/BREWIN-AND-CHEWIN-INTEGRATION.md) |
| Cold Sweat: the plan, its decisions and what was found in game (1.21.1 NeoForge) | [docs/dev/integration/climate/COLD-SWEAT-INTEGRATION.md](docs/dev/integration/climate/COLD-SWEAT-INTEGRATION.md) |
| Fruits Delight: the plan, its decisions and what was found in game (1.21.1 NeoForge) | [docs/dev/integration/cooking/FRUITS-DELIGHT-INTEGRATION.md](docs/dev/integration/cooking/FRUITS-DELIGHT-INTEGRATION.md) |
| Youkais' Feasts: the plan, its decisions and what was found in game (1.21.1 NeoForge, 1.20.1 Forge) | [docs/dev/integration/cooking/YOUKAIS-FEASTS-INTEGRATION.md](docs/dev/integration/cooking/YOUKAIS-FEASTS-INTEGRATION.md) |
| Miner's Delight: the plan, its decisions and what was found in game (1.21.1 NeoForge, 1.20.1 Forge) | [docs/dev/integration/cooking/MINERS-DELIGHT-INTEGRATION.md](docs/dev/integration/cooking/MINERS-DELIGHT-INTEGRATION.md) |
| Hearth and Harvest: the plan, its decisions and what was found in game (1.21.1 NeoForge, 1.20.1 Forge) | [docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md](docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md) |
| Extra Delight: the plan, its decisions and what was found in game (1.21.1 NeoForge) | [docs/dev/integration/cooking/EXTRA-DELIGHT-INTEGRATION.md](docs/dev/integration/cooking/EXTRA-DELIGHT-INTEGRATION.md) |
| Cultural Delights: the plan, its decisions and what was found in game (1.21.1 NeoForge) | [docs/dev/integration/cooking/CULTURAL-DELIGHTS-INTEGRATION.md](docs/dev/integration/cooking/CULTURAL-DELIGHTS-INTEGRATION.md) |
| Serene Seasons: the plan, its decisions and what was found in game (every node) | [docs/dev/integration/climate/SERENE-SEASONS-INTEGRATION.md](docs/dev/integration/climate/SERENE-SEASONS-INTEGRATION.md) |
| Let's Do: Farm & Charm: the plan, its decisions and what was found in game (both 1.21.1 nodes) | [docs/dev/integration/lets-do/FARM-AND-CHARM-INTEGRATION.md](docs/dev/integration/lets-do/FARM-AND-CHARM-INTEGRATION.md) |
| Let's Do: Candlelight, Farm & Charm's addon: the plan, its decisions and what was found in game | [docs/dev/integration/lets-do/CANDLELIGHT-INTEGRATION.md](docs/dev/integration/lets-do/CANDLELIGHT-INTEGRATION.md) |
| Let's Do: HerbalBrews: the plan, its decisions and what was found in game (both 1.21.1 nodes) | [docs/dev/integration/lets-do/HERBALBREWS-INTEGRATION.md](docs/dev/integration/lets-do/HERBALBREWS-INTEGRATION.md) |
| Let's Do: Beachparty: the plan, its decision and what was found in game (both 1.21.1 nodes) | [docs/dev/integration/lets-do/BEACHPARTY-INTEGRATION.md](docs/dev/integration/lets-do/BEACHPARTY-INTEGRATION.md) |
| Let's Do: Meadow: the plan and its decisions (both 1.21.1 nodes) | [docs/dev/integration/lets-do/MEADOW-INTEGRATION.md](docs/dev/integration/lets-do/MEADOW-INTEGRATION.md) |
| Spelunkery: the plan, its decisions and what was found in game (both 1.21.1 nodes) | [docs/dev/integration/world/SPELUNKERY-INTEGRATION.md](docs/dev/integration/world/SPELUNKERY-INTEGRATION.md) |
| No Man's Land: the plan, its decisions and what was found in game (1.21.1 NeoForge) | [docs/dev/integration/world/NO-MANS-LAND-INTEGRATION.md](docs/dev/integration/world/NO-MANS-LAND-INTEGRATION.md) |
| Mekanism: the plan, its decisions and what was found in game (1.21.1 NeoForge, 1.20.1 Forge) | [docs/dev/integration/storage/MEKANISM-INTEGRATION.md](docs/dev/integration/storage/MEKANISM-INTEGRATION.md) |
| Immersive Engineering: the plan, its decisions and what was found in game (1.21.1 NeoForge, 1.20.1 Forge) | [docs/dev/integration/storage/IMMERSIVE-ENGINEERING-INTEGRATION.md](docs/dev/integration/storage/IMMERSIVE-ENGINEERING-INTEGRATION.md) |
| Bountiful Fares: the plan, its decisions and what was found in game (both 1.21.1 nodes) | [docs/dev/integration/cooking/BOUNTIFUL-FARES-INTEGRATION.md](docs/dev/integration/cooking/BOUNTIFUL-FARES-INTEGRATION.md) |
| Croptopia: the plan, its decisions and what was found in game (every node it builds for) | [docs/dev/integration/cooking/CROPTOPIA-INTEGRATION.md](docs/dev/integration/cooking/CROPTOPIA-INTEGRATION.md) |
| Bad-water sickness rework: the design | [docs/dev/mechanics/WATER-SICKNESS.md](docs/dev/mechanics/WATER-SICKNESS.md) |
| Bad-water sickness rework: where the code goes, step by step | [docs/dev/mechanics/WATER-SICKNESS-IMPLEMENTATION.md](docs/dev/mechanics/WATER-SICKNESS-IMPLEMENTATION.md) |
| Releasing | [tools/release/publish.py](tools/release/publish.py) and [publish_curseforge.py](tools/release/publish_curseforge.py) docstrings |
| Documentation site, CHANGELOG, Modrinth and CurseForge pages | [docs/AGENTS.md](docs/AGENTS.md) and the `write-docs` skill |
