/*
 * The tasks every node shares, whatever mod loader it builds for: the Java toolchain, the seam
 * checks, what never belongs in a jar, and the task that collects the jars.
 *
 * Applied by `build.gradle.kts` (the Fabric nodes), `build.neoforge.gradle.kts` and
 * `build.forge.gradle.kts`. Each of
 * those sets three extra properties first: `thirst.requiredJava`, because the Java version a node
 * needs follows from its Minecraft version and only the node's own script can read that, and
 * `thirst.integrations` and `thirst.loaderIndependentIntegrations`, from the integration table in
 * build-logic. A script applied with `apply(from)` cannot see build-logic's classes, so it is handed
 * what it needs as plain lists.
 *
 * Two things are deliberately not here:
 *
 * - **The source directory wiring.** Loom splits `main` and `client`; ModDevGradle has no split, so
 *   the NeoForge node compiles the client sources into `main`. The two scripts wire their source
 *   sets differently on purpose, as gradle/moddev.gradle.kts explains. Which directories an
 *   integration adds is the table's to say, in build-logic; each script makes the calls itself.
 * - **What `buildAndCollect` copies.** The jar a node ships is Loom's remapped jar on Fabric and the
 *   plain `jar` on NeoForge, so each script adds its own inputs to the task registered below.
 */

/** Set by the applying script, from the Minecraft version of its node. */
val requiredJava: JavaVersion =
    JavaVersion.toVersion(project.extensions.extraProperties.get("thirst.requiredJava") as String)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(requiredJava.majorVersion.toInt())
}

configure<JavaPluginExtension> {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor.set(JvmVendorSpec.ADOPTIUM)
        languageVersion.set(JavaLanguageVersion.of(requiredJava.majorVersion))
    }
}

// Per-directory notes for contributors and backup copies of edited textures; they live next to the
// files they describe, not in the jars.
tasks.withType<ProcessResources>().configureEach {
    exclude("**/AGENTS.md", "**/*.bak")
}

// The same for the sources jar, which reads the source directories themselves rather than the processed
// resources, plus `.cache`, datagen's hash cache beside the generated resources, which Loom keeps out of
// the Fabric mod jar and build.neoforge.gradle.kts out of the NeoForge one, but nothing out of this one.
tasks.withType<Jar>().matching { it.name.endsWith("sourcesJar") }.configureEach {
    exclude("**/AGENTS.md", "**/*.bak", "**/.cache/**")
}

/**
 * Prepares what `runBenchmark` writes into and works in: the directory the report and any flight
 * recording land in, and a world whose terrain is the same on every machine.
 *
 * The benchmark opens a world of its own, `-Pthirst.benchmark.world`, passed to the server as
 * `--world` by each node's own buildscript; only the seed it is generated with lives here, because
 * a seed is read from `server.properties` and nothing on the command line can set it.
 *
 * Only an empty or missing `level-seed` is filled in, so a seed someone put there by hand is left
 * alone, and only a world created afterwards is affected: the dev world `runServer` opens already
 * exists with a seed of its own inside its `level.dat`, and is untouched either way. The report's
 * `environment.levelSeed` says what a run actually measured, so a reader never has to trust this.
 */
