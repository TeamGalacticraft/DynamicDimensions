/*
 * Copyright (c) 2021-2025 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.diffplug.gradle.spotless.SpotlessExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val modId = project.property("mod.id").toString()
val modName = project.property("mod.name").toString()
val modVersion = project.property("mod.version").toString()
val modDescription = project.property("mod.description").toString()
val modLicense = project.property("mod.license").toString()

val minecraft = project.property("minecraft.version").toString()

plugins {
    id("org.ajoberstar.grgit") version ("5.3.0")
    id("fabric-loom") version("1.10-SNAPSHOT") apply(false)
    id("dev.galacticraft.mojarn") version("0.6.1+19") apply(false)
    id("net.neoforged.moddev") version("2.0.80") apply(false)
    id("com.diffplug.spotless") version("7.0.3") apply(false)
}

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
    apply(plugin = "com.diffplug.spotless")

    group = rootProject.group
    version = rootProject.version
    description = rootProject.description

    extensions.configure<BasePluginExtension> {
        archivesName.set("$modId-${project.name}")
    }

    extensions.configure<JavaPluginExtension> {
        targetCompatibility = JavaVersion.VERSION_21
        sourceCompatibility = JavaVersion.VERSION_21
    }

    repositories {
        maven("https://maven.bai.lol") {
            content {
                includeGroup("lol.bai")
            }
        }

        maven("https://maven.ryanhcode.dev/releases")
        maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
        maven("https://maven.blamejared.com")
    }

    dependencies {
        "compileOnly"("lol.bai:badpackets:mojmap-${project.property("badpackets.version")}")
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

        inputs.properties(properties)
        filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
            expand(properties)
        }

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
        options.release.set(21)
    }

    tasks.withType<Jar> {
        from("LICENSE") {
            rename { "${it}_${modId}"}
        }

        manifest {
            attributes(
                "Specification-Title" to modId,
                "Specification-Vendor" to "Team Galacticraft",
                "Specification-Version" to modVersion,
                "Implementation-Title" to project.name,
                "Implementation-Version" to "${project.version}",
                "Implementation-Vendor" to "Team Galacticraft",
                "Implementation-Timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                "Maven-Artifact" to "${project.group}:${modName}:${project.version}",
                "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})"
            )
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

    fun processLicenseHeader(license: File): String {
        val text = license.readText()
        return "/*\n * " + text.substring(text.indexOf("Copyright"))
            .replace("\n", "\n * ")
            .replace("* \n", "*\n")
            .trim() + "/\n\n"
    }

    extensions.configure<SpotlessExtension> {
        lineEndings = com.diffplug.spotless.LineEnding.UNIX

        java {
            licenseHeader(processLicenseHeader(rootProject.file("LICENSE")))
            leadingTabsToSpaces()
            removeUnusedImports()
            trimTrailingWhitespace()
        }
    }
}
