package dev.galacticraft.dynamicdimensions.impl.mixin.compat.sable;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SableMixinPlugin implements IMixinConfigPlugin {

    private boolean sableLoaded;

    @Override
    public void onLoad(final String mixinPackage) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = loaderClass.getMethod("getInstance").invoke(null);
            this.sableLoaded = (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(instance, "sable");
            System.out.println("[DynDims/SableMixin] Sable detected via FabricLoader: " + this.sableLoaded);
        } catch (final Exception e) {
            // Not on Fabric, fall back to checking for NeoForge mod list
            try {
                Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
                Object modList = modListClass.getMethod("get").invoke(null);
                this.sableLoaded = (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, "sable");
                System.out.println("[DynDims/SableMixin] Sable detected via ModList: " + this.sableLoaded);
            } catch (final Exception ex) {
                this.sableLoaded = false;
                System.out.println("[DynDims/SableMixin] Could not determine if Sable is loaded: " + ex.getMessage());
            }
        }
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return this.sableLoaded;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}