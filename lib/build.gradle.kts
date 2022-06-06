plugins {
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    options.apply {
        title = "${rootProject.name} ${project.version} API"
    }
    exclude("**/impl/**")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = "dev.galacticraft"
            artifactId = project.property("mod.id").toString()
            version = "${project.version}"
            if (System.getenv("SNAPSHOT") == "true") {
                version += "-SNAPSHOT"
            }

            from(components["java"])

            pom {
                organization {
                    name.set("Team Galacticraft")
                    url.set("https://github.com/TeamGalacticraft")
                }

                scm {
                    url.set("https://github.com/TeamGalacticraft/DynWorlds")
                    connection.set("scm:git:git://github.com/TeamGalacticraft/DynWorlds.git")
                    developerConnection.set("scm:git:git@github.com:TeamGalacticraft/DynWorlds.git")
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/TeamGalacticraft/DynWorlds/issues")
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/TeamGalacticraft/DynWorlds/blob/main/LICENSE")
                    }
                }
            }
        }
    }

    repositories {
        if (System.getenv().containsKey("NEXUS_REPOSITORY_URL")) {
            maven(System.getenv("NEXUS_REPOSITORY_URL")) {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("NEXUS_USER")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }
        } else {
            println("No nexus repository url found, publishing to local maven repo")
            mavenLocal()
        }
    }
}
