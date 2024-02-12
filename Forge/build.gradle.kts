plugins {
    java
    eclipse
    id("net.neoforged.gradle.userdev") version("7.0.+")
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

runs {
    configureEach {
        workingDirectory(project.file("run"))
//        idea {
//            primarySourceSet(project.sourceSets.main.get())
//        }
        modSources(sourceSets.main.get(), project(":Common").sourceSets.main.get())
    }
    create("client") {}

    create("server") {}

    create("gameTestServer") { // name must match exactly for options to be applied, apparently
        systemProperty("forge.enabledGameTestNamespaces", "dynamicdimensions_test,minecraft") // minecraft because forge patches @GameTest for the filtering... and common cannot implement the patch
        modSource(sourceSets.test.get())
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
    implementation("net.neoforged:neoforge:${forge}")
    compileOnly(project(":Common", "namedElements"))
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")

    runtimeOnly("lol.bai:badpackets:neo-${badpackets}")
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
