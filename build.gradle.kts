import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("org.ajoberstar.grgit") version("5.0.0")
    id("org.cadixdev.licenser") version("0.6.1") apply(false)
    id("fabric-loom") version ("1.5-SNAPSHOT") apply (false)
    id("org.jetbrains.gradle.plugin.idea-ext") version ("1.1.7") // required for neoforge
}

val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: ""
val prerelease = (System.getenv("PRE_RELEASE") ?: "false") == "true"
val commitHash = (System.getenv("GITHUB_SHA") ?: grgit.head().id.orEmpty())

val minecraft = project.property("minecraft.version").toString()
val modName = project.property("mod.name").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()

allprojects {
    apply(plugin = "org.cadixdev.licenser")

    version = buildString {
        append(modVersion)
        if (prerelease) {
            append("-pre")
        }
        append('+')
        if (buildNumber.isNotBlank()) {
            append(buildNumber)
        } else if (commitHash.isNotEmpty()) {
            append(commitHash.substring(0, 8))
            if (!rootProject.grgit.status().isClean) {
                append("-dirty")
            }
        } else {
            append("unknown")
        }
    }

    repositories {
        mavenLocal()
    }

    extensions.configure<org.cadixdev.gradle.licenser.LicenseExtension> {
        setHeader(rootProject.file("LICENSE_HEADER.txt"))
        include("**/dev/galacticraft/**/*.java")
    }
}


subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    val badpackets = project.property("badpackets.version").toString()

    group = "dev.galacticraft.dynamicdimensions"

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17

        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        maven ("https://maven.bai.lol") {
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
                "Specification-Title" to modName,
                "Specification-Vendor" to "Team Galacticraft",
                "Specification-Version" to archiveVersion,
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion,
                "Implementation-Vendor" to "Team Galacticraft",
                "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
                "Timestamp" to System.currentTimeMillis(),
                "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                "Build-On-Minecraft" to minecraft,
                "Automatic-Module-Name" to modId
            )
        }
    }

    tasks.withType<ProcessResources> {
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

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
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

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }
}
