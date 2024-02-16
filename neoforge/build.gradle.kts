plugins {
    java
    eclipse
    id("net.neoforged.gradle.userdev") version ("7.0.+")
}

val neoforge = project.property("forge.version").toString()
val parchment = project.property("parchment.version").toString()
val badpackets = project.property("badpackets.version").toString()

runs {
    create("client") {
        modSource(sourceSets.main.get())
    }

    create("server") {
        modSource(sourceSets.main.get())
        programArgument("--nogui")
    }

    create("gameTestServer") { // name must match exactly for options to be applied
        modSources(sourceSets.main.get(), sourceSets.test.get())
    }

    configureEach {
        workingDirectory(project.file("run"))
        // minecraft because forge patches @GameTest for the filtering... and common cannot implement the patch
        systemProperty("neoforge.enabledGameTestNamespaces", "dynamicdimensions_test,minecraft")
    }
}

dependencies {
    implementation("net.neoforged:neoforge:$neoforge")
    compileOnly(project(":common", "namedElements"))

    runtimeOnly("lol.bai:badpackets:neo-$badpackets")
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

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)
}
