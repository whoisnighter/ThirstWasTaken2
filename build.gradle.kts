import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.OptionalRunMods
import com.thirstwastaken2.buildlogic.fabricAccessWidener
import com.thirstwastaken2.buildlogic.flightRecorder
import com.thirstwastaken2.buildlogic.integrations
import com.thirstwastaken2.buildlogic.integrationsFor
import java.util.zip.ZipFile

plugins {
    // Picks the Loom variant the active Minecraft version needs. See settings.gradle.kts.
    id("dev.kikugie.loom-back-compat")
    // The integration table and the rest of what both loader scripts share. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
    `maven-publish`
}

// Stonecutter supplies `mod.*` and `deps.*` per version from stonecutter.properties.toml.
// `group` stays unset on purpose: every version node shares this script, and Loom keys parts of its
// cache off the project coordinates. The publication sets its own groupId below.
val modId = property("mod.id") as String
/** Published artifact name; deliberately not the lowercase mod id. */
val modName = property("mod.name") as String
val modGroup = property("mod.group") as String
/** Minecraft range written into fabric.mod.json, e.g. `~26.2`. */
val mcCompat = property("mod.mc_compat") as String
val loaderVersion = property("deps.fabric_loader") as String

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = modName

/** Minecraft 26.1 moved to Java 25; 1.20.5 to 1.21.x run on Java 21, 1.20.1 on Java 17. */
val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

repositories {
    // Loom supplies the Minecraft and Fabric repositories.
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    // CurseMaven, for the mods published on CurseForge alone (Croptopia and its EpheroLib), by file id.
    exclusiveContent {
        forRepository { maven("https://cursemaven.com") { name = "CurseMaven" } }
        filter { includeGroup("curse.maven") }
    }
}

// Server-side gametests. They are their own source set and their own small mod, so none of it can
// reach the published jar. See src/gametest/java/AGENTS.md.
val gametest: SourceSet = sourceSets.create("gametest")

// Development tooling such as /thirst benchmark. The same arrangement as the gametests: its own source
// set and its own small mod, loaded by runServer and runBenchmark, never packaged. See
// src/dev/java/AGENTS.md.
val dev: SourceSet = sourceSets.create("dev")

loom {
    splitEnvironmentSourceSets()

    mods {
        register("thirstwastaken2") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
        register("thirstwastaken2_gametest") {
            sourceSet(gametest)
        }
        register("thirstwastaken2-dev") {
            sourceSet(dev)
        }
    }

    runs {
        register("gametest") {
            server()
            displayName = "Game Test"
            sourceSet = gametest.name
            // Turns the dedicated server into the GameTest runner. It skips the EULA prompt and the
            // normal server startup, runs every @GameTest method headlessly, then exits non-zero if
            // any of them failed. The runner only checks that the flag is set, not its value.
            systemProperties.put("fabric-api.gametest", "true")
            systemProperties.put(
                "fabric-api.gametest.report-file",
                layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath,
            )
        }

        // runServer loads the dev tools, so /thirst benchmark can be typed into its console.
        named("server") {
            sourceSet = dev.name
        }

        // Unattended benchmark: starts the dedicated server, runs `/thirst benchmark <-Pbenchmark>` from
        // the console once it is up, writes run/<version>/benchmark/latest.json and stops the server.
        register("benchmark") {
            server()
            displayName = "Benchmark"
            sourceSet = dev.name
            systemProperties.put("thirstwastaken2.benchmark", providers.gradleProperty("benchmark").getOrElse("standard"))
            systemProperties.put("thirstwastaken2.benchmark.exit", "true")
            // A world of its own inside runServer's directory, generated with the fixed seed
            // gradle/shared.gradle.kts puts in server.properties, so a run measures the same terrain
            // on every machine and on both loaders of this Minecraft version, and never the dev world.
            programArguments.addAll("--world", providers.gradleProperty("thirst.benchmark.world").get())

            // `-Pprofile` records the run with JFR, which is in every JDK, and writes
            // run/<node>/benchmark/latest.jfr for JDK Mission Control to open. `settings=profile` is the
            // heavier of JFR's two built-in profiles; the deep stack depth is what makes an allocation
            // event name the mod's own call site rather than a truncated vanilla frame, and
            // DebugNonSafepoints lets a sample land where the code really was rather than at the nearest
            // safepoint. A recording slows the run down and skews every figure in the report, which is
            // why the report says so and aggregate.py refuses a set with one in it.
            if (providers.gradleProperty("profile").isPresent) {
                jvmArguments.addAll(flightRecorder(rootProject.file("run/${project.name}/benchmark/latest.jfr")))
            }
        }
    }

    runConfigs.all {
        // `-Pagent=<file>` answers that file of agent requests once the game is up and then stops it,
        // which is what an unattended run is. Without it the agent is still there, waiting on
        // run/<node>/agent/<side>/in.jsonl. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md.
        providers.gradleProperty("agent").orNull?.let { script ->
            systemProperties.put("thirstwastaken2.agent.script", rootProject.file(script).absolutePath)
            systemProperties.put("thirstwastaken2.agent.script.exit", "true")
        }

        // `-Pdriven` says this client is driven by an agent rather than played: it opens maximised
        // and never takes the mouse pointer, so the desktop stays usable while a script drives it.
        // An unattended `-Pagent=<file>` run implies it. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md.
        if (providers.gradleProperty("driven").isPresent) {
            systemProperties.put("thirstwastaken2.agent.driven", "true")
        }

        // `-Pquickplay=<world>` opens that singleplayer world straight from launch, so an unattended
        // `-Pagent` script starts inside it. The world has to exist in run/<node>/saves.
        if (name == "client") {
            providers.gradleProperty("quickplay").orNull?.let { world ->
                programArguments.addAll("--quickPlaySingleplayer", world)
                // So client.info can say the world was asked for and never opened.
                systemProperties.put("thirstwastaken2.agent.quickplay", world)
            }
        }

        // One run directory per version. Sharing a single one would hand a 26.2 world to a 1.21.11
        // server, which fails on world format rather than on anything the mod did. The gametest
        // runner and datagen get their own again, so a failed run cannot leave a broken world behind
        // for runServer. The benchmark shares runServer's directory, world and accepted EULA, so the
        // two cannot run at the same time.
        runDirectory = when (name) {
            "gametest" -> rootProject.file("run/${project.name}/gametest")
            "datagen" -> rootProject.file("run/${project.name}/datagen")
            else -> rootProject.file("run/${project.name}")
        }
    }
}

