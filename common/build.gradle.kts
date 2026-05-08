val modId = project.property("mod.id").toString()
val minecraft = project.property("minecraft.version").toString()
val yarn = project.property("fabric.yarn.build").toString()
val fabricLoader = project.property("fabric.loader.version").toString()
val sable = project.property("sable.version").toString()

plugins {
    id("fabric-loom")
    id("dev.galacticraft.mojarn")
}

loom {
    // configure access widener
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
    mappings(mojarn.mappings("net.fabricmc:yarn:$minecraft+build.$yarn:v2"))

    // loom expects some loader classes to exist, provides mixin and mixin-extras too
    modCompileOnly("net.fabricmc:fabric-loader:${fabricLoader}")

    compileOnly("dev.ryanhcode.sable:sable-common-$minecraft:$sable")
}
