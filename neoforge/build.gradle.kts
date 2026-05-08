val modId = project.property("mod.id").toString()
val modName = project.property("mod.name").toString()
val modVersion = project.property("mod.version").toString()
val modDescription = project.property("mod.description").toString()
val modLicense = project.property("mod.license").toString()

val minecraft = project.property("minecraft.version").toString()
val neoforge = project.property("neoforge.version").toString()
val parchmentMc = project.property("parchment.mc").toString()
val parchmentVersion = project.property("parchment.version").toString()

val badpackets = project.property("badpackets.version").toString()

val sable = project.property("sable.version").toString()

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev")
    idea
}

neoForge {
    // Specify the version of NeoForge to use.
    version = neoforge

    parchment {
        minecraftVersion = parchmentMc
        mappingsVersion = parchmentVersion
    }
    
    runs {
        register("client") {
            client()
        }

        register("server") {
            server()
            programArgument("--nogui")
        }

        register("gameTest") {
            type = "gameTestServer"
        }

        configureEach {
            ideName = "${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} (${project.path})"
            // minecraft because forge patches @GameTest for the filtering... and common cannot implement the patch
            systemProperty("neoforge.enabledGameTestNamespaces", "minecraft")
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }
    

    mods {
        register("modId") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly(project(":common", "namedElements"))
    runtimeOnly("lol.bai:badpackets:neo-$badpackets")
    compileOnly("dev.ryanhcode.sable:sable-neoforge-$minecraft:$sable")
}

tasks.compileJava {
    source(project(":common").sourceSets.main.get().java)
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)

    // remove refmap on neoforge
    doLast {
        listOf("dynamicdimensions.mixins.json", "dynamicdimensions.sable.mixins.json").forEach { configName ->
            val configFile = outputs.files.asFileTree.find { it.name == configName }
            if (configFile != null) {
                val parse = groovy.json.JsonSlurper().parse(configFile)!! as MutableMap<*, *>
                parse.remove("refmap")
                configFile.writeText(groovy.json.JsonOutput.toJson(parse))
            }
        }
    }
}

tasks.javadoc {
    source(project(":common").sourceSets.main.get().allJava)
}