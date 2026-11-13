import net.neoforged.moddevgradle.dsl.ModDevExtension

/*
 * What the two ModDevGradle scripts share: build.neoforge.gradle.kts (the NeoForge nodes) and
 * build.forge.gradle.kts (the Forge node), both on the same `moddev-gradle` artifact. Their source sets,
 * mods and runs, the dev tools and the watchdog, the resource translation and its check, and the tasks
 * around Minecraft's artifacts are here once. What each loader does differently stays in its own script:
 * the loader version, the fluid API directories, the dependencies and how they are remapped, the mixin
 * wiring Forge 47 needs, the manifest's expansions and the jar `buildAndCollect` ships.
 *
 * Each script applies this right after its `neoForge { }` or `legacyForge { }` block sets the version,
 * and before anything that names a source set below; gradle/shared.gradle.kts comes later, as on Fabric.
 * A script applied with `apply(from)` sees ModDevGradle, which settings.gradle.kts resolves, but not
 * build-logic or Stonecutter, so the applying script hands over what it reads from them as extra
 * properties, all set before the `apply`:
 *
 * | Property                            | What                                                                      |
 * |-------------------------------------|---------------------------------------------------------------------------|
 * | `thirst.loader`                     | the loader's source directory name, `neoforge` or `forge`                 |
 * | `thirst.loaderName`                 | its name in task names and messages, `NeoForge` or `Forge`                |
 * | `thirst.manifest`                   | the mods.toml the integrations are appended to                            |
 * | `thirst.minecraft`                  | the node's Minecraft version, Stonecutter's `sc.current.version`          |
 * | `thirst.gametestReportOption`       | whether the gametest server takes `--report` (1.21.5 on)                  |
 * | `thirst.flightRecorder`             | the JVM arguments `-Pprofile` adds to runBenchmark, from build-logic      |
 * | `thirst.integrationManifest`        | the text the node's integrations append to the manifest, or ""           |
 * | `thirst.integrationInputs`          | each integration this loader has, by directory, to its deps value or ""  |
 * | `thirst.translateJson`              | `(json, file) -> json`, build-logic's translator for this loader          |
 * | `thirst.devRunProgramArguments`     | program arguments for the runs that load the dev tools                    |
 */

@Suppress("UNCHECKED_CAST")
fun <T> handed(name: String): T = project.extensions.extraProperties.get(name) as T

val loader: String = handed("thirst.loader")
val loaderName: String = handed("thirst.loaderName")
val requiredJava: String = handed("thirst.requiredJava")
val translateJson: (Any?, String) -> Any? = handed("thirst.translateJson")
val modId = property("mod.id") as String
/** The dev tools' mod id. NeoForge and Forge ids cannot contain a hyphen, so it is not the Fabric `-dev` spelling. */
val devModId = "thirstwastaken2_dev"
/** `-Pagent=<file>`: a file of agent requests to answer once, unattended. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md. */
val agentScript: String? = providers.gradleProperty("agent").orNull?.let { rootProject.file(it).absolutePath }
/** `-Pdriven`: this client is driven by an agent, not played. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md. */
val drivenClient: Boolean = providers.gradleProperty("driven").isPresent

val sourceSets = the<SourceSetContainer>()
val main: SourceSet = sourceSets["main"]

/*
 * One source set. ModDevGradle has no split between `main` and `client`, so the client sources compile
 * into `main` here. The Fabric nodes keep `loom.splitEnvironmentSourceSets()`, so the compiler still
 * catches client code reached from common code there.
 *
 * `src/datagen` is absent on purpose: the generators stay Fabric only, and these nodes read the datapack
 * and asset JSON that the Fabric node of the same Minecraft version writes. `src/gametest` and `src/dev`
 * are both here, each as a mod of its own, below.
 *
 * `src/client` is listed as directories of `main`. Stonecutter preprocesses every source directory of a
 * source set that lies inside the project, whatever its name, so `src/client/...` on `main` gets this
 * node's copy as `src/main/...` does. Before Stonecutter 0.10 it read only `src/<name>` for a source set
 * named `<name>`, and these scripts kept a `client` source set only to have it preprocessed.
 */
