import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.OptionalRunMods
import com.thirstwastaken2.buildlogic.flightRecorder
import com.thirstwastaken2.buildlogic.integrations
import com.thirstwastaken2.buildlogic.integrationsFor
import com.thirstwastaken2.buildlogic.neoForgeJson

plugins {
    // NeoForge's build plugin. Its version is on the plugin classpath from settings.gradle.kts,
    // which resolves it without applying it.
    id("net.neoforged.moddev")
    // The integration table and the rest of what both loader scripts share. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
    `maven-publish`
}

/*
 * The NeoForge nodes, `<version>-neoforge`. Each is one Minecraft version and one loader, with the same
 * sources as the Fabric node of that version. What this and build.forge.gradle.kts share (source sets,
 * mods and runs, the resource translation) is in gradle/moddev.gradle.kts, and what every node needs is
 * in gradle/shared.gradle.kts, both applied below. See src/main/java/com/thirstwastaken2/platform/AGENTS.md.
 */

// Stonecutter supplies `mod.*` and `deps.*` for this node from stonecutter.properties.toml.
// `group` stays unset for the same reason build.gradle.kts leaves it unset; the publication sets
// its own groupId in gradle/moddev.gradle.kts.
val modId = property("mod.id") as String
/** Published artifact name; deliberately not the lowercase mod id. */
val modName = property("mod.name") as String
/** Minecraft range written into neoforge.mods.toml, in Maven range syntax, e.g. `[26.2,26.3)`. */
val mcRange = property("mod.mc_compat") as String
val neoForgeVersion = property("deps.neoforge") as String

// The loader goes in the version so the jar is `ThirstWasTaken2-1.0.7+26.2-neoforge.jar` and never
// collides with the Fabric jar of the same Minecraft version in build/libs/.
version = "${property("mod.version")}+${sc.current.version}-neoforge"
base.archivesName = modName

/** Minecraft 26.1 moved to Java 25; 1.21.x still runs on Java 21. The same rule as the Fabric nodes. */
val requiredJava: JavaVersion =
    if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

