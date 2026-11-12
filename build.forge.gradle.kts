import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.OptionalRunMods
import com.thirstwastaken2.buildlogic.flightRecorder
import com.thirstwastaken2.buildlogic.forgeJson
import com.thirstwastaken2.buildlogic.forgeMixinConfigLine
import com.thirstwastaken2.buildlogic.integrations
import com.thirstwastaken2.buildlogic.integrationsFor
import java.util.zip.ZipFile

plugins {
    // ModDevGradle's MinecraftForge flavour. Its version is on the plugin classpath from
    // settings.gradle.kts, which resolves it without applying it.
    id("net.neoforged.moddev.legacyforge")
    // The integration table and the rest of what the loader scripts share. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
    `maven-publish`
}

/*
 * The MinecraftForge nodes, `<version>-forge`: today only `1.20.1-forge`. The same sources as the Fabric
 * node of that version. What it shares with build.neoforge.gradle.kts (source sets, mods and runs, the
 * resource translation) is in gradle/moddev.gradle.kts. What is here is what Forge 1.20.1 does
 * differently: the game runs under SRG names outside development, so the jar that ships is the remapped
 * `reobfJar`, mod dependencies are remapped the other way through the `mod*` configurations, and the
 * mixin configs are named in the jar manifest and on the run command lines rather than in mods.toml. See
 * docs/dev/VERSION-DIFFERENCES.md.
 */

val modId = property("mod.id") as String
/** Published artifact name; deliberately not the lowercase mod id. */
val modName = property("mod.name") as String
/** Minecraft range written into mods.toml, in Maven range syntax, e.g. `[1.20.1,1.20.2)`. */
val mcRange = property("mod.mc_compat") as String
val forgeVersion = property("deps.forge") as String

// The loader goes in the version so the jar never collides with the Fabric jar of the same Minecraft
// version in build/libs/.
version = "${property("mod.version")}+${sc.current.version}-forge"
base.archivesName = modName

/** Forge 47 runs on Java 17, as 1.20.1 does everywhere. */
val requiredJava: JavaVersion = JavaVersion.VERSION_17

