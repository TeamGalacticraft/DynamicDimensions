plugins {
    java
    id("fabric-loom") version("1.0-SNAPSHOT")
    id("net.galacticraft.internal.maven") version("1.0.0")
}

val buildNumber = System.getenv("BUILD_NUMBER") ?: ""
val snapshot = (System.getenv("SNAPSHOT") ?: "false") == "true"
val prerelease = (System.getenv("PRE_RELEASE") ?: "false") == "true"

val minecraft = project.property("minecraft.version").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()

val badpackets = project.property("badpackets.version").toString()

val baseArchiveName = "${modId}-common"

base {
    archivesName.set(baseArchiveName)
}

loom {
    runtimeOnlyLog4j.set(true)
    accessWidenerPath.set(project.file("${modId}.accesswidener"))

    mixin {
        useLegacyMixinAp.set(false)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft}")
    mappings(loom.officialMojangMappings())

    testCompileOnly(compileOnly("org.spongepowered:mixin:0.8.5")!!)
    api("lol.bai:badpackets:mojmap-${badpackets}")
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

maven {
    artifactId.set(baseArchiveName)
    version.set(buildString {
        append(modVersion)
        if (snapshot) {
            append("-SNAPSHOT")
        } else {
            if (buildNumber.isNotBlank()) {
                append("+")
                append(buildNumber)
            }
        }
    })
}

tasks.create("prepareWorkspace") {
    // no-op: for some reason this gets called with IJ gradle refresh... MCDev?
}

// disable remapping `fabric.loom.dontRemap` can only be set on the root project
tasks.create("copyNamedJar", Copy::class).apply {
    from(layout.buildDirectory.file("devlibs/${base.archivesName.get()}-${project.version}-dev.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename("-dev", "")
    dependsOn("jar")
}

tasks.create("copyNamedSourcesJar", Copy::class).apply {
    from(layout.buildDirectory.file("devlibs/${base.archivesName.get()}-${project.version}-sources.jar"))
    into(layout.buildDirectory.dir("libs"))
    dependsOn("sourcesJar")
}

tasks.prepareRemapJar {
    enabled = false
}

tasks.remapSourcesJar {
    enabled = false
}

tasks.forEach {
    var jar = false;
    var sources = false;
    it.dependsOn.forEach { dep ->
        if (dep is Task) {
            if (dep.name == "remapJar") {
                jar = true;
            } else if (it.name == "remapSourcesJar") {
                sources = true
            }
        }
    }
    if (jar) it.dependsOn.add("copyNamedJar")
    if (sources) it.dependsOn.add("copyNamedSourcesJar")
}
