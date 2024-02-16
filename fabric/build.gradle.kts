plugins {
    id("fabric-loom")
}

val modId = project.property("mod.id").toString()
val minecraft = project.property("minecraft.version").toString()
val parchment = project.property("parchment.version").toString()
val fabricLoader = project.property("fabric.loader.version").toString()
val fabricAPI = project.property("fabric.api.version").toString()
val fabricModules = project.property("fabric.api.modules").toString().split(',')
val badpackets = project.property("badpackets.version").toString()

loom {
    accessWidenerPath.set(project.file("src/main/resources/$modId.accesswidener"))
    // disable Minecraft-altering loom features, so that we can have one less copy of Minecraft
    interfaceInjection.enableDependencyInterfaceInjection.set(false)
    interfaceInjection.getIsEnabled().set(false)
    enableTransitiveAccessWideners.set(false)

    createRemapConfigurations(sourceSets.test.get())

    runs {
        named("client") {
            client()
            name("Fabric: Client")
        }
        named("server") {
            server()
            name("Fabric: Server")
        }
        create("gametest") {
            server()
            name("Fabric: GameTest")
            source(sourceSets.test.get())
            property("fabric-api.gametest")
            vmArgs("-ea")
        }

        configureEach {
            runDir("run")
            // copy neogradle naming format
            appendProjectPathToConfigName.set(false)
            ideConfigGenerated(true)
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
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(if (parchment.isBlank()) loom.officialMojangMappings() else loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-$parchment@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:$fabricLoader")
    compileOnly(project(":common", "namedElements"))

    fabricModules.forEach {
        modImplementation(fabricApi.module(it, fabricAPI))
    }
    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricAPI")
    modRuntimeOnly("lol.bai:badpackets:fabric-$badpackets")

    "modTestImplementation"(fabricApi.module("fabric-gametest-api-v1", fabricAPI))
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

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)
}