main.apply {
    java.srcDir("src/main/$loader/java")
    resources.srcDir("src/main/$loader/resources")
    java.srcDir("src/client/java")
    java.srcDir("src/client/$loader/java")
    resources.srcDir("src/client/resources")
    resources.srcDir("src/client/$loader/resources")
    // Written by the Fabric node's `runDatagen`, keyed by Minecraft version rather than by node so every
    // node of a Minecraft version shares one directory. See src/datagen/java/AGENTS.md.
    resources.srcDir(rootProject.file("src/main/generated/${handed<String>("thirst.minecraft")}"))
}

/*
 * The same gametests the Fabric nodes run, as their own small mod, so none of it reaches the jar.
 * `src/gametest/<loader>` holds the harness that finds and registers them, in place of Fabric API's;
 * the test classes themselves are shared, and stonecutter.gradle.kts swaps their one Fabric import.
 * See src/gametest/java/AGENTS.md.
 */
val gametest: SourceSet = sourceSets.create("gametest") {
    java.srcDir("src/gametest/$loader/java")
    resources.srcDir("src/gametest/$loader/resources")
    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

/*
 * The dev tools, as their own small mod, the same arrangement as the gametests: the agent and
 * `/thirst benchmark`, both of them here. `src/dev/<loader>` holds the entrypoints, the loader seam the
 * agent needs and this loader's `BenchmarkPlayer`, which is the one class of the benchmark that names a
 * loader: it extends the loader's `FakePlayer` where the Fabric copy extends Fabric API's.
 * See src/dev/java/AGENTS.md.
 */
val dev: SourceSet = sourceSets.create("dev") {
    java.srcDir("src/dev/$loader/java")
    resources.srcDir("src/dev/$loader/resources")
    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

/*
 * A `-javaagent` for `-Pagent` runs, JDK only: it stops the run when FML writes a loading failure's
 * crash report, which Forge 47 does the same way NeoForge's FML does. A missing dependency fails before
 * any mod mixin applies, so the dev mod's own LoadingErrorScreenMixin never runs and the error screen
 * would hold the task open. See src/watchdog/java/com/thirstwastaken2/dev/watchdog/LoadingFailureWatchdog.java.
 */
val watchdog: SourceSet = sourceSets.create("watchdog") {
    java.srcDir("src/watchdog/java")
}
val watchdogJar = tasks.register<Jar>("watchdogJar") {
    archiveBaseName.set("thirstwastaken2-watchdog")
    destinationDirectory.set(layout.buildDirectory.dir("watchdog"))
    from(watchdog.output)
    manifest.attributes("Premain-Class" to "com.thirstwastaken2.dev.watchdog.LoadingFailureWatchdog")
}
// The runs name the jar by path, which carries no task dependency.
if (agentScript != null) tasks.matching { it.name.startsWith("run") }.configureEach { dependsOn(watchdogJar) }

/*
 * The optional mods runClient loads, the same set the Fabric runClient has minus Mod Menu, which is
 * Fabric only. Each script adds them to `clientRunMods` through its own `runClientMod`, Forge's through a
 * remapping configuration. They go on that run alone: on `runtimeOnly` they would load into runServer and
 * runGametest too, and the gametests expect a server without AppleSkin. ModDevGradle's per-run
 * `additionalRuntimeClasspath` would be the place, but it refuses dependencies from Minecraft 26.2 on,
 * and a run's classpath is its source set's runtime classpath. So runClient gets a source set with no
 * sources of its own, whose runtime classpath is `main`'s plus these.
 *
 * The dev tools ride along on both of these, so an agent can drive runServer and every client through
 * run/<node>/agent/<name>/.
 *
 * Added to, never replaced. A source set's own runtime classpath is where ModDevGradle puts DevLaunch,
 * whose `Main` is the class every run is launched through, so a source set that assigns its classpath
 * outright launches nothing: `Could not find or load main class net.neoforged.devlaunch.Main`.
 */
val clientRunMods: Configuration = configurations.create("clientRunMods")
val clientRun: SourceSet = sourceSets.create("clientRun") {
    runtimeClasspath += dev.output + main.output + main.runtimeClasspath + clientRunMods
}
val serverRun: SourceSet = sourceSets.create("serverRun") {
    runtimeClasspath += dev.output + main.output + main.runtimeClasspath
}

extensions.getByType<ModDevExtension>().apply {
    mods {
        create(modId) {
            sourceSet(main)
        }
        // Mod ids cannot contain a hyphen; the Fabric nodes use the same id.
        create("thirstwastaken2_gametest") {
            sourceSet(gametest)
        }
        create(devModId) {
            sourceSet(dev)
        }
    }

    runs {
        val node = project.name
        configureEach {
            // `-Pagent=<file>` answers that file of agent requests once the game is up and then stops
            // it, which is what an unattended run is. Without it the agent is still there, waiting on
            // run/<node>/agent/<name>/in.jsonl. See src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md.
            agentScript?.let {
                systemProperty("thirstwastaken2.agent.script", it)
                systemProperty("thirstwastaken2.agent.script.exit", "true")
                jvmArguments.add(watchdogJar.flatMap { jar -> jar.archiveFile }.map { "-javaagent:${it.asFile.absolutePath}" })
            }

            // `-Pdriven` says this client is driven by an agent rather than played: it opens
            // maximised and never takes the mouse pointer, so the desktop stays usable while a
            // script drives it. An unattended `-Pagent=<file>` run implies it.
            if (drivenClient) systemProperty("thirstwastaken2.agent.driven", "true")

            // One run directory per node, for the reason build.gradle.kts gives: a world saved by
            // one Minecraft version is not readable by another, and a failed test run must not
            // leave a broken world behind for runServer. The extra clients get one each as well:
            // two running clients cannot share a directory, and theirs must not touch runClient's.
            // The benchmark falls into the last branch on purpose: it shares runServer's world and
            // accepted EULA, exactly as it does on the Fabric nodes.
            gameDirectory.set(rootProject.file(when (name) {
                "gametest" -> "run/$node/gametest"
                "manualA", "manualB" -> "run/manual-$node-${name.last()}"
                else -> "run/$node"
            }))
        }

        create("client") {
            client()
            sourceSet = clientRun
            // `-Pquickplay=<world>` opens that singleplayer world straight from launch, so an
            // unattended `-Pagent` script starts inside it. The world has to exist in run/<node>/saves.
            providers.gradleProperty("quickplay").orNull?.let { world ->
                programArguments.addAll("--quickPlaySingleplayer", world)
                // So client.info can say the world was asked for and never opened.
                systemProperty("thirstwastaken2.agent.quickplay", world)
            }
        }
        create("server") {
            server()
            sourceSet = serverRun
        }
        // Unattended benchmark: starts the dedicated server, runs `/thirst benchmark <-Pbenchmark>` from
        // the console once it is up, writes run/<node>/benchmark/latest.json and stops the server. It
        // shares runServer's directory, world and accepted EULA, so the two cannot run at the same time.
        create("benchmark") {
            server()
            sourceSet = serverRun
            systemProperty("thirstwastaken2.benchmark",
                providers.gradleProperty("benchmark").getOrElse("standard"))
            systemProperty("thirstwastaken2.benchmark.exit", "true")
            // A world of its own inside runServer's directory, generated with the fixed seed
            // gradle/shared.gradle.kts puts in server.properties, so a run measures the same terrain
            // on every machine and on every loader of this Minecraft version, and never the dev world.
            programArguments.addAll("--world", providers.gradleProperty("thirst.benchmark.world").get())

            // `-Pprofile` records the run with JFR and writes run/<node>/benchmark/latest.jfr for JDK
            // Mission Control to open; build-logic's flightRecorder says why each argument is there. A
            // recording slows the run down and skews every figure in the report, which is why the
            // report says so and aggregate.py refuses a set with one in it.
            if (providers.gradleProperty("profile").isPresent) {
                jvmArguments.addAll(handed<List<String>>("thirst.flightRecorder"))
            }
        }
        // Two more clients, for the checklist items that need a second player: MANUAL-TESTING.md's
        // "Sync to the client" section, where each player has to see their own bar and no one else's.
        // Two dev clients cannot both be `Dev` on one server, so each is named here, and
        // --quickPlayMultiplayer joins runServer on this machine from the title screen.
        listOf("A", "B").forEach { tester ->
            create("manual$tester") {
                client()
                sourceSet = clientRun
                programArguments.addAll(
                    "--username", "Tester$tester",
                    "--width", "1100", "--height", "700",
                    "--quickPlayMultiplayer", "localhost:25565",
                )
                // Each client answers its own queue, run/manual-<node>-<A|B>/agent/<A|B>/, which is
                // what "each player sees only their own bar" is read out of.
                systemProperty("thirstwastaken2.agent", tester)
            }
        }
        // The GameTest runner: a dedicated server that runs every registered test headlessly, skips
        // the EULA prompt and exits with the number of failed required tests, the same contract as the
        // Fabric runner. `runGametest` is the task name on every node.
        create("gametest") {
            type = "gameTestServer"
            sourceSet = gametest
            // Vanilla's own JUnit report, at the path the Fabric nodes write theirs to, so CI uploads
            // it the same way. The server's --report option arrived with 1.21.5; before it the harness
            // installs the same reporter itself when this property names a file.
            val report = layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath
            if (handed("thirst.gametestReportOption")) programArguments.addAll("--report", report)
            else systemProperty("thirstwastaken2.gametest.report", report)
        }

        // ModDevGradle loads every declared mod by default, and each run only has the classes of the
        // source sets on its own classpath. So each one is told exactly which mods to load: the
        // gametests are their own mod and belong to the gametest run alone, and the dev tools belong
        // to every run an agent may drive.
        val mainMod = mods.named(modId)
        val devMod = mods.named(devModId)
        val gametestMod = mods.named("thirstwastaken2_gametest")
        val devRunArguments: List<String> = handed("thirst.devRunProgramArguments")
        listOf("client", "server", "benchmark", "manualA", "manualB").forEach { run ->
            named(run) {
                loadedMods.set(mainMod.zip(devMod) { main, agent -> setOf(main, agent) })
                programArguments.addAll(devRunArguments)
            }
        }
        named("gametest") { loadedMods.set(mainMod.zip(gametestMod) { main, tests -> setOf(main, tests) }) }
    }
}

/** Written by the Fabric node's `runDatagen`; the only files whose Fabric spellings are translated. */
val generatedResources = rootProject.file("src/main/generated/${handed<String>("thirst.minecraft")}")

tasks.named<ProcessResources>("processResources") {
    // Stonecutter rewrites the versioned comments in `src/` into this node's own source tree, so
    // everything that reads those files has to wait for it.
    dependsOn("stonecutterGenerate")
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$requiredJava") }
    // Datagen's hash cache, which Loom keeps out of the Fabric mod jar and nothing keeps out of these.
    exclude("**/.cache/**")

    // Only a node that builds an integration may name its mixin config, or the loader would fail to
    // find it on every other one. It is appended to the built manifest, with the integration's mods as
    // optional dependencies, rather than templated into the source manifest every node of the loader
    // shares. One input per integration, so a change of its version reruns this.
    inputs.properties(handed<Map<String, String>>("thirst.integrationInputs"))
    val appended: String = handed("thirst.integrationManifest")
    if (appended.isNotEmpty()) {
        val manifest = destinationDir.resolve(handed<String>("thirst.manifest"))
        doLast { manifest.appendText(appended) }
    }

    // Translates the generated JSON in place, once it is copied; build-logic's ResourceTranslation.kt
    // has the tables. Only files that came from the generated root are read, and only those naming
    // Fabric are rewritten, so the rest keep their bytes. The Copy task copies every file again
    // whenever any input changes, so a translated file is never translated twice.
    val output = destinationDir
    doLast {
        generatedResources.walk().filter { it.isFile && it.extension == "json" }.forEach { source ->
            val relative = source.relativeTo(generatedResources).invariantSeparatorsPath
            val target = output.resolve(relative)
            if (!target.isFile) return@forEach
            val text = target.readText()
            if (!text.contains("\"fabric:")) return@forEach
            val translated = translateJson(groovy.json.JsonSlurper().parseText(text), relative)
            target.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(translated)))
        }
    }
}