// Not `prepareBenchmarkRun`: ModDevGradle already names a task of its own that, for the
// `benchmark` run it creates.
tasks.register("benchmarkRunDirectory") {
    description = "Makes the benchmark's output directory and fills in its seed when there is none"

    val seed = providers.gradleProperty("thirst.benchmark.seed").get()
    val properties = rootProject.file("run/${project.name}/server.properties")
    // JFR writes its recording as the JVM starts and does not create the directory for it.
    val output = rootProject.file("run/${project.name}/benchmark")
    // The run directory is shared with runServer and edited by the server itself, so this is never
    // up to date in Gradle's sense; it is a few lines of text either way.
    outputs.upToDateWhen { false }

    doLast {
        output.mkdirs()
        val key = "level-seed="
        if (!properties.isFile) {
            properties.parentFile.mkdirs()
            // The server fills in every other property with its default on the first start and keeps
            // this one. It still writes eula.txt and refuses to start until that is accepted by hand.
            properties.writeText(key + seed + System.lineSeparator())
            logger.lifecycle("Wrote $properties with the benchmark seed $seed")
            return@doLast
        }
        val lines = properties.readLines()
        val index = lines.indexOfFirst { it.startsWith(key) }
        if (index >= 0 && lines[index].substring(key.length).isNotBlank()) return@doLast
        val updated = if (index >= 0) lines.toMutableList().also { it[index] = key + seed }
        else lines + (key + seed)
        properties.writeText(updated.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        logger.lifecycle("Set the benchmark seed $seed in $properties")
    }
}

// Both plugins create their run tasks while the buildscript is evaluated, and this file is applied at
// the end of it, so the task is matched by name rather than looked up.
tasks.matching { it.name == "runBenchmark" }.configureEach { dependsOn("benchmarkRunDirectory") }

// The NeoForge and Forge nodes have a third check, `checkNeoForgeResources` and `checkForgeResources`, in
// gradle/moddev.gradle.kts beside the translation of datagen's Fabric-only JSON it guards.

/**
 * The integration directories both loaders compile, which must not name a loader any more than core
 * code may. Set by the applying script, from the integration table; see [checkLoaderSeam].
 */
@Suppress("UNCHECKED_CAST")
val loaderIndependentIntegrations: List<String> =
    project.extensions.extraProperties.get("thirst.loaderIndependentIntegrations") as List<String>

/** Every integration's package, `com.thirstwastaken2.<dir>`, from the same table; see [checkOptionalSeam]. */
@Suppress("UNCHECKED_CAST")
val integrationPackages: List<String> =
    project.extensions.extraProperties.get("thirst.integrations") as List<String>

/**
 * Fails when loader independent code names a mod loader: core code, and every integration both loaders
 * compile. `src/main/java` and `src/client/java` are compiled against Fabric API on a Fabric node, so the
 * compiler cannot catch a Fabric import there; this can. It cannot see the methods Fabric API injects
 * into vanilla classes, such as `getAttachedOrCreate`, which only a NeoForge build will report.
 */
tasks.register("checkLoaderSeam") {
    group = "verification"
    description = "Fails when loader independent sources import a mod loader's API"

    val roots = (listOf("src/main/java", "src/client/java") + loaderIndependentIntegrations.flatMap { dir ->
        listOf("src/main/$dir/java", "src/client/$dir/java")
    }).map(rootProject::file).filter(File::isDirectory)
    val forbidden = Regex("""\b(net\.fabricmc|net\.neoforged|net\.minecraftforge)\.""")
    inputs.files(roots.map { fileTree(it) { include("**/*.java") } })

    doLast {
        val offenders = roots.flatMap { root ->
            root.walk().filter { it.extension == "java" }.flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> forbidden.containsMatchIn(line) }
                    .map { (index, line) ->
                        "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                    }
            }
        }
        check(offenders.isEmpty()) {
            "Loader API in loader independent code. Route it through platform/Loader or " +
                "client/platform/ClientLoader instead:\n" + offenders.joinToString("\n")
        }
    }
}

/**
 * Fails when mod code carries a Stonecutter version conditional. Minecraft version differences belong
 * in a `platform/` package and, for injection signatures, `mixin/`; a `//?` block anywhere else means a
 * seam is missing. This replaced counting blocks as the exit ramp.
 *
 * It reads every hand-written root of the mod, found by directory: `src/main/java`, `src/client/java`
 * and every `src/main/<name>/java` and `src/client/<name>/java`, which are the loader, fluid API and
 * integration directories. A new one is covered the day it is created. An integration keeps the
 * differences of its own mod's API in its own `platform/` package; a vanilla difference goes into core
 * `platform/Vanilla`, where every integration can use it. Datagen, gametests and dev tools are outside it.
 */
