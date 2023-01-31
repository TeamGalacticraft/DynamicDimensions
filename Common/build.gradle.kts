plugins {
    java
    `maven-publish`
    id("fabric-loom") version("1.1-SNAPSHOT")
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

val minecraft = project.property("minecraft.version").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()

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

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge Snapshots"
        content {
            includeGroup("org.spongepowered")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft}")
    mappings(loom.officialMojangMappings())

    testCompileOnly(compileOnly("org.spongepowered:mixin:0.8.5")!!)
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

tasks.create("prepareWorkspace") {
    // no-op: for some reason this gets called with IJ gradle refresh... MCDev?
}

tasks.jar {
    archiveClassifier.set("")
    destinationDirectory.set(File(project.buildDir, "libs"))
}

tasks.remapJar {
    enabled = false
}

tasks.remapSourcesJar {
    enabled = false
}