repositories {
    // ModDevGradle supplies the NeoForged, Mojang and Minecraft libraries repositories.
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

/**
 * The mod loader this node builds for. Loader code lives beside the source set it belongs to, in
 * `src/main/<loader>` and `src/client/<loader>`, and only this loader's directories are compiled.
 * Everything else in `src/main/java` and `src/client/java` is loader independent; `checkLoaderSeam`
 * enforces it. See src/main/java/com/thirstwastaken2/platform/AGENTS.md.
 */
val loader = "neoforge"

/**
 * This node's copy of `src/client`, which gradle/moddev.gradle.kts compiles into `main`: the sources
 * themselves when this is the active node, which Stonecutter leaves in place, and Stonecutter's
 * generated copy otherwise. That script says why it has to be by path.
 */
val clientSources: File =
    if (sc.current.isActive) rootProject.file("src/client")
    else layout.buildDirectory.dir("generated/stonecutter/client").get().asFile

/**
 * NeoForge's two fluid APIs: IFluidHandler on 21.1, the transfer API (ResourceHandler) from 21.9. Code
 * that implements one of them lives in the directory of its generation, here and in an integration whose
 * other mod moved with NeoForge.
 */
val transferApi = sc.current.parsed >= "1.21.2"

/**
 * The optional integrations this node builds: each is a set of source directories only a node that sets
 * its deps key compiles, and only there does the manifest name its mixin config. What each one adds is a
 * row of the table in build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt, which
 * build.gradle.kts reads too; what is specific to one mod, such as its dependencies, stays below with its
 * own comment.
 */
val nodeIntegrations = integrationsFor(Loader.NEOFORGE) { findProperty(it) != null }

neoForge {
    version = neoForgeVersion
}

// What gradle/moddev.gradle.kts reads from build-logic and Stonecutter, which it cannot see; the table
// at its top says what each one is.
extra["thirst.loader"] = loader
extra["thirst.loaderName"] = "NeoForge"
extra["thirst.manifest"] = "META-INF/neoforge.mods.toml"
extra["thirst.minecraft"] = sc.current.version
extra["thirst.clientSources"] = clientSources
extra["thirst.requiredJava"] = requiredJava.majorVersion
extra["thirst.gametestReportOption"] = sc.current.parsed >= "1.21.5"
extra["thirst.flightRecorder"] = flightRecorder(rootProject.file("run/${project.name}/benchmark/latest.jfr"))
extra["thirst.integrationManifest"] = nodeIntegrations.joinToString("") { it.neoForgeManifest(modId) }
extra["thirst.integrationInputs"] = integrations.filter { Loader.NEOFORGE in it.loaders }
    .associate { it.dir to (findProperty(it.depsKey)?.toString() ?: "") }
/** The key NeoForge reads a custom ingredient's type from: vanilla's `type` until it took one of its own. */
val ingredientTypeKey = if (sc.current.parsed > "1.21.1") "neoforge:ingredient_type" else "type"
extra["thirst.translateJson"] = { json: Any?, file: String -> neoForgeJson(json, file, ingredientTypeKey) }
extra["thirst.devRunProgramArguments"] = emptyList<String>()
apply(from = rootProject.file("gradle/moddev.gradle.kts"))

sourceSets.main {
    java.srcDir(if (transferApi) "src/main/$loader-transfer/java" else "src/main/$loader-fluidhandler/java")
    nodeIntegrations.forEach { integration ->
        integration.mainRoots(transferApi).forEach { root ->
            java.srcDir("$root/java")
            resources.srcDir("$root/resources")
        }
        // From the preprocessed client sources like the rest of them.
        integration.clientJava?.let { dir ->
            java.srcDir(files(clientSources.resolve(dir)).builtBy("stonecutterGenerateClient"))
        }
    }
}

/** Create's Modrinth version id, set only on the nodes that build the Sand Filter. See src/main/create/AGENTS.md. */
val createVersion = findProperty("deps.create") as String?

/**
 * The libraries Create bundles inside its jar: Ponder (with Catnip), Flywheel and Registrate. The Sand
 * Filter extends classes whose supertypes live there, so the compiler needs them; at runtime FML reads
 * them out of Create's jar itself.
 */
val createLibraries = createVersion?.let { version ->
    val resolved = configurations.detachedConfiguration(dependencies.create("maven.modrinth:create:$version"))
        .apply { isTransitive = false }
    tasks.register<Sync>("createLibraries") {
        description = "Copies the libraries Create bundles out of its jar, for the compiler"
        from(resolved.elements.map { jars -> jars.map { zipTree(it) } }) {
            include("META-INF/jarjar/*.jar")
            eachFile { path = name }
        }
        includeEmptyDirs = false
        into(layout.buildDirectory.dir("create"))
    }
}

/** Sophisticated Core's Modrinth version id, where it has a release. See src/main/sophisticated/AGENTS.md. */
val sophisticatedCoreVersion = findProperty("deps.sophisticated_core") as String?

/** Supplementaries' Modrinth version id, on `1.21.1-neoforge` only. See src/main/supplementaries/AGENTS.md. */
val supplementariesVersion = findProperty("deps.supplementaries") as String?

/** Spelunkery's Modrinth version id, on `1.21.1-neoforge` only. See src/main/spelunkery/AGENTS.md. */
val spelunkeryVersion = findProperty("deps.spelunkery") as String?

/**
 * Bountiful Fares' Modrinth version id, and the NexusLib it requires, on `1.21.1-neoforge` only.
 * See docs/dev/integration/cooking/BOUNTIFUL-FARES-INTEGRATION.md.
 */
val bountifulFaresVersion = findProperty("deps.bountiful_fares") as String?

/**
 * Kaleidoscope Cookery's Modrinth version id, set on `1.21.1-neoforge` and nowhere else: the official
 * mod has no NeoForge build past 1.21.1. See docs/dev/integration/cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md.
 */
val kaleidoscopeCookeryVersion = findProperty("deps.kaleidoscope_cookery") as String?

/**
 * Kaleidoscope Tavern's Modrinth version id, set on `1.21.1-neoforge` and nowhere else: the official mod
 * has published no NeoForge build past 1.21.1. See docs/dev/integration/cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md.
 */
val kaleidoscopeTavernVersion = findProperty("deps.kaleidoscope_tavern") as String?

/**
 * Brewin' and Chewin's Modrinth version id, set on `1.21.1-neoforge` and nowhere else among the NeoForge
 * nodes. See docs/dev/integration/cooking/BREWIN-AND-CHEWIN-INTEGRATION.md.
 */
val brewinAndChewinVersion = findProperty("deps.brewin_and_chewin") as String?

/**
 * Let's Do: Farm & Charm's Modrinth version id, and Candlelight's, its addon, set on `1.21.1-neoforge`
 * and nowhere else among the NeoForge nodes. See docs/dev/integration/lets-do/FARM-AND-CHARM-INTEGRATION.md.
 */
val farmAndCharmVersion = findProperty("deps.farm_and_charm") as String?

/**
 * Let's Do: HerbalBrews' and Beachparty's Modrinth version ids, on `1.21.1-neoforge` and nowhere else
 * among the NeoForge nodes, like Farm & Charm. See docs/dev/integration/lets-do/.
 */
val herbalBrewsVersion = findProperty("deps.herbalbrews") as String?
val beachpartyVersion = findProperty("deps.beachparty") as String?
val vineryVersion = findProperty("deps.vinery") as String?
val meadowVersion = findProperty("deps.meadow") as String?

/**
 * Croptopia's CurseForge file id, and the EpheroLib it requires, on every NeoForge node it has a build
 * for. See src/main/croptopia/AGENTS.md.
 */
val croptopiaVersion = findProperty("deps.croptopia") as String?

/** Cold Sweat's Modrinth version id, on `1.21.1-neoforge` only. See src/main/coldsweat/AGENTS.md. */
val coldSweatVersion = findProperty("deps.cold_sweat") as String?

/** Cultural Delights' Modrinth version id, on `1.21.1-neoforge` only. See src/main/culturaldelights/AGENTS.md. */
val culturalDelightsVersion = findProperty("deps.cultural_delights") as String?

/** Serene Seasons' Modrinth version id, on every node. See src/main/sereneseasons/AGENTS.md. */
val sereneSeasonsVersion = findProperty("deps.serene_seasons") as String?

/** The optional mods runClient loads, which gradle/moddev.gradle.kts puts on the clients' runs alone. */
val clientRunMods: Configuration = configurations["clientRunMods"]

/**
 * `-PwithoutOptional=<name,...>`, which leaves optional mods out of runClient and the two extra clients.
 * The same flag and names as build.gradle.kts; see build-logic's OptionalRunMods.
 */
val optionalRunMods = OptionalRunMods(providers.gradleProperty("withoutOptional").orNull)

/** Architectury API's names, which every Let's Do mod lists among its own since it cannot load without it. */
val architecturyNames = listOf("architectury", "architectury-api")

/** Adds a mod to the clients' run only, unless `-PwithoutOptional` names it or one of the libraries it lists. */
fun runClientMod(names: List<String>, notation: String, configure: ExternalModuleDependency.() -> Unit = {}) {
    if (optionalRunMods.include(names)) dependencies.add(clientRunMods.name, notation, configure)
}

dependencies {
    // Optional integrations the client code compiles against. The mod runs without either; it never
    // takes a hard dependency, and `Loader.isModLoaded` gates every use. Mod Menu is Fabric only, so
    // `ModMenuIntegration` stays in src/client/fabric and the config screen is registered through
    // NeoForge's own IConfigScreenFactory instead.
    compileOnly("maven.modrinth:appleskin:${property("deps.appleskin")}")
    compileOnly("maven.modrinth:jade:${property("deps.jade")}")

    runClientMod(listOf("appleskin"), "maven.modrinth:appleskin:${property("deps.appleskin")}")
    runClientMod(listOf("jade"), "maven.modrinth:jade:${property("deps.jade")}")
    // AppleSkin's own config screen, on the nodes that set it. Nothing compiles against Cloth Config and
    // the mod never reaches for it, so a node whose Minecraft version has no NeoForge build of it yet
    // simply runs the dev client without it.
    findProperty("deps.cloth_config")?.let {
        runClientMod(listOf("cloth-config", "cloth_config"), "maven.modrinth:cloth-config:$it")
    }

    // Farmer's Delight, on the nodes that set it: nothing compiles against it, since the mod reaches it
    // by registry id alone, so it is only here to test its drinks, the Cooking Pot and Nourishment.
    findProperty("deps.farmersdelight")?.let {
        runClientMod(listOf("farmers-delight", "farmersdelight"), "maven.modrinth:farmers-delight:$it")
    }

    // Fruits Delight, on the node that sets it. Nothing compiles against it: its drinks are reached by
    // registry id, and src/main/fruitsdelight's mixins name their targets by string. Only here to test
    // them. Its L2 libraries are nested in its jar, which NeoForge loads itself.
    // Off by default: the L2 libraries and fruit trees slow every client start. Uncomment the line below
    // only to work on it.
    findProperty("deps.fruits_delight")?.let {
        // runClientMod(listOf("fruits-delight", "fruitsdelight", "farmers-delight", "farmersdelight"), "maven.modrinth:fruits-delight:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=fruits-delight` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("fruits-delight", "fruitsdelight", "farmers-delight", "farmersdelight"))
    }

    // Youkais' Feasts, on the node that sets it, the same way: its drinks are reached by registry id, and
    // src/main/youkaisfeasts's mixins name their targets by string. Its L2 libraries, Registrate and
    // TerraBlender are nested in its jar. Off by default, as Fruits Delight is, for its libraries and its
    // biome; uncomment the line below only to work on it.
    findProperty("deps.youkais_feasts")?.let {
        // runClientMod(listOf("youkais-feasts", "youkaisfeasts", "farmers-delight", "farmersdelight"), "maven.modrinth:gensokyo-delight-youkais-feasts:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=youkais-feasts` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("youkais-feasts", "youkaisfeasts", "farmers-delight", "farmersdelight"))
    }

    // Expanded Delight, on the node that sets it. Nothing compiles against it: its foods are reached by
    // registry id, and src/main/expandeddelight's mixin names its target by string. Only here to test it.
    findProperty("deps.expanded_delight")?.let {
        runClientMod(listOf("expanded-delight", "expandeddelight", "farmers-delight", "farmersdelight"), "maven.modrinth:expanded-delight:$it") { isTransitive = false }
    }

    // Miner's Delight, on the node that sets it. src/main/minersdelight's mixin names its copper cup, so
    // it compiles against it. Lodestone, which it requires, is on runClient only.
    findProperty("deps.miners_delight")?.let {
        compileOnly("maven.modrinth:miners-delight:$it") { isTransitive = false }
        val names = listOf("miners-delight", "minersdelight", "lodestone", "lodestonelib", "farmers-delight", "farmersdelight")
        runClientMod(names, "maven.modrinth:miners-delight:$it") { isTransitive = false }
        runClientMod(names, "maven.modrinth:lodestonelib:${property("deps.lodestone")}") { isTransitive = false }
    }

    // Hearth and Harvest, on the node that sets it. src/main/hearthandharvest's mixins name its tank,
    // sink, jug, sprinkler and cask classes, so they compile against it.
    findProperty("deps.hearth_and_harvest")?.let {
        compileOnly("maven.modrinth:hearth-and-harvest:$it") { isTransitive = false }
        // From 1.4.0 its cask extends Farmer's Delight's SyncedBlockEntity, which javac has to see.
        compileOnly("maven.modrinth:farmers-delight:${property("deps.farmersdelight")}") { isTransitive = false }
        runClientMod(listOf("hearth-and-harvest", "hearthandharvest", "farmers-delight", "farmersdelight"),
            "maven.modrinth:hearth-and-harvest:$it") { isTransitive = false }
    }

    // Extra Delight, on the node that sets it. src/main/extradelight's mixins name its bottle registry,
    // tanks and recipes, so they compile against it.
    findProperty("deps.extra_delight")?.let {
        compileOnly("maven.modrinth:extradelight:$it") { isTransitive = false }
        runClientMod(listOf("extradelight", "farmers-delight", "farmersdelight"),
            "maven.modrinth:extradelight:$it") { isTransitive = false }
    }

    // No Man's Land, on the node that sets it. src/main/nomansland's mixin names its milk cauldron, so it
    // compiles against it. NeoForge loads the Biolith and Mixed Litter nested in its jar itself. Off by
    // default: it rewrites the whole Overworld, which slows every client start and needs a new world.
    // Uncomment the line below only to work on it.
    findProperty("deps.nomansland")?.let {
        compileOnly("maven.modrinth:no-mans-land:$it") { isTransitive = false }
        // runClientMod(listOf("no-mans-land", "nomansland"), "maven.modrinth:no-mans-land:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=nomansland` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("no-mans-land", "nomansland"))
    }

    // Mekanism, on the node that sets it. src/main/mekanism's mixins name its Electric Pump and rotary
    // recipe, so they compile against it. Off in runClient by default: it is large and adds ores to every
    // new world. Uncomment the line below only to work on it.
    findProperty("deps.mekanism")?.let {
        compileOnly("maven.modrinth:mekanism:$it") { isTransitive = false }
        // runClientMod(listOf("mekanism"), "maven.modrinth:mekanism:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=mekanism` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("mekanism"))
    }

    // Immersive Engineering, on the node that sets it. src/main/immersiveengineering's mixins name its Fluid
    // Pump, Bottling Machine and item fluid handler, so they compile against it. Off in runClient by
    // default: it adds ores and villages to every new world. Uncomment the line below only to work on it.
    findProperty("deps.immersive_engineering")?.let {
        compileOnly("maven.modrinth:immersiveengineering:$it") { isTransitive = false }
        // runClientMod(listOf("immersiveengineering"), "maven.modrinth:immersiveengineering:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=immersiveengineering` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("immersiveengineering"))
    }

    // Little Logistics, on the node that sets it. src/main/littlelogistics's mixins name its Fluid Tank Car
    // and Fluid Barge, so they compile against it. Off in runClient by default, like the other mods worked
    // on one at a time; its Ponder and Flywheel are nested in its jar. Uncomment the line below to work on it.
    findProperty("deps.little_logistics")?.let {
        compileOnly("maven.modrinth:little-logistics:$it") { isTransitive = false }
        // runClientMod(listOf("littlelogistics", "little-logistics"), "maven.modrinth:little-logistics:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=littlelogistics` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("littlelogistics", "little-logistics"))
    }

    if (createVersion != null && createLibraries != null) {
        compileOnly("maven.modrinth:create:$createVersion") { isTransitive = false }
        compileOnly(files(createLibraries.map { it.destinationDir.listFiles().orEmpty().toList() })
            .builtBy(createLibraries))
        // Off in runClient by default: Create is the heaviest mod here and slows every client start.
        // Uncomment the line below only to test the Sand Filter with pipes, pumps and spouts.
        // runClientMod(listOf("create"), "maven.modrinth:create:$createVersion") { isTransitive = false }
        // Keeps `-PwithoutOptional=create` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("create"))
        // Create Pipes n Physics, a Create addon whose pipe engine the Sand Filter's tag and the intake mixin
        // are for; nothing compiles against it. Pinned by Modrinth version id, bumped by hand. Needs Create's
        // runClientMod line too. 3.2.1 crashes a client without Sable: set fluidTiltEnabled = false in
        // run/<node>/config/pipesnphysics-client.toml. See docs/dev/integration/storage/PIPES-N-PHYSICS-INTEGRATION.md.
        // runClientMod(listOf("pipesnphysics", "create-pipes-n-physics"), "maven.modrinth:create-pipes-n-physics:kH90vhqN") { isTransitive = false }
        optionalRunMods.include(listOf("pipesnphysics", "create-pipes-n-physics"))
    }

    if (sophisticatedCoreVersion != null) {
        compileOnly("maven.modrinth:sophisticated-core:$sophisticatedCoreVersion") { isTransitive = false }
        // Test the upgrades in runClient, inside a backpack.
        runClientMod(listOf("sophisticated-core", "sophisticatedcore"),
            "maven.modrinth:sophisticated-core:$sophisticatedCoreVersion") { isTransitive = false }
        findProperty("deps.sophisticated_backpacks")?.let {
            runClientMod(listOf("sophisticated-backpacks", "sophisticatedbackpacks", "sophisticated-core", "sophisticatedcore"),
                "maven.modrinth:sophisticated-backpacks:$it") { isTransitive = false }
        }
        // Off by default: its many block variants slow every client start, and the backpack is enough to
        // test the upgrades. Uncomment the lines below only to test them in a chest or barrel.
        findProperty("deps.sophisticated_storage")?.let {
            // runClientMod(listOf("sophisticated-storage", "sophisticatedstorage", "sophisticated-core", "sophisticatedcore"),
            //     "maven.modrinth:sophisticated-storage:$it") { isTransitive = false }
            // Keeps its `-PwithoutOptional` name known while the lines above are off.
            optionalRunMods.include(listOf("sophisticated-storage", "sophisticatedstorage", "sophisticated-core", "sophisticatedcore"))
        }
    }

    if (supplementariesVersion != null) {
        // Moonlight carries the soft fluid system three of the mixins target; Supplementaries itself only
        // the faucet's cauldron behaviour.
        compileOnly("maven.modrinth:supplementaries:$supplementariesVersion") { isTransitive = false }
        compileOnly("maven.modrinth:moonlight:${property("deps.moonlight")}") { isTransitive = false }
        // Off in runClient by default, with Spelunkery and Moonlight below, for the reason build.gradle.kts
        // gives. Uncomment the line below, and Moonlight's, only to test jars, goblets and faucets. The
        // gametests and runServer run without them, which is what proves the mod is unchanged when they
        // are absent.
        // runClientMod(listOf("supplementaries", "moonlight"), "maven.modrinth:supplementaries:$supplementariesVersion") { isTransitive = false }
        // Keeps `-PwithoutOptional=supplementaries` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("supplementaries", "moonlight"))
    }

    if (spelunkeryVersion != null) {
        // Its salt bucket recipe in runClient only. The mixin targets Minecraft's cooking recipe and names
        // nothing of the mod, so nothing is compiled against it. Off by default, see Supplementaries.
        // runClientMod(listOf("spelunkery", "moonlight"), "maven.modrinth:spelunkery:$spelunkeryVersion") { isTransitive = false }
        optionalRunMods.include(listOf("spelunkery", "moonlight"))
    }

    if (bountifulFaresVersion != null) {
        compileOnly("maven.modrinth:bountiful-fares:$bountifulFaresVersion") { isTransitive = false }
        // In runClient only, with NexusLib; off by default, for the reason build.gradle.kts gives.
        val names = listOf("bountiful-fares", "bountifulfares", "nexuslib")
        // runClientMod(names, "maven.modrinth:bountiful-fares:$bountifulFaresVersion") { isTransitive = false }
        // runClientMod(names, "maven.modrinth:nexuslib:${property("deps.nexuslib")}") { isTransitive = false }
        optionalRunMods.include(names)
    }

    // Moonlight Lib, which Supplementaries and Spelunkery both require, once for both. Leaving it out
    // leaves them out. Off by default with them; uncomment along with either.
    if (supplementariesVersion != null || spelunkeryVersion != null) {
        // runClientMod(listOf("moonlight"), "maven.modrinth:moonlight:${property("deps.moonlight")}") { isTransitive = false }
        optionalRunMods.include(listOf("moonlight"))
    }

    if (kaleidoscopeCookeryVersion != null) {
        compileOnly("maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookeryVersion") { isTransitive = false }
        // Test the stockpot and the teapot in runClient. The gametests and runServer run without it, which
        // is what proves the mod is unchanged when it is absent.
        runClientMod(listOf("kaleidoscope-cookery", "kaleidoscope-cookery-refabricated", "kaleidoscope_cookery"),
            "maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookeryVersion") { isTransitive = false }
    }

    if (kaleidoscopeTavernVersion != null) {
        compileOnly("maven.modrinth:kaleidoscopetavern:$kaleidoscopeTavernVersion") { isTransitive = false }
        // Test the barrel, the tap, the placed bottle and the shaker in runClient. The gametests and
        // runServer run without it, which is what proves the mod is unchanged when it is absent.
        runClientMod(listOf("kaleidoscope-tavern", "kaleidoscope-tavern-refabricated", "kaleidoscope_tavern"),
            "maven.modrinth:kaleidoscopetavern:$kaleidoscopeTavernVersion") { isTransitive = false }
        // Kaleidoscope Chinese Food, whose tap fills Kaleidoscope Cookery's stockpot and teapot; nothing
        // compiles against it. Off by default: uncomment to work on that part of src/main/kaleidoscope.
        findProperty("deps.kaleidoscope_chinese_food")?.let {
            // runClientMod(listOf("kaleidoscope-chinese-food", "kaleidoscope_chinesefood", "kaleidoscope-tavern",
            //     "kaleidoscope_tavern", "kaleidoscope-cookery", "kaleidoscope_cookery"),
            //     "maven.modrinth:kaleidoscopechinesefood:$it") { isTransitive = false }
            optionalRunMods.include(listOf("kaleidoscope-chinese-food", "kaleidoscope_chinesefood"))
        }
    }

    if (brewinAndChewinVersion != null) {
        compileOnly("maven.modrinth:brewin-and-chewin:$brewinAndChewinVersion") { isTransitive = false }
        // Test the keg in runClient. The gametests and runServer run without it, which is what proves the
        // mod is unchanged when it is absent. NeoForge loads the Greenhouse Config nested in its jar;
        // Farmer's Delight is already above, and leaving it out leaves this out.
        runClientMod(listOf("brewin-and-chewin", "brewinandchewin", "farmers-delight", "farmersdelight"),
            "maven.modrinth:brewin-and-chewin:$brewinAndChewinVersion") { isTransitive = false }
    }

    if (farmAndCharmVersion != null) {
        compileOnly("maven.modrinth:lets-do-farm-charm:$farmAndCharmVersion") { isTransitive = false }
        // From 1.1.28 its blocks extend Foundation's, and its kitchen sink is Foundation's, a library
        // nested in its jar.
        compileOnly(files(provider {
            val jar = configurations.detachedConfiguration(
                dependencies.create("maven.modrinth:lets-do-farm-charm:$farmAndCharmVersion")
            ).apply { isTransitive = false }.singleFile
            zipTree(jar).matching { include("META-INF/jars/foundation-*.jar") }.files
        }))
        // Test the well, the trough, the Cooking Pot and Candlelight's kitchen sinks in runClient. The
        // gametests and runServer run without them, which is what proves the mod is unchanged when they are
        // absent. Both require Architectury API, added below. Leaving Farm & Charm out leaves Candlelight
        // out too; leaving Candlelight out keeps Farm & Charm. Off by default: its crops and world
        // generation slow every client start. Uncomment the two lines below only to work on them.
        val names = listOf("farm-and-charm", "farm_and_charm", "lets-do-farm-charm") + architecturyNames
        // runClientMod(names, "maven.modrinth:lets-do-farm-charm:$farmAndCharmVersion") { isTransitive = false }
        // runClientMod(names + listOf("candlelight"),
        //     "maven.modrinth:lets-do-candlelight-farmcharm-compat:${property("deps.candlelight")}") { isTransitive = false }
        // Keeps their `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names + listOf("candlelight"))
    }

    if (herbalBrewsVersion != null) {
        compileOnly("maven.modrinth:lets-do-herbalbrews:$herbalBrewsVersion") { isTransitive = false }
        // The Tea Kettle and the Jug in runClient only.
        runClientMod(listOf("herbalbrews", "lets-do-herbalbrews") + architecturyNames,
            "maven.modrinth:lets-do-herbalbrews:$herbalBrewsVersion") { isTransitive = false }
    }

    if (beachpartyVersion != null) {
        compileOnly("maven.modrinth:lets-do-beachparty:$beachpartyVersion") { isTransitive = false }
        // The cocktails in runClient only. Its manifest asks for nothing but Architectury, yet its items
        // implement Curios' ICurioItem, so a game without Curios fails to load. Leaving Curios out leaves
        // Beachparty out. Off by default: its world generation and Curios slow every client start.
        // Uncomment the two lines below only to work on it.
        val names = listOf("beachparty", "lets-do-beachparty", "curios") + architecturyNames
        // runClientMod(names, "maven.modrinth:lets-do-beachparty:$beachpartyVersion") { isTransitive = false }
        // runClientMod(names, "maven.modrinth:curios:${property("deps.curios")}") { isTransitive = false }
        // Keeps their `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names)
    }

    if (meadowVersion != null) {
        compileOnly("maven.modrinth:lets-do-meadow:$meadowVersion") { isTransitive = false }
        // The wooden bucket, the wooden cauldron and the Cooking Pot in runClient only. Its Foundation is
        // nested in its jar. Off by default: its biomes and world generation slow every client start.
        // Uncomment the line below only to work on it.
        val names = listOf("meadow", "lets-do-meadow") + architecturyNames
        // runClientMod(names, "maven.modrinth:lets-do-meadow:$meadowVersion") { isTransitive = false }
        // Keeps its `-PwithoutOptional` names known while the line above is off.
        optionalRunMods.include(names)
    }

    if (vineryVersion != null) {
        // Nothing of Vinery's is compiled against: its juices and wines are ThirstConfig ids. On
        // runClient only, to drink them. Off by default: its trees, grapevines and wandering winemaker
        // slow every client start. Uncomment the line below only to work on it.
        val names = listOf("vinery", "lets-do-vinery") + architecturyNames
        // runClientMod(names, "maven.modrinth:lets-do-vinery:$vineryVersion") { isTransitive = false }
        // Keeps its `-PwithoutOptional` names known while the line above is off.
        optionalRunMods.include(names)
    }

    if (croptopiaVersion != null) {
        // Nothing of Croptopia's is compiled against, as on Fabric; see build.gradle.kts. Off by default:
        // uncomment the two lines below only to work on it.
        val names = listOf("croptopia", "epherolib")
        // runClientMod(names, "curse.maven:croptopia-415438:$croptopiaVersion") { isTransitive = false }
        // runClientMod(names, "curse.maven:epherolib-885449:${property("deps.epherolib")}") { isTransitive = false }
        // Keeps its `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names)
    }

    // Architectury API, which every Let's Do mod requires, once for all of them. Each of them lists its
    // names, so leaving it out leaves them out; leaving one of them out keeps it for the others.
    if (farmAndCharmVersion != null || herbalBrewsVersion != null || beachpartyVersion != null
        || vineryVersion != null || meadowVersion != null) {
        runClientMod(architecturyNames,
            "maven.modrinth:architectury-api:${property("deps.architectury")}") { isTransitive = false }
    }

    if (coldSweatVersion != null) {
        compileOnly("maven.modrinth:cold-sweat:$coldSweatVersion") { isTransitive = false }
        // Off in runClient by default: its temperature gauge, HUD icon and world changes get into every
        // other test and screenshot. Uncomment the line below only to work on the Cold Sweat integration
        // (the climate, the waterskin, the Boiler). The gametests and runServer run without it, which is
        // what proves the mod is unchanged when it is absent.
        // runClientMod(listOf("cold-sweat", "cold_sweat"), "maven.modrinth:cold-sweat:$coldSweatVersion") { isTransitive = false }
        // Keeps `-PwithoutOptional=cold-sweat` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("cold-sweat", "cold_sweat"))
    }

    if (culturalDelightsVersion != null) {
        compileOnly("maven.modrinth:cultural-delights:$culturalDelightsVersion") { isTransitive = false }
        // Test the vat in runClient. The gametests and runServer run without it, which is what proves the
        // mod is unchanged when it is absent. It requires Cook's Collection; Farmer's Delight is above, and
        // leaving it out leaves both out. Off by default: its trees, wild crops and Cook's Collection slow
        // every client start. Uncomment the lines below only to work on it.
        // runClientMod(listOf("cultural-delights", "culturaldelights", "cooks-collection", "cookscollection",
        //     "farmers-delight", "farmersdelight"),
        //     "maven.modrinth:cultural-delights:$culturalDelightsVersion") { isTransitive = false }
        // runClientMod(listOf("cooks-collection", "cookscollection", "cultural-delights", "culturaldelights", "farmers-delight", "farmersdelight"),
        //     "maven.modrinth:cooks-collection:${property("deps.cooks_collection")}") { isTransitive = false }
        // Keeps their `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(listOf("cultural-delights", "culturaldelights", "cooks-collection", "cookscollection",
            "farmers-delight", "farmersdelight"))
    }

    if (sereneSeasonsVersion != null) {
        val glitchCoreVersion = property("deps.glitchcore").toString()
        // GlitchCore is compiled against too: before 26.2 the dimension check reads Serene Seasons'
        // config, whose class extends one of GlitchCore's.
        compileOnly("maven.modrinth:serene-seasons:$sereneSeasonsVersion") { isTransitive = false }
        compileOnly("maven.modrinth:glitchcore:$glitchCoreVersion") { isTransitive = false }
        // Off in runClient, for the reason build.gradle.kts gives. Uncomment the two lines below only to
        // work on the Serene Seasons integration. The gametests and runServer run without it, which is
        // what proves the mod is unchanged when it is absent. NeoForge loads the Night Config nested in
        // their jars itself.
        val names = listOf("serene-seasons", "sereneseasons")
        // runClientMod(names, "maven.modrinth:serene-seasons:$sereneSeasonsVersion") { isTransitive = false }
        // runClientMod(names + listOf("glitchcore"), "maven.modrinth:glitchcore:$glitchCoreVersion") { isTransitive = false }
        // Keeps `-PwithoutOptional=serene-seasons` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(names + listOf("glitchcore"))
    }

    // A name no node loads is refused in stonecutter.gradle.kts, once every node has said what it takes.
    project.extra["thirst.optionalRunMods"] = optionalRunMods.offered
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to mcRange,
        "neoforge" to neoForgeVersion,
        "java" to requiredJava.majorVersion,
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

// The toolchain, the seam checks, the jar excludes and `buildAndCollect` are shared with the Fabric
// nodes; the Java version is handed over above. And the integration table, as far as the seam checks
// need it: a script applied with `apply(from)` cannot see the classes of build-logic, so it cannot read
// the table itself.
extra["thirst.integrations"] = integrations.map { it.dir }
extra["thirst.loaderIndependentIntegrations"] = integrations.filter { it.loaderIndependent }.map { it.dir }
apply(from = rootProject.file("gradle/shared.gradle.kts"))

// What this node hands `buildAndCollect`, which gradle/shared.gradle.kts registers: ModDevGradle
// does not remap, so the plain jar is the one that ships.
tasks.named<Copy>("buildAndCollect") {
    from(tasks.jar.flatMap { it.archiveFile }, tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
}