tasks.register("checkVersionSeam") {
    group = "verification"
    description = "Fails when mod sources outside platform/ and mixin/ contain a version conditional"

    val roots = listOf("main", "client").flatMap { set ->
        val dir = rootProject.file("src/$set")
        listOf(dir.resolve("java")) + dir.listFiles().orEmpty()
            .filter { it.isDirectory && it.name != "generated" }.map { it.resolve("java") }
    }.filter(File::isDirectory)
    val allowed = setOf("platform", "mixin")
    inputs.files(roots.map { fileTree(it) { include("**/*.java") } })

    doLast {
        val offenders = roots.flatMap { root ->
            root.walk()
                .filter { it.extension == "java" }
                .filterNot { file -> file.relativeTo(root).invariantSeparatorsPath.split('/').any(allowed::contains) }
                .flatMap { file ->
                    file.readLines().withIndex()
                        .filter { (_, line) -> line.contains("//?") }
                        .map { (index, line) ->
                            "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                        }
                }
        }
        check(offenders.isEmpty()) {
            "Version conditional outside platform/ and mixin/. Put the difference behind platform/Vanilla, " +
                "client/platform/ClientVanilla or the integration's own platform package instead:\n" +
                offenders.joinToString("\n")
        }
    }
}

/**
 * Fails when a lang file lacks a key `en_us.json` has, or has one it lacks. Every one of the nine locales
 * is kept complete: a key added to `en_us` lands in all of them in the same change, since a missing key
 * shows the raw key to that player. It reads every hand-written `assets/<namespace>/lang` directory under
 * `src/main`, the core resources and each integration's.
 */
tasks.register("checkLang") {
    group = "verification"
    description = "Fails when a lang file does not have exactly the keys en_us.json has"

    val langDirs = rootProject.file("src/main").walk()
        .onEnter { it.name != "generated" && it.name != "java" }
        .filter { it.isDirectory && it.name == "lang" && it.parentFile.parentFile.name == "assets" }
        .toList()
    inputs.files(langDirs.map { fileTree(it) { include("*.json") } })

    doLast {
        @Suppress("UNCHECKED_CAST")
        fun keys(file: File): Set<String> =
            (groovy.json.JsonSlurper().parse(file) as Map<String, Any?>).keys
        val problems = langDirs.flatMap { dir ->
            val english = dir.resolve("en_us.json")
            if (!english.isFile) return@flatMap emptyList<String>()
            val expected = keys(english)
            dir.listFiles().orEmpty().filter { it.extension == "json" && it.name != "en_us.json" }.sortedBy { it.name }
                .flatMap { file ->
                    val actual = keys(file)
                    val path = file.relativeTo(rootProject.projectDir).invariantSeparatorsPath
                    (expected - actual).map { "$path: missing $it" } + (actual - expected).map { "$path: not in en_us: $it" }
                }
        }
        check(problems.isEmpty()) {
            "Every lang file must have exactly the keys en_us.json has. Translate the missing ones:\n" +
                problems.joinToString("\n")
        }
    }
}

/**
 * Fails when a data pack file this mod ships names another mod's id without a `mod_loaded` condition
 * for that mod.
 *
 * Such a file loads fine in every dev run, since runClient has every optional mod, and breaks the
 * players who lack that one. In a registry Minecraft reads while opening a world it breaks all of them:
 * 1.3.0 and 1.4.0 shipped Cold Sweat food data naming `farmersdelight:hot_cocoa` with only Cold Sweat's
 * own `required_mods`, which it ignores outside its namespace, and with Cold Sweat and without Farmer's
 * Delight no world could be opened. So every file under `src/main` that names a namespace other than the
 * mod's, Minecraft's and the shared `c` has to be gated on it, whatever registry it is for:
 * `neoforge:conditions` with `neoforge:mod_loaded`, `fabric:load_conditions` with
 * `fabric:all_mods_loaded` (or `any_mods_loaded`), or Forge 47's `conditions` with `forge:mod_loaded`. Two things need no condition, since they already
 * tolerate a missing id: a tag entry with `"required": false`, and this mod's own drinks files, whose
 * loader skips an unknown item with a warning. The generated output is read too.
 */
tasks.register("checkDataConditions") {
    group = "verification"
    description = "Fails when a shipped data file names another mod's id without a mod_loaded condition"

    val own = setOf("minecraft", "thirstwastaken2", "c", "neoforge", "fabric", "forge")
    val dataFiles = rootProject.file("src/main").walk()
        .onEnter { it.name != "java" }
        .filter { it.isFile && it.extension == "json" && "/data/" in it.invariantSeparatorsPath }
        .filterNot { "/thirstwastaken2/drinks/" in it.invariantSeparatorsPath }
        .toList()
    inputs.files(dataFiles)

    doLast {
        val id = Regex("#?([a-z][a-z0-9_.-]*):[a-z0-9_/.-]+")

        /** Every namespace [value] names outside its conditions, skipping optional tag entries. */
        fun namespaces(value: Any?, into: MutableSet<String>) {
            when (value) {
                is Map<*, *> -> {
                    if (value["required"] == false) return
                    value.forEach { (key, child) ->
                        if (key == "neoforge:conditions" || key == "fabric:load_conditions" || key == "conditions" ||
                            key == "forge:conditions") return@forEach
                        id.matchEntire(key.toString())?.let { into += it.groupValues[1] }
                        namespaces(child, into)
                    }
                }
                is List<*> -> value.forEach { namespaces(it, into) }
                is String -> id.matchEntire(value)?.let { into += it.groupValues[1] }
            }
        }

        /** The mods a file's conditions require, on either loader. */
        fun gated(value: Any?, into: MutableSet<String>) {
            when (value) {
                is Map<*, *> -> {
                    (value["modid"] as? String)?.let { into += it }
                    if ((value["condition"] as? String)?.endsWith("_mods_loaded") == true) {
                        (value["values"] as? List<*>)?.filterIsInstance<String>()?.let { into += it }
                    }
                    value.values.forEach { gated(it, into) }
                }
                is List<*> -> value.forEach { gated(it, into) }
            }
        }

        // Forge 47 reads `conditions` on recipes and advancements only, in 1.20.1's plural folders. Anywhere
        // else the block is ignored and the file loads regardless: a loot table naming an item only an
        // integration registers failed to parse on every server without that mod (the Create Sand Filter,
        // 2026-09-28). So there it gates nothing, and is itself a mistake.
        val forgeHonours = Regex("/data/[^/]+/(recipes|advancements)/")
        val ignoredConditions = dataFiles.filter { file ->
            val json = groovy.json.JsonSlurper().parse(file) as? Map<*, *>
            json?.get("conditions") != null && !forgeHonours.containsMatchIn(file.invariantSeparatorsPath) &&
                "/data/" in file.invariantSeparatorsPath &&
                (json["conditions"] as? List<*>)?.any { (it as? Map<*, *>)?.get("type")?.toString()?.startsWith("forge:") == true } == true
        }.map { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath }.sorted()
        check(ignoredConditions.isEmpty()) {
            "Forge 47 ignores `conditions` outside recipes and advancements, so these files load without the " +
                "mod they name and fail to parse. Name nothing the mod registers (a loot table can drop " +
                "through an item tag whose entry is `required: false`):\n" + ignoredConditions.joinToString("\n")
        }

        val problems = dataFiles.sortedBy { it.invariantSeparatorsPath }.mapNotNull { file ->
            val json = groovy.json.JsonSlurper().parse(file) as? Map<*, *> ?: return@mapNotNull null
            val named = mutableSetOf<String>().also { namespaces(json, it) } - own
            val required = mutableSetOf<String>().also {
                gated(json["neoforge:conditions"], it)
                gated(json["fabric:load_conditions"], it)
                gated(json["conditions"], it)
                // Forge 47's key on a data pack registry entry, such as Cold Sweat's food data on 1.20.1:
                // its RegistryDataLoader skips an entry whose `forge:conditions` fail before decoding it.
                gated(json["forge:conditions"], it)
            }
            val ungated = named - required
            if (ungated.isEmpty()) null
            else "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}: ${ungated.sorted().joinToString()}"
        }
        check(problems.isEmpty()) {
            "These data files name a mod's ids without a condition that the mod is loaded. Without it the " +
                "file fails for everyone who lacks that mod, and in a registry read while opening a world, " +
                "no world opens. Add neoforge:conditions (neoforge:mod_loaded), conditions (forge:mod_loaded; forge:conditions on a Forge 47 data pack registry entry) or fabric:load_conditions " +
                "(fabric:all_mods_loaded) for each mod listed:\n" + problems.joinToString("\n")
        }
        logger.lifecycle("checkDataConditions: ${dataFiles.size} data files, every other mod's id gated")
    }
}

/**
 * Fails when the agent's loader independent half stops being loader independent.
 *
 * `dev/agent/core` is the queue, the envelope and the dispatch loop: plain Java and Gson, and nothing
 * else. That is what makes the settled decision in
 * src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md true rather than aspirational — one import of Minecraft, of a mod loader or of the mod itself and it no longer lifts
 * out of this project cleanly. `dev/agent/thirst` is the half that is free to name all three.
 */
tasks.register("checkAgentCore") {
    group = "verification"
    description = "Fails when the agent's core package imports Minecraft, a mod loader or the mod"

    val root = rootProject.file("src/dev/java/com/thirstwastaken2/dev/agent/core")
    val forbidden = Regex("""^import\s+(net\.minecraft|net\.fabricmc|net\.neoforged|net\.minecraftforge|"""
        + """com\.mojang|com\.thirstwastaken2(?!\.dev\.agent\.core))""")
    inputs.files(fileTree(root) { include("**/*.java") })

    doLast {
        val offenders = root.walk().filter { it.extension == "java" }.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> forbidden.containsMatchIn(line.trim()) }
                .map { (index, line) ->
                    "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                }
        }.toList()
        check(offenders.isEmpty()) {
            "The agent's core package is meant to know nothing of Minecraft, of a loader or of this " +
                "mod. Put what needs them in dev/agent/thirst instead:\n" + offenders.joinToString("\n")
        }
    }
}

