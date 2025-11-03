pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Include the local Mojarn build, clone mojarn project and build the classes!
        includeBuild("../mojarn") {
            name = "mojarn"
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