// The dev tools have a mixin config of their own, for the one mixin that records where the HUD drew
// the bar. It needs the same compatibility level as the rest, for the same reason: a node on Java 25
// writes class files Mixin refuses to read at level 21.
tasks.named<ProcessResources>("processDevResources") {
    inputs.property("java", requiredJava)
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$requiredJava") }
}

/**
 * `checkNeoForgeResources` or `checkForgeResources`: fails when a Fabric key survives in the processed
 * resources, from datagen or from a hand-written file, because the loader would load it as a broken
 * recipe or ignore the condition. CI runs it on the loader's job.
 */
tasks.register("check${loaderName}Resources") {
    group = "verification"
    description = "Fails when a Fabric-only JSON key survives into the $loaderName resources"

    val processed = tasks.named<ProcessResources>("processResources").map { it.destinationDir }
    inputs.dir(processed)
    val fabricKey = Regex(""""fabric:[^"]*"\s*:""")

    doLast {
        val root = processed.get()
        val offenders = root.walk().filter { it.isFile && it.extension == "json" }.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> fabricKey.containsMatchIn(line) }
                .map { (index, line) -> "${file.relativeTo(root).invariantSeparatorsPath}:${index + 1}: ${line.trim()}" }
        }.toList()
        check(offenders.isEmpty()) {
            "Fabric keys in the $loaderName resources. Translate the shape in build-logic's " +
                "ResourceTranslation.kt or stop writing it:\n" + offenders.joinToString("\n")
        }
    }
}

