import java.text.SimpleDateFormat
import java.util.Date

val buildNumber = System.getenv("BUILD_NUMBER") ?: ""
val snapshot = (System.getenv("SNAPSHOT") ?: "false") == "true"
val prerelease = (System.getenv("PRE_RELEASE") ?: "false") == "true"

val minecraft = project.property("minecraft.version").toString()
val modName = project.property("mod.name").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()

plugins {
    id("org.cadixdev.licenser") version("0.6.1")
}

allprojects {
    apply(plugin = "org.cadixdev.licenser")

    version = buildString {
        append(modVersion)
        if (prerelease || snapshot) {
            append("-pre")
        }
        if (buildNumber.isNotBlank()) {
            append("+")
            append(buildNumber)
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
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