/**
 * What `checkOptionalSeam` needs to know about one compiled class: its supertypes, what it names in
 * a way the verifier may resolve, and every string in its constant pool (annotations are found there).
 */
class ClassSummary(
    val name: String,
    val supertypes: List<String>,
    /** Classes named in a descriptor, or as a class constant that is not only the owner of a member. */
    val resolved: Set<String>,
    /** Every class it names at all, owners of the members it calls included. */
    val named: Set<String>,
    val strings: Set<String>,
)

/** The object types in a descriptor or an array class name, e.g. `(Lfoo/Bar;[Lbaz/Qux;)V`. */
fun descriptorTypes(descriptor: String): List<String> =
    Regex("""L([^;<]+)[;<]""").findAll(descriptor).map { it.groupValues[1] }.toList()

/**
 * Reads a class file far enough for `checkOptionalSeam`: the constant pool, the supertypes and the
 * field and method descriptors. Written out here rather than taken from ASM, which a script plugin
 * cannot declare a dependency on; the format has not changed in the parts this reads since Java 11.
 */
fun summarise(file: File): ClassSummary = java.io.DataInputStream(file.inputStream().buffered()).use { input ->
    check(input.readInt() == 0xCAFEBABE.toInt()) { "$file is not a class file" }
    input.readUnsignedShort(); input.readUnsignedShort()
    val count = input.readUnsignedShort()
    val utf8 = arrayOfNulls<String>(count)
    val classNameIndex = IntArray(count) { -1 }
    val memberOwners = mutableSetOf<Int>()
    val descriptorIndices = mutableListOf<Int>()
    var index = 1
    while (index < count) {
        when (val tag = input.readUnsignedByte()) {
            1 -> utf8[index] = input.readUTF()
            3, 4 -> input.readInt()
            5, 6 -> { input.readLong(); index++ }
            7 -> classNameIndex[index] = input.readUnsignedShort()
            8, 16, 19, 20 -> {
                val value = input.readUnsignedShort()
                if (tag == 16) descriptorIndices += value
            }
            9, 10, 11 -> { memberOwners += input.readUnsignedShort(); input.readUnsignedShort() }
            12 -> { input.readUnsignedShort(); descriptorIndices += input.readUnsignedShort() }
            15 -> { input.readUnsignedByte(); input.readUnsignedShort() }
            17, 18 -> { input.readUnsignedShort(); input.readUnsignedShort() }
            else -> error("$file: unknown constant pool tag $tag")
        }
        index++
    }
    fun className(classIndex: Int): String = utf8[classNameIndex[classIndex]]!!
    input.readUnsignedShort()
    val self = className(input.readUnsignedShort())
    val superIndex = input.readUnsignedShort()
    val supertypes = mutableListOf<String>()
    if (superIndex != 0) supertypes += className(superIndex)
    repeat(input.readUnsignedShort()) { supertypes += className(input.readUnsignedShort()) }
    repeat(2) {
        repeat(input.readUnsignedShort()) {
            input.readUnsignedShort(); input.readUnsignedShort()
            descriptorIndices += input.readUnsignedShort()
            repeat(input.readUnsignedShort()) {
                input.readUnsignedShort()
                input.skipNBytes(input.readInt().toLong() and 0xFFFFFFFFL)
            }
        }
    }

    fun typesOf(name: String) = if (name.startsWith("[")) descriptorTypes(name) else listOf(name)
    val classes = (1 until count).filter { classNameIndex[it] >= 0 }
    val named = classes.flatMap { typesOf(className(it)) }.toMutableSet()
    val resolved = classes.filterNot { it in memberOwners }.flatMap { typesOf(className(it)) }.toMutableSet()
    descriptorIndices.mapNotNull { utf8[it] }.flatMap(::descriptorTypes).forEach { named += it; resolved += it }
    ClassSummary(self, supertypes, resolved - self, named - self, utf8.filterNotNull().toSet())
}

