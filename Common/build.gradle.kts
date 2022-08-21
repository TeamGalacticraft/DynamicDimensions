plugins {
    java
    `maven-publish`
    id("fabric-loom") version("0.13-SNAPSHOT")
}

val buildNumber = System.getenv("BUILD_NUMBER") ?: ""
val snapshot = (System.getenv("SNAPSHOT") ?: "false") == "true"
val prerelease = (System.getenv("PRE_RELEASE") ?: "false") == "true"

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

    mixin {
        add(sourceSets.main.get(), "${modId}.refmap.json")
        useLegacyMixinAp.set(false)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft}")
    mappings(loom.officialMojangMappings())
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

tasks.create("prepareWorkspace") {
    // no-op: for some reason this gets called with IJ gradle refresh... MCDev?
}
