plugins {
    java
    eclipse
    id("net.neoforged.gradle.userdev") version ("7.0.+")
}

val minecraft = project.property("minecraft.version").toString()
val forge = project.property("forge.version").toString()
val modId = project.property("mod.id").toString()
var modVersion = project.property("mod.version").toString()
val modName = project.property("mod.name").toString()
val modDescription = project.property("mod.description").toString()
val badpackets = project.property("badpackets.version").toString()

base {
    archivesName.set("${modId}-neoforge")
}

runs {
    create("client") {
        workingDirectory(rootProject.file("run"))
        modSource(sourceSets.main.get())
    }

    create("server") {
        workingDirectory(rootProject.file("run"))
        modSource(sourceSets.main.get())
        programArgument("--nogui")
    }

    create("gameTestServer") { // name must match exactly for options to be applied, apparently
        workingDirectory(rootProject.file("run"))
        modSources(sourceSets.main.get(), sourceSets.test.get())
        systemProperty("neoforge.enabledGameTestNamespaces", "dynamicdimensions_test,minecraft") // minecraft because forge patches @GameTest for the filtering... and common cannot implement the patch
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${forge}")
    implementation(project(":common", "namedElements"))

    runtimeOnly("lol.bai:badpackets:neo-${badpackets}")
    testImplementation(project.project(":common").sourceSets.test.get().output)
}

tasks.compileJava {
    source(project(":common").sourceSets.main.get().java)
}

tasks.compileTestJava {
    source(project(":common").sourceSets.test.get().java)
}

tasks.javadoc {
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.sourcesJar {
    from(project(":common").sourceSets.main.get().allSource)
}

tasks.processTestResources {
    from(project(":common").sourceSets.test.get().resources)
}

tasks.withType<ProcessResources>().matching { !it.name.startsWith("neo") }.configureEach {
    from(project(":common").sourceSets.main.get().resources)

    filesMatching("META-INF/mods.toml") {
        expand(
                "mod_version" to project.version,
                "mod_id" to modId,
                "mod_name" to modName,
                "mod_description" to modDescription
        )
    }
}