/**
 * The mod loader this node builds for. Loader code lives beside the source set it belongs to, in
 * `src/main/<loader>` and `src/client/<loader>`, and only this loader's directories are compiled.
 * Everything else in `src/main/java` and `src/client/java` is loader independent; `checkLoaderSeam`
 * enforces it. The NeoForge node has a script of its own, build.neoforge.gradle.kts. See
 * src/main/java/com/thirstwastaken2/platform/AGENTS.md.
 *
 * The directories sit inside `src/main` and `src/client`, beside the source set each belongs to.
 * Stonecutter rewrites the versioned comments of every source directory inside the project, so where
 * they sit is a convention: before 0.10 it read only `src/<source set>`, and anywhere else every node
 * but the active one compiled an empty directory.
 */
val loader = "fabric"

sourceSets.main {
    java.srcDir("src/main/$loader/java")
    resources.srcDir("src/main/$loader/resources")
}
sourceSets.named("client") {
    java.srcDir("src/client/$loader/java")
    resources.srcDir("src/client/$loader/resources")
}

/**
 * The mod's access widener, written for this node as it is configured: what Fabric API opens only to
 * itself and NeoForge and Forge already make public, such as the menu type's constructor. Its namespace
 * and one of its entries depend on the Minecraft version; see build-logic's AccessWidener.kt. The
 * manifest names it, and Loom applies it to the game the mod compiles against.
 */
val accessWidener = layout.buildDirectory.file("generated/accesswidener/thirstwastaken2.accesswidener").get().asFile
accessWidener.parentFile.mkdirs()
accessWidener.writeText(fabricAccessWidener(
    unobfuscated = sc.current.parsed >= "26.1",
    blockEntityTypeConstructor = sc.current.parsed >= "1.21.2",
))
sourceSets.main { resources.srcDir(accessWidener.parentFile) }
loom { accessWidenerPath = accessWidener }
/**
 * Player data sync and clientbound payloads, one directory per generation of Fabric's networking:
 * payload types and attachments that sync themselves from 1.20.5, plain channels before it. Each holds
 * a `platform/FabricNetworking` and a client `ClientboundReceivers` with the same signatures, the way
 * NeoForge's fluid API directories do in build.neoforge.gradle.kts.
 */
val networking = if (sc.current.parsed >= "1.20.5") "payload" else "legacypayload"
sourceSets.main { java.srcDir("src/main/$loader-$networking/java") }
sourceSets.named("client") { java.srcDir("src/client/$loader-$networking/java") }
// The dev tools have loader code of their own, under the same rule: the entrypoints, and the small
// seam the agent needs beyond the mod's own `platform/Loader`. See src/dev/java/AGENTS.md.
dev.java.srcDir("src/dev/$loader/java")
dev.resources.srcDir("src/dev/$loader/resources")
// So do the gametests, for the one test that needs a connection reporting the mod's channel.
gametest.java.srcDir("src/gametest/$loader/java")

/**
 * The optional integrations this node builds: each is a set of source directories only a node that sets
 * its deps key compiles, and only there does the manifest name its mixin config and entrypoints. What
 * each one adds is a row of the table in build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt,
 * which build.neoforge.gradle.kts reads too; what is specific to one mod, such as its dependencies,
 * stays below with its own comment.
 */
val nodeIntegrations = integrationsFor(Loader.FABRIC) { findProperty(it) != null }

nodeIntegrations.forEach { integration ->
    // Fabric has one fluid API, so no integration here has a directory per generation.
    sourceSets.main {
        integration.mainRoots(transferApi = false).forEach { root ->
            java.srcDir("$root/java")
            resources.srcDir("$root/resources")
        }
    }
    // A client-only dependency such as Jade compiles with the rest of the client rather than with `main`.
    integration.clientJava?.let { dir -> sourceSets.named("client") { java.srcDir("src/client/$dir") } }
    // The benchmark's own operations for the integration. They run only when the mod is on the
    // benchmark's classpath, which `-Pcreate` asks for below for Create Fly.
    integration.devJava?.let(dev.java::srcDir)
}

/** The Create Fly version this node compiles the Sand Filter against, or null. See src/main/createfly/AGENTS.md. */
val createFly = findProperty("deps.create_fly") as String?

/**
 * Create Fly's classes, without the files that make it a mod. Its class tweaker makes vanilla's
 * `Container` implement one of Create's interfaces, and Loom bakes the tweakers of every mod on the
 * compile classpath into the one Minecraft jar all of this node's runs share - so the gametests and
 * runServer, which run without Create Fly, would fail to load `Container`. Turning transitive tweakers
 * off is not an option, because Fabric API's own injected methods need them. Compiled against as a
 * plain library, Create Fly leaves the Minecraft jar alone; in runClient and in a player's game,
 * Fabric Loader applies its tweaker from the real jar as usual.
 */
