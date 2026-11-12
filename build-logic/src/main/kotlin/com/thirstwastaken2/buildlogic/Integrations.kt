package com.thirstwastaken2.buildlogic

/** A mod loader a node builds for, by the name its source directories use: `src/main/<id>`. */
enum class Loader(val id: String) {
    FABRIC("fabric"),
    NEOFORGE("neoforge"),
    /** MinecraftForge 47, the `1.20.1-forge` node; see docs/dev/VERSION-DIFFERENCES.md. */
    FORGE("forge"),
    ;

    /** What follows the Minecraft version in the name of a node of this loader: nothing on Fabric. */
    val nodeSuffix: String get() = if (this == FABRIC) "" else "-$id"
}

/**
 * The loader a node builds for, from its name: `26.2.x-neoforge` is NeoForge, and a node with no loader
 * in its name is Fabric. Every script that needs a node's loader asks this, or `tools/node_names.py`.
 */
fun loaderOf(node: String): Loader =
    Loader.entries.firstOrNull { it != Loader.FABRIC && node.endsWith(it.nodeSuffix) } ?: Loader.FABRIC

/**
 * A node's name without its loader: `26.2.x` for `26.2.x-neoforge`. It names the table in
 * stonecutter.properties.toml that every loader of that Minecraft version shares.
 */
fun minecraftOf(node: String): String = node.removeSuffix(loaderOf(node).nodeSuffix)

/**
 * One optional integration: its own source directories, compiled only on a node that sets [depsKey] in
 * stonecutter.properties.toml, and what the built manifest must say on such a node and only there.
 *
 * What is specific to one mod stays in the loader script that needs it, with its own comment: the
 * dependencies themselves, the `runClientMod` names, Create Fly's classes without its class tweaker,
 * the libraries Create bundles, the nested jars Loom does not unpack. If a new integration needs
 * something this cannot say, add a block for it there rather than a field only one row uses.
 */
data class Integration(
    /** `src/main/<dir>`, `src/client/<dir>`, `src/dev/<dir>`, and the Java package `com.thirstwastaken2.<dir>`. */
    val dir: String,
    /** The property whose presence says this node builds it, e.g. `deps.sophisticated_core`. */
    val depsKey: String,
    /** What may compile it. One naming both must not name either loader's API; `checkLoaderSeam` checks. */
    val loaders: Set<Loader>,
    /** Has `src/client/<dir>/java`. */
    val client: Boolean = false,
    /** Has `src/dev/<dir>/java`, for the benchmark. Fabric only today. */
    val dev: Boolean = false,
    /** Also has `src/main/<dir>-transfer` or `src/main/<dir>-fluidhandler`, by NeoForge's fluid API generation. */
    val fluidApiSplit: Boolean = false,
    /** Its mixin config, named in the built manifest on a node that builds it; null for one with no mixins. */
    val mixinConfig: String? = null,
    /** Where the mixin config goes in `fabric.mod.json`'s `mixins`; appended when null. */
    val fabricMixinIndex: Int? = null,
    /** Entrypoints added to `fabric.mod.json`, appended to a key that already has some. */
    val fabricEntrypoints: Map<String, List<String>> = emptyMap(),
    /** Mod ids named as optional dependencies in `neoforge.mods.toml`, and in Forge's `mods.toml`. */
    val neoForgeDependencies: List<String> = emptyList(),
    /**
     * Those of [neoForgeDependencies] this mod loads after (`ordering = "AFTER"`), because a data file of
     * ours replaces one of theirs at the same id, and the mod loaded later wins.
     */
    val loadAfter: Set<String> = emptySet(),
) {
    /** Whether more than one loader compiles the integration, so that none of its code may name one. */
    val loaderIndependent: Boolean get() = loaders.size > 1

    /**
     * The roots under `src/main` it adds, each with a `java` and a `resources` directory, relative to the
     * project. [transferApi] is NeoForge's fluid API generation: the transfer API from 1.21.2, or
     * `IFluidHandler` before it.
     */
    fun mainRoots(transferApi: Boolean): List<String> {
        val generation = if (transferApi) "transfer" else "fluidhandler"
        return listOfNotNull("src/main/$dir", if (fluidApiSplit) "src/main/$dir-$generation" else null)
    }

    /** The Java directory it adds to the client sources, relative to their root, or null. */
    val clientJava: String? get() = if (client) "$dir/java" else null

    /** The Java directory it adds to the dev tools, relative to the project, or null. */
    val devJava: String? get() = if (dev) "src/dev/$dir/java" else null

    /** What is appended to `neoforge.mods.toml`: the mixin config, then one optional dependency per mod. */
    fun neoForgeManifest(modId: String): String = buildString {
        if (mixinConfig != null) append("\n[[mixins]]\nconfig = \"$mixinConfig\"\n")
        neoForgeDependencies.forEach { dependency ->
            append("\n[[dependencies.$modId]]\nmodId = \"$dependency\"\ntype = \"optional\"\nordering = \"${ordering(dependency)}\"\nside = \"BOTH\"\n")
        }
    }

    /**
     * What is appended to Forge 47's `mods.toml`: one optional dependency per mod, in its spelling
     * (`mandatory` and `versionRange` rather than `type`). Forge 47 reads no mixin configs from that file;
     * build.forge.gradle.kts names them in the jar manifest.
     */
    fun forgeManifest(modId: String): String = buildString {
        neoForgeDependencies.forEach { dependency ->
            append("\n[[dependencies.$modId]]\nmodId = \"$dependency\"\nmandatory = false\nversionRange = \"[0,)\"\nordering = \"${ordering(dependency)}\"\nside = \"BOTH\"\n")
        }
    }

    private fun ordering(dependency: String): String = if (dependency in loadAfter) "AFTER" else "NONE"

    /** Adds the mixin config and the entrypoints to a parsed `fabric.mod.json`. */
    fun patchFabricManifest(json: MutableMap<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val mixins = json.getValue("mixins") as MutableList<Any?>
        if (mixinConfig != null) {
            if (fabricMixinIndex != null) mixins.add(fabricMixinIndex, mixinConfig) else mixins.add(mixinConfig)
        }
        if (fabricEntrypoints.isEmpty()) return
        @Suppress("UNCHECKED_CAST")
        val entrypoints = json.getValue("entrypoints") as MutableMap<String, Any?>
        fabricEntrypoints.forEach { (key, classes) ->
            @Suppress("UNCHECKED_CAST")
            val existing = entrypoints[key] as MutableList<Any?>?
            if (existing != null) existing.addAll(classes) else entrypoints[key] = classes.toMutableList()
        }
    }
}