repositories {
    // ModDevGradle supplies the Forge, Mojang and Minecraft libraries repositories.
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    // The libraries Create bundles, which the Sand Filter compiles against; see createLibraries.
    exclusiveContent {
        forRepository { maven("https://maven.createmod.net") { name = "Create" } }
        filter { includeGroup("dev.engine-room.flywheel"); includeGroup("net.createmod.ponder") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.tterrag.com") { name = "tterrag" } }
        filter { includeGroup("com.tterrag.registrate") }
    }
    // CurseMaven, for the mods published on CurseForge alone (Croptopia and its EpheroLib), by file id.
    exclusiveContent {
        forRepository { maven("https://cursemaven.com") { name = "CurseMaven" } }
        filter { includeGroup("curse.maven") }
    }
}

/** See build.neoforge.gradle.kts: the loader this node builds for, by its source directory name. */
val loader = "forge"

/** This node's copy of `src/client`; see build.neoforge.gradle.kts. */
val clientSources: File =
    if (sc.current.isActive) rootProject.file("src/client")
    else layout.buildDirectory.dir("generated/stonecutter/client").get().asFile

/*
 * The optional integrations this node builds, from the same table build.neoforge.gradle.kts reads: each
 * row's source directories, its optional dependencies appended to the built mods.toml, and its mixin
 * config, which Forge 47 reads from the jar manifest rather than mods.toml (see `mixin` below). Forge 47
 * has one fluid API, so a row split by fluid API generation takes its `fluidhandler` half.
 */
val nodeIntegrations = integrationsFor(Loader.FORGE) { findProperty(it) != null }

legacyForge {
    version = "${sc.current.version}-$forgeVersion"
    // Opens what Fabric API's transitive access wideners open on the Fabric node. The same file ships in
    // the jar, where Forge applies it for players.
    accessTransformers.from(rootProject.file("src/main/$loader/resources/META-INF/accesstransformer.cfg"))
}

// What gradle/moddev.gradle.kts reads from build-logic and Stonecutter; the table at its top says what
// each one is.
extra["thirst.loader"] = loader
extra["thirst.loaderName"] = "Forge"
extra["thirst.manifest"] = "META-INF/mods.toml"
extra["thirst.minecraft"] = sc.current.version
extra["thirst.clientSources"] = clientSources
extra["thirst.requiredJava"] = requiredJava.majorVersion
// 1.20.1 has no --report option, so the harness installs the JUnit reporter itself.
extra["thirst.gametestReportOption"] = false
extra["thirst.flightRecorder"] = flightRecorder(rootProject.file("run/${project.name}/benchmark/latest.jfr"))
extra["thirst.integrationManifest"] = nodeIntegrations.joinToString("") { it.forgeManifest(modId) }
extra["thirst.integrationInputs"] = integrations.filter { Loader.FORGE in it.loaders }
    .associate { it.dir to (findProperty(it.depsKey)?.toString() ?: "") }
extra["thirst.translateJson"] = { json: Any?, file: String -> forgeJson(json, file) }
// The dev tools' mixins, only where the dev tools are loaded. The mod's own configs reach every run
// through `mixin` below.
extra["thirst.devRunProgramArguments"] = listOf("--mixin.config", "thirstwastaken2.dev.mixins.json")
apply(from = rootProject.file("gradle/moddev.gradle.kts"))

sourceSets.main {
    // Forge 47 has one fluid API, IFluidHandler through capabilities.
    java.srcDir("src/main/$loader-fluidhandler/java")
    nodeIntegrations.forEach { integration ->
        integration.mainRoots(transferApi = false).forEach { root ->
            java.srcDir("$root/java")
            resources.srcDir("$root/resources")
        }
        integration.clientJava?.let { dir ->
            java.srcDir(files(clientSources.resolve(dir)).builtBy("stonecutterGenerateClient"))
        }
    }
}

/**
 * The libraries Create bundles in its jar (Ponder with Catnip, Flywheel, Registrate), as the Maven
 * coordinates its `META-INF/jarjar/metadata.json` names, so a new Create brings its own. They are in SRG
 * names like Create, and a remapping configuration takes only modules, so they are compiled against
 * from their own Mavens rather than copied out of the jar as on NeoForge. MixinExtras is left out: this
 * mod nests its own.
 */
val createLibraries: List<String> = (findProperty("deps.create") as String?)?.let { version ->
    val jar = configurations.detachedConfiguration(dependencies.create("maven.modrinth:create:$version"))
        .apply { isTransitive = false }.singleFile
    ZipFile(jar).use { zip ->
        val metadata = zip.getInputStream(zip.getEntry("META-INF/jarjar/metadata.json")).reader().readText()
        @Suppress("UNCHECKED_CAST")
        val jars = (groovy.json.JsonSlurper().parseText(metadata) as Map<String, Any?>)["jars"] as List<Map<String, Map<String, String>>>
        jars.map { "${it.getValue("identifier")["group"]}:${it.getValue("identifier")["artifact"]}:${it.getValue("version")["artifactVersion"]}" }
            .filterNot { it.startsWith("io.github.llamalad7:") }
    }
}.orEmpty()

/*
 * The optional mods runClient loads, which gradle/moddev.gradle.kts puts on the clients' runs alone. They
 * are Forge mods published under SRG names, so they are declared on a remapping configuration, the way
 * `modImplementation` is, whose jars in Mojang's names `clientRunMods` resolves to.
 */
val modClientRunMods: Configuration = obfuscation.createRemappingConfiguration(configurations["clientRunMods"])

/** `-PwithoutOptional=<name,...>`; see build-logic's OptionalRunMods. */
val optionalRunMods = OptionalRunMods(providers.gradleProperty("withoutOptional").orNull)

/** Adds a mod to the clients' run only, unless `-PwithoutOptional` names it or one of the libraries it lists. */
fun runClientMod(names: List<String>, notation: String, configure: ExternalModuleDependency.() -> Unit = {}) {
    if (optionalRunMods.include(names)) dependencies.add(modClientRunMods.name, notation, configure)
}

/*
 * The mixin configs: named in the jar manifest for players and passed to every run in development. The
 * refmap is what maps each injection's Mojang names to the SRG names the game runs under outside
 * development; the annotation processor writes it while compiling `main`.
 */
val refmap = "$modId.refmap.json"

// A mixin that names its target by string does so because the mod is optional and nothing compiles
// against it (Fruits Delight and the L2 Library nested in it). Forge's annotation processor, unlike Loom's,
// fails the build when such a target is not on the classpath. A note rather than Fabric's warning: the
// target is missing on purpose, and on CI every warning becomes an annotation that buries a real one.
// A public target named by string is the same choice, made to keep the mod's class off the load path.
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-AMSG_MIXIN_SOFT_TARGET_NOT_FOUND=note")
    options.compilerArgs.add("-AMSG_MIXIN_SOFT_TARGET_IS_PUBLIC=note")
}