val createFlyClasses = createFly?.let { version ->
    val resolved = configurations.detachedConfiguration(dependencies.create("maven.modrinth:create-fly:$version"))
        .apply { isTransitive = false }
    tasks.register<Jar>("createFlyClasses") {
        description = "Copies Create Fly's classes into a jar Loom does not treat as a mod"
        destinationDirectory = layout.buildDirectory.dir("createfly")
        archiveFileName = "create-fly-$version-classes.jar"
        from(resolved.elements.map { jars -> jars.map { zipTree(it) } }) {
            include("com/zurrtum/**")
        }
    }
}

/**
 * Supplementaries' Modrinth version id, set on the two 1.21.1 nodes and nowhere else, since
 * Supplementaries has no release for a newer Minecraft version. See src/main/supplementaries/AGENTS.md.
 */
val supplementaries = findProperty("deps.supplementaries") as String?

/**
 * Kaleidoscope Cookery's Modrinth version id: Refabricated, the Fabric port, on every Fabric node.
 * See docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md.
 */
val kaleidoscopeCookery = findProperty("deps.kaleidoscope_cookery") as String?

/**
 * Kaleidoscope Tavern's Modrinth version id: Refabricated, the Fabric port, on every Fabric node.
 * See docs/dev/integration/cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md.
 */
val kaleidoscopeTavern = findProperty("deps.kaleidoscope_tavern") as String?

/**
 * Brewin' and Chewin's Modrinth version id, set on `1.21.1` and `1.21.1-neoforge` only: it has no
 * release for a newer Minecraft version. See docs/dev/integration/cooking/BREWIN-AND-CHEWIN-INTEGRATION.md.
 */
val brewinAndChewin = findProperty("deps.brewin_and_chewin") as String?

/**
 * Let's Do: Farm & Charm's Modrinth version id, and Candlelight's, its addon, set on `1.21.1` and
 * `1.21.1-neoforge` only. See docs/dev/integration/lets-do/FARM-AND-CHARM-INTEGRATION.md.
 */
val farmAndCharm = findProperty("deps.farm_and_charm") as String?

/**
 * Let's Do: HerbalBrews' and Beachparty's Modrinth version ids, on `1.21.1` and `1.21.1-neoforge` only,
 * like Farm & Charm. See docs/dev/integration/lets-do/.
 */
val herbalBrews = findProperty("deps.herbalbrews") as String?
val beachparty = findProperty("deps.beachparty") as String?
val vinery = findProperty("deps.vinery") as String?
val meadow = findProperty("deps.meadow") as String?

/**
 * Croptopia's CurseForge file id, and the EpheroLib it requires, on every Fabric node it has a build for.
 * See src/main/croptopia/AGENTS.md.
 */
val croptopia = findProperty("deps.croptopia") as String?

/**
 * Spelunkery's Modrinth version id, on `1.21.1` and `1.21.1-neoforge` only. Nothing is compiled against
 * it. See docs/dev/integration/world/SPELUNKERY-INTEGRATION.md.
 */
val spelunkery = findProperty("deps.spelunkery") as String?

/**
 * Bountiful Fares' Modrinth version id, and the NexusLib it requires, on `1.21.1` and `1.21.1-neoforge`
 * only. See docs/dev/integration/cooking/BOUNTIFUL-FARES-INTEGRATION.md.
 */
val bountifulFares = findProperty("deps.bountiful_fares") as String?

/**
 * Serene Seasons' Modrinth version id, on every node. See docs/dev/integration/climate/SERENE-SEASONS-INTEGRATION.md.
 */
val sereneSeasons = findProperty("deps.serene_seasons") as String?

/**
 * The mods a Modrinth mod bundles inside its own jar: Moonlight's CodecUI, which it reads on its first
 * line, the Night Config that Forge Config API Port is built on, and Brewin' and Chewin's Greenhouse
 * Config, whose TOML support nests a Night Config of its own. Loom does not unpack a dependency's
 * nested jars into a run - which is why Cloth Config is named by hand for AppleSkin below - so they are
 * taken out of the jar the run already resolves, and are therefore always the versions that mod ships.
 * CodecUI is published nowhere else: Moonlight's own build reads it from a local Maven. They go back on
 * a mod configuration so that Loom remaps them, since a published nested jar is in intermediary names
 * and a run is not.
 *
 * Unpacked while the build is configured rather than by a task, because Loom resolves the mod
 * configurations then: a jar a task writes afterwards is not there to be remapped and the run starts
 * without it. The marker file makes this one directory listing on every build after the first.
 */
fun nestedMods(project: String, version: String): List<File> {
    val into = layout.buildDirectory.dir("nested/$project/$version").get().asFile
    // Renamed when what is unpacked changes, so a directory unpacked the old way is unpacked again.
    val unpacked = File(into, ".unpacked-nested")
    if (!unpacked.isFile) {
        val jar = configurations.detachedConfiguration(
            dependencies.create("maven.modrinth:$project:$version")
        ).apply { isTransitive = false }.singleFile
        into.mkdirs()
        // Each jar's own fabric.mod.json names the jars nested in it, one level at a time, so follow it
        // down as Fabric Loader does: Greenhouse Config's TOML support, nested in Brewin' and Chewin',
        // nests its Night Config in turn, under META-INF/jarjar rather than META-INF/jars.
        val pending = ArrayDeque(listOf(jar))
        while (pending.isNotEmpty()) {
            ZipFile(pending.removeFirst()).use { zip ->
                val manifest = zip.getEntry("fabric.mod.json") ?: return@use
                @Suppress("UNCHECKED_CAST")
                val json = groovy.json.JsonSlurper().parse(zip.getInputStream(manifest)) as Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                (json["jars"] as List<Map<String, Any?>>?).orEmpty().forEach { nested ->
                    val entry = zip.getEntry(nested["file"].toString()) ?: return@forEach
                    val out = File(into, entry.name.substringAfterLast('/'))
                    zip.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
                    pending.add(out)
                }
            }
        }
        unpacked.writeText(version)
    }
    return into.listFiles().orEmpty().filter { it.extension == "jar" }
}

