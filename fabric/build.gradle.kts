plugins {
    idea
    id("fabric-loom")
}

val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()
val modDescription = project.property("mod.description").toString()

val minecraft = project.property("minecraft.version").toString()
val fabricLoader = project.property("fabric.loader.version").toString()
val fabricApi = project.property("fabric.api.version").toString()
val fabricModules = project.property("fabric.api.modules").toString().split(',')
val badpackets = project.property("badpackets.version").toString()

base.archivesName.set("${modId}-fabric")

loom {
    accessWidenerPath.set(project.file("src/main/resources/${modId}.accesswidener"))
    createRemapConfigurations(sourceSets.test.get())

    runs {
        named("client") {
            client()
            name("Fabric Client")
            ideConfigGenerated(true)
            runDir(rootProject.file("run").toString())
        }
        named("server") {
            server()
            name("Fabric Server")
            ideConfigGenerated(true)
            runDir(rootProject.file("run").toString())
        }
        create("gametest") {
            server()
            name("Fabric Gametest")
            source(sourceSets.test.get())
            ideConfigGenerated(true)
            property("fabric-api.gametest")
            vmArgs("-ea")
        }
    }

    mods {
        create("dynamicdimensions") {
            sourceSet(sourceSets.main.get())
        }
        create("dynamicdimensions_test") {
            sourceSet(sourceSets.test.get())
        }
    }
}

dependencies {
    val fapi = project.extensions.getByName<net.fabricmc.loom.configuration.FabricApiExtension>("fabricApi")
    minecraft("com.mojang:minecraft:${minecraft}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${fabricLoader}")
    compileOnly(project(":common", "namedElements"))

    fabricModules.forEach {
        modImplementation(fapi.module(it, fabricApi))
    }
    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricApi")
    modRuntimeOnly("lol.bai:badpackets:fabric-${badpackets}")

    "modTestImplementation"(fapi.module("fabric-gametest-api-v1", fabricApi))
    testImplementation(project.project(":common").sourceSets.test.get().output)
}

tasks.compileJava {
    source(project(":common").sourceSets.main.get().java)
}

tasks.javadoc {
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.sourcesJar {
    from(project(":common").sourceSets.main.get().allSource)
}

tasks.processTestResources {
    from(project(":common").sourceSets.test.get().resources)
}

tasks.withType<ProcessResources> {
    from(project(":common").sourceSets.main.get().resources)

    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(
                "mod_version" to project.version,
                "mod_id" to modId,
                "mod_name" to modName,
                "mod_description" to modDescription
        )
    }
}

for (configuration in listOf(configurations.apiElements, configurations.runtimeElements)) {
    configuration.get().artifacts.removeIf {
        it.file.absolutePath.equals(tasks.jar.get().archiveFile.get().asFile.absolutePath) && it.buildDependencies.getDependencies(null).contains(tasks.jar.get())
    }
}
