/*
 * Copyright (c) 2021-2022 Team Galacticraft
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

plugins {
    id("fabric-loom") version "0.12-SNAPSHOT" apply false
    id("io.github.juuxel.loom-quiltflower") version("1.7.3") apply false
    id("org.cadixdev.licenser") version "0.6.1" apply false
}

val minecraft = rootProject.property("minecraft.version").toString()
val loader = rootProject.property("loader.version").toString()
val fabric = rootProject.property("fabric.version").toString()

allprojects {
    apply(plugin = "org.cadixdev.licenser")

    extensions.getByType(org.cadixdev.gradle.licenser.LicenseExtension::class).apply {
        setHeader(rootProject.file("LICENSE_HEADER.txt"))
        include("**/dev/galacticraft/**/*.java")
        include("build.gradle.kts")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "fabric-loom")
    apply(plugin = "io.github.juuxel.loom-quiltflower")

    val modId = project.property("mod.id").toString()
    val modVersion = project.property("mod.version").toString()
    val modName = project.property("mod.name").toString()
    val fabricModules = project.property("fabric.modules").toString().split(',')

    group = "dev.galacticraft"
    version = modVersion

    extensions.getByType(BasePluginExtension::class).archivesName.set(modName)

    extensions.getByType(JavaPluginExtension::class).apply {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class).apply {
        shareCaches()
        if (project.file("src/main/resources/${modId}.accesswidener").exists()) {
            accessWidenerPath.set(project.file("src/main/resources/${modId}.accesswidener"))
        }
    }

    dependencies {
        "minecraft"("com.mojang:minecraft:$minecraft")
        "mappings"(project.extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class).officialMojangMappings())
        "modImplementation"("net.fabricmc:fabric-loader:$loader")

        val fabricApi = net.fabricmc.loom.configuration.FabricApiExtension(this@subprojects)
        fabricModules.forEach {
            "modCompileOnly"(fabricApi.module(it, fabric))
        }
        
        "modRuntimeOnly"("net.fabricmc.fabric-api:fabric-api:$fabric")
    }

    tasks.withType<ProcessResources>() {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "mod_id" to modId,
                "mod_name" to modName
            )
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
        dependsOn(tasks.getByName("checkLicenses"))
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<Jar>() {
        from("LICENSE")
        manifest {
            attributes(
                "Implementation-Title" to modName,
                "Implementation-Version" to "${project.version}",
                "Implementation-Vendor" to "Team Galacticraft",
                "Implementation-Timestamp" to java.time.format.DateTimeFormatter.ISO_DATE_TIME,
                "Maven-Artifact" to "${project.group}:${modName}:${project.version}",
                "ModSide" to "BOTH"
            )
        }
    }
}
