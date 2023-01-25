plugins {
    idea
    id("fabric-loom") version "1.0-SNAPSHOT"
    id("net.galacticraft.internal.maven") version("1.0.0")
}

val buildNumber = System.getenv("BUILD_NUMBER") ?: ""
val snapshot = (System.getenv("SNAPSHOT") ?: "false") == "true"
val prerelease = (System.getenv("PRE_RELEASE") ?: "false") == "true"

val minecraft = project.property("minecraft.version").toString()
val fabricLoader = project.property("fabric.loader.version").toString()
val fabricApi = project.property("fabric.api.version").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()
val modDescription = project.property("mod.description").toString()
val fabricModules = project.property("fabric.modules").toString().split(',')
val badpackets = project.property("badpackets.version").toString()

val baseArchiveName = "${modId}-fabric"

base {
    archivesName.set(baseArchiveName)
}

loom {
    accessWidenerPath.set(project(":Common").file("${modId}.accesswidener"))
    createRemapConfigurations(sourceSets.test.get())

    runs {
        named("client") {
            client()
            name("Fabric Client")
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            name("Fabric Server")
            ideConfigGenerated(true)
            runDir("run")
        }
        create("gametest") {
            server()
            name("Fabric Gametest")
            source(sourceSets.test.get())
            ideConfigGenerated(true)
            vmArgs("-ea", "-Dfabric-api.gametest", "-Dfabric-api.gametest.report-file=${project.buildDir}/junit.xml")
        }
    }

    mixin {
        add(project(":Common").sourceSets.main.get(), "${modId}.refmap.json")
        add(sourceSets.main.get(), "${baseArchiveName}.refmap.json")
    }

    mods {
        create("dyndims") {
            sourceSet(sourceSets.main.get())
        }
        create("dyndims_test") {
            sourceSet(sourceSets.test.get())
        }
    }
}

dependencies {
    val fapi = project.extensions.getByName<net.fabricmc.loom.configuration.FabricApiExtension>("fabricApi")
    minecraft("com.mojang:minecraft:${minecraft}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${fabricLoader}")

    fabricModules.forEach {
        modImplementation(fapi.module(it, fabricApi))
    }

    "modTestImplementation"(fapi.module("fabric-gametest-api-v1", fabricApi))

    testCompileOnly(project.project(":Common").sourceSets.test.get().output)
    testRuntimeOnly(modRuntimeOnly("lol.bai:badpackets:fabric-${badpackets}") {
        isTransitive = false
    })

    testImplementation(implementation(project(":Common", "namedElements"))!!)
}

tasks.compileTestJava {
    source(project(":Common").sourceSets.test.get().allSource)
}

tasks.processTestResources {
    from(project(":Common").sourceSets.test.get().resources)
}

tasks.withType<ProcessResources> {
    from(project(":Common").sourceSets.main.get().resources)
    from(loom.accessWidenerPath)

    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(
            "mod_version" to project.version,
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_description" to modDescription
        )
    }

    filesMatching("*.mixins.json") {
        expand("mapping" to baseArchiveName)
    }
}

tasks.compileJava {
    source(project(":Common").sourceSets.main.get().allSource)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${modName}" }
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