/*
 * Every ModDevGradle node decompiles and recompiles Minecraft in createMinecraftArtifacts, which takes
 * minutes and gigabytes each. With org.gradle.parallel=true several nodes would do it at once, so a
 * shared build service lets one run at a time. The class is compiled once for this script, so every
 * node that applies it, on either loader, registers the same service. ModDevGradle needs the
 * Stonecutter output before it builds the Minecraft artifacts it compiles against.
 */
abstract class CreateMinecraftArtifactsMutex : BuildService<BuildServiceParameters.None>
val createMinecraftArtifactsMutex = gradle.sharedServices
    .registerIfAbsent("createMinecraftArtifactsMutex", CreateMinecraftArtifactsMutex::class) { maxParallelUsages = 1 }

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
    usesService(createMinecraftArtifactsMutex)
}

// Vanilla's reporter writes the file but not the directory it goes in.
tasks.named("runGametest") {
    val reportDir = layout.buildDirectory.dir("gametest")
    doFirst { reportDir.get().asFile.mkdirs() }
}

tasks.named<Jar>("jar") {
    // The MIT notice of the code this mod is based on has to ship with it, alongside the GPL.
    from(files(rootProject.file("LICENSE"), rootProject.file("CREDITS.md"),
            rootProject.file("licenses/ThirstWasTaken-MIT.txt"))) {
        rename { name -> name.substringBeforeLast('.') + "_$modId" + name.removePrefix(name.substringBeforeLast('.')) }
    }
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = property("mod.group") as String
            from(components["java"])
        }
    }
    repositories { }
}
