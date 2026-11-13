import com.thirstwastaken2.buildlogic.Loader
import com.thirstwastaken2.buildlogic.loaderOf
import com.thirstwastaken2.buildlogic.minecraftOf
import com.thirstwastaken2.buildlogic.parseWithoutOptional

plugins {
    id("dev.kikugie.stonecutter")
    // parseWithoutOptional, below. See settings.gradle.kts.
    id("thirstwastaken2.build-logic")
}

// The version the source tree is currently checked out for. Switch it with
// `./gradlew "Set active project to <name>"`; every version is built regardless by `chiseledBuild`.
stonecutter active "26.3.x"


stonecutter parameters {
    // Every node is `<version>` on Fabric or `<version>-<loader>` on another loader, `26.2.x-neoforge`.
    // Tagging both parts lets stonecutter.properties.toml keep what the loaders share in `["26.2.x"]` and
    // the rest in `[fabric."26.2.x"]` or `[neoforge."26.2.x"]`; see the top of that file.
    properties {
        tags(minecraftOf(current.project), loaderOf(current.project).id)
    }

    // Stonecutter 0.10 reads every file in a source directory, Markdown included, and applies the
    // replacements below to the ones without comment syntax of its own. The AGENTS.md files there name
    // both spellings of a rename on purpose, so it leaves them alone.
    files.exclude("**/*.md")

    // Bakes the target Minecraft version into the jar, so the startup log line is never stale.
    swaps["minecraft"] = "\"${current.version}\";"

    // Every replacement also runs backwards: on each node whose condition is false, and so on the sources
    // themselves when "Reset active project" brings them back from another node. Two rules keep that a
    // round trip, which `git diff` after Set and Reset active project shows:
    //
    // - What a rule writes is the same on every node it applies to. A value chosen per node, such as
    //   `critereon` before 1.21.11, is a rule of its own per value, or the backward pass, which knows
    //   only the node it is going to, looks for the wrong text.
    // - What a rule writes cannot occur in the sources for any other reason, since the backward pass
    //   rewrites every occurrence. Where the older spelling is ordinary code, it is written in a form
    //   nothing else uses (see InteractionResult.CONSUME below).
    //
    // All string rules are matched together, leftmost and then longest first, so a rule for
    // `Identifier.parse(` wins over the one for `Identifier` at the same place.
    replacements {
        // 1.21.11 renamed ResourceLocation to Identifier, ResourceKey#location to #identifier, and
        // moved Util into net.minecraft.util, changing nothing else about any of them.
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
            replace(".identifier()", ".location()")
            replace("net.minecraft.util.Util", "net.minecraft.Util")
        }
        // 1.21 made ResourceLocation's constructors private behind factory methods of the same meaning.
        string(current.parsed >= "1.21" && current.parsed < "1.21.11") {
            replace("Identifier.fromNamespaceAndPath(", "ResourceLocation.fromNamespaceAndPath(")
            replace("Identifier.withDefaultNamespace(", "ResourceLocation.withDefaultNamespace(")
            replace("Identifier.parse(", "ResourceLocation.parse(")
        }
        // Before 1.21 all three are constructors, spelled apart so each finds its way back: two
        // arguments; `minecraft` named outright; and one string, cast to say so.
        string(current.parsed < "1.21") {
            replace("Identifier.fromNamespaceAndPath(", "new ResourceLocation(")
            replace("Identifier.withDefaultNamespace(", "new ResourceLocation(\"minecraft\", ")
            replace("Identifier.parse(", "new ResourceLocation((String) ")
        }

        // 26.1 renamed the HUD draw target while keeping the drawing methods identical but one, so
        // the whole difference is two names.
        string(current.parsed < "26.1") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            // The one: its item icon method lost its prefix in the same rename.
            replace(".fakeItem(", ".renderFakeItem(")
            // The Fabric data generation API renamed both of these for 26.1 without changing what
            // they do, so the datagen providers name the newer pair and get the older one here.
            replace("FabricPackOutput", "FabricDataOutput")
            replace("FabricTagsProvider", "FabricTagProvider")
        }

        // 26.2 split the advancement trigger classes out of `net.minecraft.advancements` into
        // `triggers` and `predicates`, keeping every class name. Only the datagen providers name
        // them, and only in imports, so each one is replaced whole rather than by package prefix:
        // `CriteriaTriggers` and `Criterion` did not move into `criterion` with the rest.
        // The package they came from was spelled `critereon` until 1.21.11, so the five that moved
        // have a rule per spelling.
        string(current.parsed < "26.2") {
            replace("net.minecraft.advancements.triggers.CriteriaTriggers",
                    "net.minecraft.advancements.CriteriaTriggers")
            // With the semicolon, so the older side cannot match CriterionTriggerInstance, which stayed.
            replace("net.minecraft.advancements.triggers.Criterion;",
                    "net.minecraft.advancements.Criterion;")
        }
        val movedTriggers = listOf("InventoryChangeTrigger", "ImpossibleTrigger", "PlayerTrigger",
            "RecipeCraftedTrigger", "RecipeUnlockedTrigger")
        string(current.parsed >= "1.21.11" && current.parsed < "26.2") {
            movedTriggers.forEach { replace("net.minecraft.advancements.triggers.$it", "net.minecraft.advancements.criterion.$it") }
        }
        string(current.parsed < "1.21.11") {
            movedTriggers.forEach { replace("net.minecraft.advancements.triggers.$it", "net.minecraft.advancements.critereon.$it") }
        }

        // 26.3 renamed every PushReaction constant and split LootPoolSingletonContainer into three
        // classes, of which UniformContainerBase is the one the entry builders are typed on. Neither
        // changed what the name means, and both keep the package they were in.
        string(current.parsed < "26.3") {
            replace("PushReaction.POPPED", "PushReaction.DESTROY")
            replace("UniformContainerBase", "LootPoolSingletonContainer")
            // 26.3 moved the renderer's pipeline type out of Blaze3D into Renderpearl, keeping the
            // class name. The dev HUD mixin names it twice: once as an import, once inside the
            // descriptor of the method it injects into.
            replace("com.mojang.renderpearl.api.pipeline.RenderPipeline", "com.mojang.blaze3d.pipeline.RenderPipeline")
            replace("Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;", "Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
        }

        // 1.21.2 renamed the server-side CONSUME result to SUCCESS_SERVER, both meaning "done, and the
        // client already swung", and Registry#get(ResourceKey) to getValue.
        //
        // The older side is written fully qualified. A replacement also runs backwards, on every node its
        // condition is false for and on "Reset active project", and CONSUME still exists from 1.21.2 with a
        // meaning of its own (no swing, which WaterskinItem wants on every version). A plain
        // `InteractionResult.CONSUME` would be turned into SUCCESS_SERVER there; only this spelling is.
        string(current.parsed < "1.21.2") {
            replace("InteractionResult.SUCCESS_SERVER", "net.minecraft.world.InteractionResult.CONSUME")
            replace("CREATIVE_MODE_TAB.getValue(", "CREATIVE_MODE_TAB.get(")
        }

        // 1.21.4 moved the data generator's model classes under net.minecraft.client, unchanged, and
        // Fabric API moved its model provider into its client package with them.
        string(current.parsed < "1.21.4") {
            replace("net.minecraft.client.data.models.", "net.minecraft.data.models.")
            replace("net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider",
                    "net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider")
        }

        // 1.21.5 renamed the Confusion effect to Nausea and Entity#moveTo to snapTo.
        string(current.parsed < "1.21.5") {
            replace("MobEffects.NAUSEA", "MobEffects.CONFUSION")
            replace(".snapTo(", ".moveTo(")
        }

        // The NeoForge and Forge nodes run the same tests through a harness of their own, which reads a
        // @GameTest annotation from src/gametest/<loader> instead of Fabric API's. Only the import
        // changes, so a test keeps writing `@GameTest` with no arguments. See src/gametest/java/AGENTS.md.
        // That harness sets the template itself on every Minecraft version, so the rule below is for
        // Fabric only. A rule per loader, so each harness's import finds its way back.
        val loader = loaderOf(current.project)
        Loader.entries.filter { it != Loader.FABRIC }.forEach { harness ->
            string(loader == harness) {
                replace("import net.fabricmc.fabric.api.gametest.v1.GameTest;",
                        "import com.thirstwastaken2.gametest.${harness.id}.GameTest;")
            }
        }

        // Fabric API's own @GameTest arrived with 1.21.5. Before it a test uses vanilla's annotation and
        // names Fabric's empty structure as its template; no test body changes.
        string(loader == Loader.FABRIC && current.parsed < "1.21.5") {
            replace("import net.fabricmc.fabric.api.gametest.v1.GameTest;",
                    "import net.minecraft.gametest.framework.GameTest;")
            replace("@GameTest",
                    "@GameTest(template = net.fabricmc.fabric.api.gametest.v1.FabricGameTest.EMPTY_STRUCTURE)")
        }
    }
}

// `-PwithoutOptional=<name,...>` leaves optional mods out of a node's dev clients (build.gradle.kts and
// build.neoforge.gradle.kts). Every node sees the flag, and most names only mean something on some
// nodes, Sophisticated Core on NeoForge for one, so a node quietly ignores a name it does not load. A
// name no node loads is a typo, and would leave the mod in the run while claiming to test without it.
gradle.projectsEvaluated {
    val asked = parseWithoutOptional(providers.gradleProperty("withoutOptional").orNull)
    if (asked.isEmpty()) return@projectsEvaluated
    val known = rootProject.subprojects.flatMap { node ->
        (node.extensions.extraProperties.properties["thirst.optionalRunMods"] as Set<*>?).orEmpty().map(Any?::toString)
    }.toSet()
    val unknown = asked - known
    if (unknown.isNotEmpty()) {
        throw GradleException("-PwithoutOptional names $unknown, which no node's runClient loads. " +
            "It takes: ${known.sorted().joinToString()}")
    }
}
