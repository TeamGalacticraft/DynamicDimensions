pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
        }
        maven("https://repo.spongepowered.org/repository/maven-public") {
            name = "Sponge Snapshots"
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
