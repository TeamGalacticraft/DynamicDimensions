pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.terradevelopment.net/repository/maven-releases") {
            // https://maven.galacticraft.net/repository/maven-releases
            name = "Galacticraft"
            content {
                includeGroup("dev.galacticraft")
                includeGroup("dev.galacticraft.mojarn")
            }
        }
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
            content {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
            }
        }
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForge"
        }
    }
}

plugins {
    // required by neoforge
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.9.0")
}

rootProject.name = "DynamicDimensions"

include("common")
include("fabric")
include("neoforge")
project(":common").name = "common"
project(":fabric").name = "fabric"
project(":neoforge").name = "neoforge"
