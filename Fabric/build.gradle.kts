plugins {
    idea
    `maven-publish`
    id("fabric-loom") version "0.13-SNAPSHOT"
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

val baseArchiveName = "${modId}-fabric"

base {
    archivesName.set(baseArchiveName)
}

sourceSets {
    create("gametest") {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

loom {
    splitEnvironmentSourceSets()

    runtimeOnlyLog4j.set(true)
    accessWidenerPath.set(project.file("src/main/resources/${modId}.accesswidener"))

    createRemapConfigurations(sourceSets.getByName("gametest"))

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("run")
        }
        create("gametest") {
            name("Gametest")
            server()
            source(sourceSets.getByName("gametest"))
            ideConfigGenerated(true)
            vmArgs("-ea", "-Dfabric-api.gametest", "-Dfabric-api.gametest.report-file=${project.buildDir}/junit.xml")
        }
    }

    mixin {
        add(project(":Common").sourceSets.main.get(), "${modId}-common.refmap.json")
        add(sourceSets.main.get(), "${modId}-fabric.refmap.json")
    }

    mods {
        create("dyndims") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
        create("dyndims-gametest") {
            sourceSet(sourceSets.getByName("gametest"))
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
    "modGametestImplementation"(fapi.module("fabric-gametest-api-v1", fabricApi))

    implementation(project(":Common", "namedElements"))
}

tasks.processResources {
    from(project(":Common").sourceSets.main.get().resources)
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(
            "mod_version" to project.version,
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_description" to modDescription
        )
    }

    filesMatching("${modId}.mixins.json") {
        expand("mod_id" to modId)
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

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = group.toString()
            artifactId = baseArchiveName
            version = buildString {
                append(modVersion)
                if (snapshot) {
                    append("-SNAPSHOT")
                } else {
                    if (buildNumber.isNotBlank()) {
                        append("+")
                        append(buildNumber)
                    }
                }
            }

            from(components["java"])

            pom {
                name.set(modName)
                inceptionYear.set("2021")

                organization {
                    name.set("Team Galacticraft")
                    url.set("https://github.com/TeamGalacticraft")
                }

                scm {
                    url.set("https://github.com/TeamGalacticraft/DynamicDimensions")
                    connection.set("scm:git:git://github.com/TeamGalacticraft/DynamicDimensions.git")
                    developerConnection.set("scm:git:git@github.com:TeamGalacticraft/DynamicDimensions.git")
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/TeamGalacticraft/DynamicDimensions/issues")
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/TeamGalacticraft/DynamicDimensions/blob/main/LICENSE")
                    }
                }
            }
        }
    }

    repositories {
        if (System.getenv().containsKey("NEXUS_REPOSITORY_URL")) {
            maven(System.getenv("NEXUS_REPOSITORY_URL")!!) {
                credentials {
                    username = System.getenv("NEXUS_USER")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
        }
    }
}