/**
 * Every optional integration with source directories of its own. The order is the order they are
 * written into a manifest, so keep a new row at the end.
 *
 * Each integration's own `AGENTS.md` says what it does; this says only how it is built.
 */
val integrations: List<Integration> = listOf(
    // Fabric only: a port of Create with no release for every Minecraft version. See src/main/createfly/AGENTS.md.
    Integration(
        dir = "createfly",
        depsKey = "deps.create_fly",
        loaders = setOf(Loader.FABRIC),
        client = true,
        dev = true,
        mixinConfig = "thirstwastaken2.createfly.mixins.json",
        // Straight after the mod's own config, ahead of the client one.
        fabricMixinIndex = 1,
        fabricEntrypoints = mapOf(
            "thirstwastaken2:createfly" to listOf("com.thirstwastaken2.createfly.CreateFlyEntrypoint"),
            "thirstwastaken2:createfly_client" to listOf("com.thirstwastaken2.client.createfly.CreateFlyClientEntrypoint"),
        ),
    ),
    // NeoForge only. See src/main/create/AGENTS.md.
    Integration(
        dir = "create",
        depsKey = "deps.create",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.create.mixins.json",
        neoForgeDependencies = listOf("create"),
    ),
    // The same Sand Filter on Forge 47, for Create 6's 1.20.1 build: the same deps key, its own directory,
    // since every class names the loader's fluid and capability API. See src/main/createforge/AGENTS.md.
    Integration(
        dir = "createforge",
        depsKey = "deps.create",
        loaders = setOf(Loader.FORGE),
        mixinConfig = "thirstwastaken2.createforge.mixins.json",
        neoForgeDependencies = listOf("create"),
    ),
    // Fabric only: Farmer's Delight Refabricated's Cooking Pot, whose match never tests a custom
    // ingredient, so salt water boiled Pure. The original on NeoForge and Forge needs nothing. Everything
    // else of Farmer's Delight is data and registry ids in core. See src/main/farmersdelight/AGENTS.md.
    Integration(
        dir = "farmersdelight",
        depsKey = "deps.farmersdelight",
        loaders = setOf(Loader.FABRIC),
        mixinConfig = "thirstwastaken2.farmersdelight.mixins.json",
    ),
    // NeoForge only; Core's tanks move fluid through the fluid API of their NeoForge generation.
    // See src/main/sophisticated/AGENTS.md.
    Integration(
        dir = "sophisticated",
        depsKey = "deps.sophisticated_core",
        loaders = setOf(Loader.NEOFORGE),
        client = true,
        fluidApiSplit = true,
        mixinConfig = "thirstwastaken2.sophisticated.mixins.json",
        neoForgeDependencies = listOf("sophisticatedcore"),
    ),
    // Both loaders: everything it touches is in Moonlight Lib, which is multi loader. Moonlight is a
    // dependency of its own because three of the four mixins are its, and other mods ship it. The
    // second Jade plugin is a Fabric entrypoint; NeoForge finds it by its annotation. Jade reads that
    // entrypoint whether or not Supplementaries is installed, so the class it names asks the gate before
    // it loads anything of Moonlight's.
    // See src/main/supplementaries/AGENTS.md.
    Integration(
        dir = "supplementaries",
        depsKey = "deps.supplementaries",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        client = true,
        mixinConfig = "thirstwastaken2.supplementaries.mixins.json",
        fabricEntrypoints = mapOf("jade" to listOf("com.thirstwastaken2.client.supplementaries.SupplementariesJade")),
        neoForgeDependencies = listOf("supplementaries", "moonlight"),
    ),
    // Both loaders: the official mod on NeoForge 1.21.1, Refabricated, the Fabric port with the same mod
    // id and package, on every Fabric node. The Jade reader is a Fabric entrypoint; NeoForge finds it by
    // its annotation. Jade reads it whether or not Kaleidoscope Cookery is installed, so it names none of
    // its classes. See src/main/kaleidoscope/AGENTS.md.
    Integration(
        dir = "kaleidoscope",
        depsKey = "deps.kaleidoscope_cookery",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE, Loader.FORGE),
        client = true,
        mixinConfig = "thirstwastaken2.kaleidoscope.mixins.json",
        fabricEntrypoints = mapOf("jade" to listOf("com.thirstwastaken2.client.kaleidoscope.KaleidoscopeJade")),
        neoForgeDependencies = listOf("kaleidoscope_cookery"),
    ),
    // Every loader, as Kaleidoscope Cookery: the official mod on NeoForge 1.21.1 and Forge 1.20.1,
    // Refabricated on every Fabric node, one mod id and package. What it touches names neither loader's
    // fluid API. Its own directory rather than Cookery's, since the two mods are installed apart. The Jade
    // reader is a Fabric entrypoint; NeoForge and Forge find it by its annotation.
    // See src/main/kaleidoscopetavern/AGENTS.md.
    Integration(
        dir = "kaleidoscopetavern",
        depsKey = "deps.kaleidoscope_tavern",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE, Loader.FORGE),
        client = true,
        mixinConfig = "thirstwastaken2.kaleidoscopetavern.mixins.json",
        fabricEntrypoints = mapOf("jade" to listOf("com.thirstwastaken2.client.kaleidoscopetavern.KaleidoscopeTavernJade")),
        neoForgeDependencies = listOf("kaleidoscope_tavern"),
    ),
    // Both loaders: everything it touches is in the mod's own common module, which names neither. Only
    // the two 1.21.1 nodes set the key, since the mod has no build for a newer Minecraft version. The
    // Jade reader is a Fabric entrypoint; NeoForge finds it by its annotation. Jade reads it whether or
    // not Brewin' and Chewin' is installed, so it names none of its classes.
    // See src/main/brewinandchewin/AGENTS.md.
    Integration(
        dir = "brewinandchewin",
        depsKey = "deps.brewin_and_chewin",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        client = true,
        fabricEntrypoints = mapOf("jade" to listOf("com.thirstwastaken2.client.brewinandchewin.BrewinAndChewinJade")),
        mixinConfig = "thirstwastaken2.brewinandchewin.mixins.json",
        neoForgeDependencies = listOf("brewinandchewin"),
    ),
    // NeoForge only: Cold Sweat has no Fabric build and nothing past 1.21.1, so `1.21.1-neoforge` is the
    // one NeoForge node that sets the key; `coldsweatforge` below is its Forge 1.20.1 build.
    // See src/main/coldsweat/AGENTS.md.
    Integration(
        dir = "coldsweat",
        depsKey = "deps.cold_sweat",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.coldsweat.mixins.json",
        neoForgeDependencies = listOf("cold_sweat"),
    ),
    // NeoForge only: the vat and its drinks are 0.18, which has no Fabric build; the Fabric port stopped
    // at 0.17 and is not built against. Only `1.21.1-neoforge` sets the key.
    // See src/main/culturaldelights/AGENTS.md.
    Integration(
        dir = "culturaldelights",
        depsKey = "deps.cultural_delights",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.culturaldelights.mixins.json",
        neoForgeDependencies = listOf("culturaldelights"),
    ),
    // NeoForge only: the mod has no Fabric build and nothing past 1.21.1. Its mixins name their targets
    // by string, since one is in L2 Core, nested in its jar, so nothing compiles against it.
    // See src/main/fruitsdelight/AGENTS.md.
    Integration(
        dir = "fruitsdelight",
        depsKey = "deps.fruits_delight",
        loaders = setOf(Loader.NEOFORGE, Loader.FORGE),
        mixinConfig = "thirstwastaken2.fruitsdelight.mixins.json",
        neoForgeDependencies = listOf("fruitsdelight"),
    ),
    // NeoForge 1.21.1 and Forge 1.20.1, the mod's only builds: no Fabric build and nothing past 1.21.1.
    // Its mixins name their targets by string, one of them in the L2 Core nested in its jar, so nothing
    // compiles against it. See src/main/youkaisfeasts/AGENTS.md.
    Integration(
        dir = "youkaisfeasts",
        depsKey = "deps.youkais_feasts",
        loaders = setOf(Loader.NEOFORGE, Loader.FORGE),
        mixinConfig = "thirstwastaken2.youkaisfeasts.mixins.json",
        neoForgeDependencies = listOf("youkaisfeasts"),
    ),
    // NeoForge only: the mod has no Fabric build and nothing past 1.21.1. Its one mixin is on Farmer's
    // Delight's Cooking Pot recipe, named by string, so nothing compiles against either.
    // See src/main/expandeddelight/AGENTS.md.
    Integration(
        dir = "expandeddelight",
        depsKey = "deps.expanded_delight",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.expandeddelight.mixins.json",
        neoForgeDependencies = listOf("expandeddelight"),
    ),
    // NeoForge 1.21.1 and Forge 1.20.1, its only builds for a version this mod supports, which name the
    // mod `minersdelight` and `miners_delight`; both are listed, an optional dependency on an absent id
    // being harmless. Its one mixin names the copper cup's class, so the scripts compile against it.
    // See src/main/minersdelight/AGENTS.md.
    Integration(
        dir = "minersdelight",
        depsKey = "deps.miners_delight",
        loaders = setOf(Loader.NEOFORGE, Loader.FORGE),
        mixinConfig = "thirstwastaken2.minersdelight.mixins.json",
        neoForgeDependencies = listOf("minersdelight", "miners_delight"),
    ),
    // Both loaders and every node: Serene Seasons ships them all. No mixins; the calendar is read
    // through its API when the drain recomputes, and the entrypoint hands the drain a SeasonalClimate.
    // Fabric finds the entrypoint through `thirstwastaken2:integration`, NeoForge and Forge by its annotation.
    // See src/main/sereneseasons/AGENTS.md.
    Integration(
        dir = "sereneseasons",
        depsKey = "deps.serene_seasons",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE, Loader.FORGE),
        fabricEntrypoints = mapOf(
            "thirstwastaken2:integration" to listOf("com.thirstwastaken2.sereneseasons.SereneSeasonsEntrypoint"),
        ),
        neoForgeDependencies = listOf("sereneseasons"),
    ),
    // The same Cold Sweat integration on Forge 47, for Cold Sweat's 1.20.1 build: the same deps key, its
    // own directory, since the waterskin's tank branch, the presence check and the recipes name the
    // loader's fluid API, its mod list and its conditions. See src/main/coldsweatforge/AGENTS.md.
    Integration(
        dir = "coldsweatforge",
        depsKey = "deps.cold_sweat",
        loaders = setOf(Loader.FORGE),
        mixinConfig = "thirstwastaken2.coldsweatforge.mixins.json",
        neoForgeDependencies = listOf("cold_sweat"),
    ),
    // Both loaders: everything it touches is in Farm & Charm's Architectury common module, which names
    // neither. Only the two 1.21.1 nodes set the key; the mod's 1.20.1 line is no longer updated and
    // there is nothing newer. Candlelight, its addon, needs no row: its kitchen sinks are Farm & Charm's
    // sink block, and its cooking runs on Farm & Charm's recipes. See src/main/farmandcharm/AGENTS.md.
    Integration(
        dir = "farmandcharm",
        depsKey = "deps.farm_and_charm",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.farmandcharm.mixins.json",
        neoForgeDependencies = listOf("farm_and_charm"),
    ),
    // Let's Do: HerbalBrews: its Tea Kettle refuses sea water, and a placed Jug is drunk as the teas in
    // it. Written once in Architectury's common module like Farm & Charm, so one directory serves both
    // loaders, on the two 1.21.1 nodes only. See src/main/herbalbrews/AGENTS.md.
    Integration(
        dir = "herbalbrews",
        depsKey = "deps.herbalbrews",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.herbalbrews.mixins.json",
        neoForgeDependencies = listOf("herbalbrews"),
    ),
    // Let's Do: Beachparty: a placed cocktail's sips restore a third of the glass each. Same nodes and
    // shape as HerbalBrews. See src/main/beachparty/AGENTS.md.
    Integration(
        dir = "beachparty",
        depsKey = "deps.beachparty",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.beachparty.mixins.json",
        neoForgeDependencies = listOf("beachparty"),
    ),
    // Let's Do: Meadow: its wooden bucket keeps the grade, its wooden cauldron is standing water that
    // takes no sea water, and its Cooking Pot takes no sea water. Same nodes and shape as HerbalBrews.
    // See src/main/meadow/AGENTS.md.
    Integration(
        dir = "meadow",
        depsKey = "deps.meadow",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.meadow.mixins.json",
        neoForgeDependencies = listOf("meadow"),
    ),
    // Spelunkery: a cooking recipe that makes its salt bucket takes only sea water. Nothing of the mod is
    // compiled against; the mixin targets Minecraft's class. See src/main/spelunkery/AGENTS.md.
    Integration(
        dir = "spelunkery",
        depsKey = "deps.spelunkery",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.spelunkery.mixins.json",
        neoForgeDependencies = listOf("spelunkery"),
    ),
    // NeoForge only: Hearth and Harvest's newest build is NeoForge 1.21.1, and its tanks move water as
    // NeoForge fluid stacks. Its salt recipe is replaced by one in core's data, so this mod loads after it.
    // See src/main/hearthandharvest/AGENTS.md.
    Integration(
        dir = "hearthandharvest",
        depsKey = "deps.hearth_and_harvest",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.hearthandharvest.mixins.json",
        neoForgeDependencies = listOf("hearthandharvest"),
        loadAfter = setOf("hearthandharvest"),
    ),
    // NeoForge only: Extra Delight's only official build is NeoForge 1.21.1. Its jars, kegs, vat, mixing
    // bowl and tap move water as NeoForge fluid stacks. See src/main/extradelight/AGENTS.md.
    Integration(
        dir = "extradelight",
        depsKey = "deps.extra_delight",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.extradelight.mixins.json",
        neoForgeDependencies = listOf("extradelight"),
    ),
    // NeoForge only: No Man's Land's only build is NeoForge 1.21.1. Its milk cauldron's sips restore a
    // quarter of a milk bucket each. See src/main/nomansland/AGENTS.md.
    Integration(
        dir = "nomansland",
        depsKey = "deps.nomansland",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.nomansland.mixins.json",
        neoForgeDependencies = listOf("nomansland"),
    ),
    // Its 1.20.1 build on Forge 47: from 1.4.1 the 1.21.1 mod ported back, whose tanks get the same mixins
    // as on NeoForge; the salt recipe is a mixin too, since on Forge the mod's own file wins over a
    // replacement. 1.0.12c, the early mod, still gets its Jug and salt mixins.
    // See src/main/hearthandharvestforge/AGENTS.md.
    Integration(
        dir = "hearthandharvestforge",
        depsKey = "deps.hearth_and_harvest",
        loaders = setOf(Loader.FORGE),
        mixinConfig = "thirstwastaken2.hearthandharvestforge.mixins.json",
        neoForgeDependencies = listOf("hearthandharvest"),
    ),
    // Every loader: Croptopia's crafting recipes take no sea water. Nothing of the mod is compiled
    // against; the mixin targets Minecraft's crafting recipes. Croptopia has no build for 26.3 or 1.21.11,
    // so those nodes do not set the key. See src/main/croptopia/AGENTS.md.
    Integration(
        dir = "croptopia",
        depsKey = "deps.croptopia",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE, Loader.FORGE),
        mixinConfig = "thirstwastaken2.croptopia.mixins.json",
        neoForgeDependencies = listOf("croptopia"),
    ),
    // NeoForge only: Mekanism's newest build is NeoForge 1.21.1, and its tanks and pipes move water as
    // NeoForge fluid stacks. Its Electric Pump stamps what it pumps, and its rotary recipes distil.
    // See src/main/mekanism/AGENTS.md.
    Integration(
        dir = "mekanism",
        depsKey = "deps.mekanism",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.mekanism.mixins.json",
        neoForgeDependencies = listOf("mekanism"),
    ),
    // The same two mixins on Forge 47, for Mekanism's 1.20.1 build: the same deps key, its own directory,
    // since both name the loader's fluid stack. See src/main/mekanismforge/AGENTS.md.
    Integration(
        dir = "mekanismforge",
        depsKey = "deps.mekanism",
        loaders = setOf(Loader.FORGE),
        mixinConfig = "thirstwastaken2.mekanismforge.mixins.json",
        neoForgeDependencies = listOf("mekanism"),
    ),
    // NeoForge only: Immersive Engineering's newest build is NeoForge 1.21.1. Its Fluid Pump stamps what it
    // pumps, its Bottling Machine stamps the bottles it fills, and its fluid items keep grades apart.
    // See src/main/immersiveengineering/AGENTS.md.
    Integration(
        dir = "immersiveengineering",
        depsKey = "deps.immersive_engineering",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.immersiveengineering.mixins.json",
        neoForgeDependencies = listOf("immersiveengineering"),
    ),
    // The pump and Bottling Machine mixins on Forge 47, for its 1.20.1 build: the same deps key, its own
    // directory, since both name the loader's fluid stack. See src/main/immersiveengineeringforge/AGENTS.md.
    Integration(
        dir = "immersiveengineeringforge",
        depsKey = "deps.immersive_engineering",
        loaders = setOf(Loader.FORGE),
        mixinConfig = "thirstwastaken2.immersiveengineeringforge.mixins.json",
        neoForgeDependencies = listOf("immersiveengineering"),
    ),
    // Both loaders: everything it touches is in the mod's common module, which names neither. Only the two
    // 1.21.1 nodes set the key, since its 1.20.x lines stopped in 2024 and it has no newer build. Its mixins
    // name the cup, the Fermentation Vessel and Minecraft's two crafting recipes.
    // See src/main/bountifulfares/AGENTS.md.
    Integration(
        dir = "bountifulfares",
        depsKey = "deps.bountiful_fares",
        loaders = setOf(Loader.FABRIC, Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.bountifulfares.mixins.json",
        neoForgeDependencies = listOf("bountifulfares"),
    ),
    // NeoForge only: Little Logistics' newest build is NeoForge 1.21.1. A bucket clicked on its Fluid Tank
    // Car or Fluid Barge keeps the grade of the water. See src/main/littlelogistics/AGENTS.md.
    Integration(
        dir = "littlelogistics",
        depsKey = "deps.little_logistics",
        loaders = setOf(Loader.NEOFORGE),
        mixinConfig = "thirstwastaken2.littlelogistics.mixins.json",
        neoForgeDependencies = listOf("littlelogistics"),
    ),
    // The same on Forge 47, for its 1.20.1 build, which adds the Fluid Hopper: the same deps key, its own
    // directory, since the mixins name the loader's fluid handler. See src/main/littlelogisticsforge/AGENTS.md.
    Integration(
        dir = "littlelogisticsforge",
        depsKey = "deps.little_logistics",
        loaders = setOf(Loader.FORGE),
        mixinConfig = "thirstwastaken2.littlelogisticsforge.mixins.json",
        neoForgeDependencies = listOf("littlelogistics"),
    ),
)

/**
 * The integrations a node builds: those its loader may compile whose deps key the node sets.
 * [isSet] is the node's `findProperty(key) != null`.
 */
fun integrationsFor(loader: Loader, isSet: (String) -> Boolean): List<Integration> =
    integrations.filter { loader in it.loaders && isSet(it.depsKey) }