/**
 * Every datapack and asset JSON the mod ships, written by `src/datagen`. The directory is keyed by
 * Minecraft version rather than by build node, because two nodes of the same Minecraft version on
 * different loaders produce byte-identical files and should share one directory. It is a resource
 * root of `main`, so the jar picks it up with no further wiring.
 *
 * Regenerate with `:<version>:runDatagen`; `:<version>:checkDatagen` fails when the committed files
 * and the generators have drifted apart. See src/datagen/java/AGENTS.md.
 */
val generatedResources: File = rootProject.file("src/main/generated/${sc.current.version}")

fabricApi.configureDataGeneration {
    outputDirectory.set(generatedResources)
    // Its own source set and its own small mod, like the gametests and the dev tools, so none of the
    // generator code can reach the published jar.
    createSourceSet = true
    modId = "thirstwastaken2-datagen"
    // The item model providers live in net.minecraft.client.data, so datagen runs as a client.
    client = true
    // Every advancement the mod awards by id is `minecraft:impossible`, which strict validation
    // reads as an unreachable advancement.
    strictValidation = false
}

/*
 * 1.20.1 writes recipes and advancements through types the later providers share nothing with, and has
 * no data components for their ingredients and results to carry, so its own two providers in
 * src/datagen/legacy replace those in src/datagen/java there. See src/datagen/java/AGENTS.md.
 */
if (sc.current.parsed < "1.20.5") {
    sourceSets.named("datagen") {
        java {
            srcDir("src/datagen/legacy/java")
            exclude("com/thirstwastaken2/datagen/ThirstRecipeProvider.java",
                    "com/thirstwastaken2/datagen/ThirstAdvancementProvider.java",
                    "com/thirstwastaken2/datagen/FarmersDelightRecipeProvider.java")
        }
    }
}

// Loom adds the datagen output to `main`'s resources by reading the source directories back and
// setting them again, which flattens them to plain files and loses the task dependency Stonecutter
// had attached to the one it generates. Without this, building any node other than the active one
// fails: the tasks that read those resources have not been told to wait for them.
tasks.named("processResources") { dependsOn("stonecutterGenerate") }

/*
 * Datagen keeps a hash cache of what it last wrote and skips a file whose hash still matches, so a
 * generated file edited by hand survives a regeneration, and a file no longer generated at all is
 * only deleted while the cache still remembers writing it. Emptying the directory first costs
 * nothing at 58 small files and makes the task mean what its name says: what is on disk afterwards
 * is what the generators produce, and `checkDatagen` can trust the difference.
 */
tasks.named("runDatagen") {
    doFirst {
        generatedResources.deleteRecursively()
    }
}

// The gametests and the dev tools compile and run against the mod itself and against everything the
// mod uses.
gametest.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
gametest.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
dev.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
dev.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
// runServer and runBenchmark run the dev source set. The published jar carries the client classes on
// a dedicated server too, and a mod that loads one of them by name there, as Jade does with the `jade`
// entrypoint, crashes a dev server that lacks them. Only the classes are added, not the client's
// runtime mods, so the client-only dependencies stay out of the server.
dev.runtimeClasspath += sourceSets["client"].output
// The agent's client probes read the HUD, the framebuffer and the key state, so the dev source set
// compiles against `client` as well as against `main`. Loom's split keeps the two apart for the mod,
// where common code reaching a client class is a real mistake; a tool whose whole job is to read what
// a client holds is on both sides by definition. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md.
dev.compileClasspath += sourceSets["client"].compileClasspath + sourceSets["client"].output

/**
 * What runClient runs: the dev tools on top of the client. It cannot simply be handed `dev` the way
 * runServer is, because `dev`'s runtime classpath deliberately carries the client's *classes* without
 * the client's runtime mods, and runClient is meant to load AppleSkin, Jade and the rest. This source
 * set has no sources of its own and exists only to put both on one classpath — the same arrangement
 * build.neoforge.gradle.kts uses for its own extra clients.
 */
val devClient: SourceSet = sourceSets.create("devClient") {
    runtimeClasspath = dev.output + dev.runtimeClasspath + sourceSets["client"].runtimeClasspath
}

loom {
    runs {
        // runClient loads the dev tools too, so an agent can drive a real client through
        // run/<node>/agent/client/. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md.
        named("client") {
            sourceSet = devClient.name
        }
    }
}

/**
 * Adds a client-only mod dependency. Loom prefixes these configurations with `mod` where it remaps
 * dependencies and leaves them bare where it does not, so resolve the name that actually exists.
 */
fun clientMod(configuration: String, notation: Any) {
    val prefixed = "mod${configuration.replaceFirstChar(Char::uppercase)}"
    val target = if (configurations.findByName(prefixed) != null) prefixed else configuration
    dependencies.add(target, notation)
}

/** `-PwithoutOptional=<name,...>`, which leaves optional mods out of runClient. See build-logic's OptionalRunMods. */
val optionalRunMods = OptionalRunMods(providers.gradleProperty("withoutOptional").orNull)

/** Adds a mod to runClient only, unless `-PwithoutOptional` names it or one of the libraries it lists. */
/** Architectury API's names, which every Let's Do mod lists among its own since it cannot load without it. */
val architecturyNames = listOf("architectury", "architectury-api")

