plugins {
    `maven-publish`
}

loom {
    runs {
        clear()
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    options.apply {
        title = "DynWorlds ${project.version} API"
    }
    exclude("**/impl/**")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = "dev.galacticraft"
            artifactId = project.property("mod.name").toString()

            from(components["java"])
        }
    }
    repositories {
        maven("https://maven.galacticraft.dev/") {
            name = "maven"
            credentials(PasswordCredentials::class)
            authentication {
                register("basic", BasicAuthentication::class)
            }
        }
    }
}
