pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
        }
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForge"
        }
    }
}

rootProject.name = "DynamicDimensions"
include("common")
include("fabric")
include("neoforge")
project(":common").name = "common"
project(":fabric").name = "fabric"
project(":neoforge").name = "neoforge"
