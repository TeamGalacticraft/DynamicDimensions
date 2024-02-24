plugins {
    eclipse
    id("net.neoforged.gradle.userdev") version("7.0.+")
}

val modId = project.property("mod.id").toString()
val neoforge = project.property("forge.version").toString()
val parchment = project.property("parchment.version").toString()
val badpackets = project.property("badpackets.version").toString()

runs {
    create("client")
    create("server").programArgument("--nogui")
    create("gameTestServer") // name must match exactly for options to be applied

    configureEach {
        modSources(sourceSets.main.get())
        workingDirectory(project.file("run"))
        // minecraft because forge patches @GameTest for the filtering... and common cannot implement the patch
        systemProperty("neoforge.enabledGameTestNamespaces", "$modId,minecraft")
    }
}

dependencies {
    implementation("net.neoforged:neoforge:$neoforge")
    compileOnly(project(":common", "namedElements"))

    runtimeOnly("lol.bai:badpackets:neo-$badpackets")
}

tasks.compileJava {
    source(project(":common").sourceSets.main.get().java)
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)
}

tasks.javadoc {
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.sourcesJar {
    from(project(":common").sourceSets.main.get().allSource)
}
