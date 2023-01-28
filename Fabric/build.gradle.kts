plugins {
    idea
    `maven-publish`
    id("fabric-loom") version("1.0-SNAPSHOT")
    id("io.github.juuxel.loom-quiltflower") version("1.8.0")
}

val minecraft = project.property("minecraft.version").toString()
val fabricLoader = project.property("fabric.loader.version").toString()
val fabricApi = project.property("fabric.api.version").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()
val modDescription = project.property("mod.description").toString()
val fabricModules = project.property("fabric.api.modules").toString().split(',')
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
    implementation(project(":Common", "namedElements"))

    fabricModules.forEach {
        modImplementation(fapi.module(it, fabricApi))
    }
    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricApi")
    modRuntimeOnly("lol.bai:badpackets:fabric-${badpackets}")

    "modTestImplementation"(fapi.module("fabric-gametest-api-v1", fabricApi))
    testImplementation(project.project(":Common").sourceSets.test.get().output)
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
            version = rootProject.version.toString()

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

for (configuration in listOf(configurations.apiElements, configurations.runtimeElements)) {
    configuration.get().artifacts.removeIf {
        it.file.absolutePath.equals(tasks.jar.get().archiveFile.get().asFile.absolutePath) && it.buildDependencies.getDependencies(null).contains(tasks.jar.get())
    }
}
