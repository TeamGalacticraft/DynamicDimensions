plugins {
    id("org.ajoberstar.grgit") version ("5.2.1")
    id("org.cadixdev.licenser") version("0.6.1") apply(false)
    id("fabric-loom") version("1.5-SNAPSHOT") apply(false)
    id("org.jetbrains.gradle.plugin.idea-ext") version("1.1.7") // required for neoforge
}

val minecraft = project.property("minecraft.version").toString()
val modId = project.property("mod.id").toString()
val modName = project.property("mod.name").toString()
val modVersion = project.property("mod.version").toString()
val modDescription = project.property("mod.description").toString()
val modLicense = project.property("mod.license").toString()

group = "dev.galacticraft"
version = buildString {
    append(modVersion)
    val env = System.getenv()
    if (env.containsKey("PRE_RELEASE") && env["PRE_RELEASE"] == "true") {
        append("-pre")
    }
    append('+')
    if (env.containsKey("GITHUB_RUN_NUMBER")) {
        append(env["GITHUB_RUN_NUMBER"])
    } else {
        val grgit = extensions.findByType<org.ajoberstar.grgit.Grgit>()
        if (grgit?.head() != null) {
            append(grgit.head().id.substring(0, 8))
            if (!grgit.status().isClean) {
                append("-dirty")
            }
        } else {
            append("unknown")
        }
    }
}
description = modDescription

println("$modName: $version")

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "org.cadixdev.licenser")

    group = rootProject.group
    version = rootProject.version
    description = rootProject.description

    extensions.getByType<BasePluginExtension>().archivesName.set("$modId-${project.name}")

    val badpackets = project.property("badpackets.version").toString()

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17

        withJavadocJar()
        withSourcesJar()
    }

    extensions.configure<org.cadixdev.gradle.licenser.LicenseExtension> {
        setHeader(rootProject.file("LICENSE_HEADER.txt"))
        include("**/dev/galacticraft/**/*.java")
    }

    repositories {
        maven("https://maven.parchmentmc.org") {
            name = "ParchmentMC"
            content {
                includeGroup("org.parchmentmc.data")
            }
        }
        maven("https://maven.bai.lol") {
            content {
                includeGroup("lol.bai")
            }
        }
    }

    dependencies {
        "compileOnly"("lol.bai:badpackets:mojmap-${badpackets}")
    }

    tasks.withType<Jar> {
        from("LICENSE") {
            rename { "${it}_${modName}" }
        }

        manifest {
            attributes(
                    "Specification-Title" to modId,
                    "Specification-Vendor" to "Team Galacticraft",
                    "Specification-Version" to modVersion,
                    "Implementation-Title" to archiveBaseName,
                    "Implementation-Version" to archiveVersion,
                    "Implementation-Vendor" to "Team Galacticraft",
                    "Implementation-Timestamp" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(java.util.Date()),
                    "Timestamp" to System.currentTimeMillis(),
                    "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                    "Built-On-Minecraft" to minecraft,
                    "Automatic-Module-Name" to modId
            )
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<ProcessResources> {
        val properties = mapOf(
                "mod_id" to modId,
                "mod_name" to modName,
                "mod_description" to modDescription,
                "mod_license" to modLicense,
                "mod_version" to project.version,
                "min_minecraft" to project.property("minecraft.version.min"),
                "min_fabric_loader" to project.property("fabric.loader.version.min"),
                "min_neoforge" to project.property("neoforge.version.min"),
        )

        filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "META-INF/mods.toml", "*.mixins.json")) {
            expand(properties)
        }
        inputs.properties(properties);

        // Minify json resources
        // https://stackoverflow.com/questions/41028030/gradle-minimize-json-resources-in-processresources#41029113
        doLast {
            fileTree(
                    mapOf(
                            "dir" to outputs.files.asPath,
                            "includes" to listOf("**/*.json", "**/*.mcmeta")
                    )
            ).forEach { file: File ->
                file.writeText(groovy.json.JsonOutput.toJson(groovy.json.JsonSlurper().parse(file)))
            }
        }
    }

    extensions.configure<PublishingExtension> {
        publications {
            register("mavenJava", MavenPublication::class) {
                artifactId = extensions.getByType<BasePluginExtension>().archivesName.get()
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
                            name.set(modLicense)
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

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }
}

