plugins {
    java
    eclipse
    id("net.minecraftforge.gradle") version("5.1.+")
    id("org.spongepowered.mixin") version("0.7-SNAPSHOT")
    `maven-publish`
}

val minecraft = project.property("minecraft.version").toString()
val forge = project.property("forge.version").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()
val modDescription = project.property("mod.description").toString()
val badpackets = project.property("badpackets.version").toString()

val baseArchiveName = "${modId}-forge"

base {
    archivesName.set(baseArchiveName)
}

mixin {
    add(sourceSets.main.get(), "${baseArchiveName}.refmap.json")

    config("${modId}.mixins.json")
}

minecraft {
    mappings("official", minecraft)

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            args("-mixin.config=${modId}.mixins.json")
            ideaModule("${rootProject.name}.${project.name}.main")
            taskName("runClient")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                    source(project(":Common").sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run"))
            args("-mixin.config=${modId}.mixins.json")
            ideaModule("${rootProject.name}.${project.name}.main")
            taskName("runServer")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                    source(project(":Common").sourceSets.main.get())
                }
            }
        }

        create("gameTestServer") { // name must match exactly for options to be applied, apparently
            workingDirectory(project.file("run"))
            args("-mixin.config=${modId}.mixins.json", "-mixin.config=dynamicdimensions_test.mixins.json")
            ideaModule("${rootProject.name}.${project.name}.test")
            taskName("runGametest")
            sources(sourceSets.main.get(), sourceSets.test.get())
            property("forge.enabledGameTestNamespaces", "dynamicdimensions_test,minecraft") // minecraft because forge patches @GameTest for the filtering... and common cannot implement the patch
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                    source(project(":Common").sourceSets.main.get())
                }
                create("dynamicdimensions_test") {
                    source(sourceSets.test.get())
                    source(project(":Common").sourceSets.test.get())
                }
            }
            forceExit = false
        }
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
    minecraft("net.minecraftforge:forge:${minecraft}-${forge}")
    implementation(project(":Common", "namedElements"))
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")

    runtimeOnly(fg.deobf("lol.bai:badpackets:forge-${badpackets}"))
    testImplementation(project.project(":Common").sourceSets.test.get().output)
}

tasks.compileJava {
    source(project(":Common").sourceSets.main.get().java)
}

tasks.processTestResources {
    from(project(":Common").sourceSets.test.get().resources)
}

tasks.withType<ProcessResources> {
    from(project(":Common").sourceSets.main.get().resources)

    filesMatching("*.mixins.json") {
        expand("mapping" to baseArchiveName)
    }

    filesMatching("META-INF/mods.toml") {
        expand(
            "mod_version" to project.version,
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_description" to modDescription
        )
    }
}

tasks.jar {
    finalizedBy("reobfJar")
}


publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = group.toString()
            artifactId = baseArchiveName
            version = rootProject.version.toString()

            artifact(tasks.jar)

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