/**
 * Fails when a class the game loads whether or not an optional mod is installed names that mod.
 *
 * **Why this exists.** 1.0.9 crashed every NeoForge client without Sophisticated Core, from an
 * entrypoint that asked the gate first. The JVM verifies a class whole when it links it, before any
 * code in it runs, and verifying the entrypoint's tab factories loaded a class whose superclass is
 * Sophisticated's. No run could see it: runClient always has every optional mod, and runServer and
 * runGametest never load a client entrypoint. This reads the compiled classes, so a lambda or a
 * generic argument counts the same as an import.
 *
 * **What is always loaded**, the roots: every entrypoint in `fabric.mod.json`, every class carrying
 * `@Mod`, `@EventBusSubscriber`, this mod's `@IntegrationEntrypoint` or Jade's `@WailaPlugin`, and
 * every mixin config's plugin. A root may name Minecraft, the JDK, the loaders, the libraries every
 * game ships and this mod's own classes. An entrypoint another mod reads, `jade` or `modmenu`, may also name that mod, since only it loads the
 * class.
 *
 * **What a root must not do**, the two ways 1.0.9 could have happened:
 *
 * - name another mod's class anywhere, even in code a gate keeps from running;
 * - name one of this mod's own classes in a way the verifier resolves (a descriptor, a cast, a
 *   supertype) when that class, or a supertype of it, is another mod's. Calling a static method on
 *   such a class is fine, and is exactly how a gate hands over: the class is only loaded when the call
 *   runs. `DrinkingUpgradeTabs` is that shape.
 *
 * It also fails when core code, `src/main/java` and `src/client/java` and the loader directories, refers
 * to an integration package at all, since those are only compiled on some nodes and name other mods.
 * The packages are every row of the integration table, so a new integration is covered by its row.
 */
