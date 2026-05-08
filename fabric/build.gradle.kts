val modId = project.property("mod.id").toString()
val minecraft = project.property("minecraft.version").toString()
val yarn = project.property("fabric.yarn.build").toString()
val fabricLoader = project.property("fabric.loader.version").toString()
val fabricAPI = project.property("fabric.api.version").toString()
val fabricModules = project.property("fabric.api.modules").toString().split(',')
val badpackets = project.property("badpackets.version").toString()
val sable = project.property("sable.version").toString()
val veil = project.property("veil.version").toString()

plugins {
    id("fabric-loom")
    id("dev.galacticraft.mojarn")
}

loom {
    // configure access widener
    if (project(":fabric").file("src/main/resources/${modId}.accesswidener").exists()) {
        accessWidenerPath.set(project(":fabric").file("src/main/resources/${modId}.accesswidener"))
    }

    // disable Minecraft-altering loom features, so that we can have one less copy of Minecraft
    interfaceInjection.enableDependencyInterfaceInjection.set(false)
    interfaceInjection.getIsEnabled().set(false)
    enableTransitiveAccessWideners.set(false)

    mixin {
        defaultRefmapName.set("dynamicdimensions.refmap.json")
    }

    runs {
        named("client") {
            client()
            name("Client")
        }
        named("server") {
            server()
            name("Server")
        }
        create("gametest") {
            server()
            name("GameTest")
            property("fabric-api.gametest")
            vmArgs("-ea")
        }

        configureEach {
            runDir("run")
            ideConfigGenerated(true)
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(mojarn.mappings("net.fabricmc:yarn:$minecraft+build.$yarn:v2"))
    modImplementation("net.fabricmc:fabric-loader:$fabricLoader")
    compileOnly(project(":common", "namedElements"))

    fabricModules.forEach {
        modImplementation(fabricApi.module(it, fabricAPI))
    }
    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricAPI")
    modRuntimeOnly("lol.bai:badpackets:fabric-$badpackets")
    compileOnly("dev.ryanhcode.sable:sable-common-$minecraft:$sable")
}

tasks.compileJava {
    source(project(":common").sourceSets.main.get().java)
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)
}

tasks.javadoc {
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.validateAccessWidener {
    enabled = false // access widener validated by :common
}
