plugins {
    id("fabric-loom")
}

val modId = project.property("mod.id").toString()
val minecraft = project.property("minecraft.version").toString()
val parchment = project.property("parchment.version").toString()
val fabricLoader = project.property("fabric.loader.version").toString()

loom {
    if (project(":fabric").file("src/main/resources/${modId}.accesswidener").exists()) {
        accessWidenerPath.set(project(":fabric").file("src/main/resources/${modId}.accesswidener"))
    }

    // disable Minecraft-altering loom features, so that we can have one less copy of Minecraft
    interfaceInjection.enableDependencyInterfaceInjection.set(false)
    interfaceInjection.getIsEnabled().set(false)
    enableTransitiveAccessWideners.set(false)

    mixin.useLegacyMixinAp.set(false)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(if (parchment.isBlank()) loom.officialMojangMappings() else loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-$parchment@zip")
    })

    // loom expects some loader classes to exist, mod spec provides mixin and mixin-extras too
    modCompileOnly("net.fabricmc:fabric-loader:${fabricLoader}")
}