// Forge 47 marks for removal what 1.21 replaced: ResourceLocation's constructors,
// FMLJavaModLoadingContext.get(), ItemBlockRenderTypes.setRenderLayer. They are the 1.20.1 API the
// Fabric 1.20.1 node shares and nothing removes them on this Minecraft version, so the forty warnings
// they gave each build said nothing.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-removal")
}

mixin {
    add(sourceSets.main.get(), refmap)
    config("$modId.mixins.json")
    config("$modId.client.mixins.json")
    nodeIntegrations.mapNotNull { it.mixinConfig }.forEach { config(it) }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    // MixinExtras, which the core mixins use. Fabric Loader and NeoForge ship it; Forge 47 does not, so
    // the jar carries it nested, and Forge loads the newest copy any mod nests.
    val mixinExtras = property("deps.mixinextras") as String
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtras")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:$mixinExtras")!!)

    // Optional integrations the client code compiles against; see build.neoforge.gradle.kts. Mod Menu is
    // Fabric only, and Forge's own config screen factory is used instead.
    modCompileOnly("maven.modrinth:appleskin:${property("deps.appleskin")}")
    modCompileOnly("maven.modrinth:jade:${property("deps.jade")}")

    runClientMod(listOf("appleskin"), "maven.modrinth:appleskin:${property("deps.appleskin")}")
    runClientMod(listOf("jade"), "maven.modrinth:jade:${property("deps.jade")}")
    findProperty("deps.cloth_config")?.let {
        runClientMod(listOf("cloth-config", "cloth_config"), "maven.modrinth:cloth-config:$it")
    }
    findProperty("deps.create")?.let { create ->
        // Create and the libraries it bundles (Ponder with Catnip, Flywheel, Registrate) are all in SRG
        // names, so all are remapped; the Sand Filter extends classes whose supertypes live in them. At
        // runtime FML reads the libraries out of Create's jar itself. See src/main/createforge/AGENTS.md.
        modCompileOnly("maven.modrinth:create:$create") { isTransitive = false }
        createLibraries.forEach { modCompileOnly(it) { isTransitive = false } }
        // Off in runClient by default, for the reason build.neoforge.gradle.kts gives. Uncomment the line
        // below only to test the Sand Filter.
        // runClientMod(listOf("create"), "maven.modrinth:create:$create") { isTransitive = false }
        // Keeps `-PwithoutOptional=create` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("create"))
    }
    findProperty("deps.serene_seasons")?.let { sereneSeasons ->
        val glitchCore = property("deps.glitchcore").toString()
        // Read through its API alone, but its Forge jar is in SRG names, so it is remapped like the other
        // mods. GlitchCore is compiled against too: the dimension check reads Serene Seasons' config,
        // whose class extends GlitchCore's. Forge ships the Night Config under that.
        modCompileOnly("maven.modrinth:serene-seasons:$sereneSeasons") { isTransitive = false }
        modCompileOnly("maven.modrinth:glitchcore:$glitchCore") { isTransitive = false }
        // Off in runClient, for the reason build.gradle.kts gives. Uncomment the two lines below only to
        // work on the Serene Seasons integration.
        val names = listOf("serene-seasons", "sereneseasons")
        // runClientMod(names, "maven.modrinth:serene-seasons:$sereneSeasons") { isTransitive = false }
        // runClientMod(names + listOf("glitchcore"), "maven.modrinth:glitchcore:$glitchCore") { isTransitive = false }
        // Keeps `-PwithoutOptional=serene-seasons` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(names + listOf("glitchcore"))
    }
    findProperty("deps.kaleidoscope_cookery")?.let { kaleidoscopeCookery ->
        // Mixed into, and in SRG names, so remapped. See src/main/kaleidoscope/AGENTS.md.
        modCompileOnly("maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookery") { isTransitive = false }
        runClientMod(listOf("kaleidoscope-cookery", "kaleidoscope-cookery-refabricated", "kaleidoscope_cookery"),
            "maven.modrinth:kaleidoscope-cookery:$kaleidoscopeCookery") { isTransitive = false }
    }
    findProperty("deps.kaleidoscope_tavern")?.let { kaleidoscopeTavern ->
        // Mixed into, and in SRG names, so remapped. See src/main/kaleidoscopetavern/AGENTS.md.
        modCompileOnly("maven.modrinth:kaleidoscopetavern:$kaleidoscopeTavern") { isTransitive = false }
        runClientMod(listOf("kaleidoscope-tavern", "kaleidoscope-tavern-refabricated", "kaleidoscope_tavern"),
            "maven.modrinth:kaleidoscopetavern:$kaleidoscopeTavern") { isTransitive = false }
        // Kaleidoscope Chinese Food, as in build.neoforge.gradle.kts: off by default.
        findProperty("deps.kaleidoscope_chinese_food")?.let {
            // runClientMod(listOf("kaleidoscope-chinese-food", "kaleidoscope_chinesefood", "kaleidoscope-tavern",
            //     "kaleidoscope_tavern", "kaleidoscope-cookery", "kaleidoscope_cookery"),
            //     "maven.modrinth:kaleidoscopechinesefood:$it") { isTransitive = false }
            optionalRunMods.include(listOf("kaleidoscope-chinese-food", "kaleidoscope_chinesefood"))
        }
    }
    // Fruits Delight: nothing compiles against it, its mixins name their targets by string. Only here to
    // test them. Its L2 libraries are nested in its jar, which Forge loads itself. Off in runClient by
    // default, for the reason build.neoforge.gradle.kts gives; uncomment the lines below to work on it.
    findProperty("deps.fruits_delight")?.let {
        // runClientMod(listOf("fruits-delight", "fruitsdelight", "farmers-delight", "farmersdelight"),
        //     "maven.modrinth:fruits-delight:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=fruits-delight` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(listOf("fruits-delight", "fruitsdelight", "farmers-delight", "farmersdelight"))
    }
    // Youkais' Feasts: nothing compiles against it, its mixins name their targets by string. Its L2
    // libraries, Registrate, TerraBlender and MixinExtras are nested in its jar. Off in runClient by
    // default, for the reason build.neoforge.gradle.kts gives; uncomment the lines below to work on it.
    findProperty("deps.youkais_feasts")?.let {
        // runClientMod(listOf("youkais-feasts", "youkaisfeasts", "farmers-delight", "farmersdelight"),
        //     "maven.modrinth:gensokyo-delight-youkais-feasts:$it") { isTransitive = false }
        // Keeps `-PwithoutOptional=youkais-feasts` in the agent scripts a known name while the lines above are off.
        optionalRunMods.include(listOf("youkais-feasts", "youkaisfeasts", "farmers-delight", "farmersdelight"))
    }
    // Hearth and Harvest: from 1.4.1 its 1.20.1 build is the 1.21.1 mod ported back, whose tanks are
    // mixed into, in SRG names, so remapped. Its Keg extends a Farmer's Delight class, so that is
    // compiled against too. See src/main/hearthandharvestforge/AGENTS.md.
    findProperty("deps.hearth_and_harvest")?.let {
        modCompileOnly("maven.modrinth:hearth-and-harvest:$it") { isTransitive = false }
        modCompileOnly("maven.modrinth:farmers-delight:${property("deps.farmersdelight")}") { isTransitive = false }
        runClientMod(listOf("hearth-and-harvest", "hearthandharvest", "farmers-delight", "farmersdelight"),
            "maven.modrinth:hearth-and-harvest:$it") { isTransitive = false }
    }
    findProperty("deps.miners_delight")?.let { minersDelight ->
        // Mixed into, and in SRG names, so remapped. See src/main/minersdelight/AGENTS.md.
        modCompileOnly("maven.modrinth:miners-delight:$minersDelight") { isTransitive = false }
        runClientMod(listOf("miners-delight", "miners_delight", "farmers-delight", "farmersdelight"),
            "maven.modrinth:miners-delight:$minersDelight") { isTransitive = false }
    }
    findProperty("deps.cold_sweat")?.let { coldSweat ->
        // Mixed into and read through its API, in SRG names, so remapped. See src/main/coldsweatforge/AGENTS.md.
        modCompileOnly("maven.modrinth:cold-sweat:$coldSweat") { isTransitive = false }
        // Off in runClient by default, for the reason build.neoforge.gradle.kts gives. Uncomment the line
        // below only to work on the Cold Sweat integration.
        // runClientMod(listOf("cold-sweat", "cold_sweat"), "maven.modrinth:cold-sweat:$coldSweat") { isTransitive = false }
        // Keeps `-PwithoutOptional=cold-sweat` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("cold-sweat", "cold_sweat"))
    }
    findProperty("deps.mekanism")?.let { mekanism ->
        // Mixed into, and in SRG names, so remapped. See src/main/mekanismforge/AGENTS.md. Off in runClient
        // by default, for the reason build.neoforge.gradle.kts gives; uncomment the line below to work on it.
        modCompileOnly("maven.modrinth:mekanism:$mekanism") { isTransitive = false }
        // runClientMod(listOf("mekanism"), "maven.modrinth:mekanism:$mekanism") { isTransitive = false }
        // Keeps `-PwithoutOptional=mekanism` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("mekanism"))
    }
    findProperty("deps.immersive_engineering")?.let { ie ->
        // Mixed into, and in SRG names, so remapped. See src/main/immersiveengineeringforge/AGENTS.md. Off in
        // runClient by default, for the reason build.neoforge.gradle.kts gives; uncomment the line below to work on it.
        modCompileOnly("maven.modrinth:immersiveengineering:$ie") { isTransitive = false }
        // runClientMod(listOf("immersiveengineering"), "maven.modrinth:immersiveengineering:$ie") { isTransitive = false }
        // Keeps `-PwithoutOptional=immersiveengineering` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("immersiveengineering"))
    }
    findProperty("deps.little_logistics")?.let { ll ->
        // Mixed into, and in SRG names, so remapped. See src/main/littlelogisticsforge/AGENTS.md. Off in
        // runClient by default, like the other mods worked on one at a time; uncomment the line below to work on it.
        modCompileOnly("maven.modrinth:little-logistics:$ll") { isTransitive = false }
        // runClientMod(listOf("littlelogistics", "little-logistics"), "maven.modrinth:little-logistics:$ll") { isTransitive = false }
        // Keeps `-PwithoutOptional=littlelogistics` in the agent scripts a known name while the line above is off.
        optionalRunMods.include(listOf("littlelogistics", "little-logistics"))
    }
    // Croptopia: nothing compiles against it, as on Fabric; see build.gradle.kts. In SRG names, so remapped.
    // Off in runClient by default; uncomment the two lines below only to work on it.
    findProperty("deps.croptopia")?.let {
        val names = listOf("croptopia", "epherolib")
        // runClientMod(names, "curse.maven:croptopia-415438:$it") { isTransitive = false }
        // runClientMod(names, "curse.maven:epherolib-885449:${property("deps.epherolib")}") { isTransitive = false }
        // Keeps its `-PwithoutOptional` names known while the lines above are off.
        optionalRunMods.include(names)
    }
    // Test the drinks and meals Farmer's Delight adds, and the Cooking Pot recipes. Reached by id only.
    findProperty("deps.farmersdelight")?.let {
        runClientMod(listOf("farmers-delight", "farmersdelight"), "maven.modrinth:farmers-delight:$it")
    }

    project.extra["thirst.optionalRunMods"] = optionalRunMods.offered
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to mcRange,
    )
    inputs.properties(props)
    inputs.property("refmap", refmap)
    filesMatching("META-INF/mods.toml") { expand(props) }
    // What a mixin config needs on Forge and the shared source configs leave out; see
    // build-logic's forgeMixinConfigLine.
    filesMatching("*.mixins.json") { filter { line -> forgeMixinConfigLine(line, refmap) } }
}