fun runClientMod(names: List<String>, notation: Any) {
    if (optionalRunMods.include(names)) clientMod("clientRuntimeOnly", notation)
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // No-op from 26.1 on, which ships unobfuscated.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    // Optional integrations. The mod runs without any of them, but all are compiled against, so they
    // have to resolve on every version.
    clientMod("clientCompileOnly", "maven.modrinth:modmenu:${property("deps.modmenu")}")
    clientMod("clientCompileOnly", "maven.modrinth:appleskin:${property("deps.appleskin")}")
    clientMod("clientCompileOnly", "maven.modrinth:jade:${property("deps.jade")}")
    // Test the client HUD and food tooltips alongside AppleSkin in runClient.
    runClientMod(listOf("appleskin"), "maven.modrinth:appleskin:${property("deps.appleskin")}")
    // AppleSkin uses Cloth Config for its Mod Menu configuration screen.
    runClientMod(listOf("cloth-config", "cloth_config"), "maven.modrinth:cloth-config:${property("deps.cloth_config")}")
    runClientMod(listOf("modmenu"), "maven.modrinth:modmenu:${property("deps.modmenu")}")
    // Test the water purity line Jade shows when looking at water or a cauldron.
    runClientMod(listOf("jade"), "maven.modrinth:jade:${property("deps.jade")}")
    // Test the drinks and meals Farmer's Delight adds, and the c:drinks tag it fills.
    findProperty("deps.farmersdelight")?.let { farmersDelight ->
        val names = listOf("farmers-delight-refabricated", "farmersdelight")
        // The Cooking Pot fix in src/main/farmersdelight mixes into its recipe class.
        "modCompileOnly"("maven.modrinth:farmers-delight-refabricated:$farmersDelight") { isTransitive = false }
        runClientMod(names, "maven.modrinth:farmers-delight-refabricated:$farmersDelight")
        // The 1.20.1 build nests Fabric ASM, which adds its Cooking Pot recipe book to a vanilla enum, and
        // the Porting Lib modules it runs on. Loom leaves nested mods packed, and without them it fails to
        // start. Later builds need none of theirs in development. Unpacked when the classpath is resolved:
        // the Loom that builds 1.20.1 refuses a dependency resolved while the project is still configuring.
        if (sc.current.parsed < "1.20.5") {
            runClientMod(names, files(provider { nestedMods("farmers-delight-refabricated", farmersDelight.toString()) }))
        }
    }

    if (createFlyClasses != null) {
        // The Sand Filter extends Create classes on both sides, so both source sets compile against it.
        compileOnly(files(createFlyClasses))
        "clientCompileOnly"(files(createFlyClasses))
        // Off in runClient by default: Create is the heaviest mod here and slows every client start.
        // Uncomment the line below only to test the Sand Filter with pipes, pumps and spouts.
        // runClientMod(listOf("create-fly", "create"), "maven.modrinth:create-fly:$createFly")
        // Keeps `-PwithoutOptional=create` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("create-fly", "create"))
        // `-Pcreate` puts Create Fly on runServer and runBenchmark too, to benchmark the mod with it
        // installed. Off by default, so the usual benchmark measures the mod alone.
        if (providers.gradleProperty("create").isPresent) {
            "devRuntimeOnly"("maven.modrinth:create-fly:$createFly")
        }
    }

    if (supplementaries != null) {
        val moonlight = property("deps.moonlight").toString()
        // Mixed into, so both have to be remapped mods rather than plain libraries: Moonlight for the
        // soft fluid system, Supplementaries for its faucet's cauldron behaviour.
        "modCompileOnly"("maven.modrinth:supplementaries:$supplementaries") { isTransitive = false }
        "modCompileOnly"("maven.modrinth:moonlight:$moonlight") { isTransitive = false }
        // Off in runClient by default, with Spelunkery and Moonlight below: Moonlight builds its dynamic
        // resources at every start, and Spelunkery adds world generation. Uncomment the line below, and
        // Moonlight's, only to test jars, goblets and faucets. The gametests and runServer run without
        // them, which is what proves the mod is unchanged when they are absent.
        // runClientMod(listOf("supplementaries", "moonlight"), "maven.modrinth:supplementaries:$supplementaries")
        // Keeps `-PwithoutOptional=supplementaries` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("supplementaries", "moonlight"))
    }

    if (spelunkery != null) {
        // Its salt bucket recipe in runClient only. The mixin targets Minecraft's cooking recipe and names
        // nothing of the mod, so nothing is compiled against it. Off by default, see Supplementaries.
        // runClientMod(listOf("spelunkery", "moonlight"), "maven.modrinth:spelunkery:$spelunkery")
        optionalRunMods.include(listOf("spelunkery", "moonlight"))
    }

    if (bountifulFares != null) {
        // Mixed into, so it has to be a remapped mod rather than a plain library, as Brewin' and Chewin' is.
        "modCompileOnly"("maven.modrinth:bountiful-fares:$bountifulFares") { isTransitive = false }
        // The cup, the vessel and the teas in runClient only, with the NexusLib it requires; Cloth Config
        // is above, for AppleSkin. The gametests and runServer run without them, which is what proves the
        // mod is unchanged when they are absent. Off by default: its trees and crops add world generation.
        // Uncomment the two lines below only to work on it.
        val names = listOf("bountiful-fares", "bountifulfares", "nexuslib")
        // runClientMod(names, "maven.modrinth:bountiful-fares:$bountifulFares")
        // runClientMod(names, "maven.modrinth:nexuslib:${property("deps.nexuslib")}")
        optionalRunMods.include(names)
    }

    // Moonlight Lib, which Supplementaries and Spelunkery both require, once for both. Leaving it out
    // leaves them out. CodecUI comes out of Moonlight's own jar, since Loom leaves a dependency's nested
    // mods packed. Off by default with them; uncomment along with either.
    if (supplementaries != null || spelunkery != null) {
        val moonlight = property("deps.moonlight").toString()
        // runClientMod(listOf("moonlight"), "maven.modrinth:moonlight:$moonlight")
        // runClientMod(listOf("moonlight"), files(nestedMods("moonlight", moonlight)))
        optionalRunMods.include(listOf("moonlight"))
    }

    if (kaleidoscopeCookery != null) {
        // Mixed into, so it has to be a remapped mod rather than a plain library, as Supplementaries is.
        "modCompileOnly"("maven.modrinth:kaleidoscope-cookery-refabricated:$kaleidoscopeCookery") { isTransitive = false }
        // Test the stockpot and the teapot in runClient. The gametests and runServer run without it, which
        // is what proves the mod is unchanged when it is absent.
        val names = listOf("kaleidoscope-cookery", "kaleidoscope-cookery-refabricated", "kaleidoscope_cookery")
        runClientMod(names, "maven.modrinth:kaleidoscope-cookery-refabricated:$kaleidoscopeCookery")
        // Required by it on the 1.21.x nodes, optional on 26.x, where no table names it. Its Night Config
        // comes out of its own jar, since Loom leaves a dependency's nested mods packed.
        findProperty("deps.forge_config_api_port")?.let { forgeConfigApiPort ->
            val library = names + listOf("forge-config-api-port", "forgeconfigapiport")
            runClientMod(library, "maven.modrinth:forge-config-api-port:$forgeConfigApiPort")
            // Unpacked when the classpath is resolved; see Farmer's Delight above.
            runClientMod(library, files(provider { nestedMods("forge-config-api-port", forgeConfigApiPort.toString()) }))
        }
    }

    if (kaleidoscopeTavern != null) {
        // Mixed into, so it has to be a remapped mod rather than a plain library, as Kaleidoscope Cookery is.
        "modCompileOnly"("maven.modrinth:kaleidoscope-tavern-refabricated:$kaleidoscopeTavern") { isTransitive = false }
        // Test the barrel, the tap, the placed bottle and the shaker in runClient. The gametests and
        // runServer run without it, which is what proves the mod is unchanged when it is absent.
        val names = listOf("kaleidoscope-tavern", "kaleidoscope-tavern-refabricated", "kaleidoscope_tavern")
        runClientMod(names, "maven.modrinth:kaleidoscope-tavern-refabricated:$kaleidoscopeTavern")
        // On 1.20.1 it requires Reach Entity Attributes, nested in its own jar beside Fabric API modules
        // the run already has, so only that one comes out. Unpacked when the classpath is resolved; see
        // Farmer's Delight above.
        runClientMod(names, files(provider {
            nestedMods("kaleidoscope-tavern-refabricated", kaleidoscopeTavern).filter { it.name.startsWith("reach-entity-attributes") }
        }))
        // Required by it on the 1.21.x and 1.20.1 nodes, as by Kaleidoscope Cookery, and named again here
        // so leaving Cookery out leaves it in. Gradle adds the same dependency once.
        findProperty("deps.forge_config_api_port")?.let { forgeConfigApiPort ->
            val library = names + listOf("forge-config-api-port", "forgeconfigapiport")
            runClientMod(library, "maven.modrinth:forge-config-api-port:$forgeConfigApiPort")
            runClientMod(library, files(provider { nestedMods("forge-config-api-port", forgeConfigApiPort.toString()) }))
        }
    }

    if (brewinAndChewin != null) {
        // Mixed into, so it has to be a remapped mod rather than a plain library, as Kaleidoscope Cookery is.
        "modCompileOnly"("maven.modrinth:brewin-and-chewin:$brewinAndChewin") { isTransitive = false }
        // Test the keg in runClient. The gametests and runServer run without it, which is what proves the
        // mod is unchanged when it is absent. Greenhouse Config, which it requires, comes out of its own
        // jar, since Loom leaves a dependency's nested mods packed. Farmer's Delight is already above, and
        // leaving it out leaves this out.
        val names = listOf("brewin-and-chewin", "brewinandchewin", "farmers-delight-refabricated", "farmersdelight")
        runClientMod(names, "maven.modrinth:brewin-and-chewin:$brewinAndChewin")
        runClientMod(names, files(nestedMods("brewin-and-chewin", brewinAndChewin)))
    }

    if (farmAndCharm != null) {
        // Mixed into, so it has to be a remapped mod rather than a plain library, as Brewin' and Chewin' is.
        "modCompileOnly"("maven.modrinth:lets-do-farm-charm:$farmAndCharm") { isTransitive = false }
        // From 1.1.28 its blocks extend Foundation's, and its kitchen sink is Foundation's, a library
        // nested in its jar; mixed into as well, so remapped.
        "modCompileOnly"(files(nestedMods("lets-do-farm-charm", farmAndCharm).filter { it.name.startsWith("foundation") }))
        // Test the well, the trough, the Cooking Pot and Candlelight's kitchen sinks in runClient. The
        // gametests and runServer run without them, which is what proves the mod is unchanged when they are
        // absent. Both require Architectury API, added below. Leaving Farm & Charm out leaves Candlelight
        // out too; leaving Candlelight out keeps Farm & Charm. Off by default: its crops and world
        // generation slow every client start. Uncomment the two lines below only to work on them.
        val names = listOf("farm-and-charm", "farm_and_charm", "lets-do-farm-charm") + architecturyNames
        // runClientMod(names, "maven.modrinth:lets-do-farm-charm:$farmAndCharm")
        // runClientMod(names + listOf("candlelight"),
        //     "maven.modrinth:lets-do-candlelight-farmcharm-compat:${property("deps.candlelight")}")
        // Keeps their `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names + listOf("candlelight"))
    }

    if (herbalBrews != null) {
        // Mixed into, like Farm & Charm. The Tea Kettle and the Jug in runClient only.
        "modCompileOnly"("maven.modrinth:lets-do-herbalbrews:$herbalBrews") { isTransitive = false }
        runClientMod(listOf("herbalbrews", "lets-do-herbalbrews") + architecturyNames,
            "maven.modrinth:lets-do-herbalbrews:$herbalBrews")
    }

    if (beachparty != null) {
        // Mixed into, like Farm & Charm. The cocktails in runClient only. Its manifest asks for nothing
        // but Architectury, yet its client entrypoint names Trinkets' renderer unconditionally, so a
        // client without Trinkets fails to start. Trinkets nests its Cardinal Components, which Loom
        // leaves packed. Leaving Trinkets out leaves Beachparty out. Off by default: its world generation
        // and Trinkets slow every client start. Uncomment the three lines below only to work on it.
        val trinkets = property("deps.trinkets").toString()
        val names = listOf("beachparty", "lets-do-beachparty", "trinkets") + architecturyNames
        "modCompileOnly"("maven.modrinth:lets-do-beachparty:$beachparty") { isTransitive = false }
        // runClientMod(names, "maven.modrinth:lets-do-beachparty:$beachparty")
        // runClientMod(names, "maven.modrinth:trinkets:$trinkets")
        // runClientMod(names, files(nestedMods("trinkets", trinkets)))
        // Keeps their `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names)
    }

    if (meadow != null) {
        // Mixed into, like Farm & Charm. Nothing here names a Foundation class, so Foundation is not on the
        // compile classpath. The wooden bucket, the wooden cauldron and the Cooking Pot in runClient only;
        // its manifest also asks for Cloth Config, which is there already, and for Foundation, nested in
        // its jar, which Loom leaves packed, so it is unpacked onto runClient as Brewin' and Chewin's
        // libraries are. Off by default: its biomes and world generation slow every client start.
        // Uncomment the two lines below only to work on it.
        val names = listOf("meadow", "lets-do-meadow") + architecturyNames
        "modCompileOnly"("maven.modrinth:lets-do-meadow:$meadow") { isTransitive = false }
        // runClientMod(names, "maven.modrinth:lets-do-meadow:$meadow")
        // runClientMod(names, files(nestedMods("lets-do-meadow", meadow)))
        // Keeps its `-PwithoutOptional` names known while the line above is off.
        optionalRunMods.include(names)
    }

    if (vinery != null) {
        // Nothing of Vinery's is compiled against: its juices and wines are ThirstConfig ids. On
        // runClient only, to drink them. Off by default: its trees, grapevines and wandering winemaker
        // slow every client start. Uncomment the line below only to work on it.
        val names = listOf("vinery", "lets-do-vinery") + architecturyNames
        // runClientMod(names, "maven.modrinth:lets-do-vinery:$vinery")
        // Keeps its `-PwithoutOptional` names known while the line above is off.
        optionalRunMods.include(names)
    }

    if (croptopia != null) {
        // Nothing of Croptopia's is compiled against: its drinks are ThirstConfig ids and the mixin targets
        // Minecraft's crafting recipes. On runClient only, through CurseMaven. Off by default: its dozens
        // of crops and fruit trees slow every client start. Uncomment the two lines below only to work on it.
        val names = listOf("croptopia", "epherolib")
        // runClientMod(names, "curse.maven:croptopia-415438:$croptopia")
        // runClientMod(names, "curse.maven:epherolib-885449:${property("deps.epherolib")}")
        // Keeps its `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names)
    }

    // Architectury API, which every Let's Do mod requires, once for all of them. Each of them lists its
    // names, so leaving it out leaves them out; leaving one of them out keeps it for the others.
    if (farmAndCharm != null || herbalBrews != null || beachparty != null || vinery != null || meadow != null) {
        runClientMod(architecturyNames, "maven.modrinth:architectury-api:${property("deps.architectury")}")
    }

    if (sereneSeasons != null) {
        val glitchCore = property("deps.glitchcore").toString()
        // Read through its API alone, but its 1.21.x jars are in intermediary names, so it is a remapped
        // mod like the others. GlitchCore and the Night Config nested in Serene Seasons are compiled
        // against too: before 26.2 the dimension check reads Serene Seasons' config, whose class extends
        // GlitchCore's, which extends Night Config's.
        "modCompileOnly"("maven.modrinth:serene-seasons:$sereneSeasons") { isTransitive = false }
        "modCompileOnly"("maven.modrinth:glitchcore:$glitchCore") { isTransitive = false }
        // Unpacked when the classpath is resolved; see Farmer's Delight above.
        compileOnly(files(provider { nestedMods("serene-seasons", sereneSeasons) }))
        // Off in runClient: it recolours grass and leaves by the season and snows on plains in winter,
        // which gets into every other test and screenshot. Uncomment the three lines below only to work
        // on the Serene Seasons integration. The gametests and runServer run without it, which is what
        // proves the mod is unchanged when it is absent. The Night Config it nests comes out of its jar,
        // since Loom leaves nested mods packed; GlitchCore nests the same one.
        val names = listOf("serene-seasons", "sereneseasons")
        // runClientMod(names, "maven.modrinth:serene-seasons:$sereneSeasons")
        // runClientMod(names, files(provider { nestedMods("serene-seasons", sereneSeasons.toString()) }))
        // runClientMod(names + listOf("glitchcore"), "maven.modrinth:glitchcore:$glitchCore")
        // Keeps `-PwithoutOptional=serene-seasons` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(names + listOf("glitchcore"))
    }

    // A name no node loads is refused in stonecutter.gradle.kts, once every node has said what it takes.
    project.extra["thirst.optionalRunMods"] = optionalRunMods.offered
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to mcCompat,
        "loader" to loaderVersion,
        "java" to requiredJava.majorVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }

    // Only a node that builds an integration may name its mixin config and entrypoints, or Fabric Loader
    // would fail to find them on every other one. They are added to the built manifest rather than
    // templated into the source, which has to stay valid JSON for Loom to read. One input per
    // integration, so a change of its version reruns this.
    integrations.filter { Loader.FABRIC in it.loaders }.forEach { integration ->
        inputs.property(integration.dir, findProperty(integration.depsKey)?.toString() ?: "")
    }
    if (nodeIntegrations.isNotEmpty()) {
        val manifest = destinationDir.resolve("fabric.mod.json")
        doLast {
            @Suppress("UNCHECKED_CAST")
            val json = groovy.json.JsonSlurper().parse(manifest) as MutableMap<String, Any?>
            nodeIntegrations.forEach { it.patchFabricManifest(json) }
            manifest.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)))
        }
    }
}