tasks.register("checkOptionalSeam") {
    group = "verification"
    description = "Fails when a class loaded without an optional mod installed names that mod"

    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    // `client` is Loom's split source set on Fabric. The NeoForge and Forge nodes have none: their client
    // sources compile into `main`.
    val checked = listOfNotNull(sourceSets.getByName("main"), sourceSets.findByName("client"))
    dependsOn(checked.map { it.classesTaskName })
    val classDirs = checked.flatMap { it.output.classesDirs.files }
    val resourceDirs = checked.mapNotNull { it.output.resourcesDir }
    inputs.files(classDirs, resourceDirs)

    val coreRoots = listOf("src/main/java", "src/client/java", "src/main/fabric", "src/main/neoforge",
        "src/client/fabric", "src/client/neoforge", "src/main/neoforge-fluidhandler", "src/main/neoforge-transfer",
        "src/main/fabric-payload", "src/client/fabric-payload", "src/main/fabric-legacypayload",
        "src/client/fabric-legacypayload", "src/main/forge", "src/client/forge", "src/main/forge-fluidhandler")
        .map(rootProject::file)
    inputs.files(coreRoots.map { fileTree(it) { include("**/*.java") } })

    doLast {
        /** What every root may name. A new library the game always ships belongs here. */
        val always = listOf("java/", "javax/", "jdk/", "sun/", "net/minecraft/", "com/mojang/", "org/slf4j/",
            "org/apache/logging/", "com/google/", "it/unimi/", "org/jetbrains/", "org/jspecify/", "io/netty/",
            "org/joml/", "org/lwjgl/", "org/spongepowered/", "org/objectweb/", "com/llamalad7/",
            "net/fabricmc/", "net/neoforged/", "net/minecraftforge/", "com/thirstwastaken2/")
        /** What a root another mod loads may name as well, keyed by what makes it a root. */
        val loadedBy = mapOf(
            "jade" to listOf("snownee/jade/"),
            "modmenu" to listOf("com/terraformersmc/modmenu/"),
        )

        val summaries = classDirs.filter(File::isDirectory)
            .flatMap { dir -> dir.walk().filter { it.isFile && it.extension == "class" }.toList() }
            .map(::summarise).associateBy { it.name }

        // Roots, each with the packages it may name.
        val roots = mutableMapOf<String, MutableSet<String>>()
        fun root(className: String, loader: String?) {
            roots.getOrPut(className.replace('.', '/')) { always.toMutableSet() }
                .addAll(loader?.let(loadedBy::get).orEmpty())
        }
        resourceDirs.filter(File::isDirectory).forEach { dir ->
            dir.resolve("fabric.mod.json").takeIf(File::isFile)?.let { manifest ->
                @Suppress("UNCHECKED_CAST")
                val entrypoints = (groovy.json.JsonSlurper().parse(manifest) as Map<String, Any?>)["entrypoints"]
                    as Map<String, List<Any?>>? ?: emptyMap()
                entrypoints.forEach { (key, values) ->
                    values.forEach { value ->
                        val name = (if (value is Map<*, *>) value["value"] else value) as String
                        root(name.substringBefore("::"), key)
                    }
                }
            }
            dir.walk().filter { it.isFile && it.name.endsWith(".mixins.json") }.forEach { config ->
                ((groovy.json.JsonSlurper().parse(config) as Map<*, *>)["plugin"] as String?)?.let { root(it, null) }
            }
        }
        summaries.values.forEach { summary ->
            val strings = summary.strings
            if ("Lnet/neoforged/fml/common/Mod;" in strings || "Lnet/neoforged/fml/common/EventBusSubscriber;" in strings ||
                "Lnet/minecraftforge/fml/common/Mod;" in strings || "Lnet/minecraftforge/fml/common/Mod\$EventBusSubscriber;" in strings ||
                "Lcom/thirstwastaken2/platform/IntegrationEntrypoint;" in strings) {
                root(summary.name, null)
            }
            if ("Lsnownee/jade/api/WailaPlugin;" in strings) root(summary.name, "jade")
        }

        roots.keys.sorted().forEach { logger.info("checkOptionalSeam root: ${it.replace('/', '.')}") }

        fun allowed(type: String, packages: Set<String>) = packages.any(type::startsWith)
        /** The first of the class's supertypes, all the way up, that a root with these packages may not name. */
        fun foreignAncestor(type: String, packages: Set<String>, seen: MutableSet<String> = mutableSetOf()): String? {
            if (!seen.add(type)) return null
            if (!allowed(type, packages)) return type
            val summary = summaries[type] ?: return null
            return summary.supertypes.firstNotNullOfOrNull { foreignAncestor(it, packages, seen) }
        }

        val offenders = mutableListOf<String>()
        roots.forEach { (name, packages) ->
            val summary = summaries[name]
            if (summary == null) {
                // Named by a manifest, compiled nowhere on this node: the loader would fail on it anyway.
                offenders += "$name: named as an entrypoint or mixin plugin, but no such class was compiled"
                return@forEach
            }
            // The root itself and every class nested in it that the verifier needs are the same file
            // only for lambdas; an inner class is its own root only once something loads it.
            summary.named.filterNot { allowed(it, packages) }.sorted().forEach {
                offenders += "$name names ${it.replace('/', '.')}"
            }
            (summary.resolved + summary.supertypes).filter { allowed(it, packages) }.sorted().forEach { type ->
                foreignAncestor(type, packages)?.takeIf { it != type }?.let { ancestor ->
                    offenders += "$name resolves ${type.replace('/', '.')}, which extends " +
                        "${ancestor.replace('/', '.')}; call into it through a method on another class instead"
                }
            }
        }

        val integration = Regex("""\bcom\.thirstwastaken2\.(client\.)?(${integrationPackages.joinToString("|")})\b""")
        coreRoots.filter(File::isDirectory).forEach { dir ->
            dir.walk().filter { it.extension == "java" }.forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    if (integration.containsMatchIn(line)) {
                        offenders += "${file.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: " +
                            "core code refers to an integration: ${line.trim()}"
                    }
                }
            }
        }

        check(offenders.isEmpty()) {
            "A class the game loads without an optional mod installed would load that mod's classes, which " +
                "crashes a game that does not have it (see the note on checkOptionalSeam in " +
                "gradle/shared.gradle.kts):\n" + offenders.joinToString("\n")
        }
        logger.lifecycle("checkOptionalSeam: ${roots.size} classes loaded without the optional mods, all clean")
    }
}

/**
 * The types one compiled class of `com.thirstwastaken2.api` shows other mods: its supertypes when it is
 * public, and the types in the descriptor and generic signature of every public or protected field and
 * method, each as `member: type`. Synthetic members, the lambdas and bridges the compiler adds, are left
 * out; nobody can call them by name.
 */
