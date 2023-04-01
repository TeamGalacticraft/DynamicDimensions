import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("org.ajoberstar.grgit") version("5.0.0")
    id("org.cadixdev.licenser") version("0.6.1") apply(false)
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

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }

    extensions.configure<org.cadixdev.gradle.licenser.LicenseExtension> {
        setHeader(rootProject.file("LICENSE_HEADER.txt"))
        include("**/dev/galacticraft/**/*.java")
    }
}


subprojects {
    apply(plugin = "java")

    val badpackets = project.property("badpackets.version").toString()

    group = "dev.galacticraft"

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

//    if (project.name != "Common") {
//        tasks.getByName<JavaCompile>("compileJava") {
//            source(project(":Common").extensions.getByType<JavaPluginExtension>().sourceSets.getByName("main").allSource)
//        }
//    }

    tasks.withType<Javadoc> {
        exclude("**/impl/**")
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
}
