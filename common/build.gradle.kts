plugins {
    java
    id("fabric-loom")
}

val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()
val minecraft = project.property("minecraft.version").toString()
val fabricLoader = project.property("fabric.loader.version").toString()

base.archivesName.set("${modId}-common")

loom {
    runtimeOnlyLog4j.set(true)
    accessWidenerPath.set(project(":fabric").file("src/main/resources/${modId}.accesswidener"))

    mixin {
        useLegacyMixinAp.set(false)
    }
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge Snapshots"
        content {
            includeGroup("org.spongepowered")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())

    // loom expects some loader classes to exist
    compileOnly("net.fabricmc:fabric-loader:${fabricLoader}")
    testCompileOnly(compileOnly("org.spongepowered:mixin:0.8.5")!!)
}

tasks.processResources {
    filesMatching("pack.mcmeta") {
        expand(
                "mod_version" to project.version,
                "mod_id" to modId,
                "mod_name" to modName
        )
    }
}

tasks.withType<net.fabricmc.loom.task.AbstractRemapJarTask> {
    targetNamespace.set("named")
}