fun apiSurface(file: File): List<String> = java.io.DataInputStream(file.inputStream().buffered()).use { input ->
    check(input.readInt() == 0xCAFEBABE.toInt()) { "$file is not a class file" }
    input.readUnsignedShort(); input.readUnsignedShort()
    val count = input.readUnsignedShort()
    val utf8 = arrayOfNulls<String>(count)
    val classNameIndex = IntArray(count) { -1 }
    var index = 1
    while (index < count) {
        when (input.readUnsignedByte()) {
            1 -> utf8[index] = input.readUTF()
            3, 4 -> input.readInt()
            5, 6 -> { input.readLong(); index++ }
            7 -> classNameIndex[index] = input.readUnsignedShort()
            8, 16, 19, 20 -> input.readUnsignedShort()
            9, 10, 11, 12, 17, 18 -> { input.readUnsignedShort(); input.readUnsignedShort() }
            15 -> { input.readUnsignedByte(); input.readUnsignedShort() }
            else -> error("$file: unknown constant pool tag")
        }
        index++
    }
    fun className(classIndex: Int): String = utf8[classNameIndex[classIndex]]!!
    val publicOrProtected = 0x0001 or 0x0004
    val synthetic = 0x1000

    val classAccess = input.readUnsignedShort()
    val self = className(input.readUnsignedShort())
    val exposed = mutableListOf<String>()
    val supertypes = mutableListOf<String>()
    input.readUnsignedShort().takeIf { it != 0 }?.let { supertypes += className(it) }
    repeat(input.readUnsignedShort()) { supertypes += className(input.readUnsignedShort()) }
    val isPublic = classAccess and 0x0001 != 0
    if (isPublic) supertypes.forEach { exposed += "$self extends: $it" }

    /** Reads the attributes that follow a member or the class, returning its generic signature if any. */
    fun signature(): String? {
        var found: String? = null
        repeat(input.readUnsignedShort()) {
            val name = utf8[input.readUnsignedShort()]
            val length = input.readInt().toLong() and 0xFFFFFFFFL
            if (name == "Signature") found = utf8[input.readUnsignedShort()] else input.skipNBytes(length)
        }
        return found
    }
    repeat(2) {
        repeat(input.readUnsignedShort()) {
            val access = input.readUnsignedShort()
            val name = utf8[input.readUnsignedShort()]!!
            val descriptor = utf8[input.readUnsignedShort()]!!
            val generic = signature()
            if (isPublic && access and publicOrProtected != 0 && access and synthetic == 0) {
                (descriptorTypes(descriptor) + descriptorTypes(generic.orEmpty())).distinct()
                    .forEach { exposed += "$self.$name: $it" }
            }
        }
    }
    signature()?.takeIf { isPublic }?.let { generic -> descriptorTypes(generic).forEach { exposed += "$self<>: $it" } }
    exposed
}

/**
 * Fails when the public API names one of the mod's internal types.
 *
 * Everything in `com.thirstwastaken2.api` is a promise to other mods (docs/docs/developers/java-api.md): a signature
 * there changes only after a deprecation. That promise is only worth something while the API is made of
 * Minecraft's types, the JDK's and its own. A public method that takes a `ThirstData` or returns a
 * `WaterQuality` would freeze that internal type's shape too, so this reads the compiled `api` classes
 * and fails on any public or protected member, or public supertype, that names a `com.thirstwastaken2`
 * type outside `api`. The body of a method may use anything; only what a caller sees counts.
 *
 * The API holds no version conditional either, so it looks the same on every node and a mod compiled
 * against one node's jar links against every other's. `checkVersionSeam` already fails on a `//?` in
 * `api/`, since it only lets `platform/` and `mixin/` have one.
 */
tasks.register("checkApiSurface") {
    group = "verification"
    description = "Fails when the public API in com.thirstwastaken2.api exposes an internal type"

    val main = project.extensions.getByType<SourceSetContainer>().getByName("main")
    dependsOn(main.classesTaskName)
    val classDirs = main.output.classesDirs.files
    inputs.files(classDirs)

    doLast {
        val apiClasses = classDirs.map { it.resolve("com/thirstwastaken2/api") }.filter(File::isDirectory)
            .flatMap { dir -> dir.walk().filter { it.isFile && it.extension == "class" }.toList() }
        check(apiClasses.isNotEmpty()) { "No compiled classes under com/thirstwastaken2/api; nothing was checked" }
        val offenders = apiClasses.flatMap(::apiSurface)
            .filter { entry ->
                val type = entry.substringAfterLast(": ")
                type.startsWith("com/thirstwastaken2/") && !type.startsWith("com/thirstwastaken2/api/")
            }
            .map { it.replace('/', '.') }
            .sorted()
        check(offenders.isEmpty()) {
            "The public API names internal types, which would make them part of the API. Hand out a " +
                "Minecraft, JDK or com.thirstwastaken2.api type instead:\n" + offenders.joinToString("\n")
        }
        logger.lifecycle("checkApiSurface: ${apiClasses.size} API classes, no internal type exposed")
    }
}

/** Collects the jars every node produces into one directory, for `chiseledBuild`. */
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds the mod jar and copies it to build/libs/"
    into(rootProject.layout.buildDirectory.dir("libs"))
}

