pluginManagement {
    // The build code both loader scripts and stonecutter.gradle.kts share: the integration table, the
    // `-PwithoutOptional` bookkeeping, the JFR arguments. Data and pure functions only, never Loom or
    // ModDevGradle; see build-logic/settings.gradle.kts. A script puts it on its classpath with
    // `id("thirstwastaken2.build-logic")`.
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    }
}

plugins {
    // One source tree, one jar per Minecraft version. See the root AGENTS.md.
    id("dev.kikugie.stonecutter") version "0.10"
    // 26.1 dropped obfuscation, which changed how Loom consumes mod dependencies. This applies the
    // Loom variant each version needs and keeps `modImplementation` meaning the same thing on both.
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    // NeoForge's build plugin, for the one NeoForge node. Resolved here and applied only by
    // build.neoforge.gradle.kts: applying it to a Fabric node would put a second Minecraft provider
    // on a project Loom already owns.
    id("net.neoforged.moddev") version "2.0.148" apply false
    // The same plugin family for MinecraftForge, for the 1.20.1 Forge node, applied only by
    // build.forge.gradle.kts. It remaps Minecraft and mod dependencies to Mojang's names for development
    // and the jar back to SRG names for players.
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
    // Provisions the JDK a version needs when it is not installed locally: 26.1+ wants Java 25,
    // 1.21.x wants Java 21.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        version("1.20.1", "1.20.1")
        // MinecraftForge 47, not NeoForge: nearly every 1.20.1 modpack runs Forge, and NeoForge 47.1
        // is no longer maintained. See docs/dev/VERSION-DIFFERENCES.md.
        version("1.20.1-forge", "1.20.1").buildscript = "build.forge.gradle.kts"
        versions("1.21.1", "1.21.11")
        // NeoForge on 1.21.1 only: 1.21 is a separate NeoForge generation (21.0), unlike on Fabric.
        // 1.21.11 is Fabric only: its NeoForge node was dropped, as few NeoForge packs run it.
        version("1.21.1-neoforge", "1.21.1").buildscript = "build.neoforge.gradle.kts"
        version("26.1.x", "26.1.2")
        version("26.1.x-neoforge", "26.1.2").buildscript = "build.neoforge.gradle.kts"
        version("26.2.x", "26.2")
        // The NeoForge node: the same Minecraft version as 26.2.x, its own buildscript, and a name
        // of its own so the Fabric nodes keep theirs. `sc.current.version` is "26.2" on both, so
        // they share src/main/generated/26.2.
        version("26.2.x-neoforge", "26.2").buildscript = "build.neoforge.gradle.kts"
        version("26.3.x", "26.3")
        version("26.3.x-neoforge", "26.3").buildscript = "build.neoforge.gradle.kts"
        vcsVersion = "26.3.x"
    }
}

rootProject.name = "ThirstWasTaken2"
