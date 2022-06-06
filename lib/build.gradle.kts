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
        }
    }
    repositories {
        if (System.getenv().containsKey("NEXUS_REPOSITORY_URL")) {
            maven(System.getenv("NEXUS_REPOSITORY_URL")) {
                name = "maven"
                credentials(PasswordCredentials::class)
                authentication {
                    register("basic", BasicAuthentication::class)
                }
            }
        }
    }
}