/*
 * The gametests are written against a fresh config, which is what CI runs them on. A run directory kept
 * between local runs keeps the file the last run saved, and a value already in that file is never
 * replaced by a newer default, so without this a local run would test the defaults of whichever version
 * first wrote it.
 *
 * The world goes too, for the same reason and for time. The runner reopens it rather than making it anew,
 * so it kept every mock player any run had joined, tens of thousands of files, and saving into a
 * directory that size made shutting the server down slower than the tests. `world` is the Fabric and
 * Forge runners' and NeoForge 1.21.1's, `gametestserver` vanilla's own from 1.21.5.
 */
val gametestDir = rootProject.file("run/${project.name}/gametest")
tasks.matching { it.name == "runGametest" }.configureEach {
    doFirst {
        gametestDir.resolve("config/thirstwastaken2.json").delete()
        gametestDir.resolve("world").deleteRecursively()
        gametestDir.resolve("gametestserver").deleteRecursively()
        // A fresh run directory, as on CI, has none, and the server logs that as an error with a stack
        // trace before writing its defaults. An empty file gives the same defaults without it.
        gametestDir.resolve("server.properties").takeUnless { it.isFile }?.apply {
            parentFile.mkdirs()
            writeText("")
        }
    }
}

/*
 * The test server's heap, capped. Left alone the JVM takes a quarter of the machine's memory, which on
 * top of the Gradle daemon and an IDE pushes a 16 GB machine into paging. A headless server with one
 * small world needs far less. `-PgametestHeap=<size>` raises it for a run that needs more.
 */
val gametestHeap = providers.gradleProperty("gametestHeap").getOrElse("1536m")
tasks.withType<JavaExec>().matching { it.name == "runGametest" }.configureEach {
    maxHeapSize = gametestHeap
}

/*
 * `-Ptests=WaterSickness,Canteen` runs only those test classes, named with or without the `GameTest`
 * suffix, case ignored. Every runner, Fabric API's and the NeoForge and Forge harnesses, finds the test
 * classes in the `fabric-gametest` entrypoints of the gametest mod's fabric.mod.json, so the filter
 * drops the others from the built copy of that file. A name matching no class fails the build rather
 * than run nothing. Without the property every class runs, which is what CI does.
 */
val gametestFilter = providers.gradleProperty("tests").getOrElse("")
    .split(',').map { it.trim().lowercase().removeSuffix("gametest") }.filter { it.isNotEmpty() }
tasks.named<ProcessResources>("processGametestResources") {
    inputs.property("tests", gametestFilter.joinToString(","))
    if (gametestFilter.isNotEmpty()) {
        val manifest = destinationDir.resolve("fabric.mod.json")
        doLast {
            @Suppress("UNCHECKED_CAST")
            val json = groovy.json.JsonSlurper().parse(manifest) as MutableMap<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val entrypoints = json["entrypoints"] as MutableMap<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val classes = entrypoints["fabric-gametest"] as List<String>
            fun key(name: String) = name.substringAfterLast('.').lowercase().removeSuffix("gametest")
            val unknown = gametestFilter.filter { wanted -> classes.none { key(it) == wanted } }
            require(unknown.isEmpty()) {
                "-Ptests names no test class: ${unknown.joinToString()}. The classes are:\n" +
                    classes.joinToString("\n") { it.substringAfterLast('.') }
            }
            entrypoints["fabric-gametest"] = classes.filter { key(it) in gametestFilter }
            manifest.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)))
        }
    }
}

/*
 * Every versioned directory under `src/`, on every node, including the ones this node does not compile.
 *
 * Stonecutter 0.10 preprocesses only the directories a node's source sets list, and "Set active project
 * to <node>" and "Reset active project" write back only that node's. A directory only some nodes compile,
 * an integration's or another loader's, would keep whatever node last switched to it: "Reset active
 * project" from `1.20.1-forge` left `src/main/forge` and every Forge-only integration in 1.20.1's
 * spelling. Before 0.10 Stonecutter rewrote the whole of `src/<source set>` and this was not needed.
 *
 * The `versioned` source set lists every such directory this node's own source sets leave out, so a
 * switch to any node rewrites the whole tree. It exists only to be preprocessed: nothing compiles,
 * packages or runs it. `src/main/generated` is datagen output, one directory per Minecraft version,
 * never versioned.
 */
the<SourceSetContainer>().let { sourceSets ->
    val listed = sourceSets.flatMap { it.java.srcDirs + it.resources.srcDirs }
        .map { it.relativeTo(projectDir).invariantSeparatorsPath }.toSet()
    val unlisted = rootProject.file("src").listFiles().orEmpty().filter(File::isDirectory)
        .flatMap { set -> listOf(set) + set.listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in setOf("java", "resources", "generated") } }
        .flatMap { listOf(it.resolve("java"), it.resolve("resources")) }
        .filter(File::isDirectory)
        .map { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath }
        .filterNot(listed::contains)
    sourceSets.create("versioned") {
        java.setSrcDirs(unlisted)
        resources.setSrcDirs(emptyList<Any>())
    }
}