// 1.21 renamed the data pack directories from plural to singular. The gametests' own data is written
// the newer way and renamed here for the versions before it, rather than kept twice.
if (sc.current.parsed < "1.21") {
    tasks.named<ProcessResources>("processGametestResources") {
        eachFile { path = path.replace("/tags/item/", "/tags/items/") }
        includeEmptyDirs = false
    }
}

// The client mixins have their own configs, in the client source set: the loader independent one in
// src/client/resources and Fabric's own in src/client/fabric/resources.
tasks.named<ProcessResources>("processClientResources") {
    inputs.property("java", requiredJava.majorVersion)
    filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
}

// The dev tools have a mixin config of their own, for the one mixin that records where the HUD drew
// the bar. It needs the same compatibility level as the rest, for the same reason: a node on Java 25
// writes class files Mixin refuses to read at level 21.
tasks.named<ProcessResources>("processDevResources") {
    inputs.property("java", requiredJava.majorVersion)
    filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
}

// Keep the client source set's runtime classpath entry present so Fabric Loader does not report
// build/resources/client as a missing path during runClient.
tasks.named("runClient") {
    doFirst {
        layout.buildDirectory.dir("resources/client").get().asFile.mkdirs()
    }
}

// Registered lazily: withSourcesJar(), in gradle/shared.gradle.kts below, adds the task after this
// block is evaluated. The dependency is the one described above, which the sources jar needs for the
// same reason processResources does. What the sources jar leaves out is in gradle/shared.gradle.kts.
tasks.withType<Jar>().matching { it.name.endsWith("sourcesJar") }.configureEach {
    dependsOn("stonecutterGenerate")
    mustRunAfter("runDatagen")
}