tasks.named<ProcessResources>("processDevResources") {
    filesMatching("*.mixins.json") { filter { line -> forgeMixinConfigLine(line, refmap = null) } }
}

// 1.21 renamed the data pack directories from plural to singular; the gametests' own data is written the
// newer way and renamed here, as build.gradle.kts does for the Fabric node.
tasks.named<ProcessResources>("processGametestResources") {
    eachFile { path = path.replace("/tags/item/", "/tags/items/") }
    includeEmptyDirs = false
}

// Forge 47's gametest server is the dedicated server, which makes its world from server.properties,
// not vanilla's GameTestServer with its flat world and no mob spawning. Left at the defaults it ran the
// tests in a random world, and that made them flaky: a mock player joins at the world spawn, scattered
// within ten blocks, so two players of one test could stand in different biomes, in swamp water or in
// the air. That failed five tests at random on CI, by a climate factor or a biome check, and generating
// 441 chunks of real terrain took 31 seconds before the first test. A flat plains world without
// structures or mobs is what every other node's runner tests in.
// gradle/shared.gradle.kts deletes the world before each run, so it is made anew with these.
tasks.named("runGametest") {
    val properties = rootProject.file("run/${project.name}/gametest/server.properties")
    doFirst {
        val wanted = mapOf(
            "level-type" to "minecraft\\:flat",
            // The default superflat, written out: the server logs an error for an empty one.
            "generator-settings" to "{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1}," +
                "{\"block\":\"minecraft:dirt\",\"height\":2},{\"block\":\"minecraft:grass_block\",\"height\":1}]," +
                "\"biome\":\"minecraft:plains\"}",
            "generate-structures" to "false",
            "spawn-monsters" to "false",
            "spawn-animals" to "false",
            "spawn-npcs" to "false",
        )
        val lines = if (properties.isFile) properties.readLines() else emptyList()
        val kept = lines.filter { line -> wanted.keys.none { line.startsWith("$it=") } }
        properties.parentFile.mkdirs()
        properties.writeText((kept + wanted.map { (k, v) -> "$k=$v" })
            .joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
    }
}

extra["thirst.integrations"] = integrations.map { it.dir }
extra["thirst.loaderIndependentIntegrations"] = integrations.filter { it.loaderIndependent }.map { it.dir }
apply(from = rootProject.file("gradle/shared.gradle.kts"))

tasks.jar {
    // Forge 47 finds a mod's mixin configs here and nowhere else in a jar; `mixin` above only reaches
    // the development runs.
    manifest.attributes("MixinConfigs" to mixin.configs.map { it.joinToString(",") })
}

// What this node hands `buildAndCollect`: the jar remapped to SRG names, which is the one players run.
// The plain `jar` keeps Mojang's names and only loads in development.
tasks.named<Copy>("buildAndCollect") {
    from(tasks.named<Jar>("reobfJar").flatMap { it.archiveFile }, tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
}