// checkDataConditions, from gradle/shared.gradle.kts, reads the generated data too, which runDatagen
// rewrites when checkDatagen runs in the same build, as on CI. Gradle fails a task that reads another's
// output without an order, so it reads what datagen has just written.
tasks.matching { it.name == "checkDataConditions" }.configureEach {
    mustRunAfter("runDatagen")
}

// The toolchain, the seam checks, the jar excludes and `buildAndCollect` are shared with the
// NeoForge node, which cannot apply this script. The Java version is passed in because it follows
// from the node's Minecraft version, which only this script can read.
extra["thirst.requiredJava"] = requiredJava.majorVersion
// And the integration table, as far as the seam checks need it: a script applied with `apply(from)`
// cannot see the classes of build-logic, so it cannot read the table itself.
extra["thirst.integrations"] = integrations.map { it.dir }
extra["thirst.loaderIndependentIntegrations"] = integrations.filter { it.loaderIndependent }.map { it.dir }
apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.jar {
    // The MIT notice of the code this mod is based on has to ship with it, alongside the GPL.
    from(files(rootProject.file("LICENSE"), rootProject.file("CREDITS.md"),
            rootProject.file("licenses/ThirstWasTaken-MIT.txt"))) {
        rename { name -> name.substringBeforeLast('.') + "_$modId" + name.removePrefix(name.substringBeforeLast('.')) }
    }
}

/**
 * Regenerates the datapack and asset JSON and fails when the result differs from what is committed.
 * This is the check that keeps `src/datagen` and `src/main/generated` from drifting apart: editing a
 * generated file by hand, or a generator without regenerating, both fail here.
 *
 * It asks git rather than diffing trees itself, because git already knows which files are committed
 * and which are new, and datagen writes in place.
 */
tasks.register("checkDatagen") {
    group = "verification"
    description = "Fails when the committed generated resources do not match what the generators produce"
    dependsOn("runDatagen")

    val root = rootProject.projectDir
    val generated = generatedResources

    doLast {
        val relative = root.toPath().relativize(generated.toPath()).toString().replace('\\', '/')
        val process = ProcessBuilder("git", "status", "--porcelain", "--", relative)
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val changes = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0) { "git status failed:\n$changes" }
        check(changes.isEmpty()) {
            "Generated resources are out of date. Run \":${project.name}:runDatagen\" and commit:\n$changes"
        }
    }
}

// What this node hands `buildAndCollect`, which gradle/shared.gradle.kts registers: Loom's remapped
// jar, not the plain one.
tasks.named<Copy>("buildAndCollect") {
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = modGroup
            from(components["java"])
        }
    }
    repositories { }
}
