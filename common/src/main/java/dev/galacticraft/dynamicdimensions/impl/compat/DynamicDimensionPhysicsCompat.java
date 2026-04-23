package dev.galacticraft.dynamicdimensions.impl.compat;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionProperties;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class DynamicDimensionPhysicsCompat {
    private DynamicDimensionPhysicsCompat() {
    }

    public static void apply(ResourceKey<Level> key, DynamicDimensionProperties properties) {
        SableDimensionPhysicsCompat.apply(key, properties);
    }

    public static void remove(ResourceKey<Level> key) {
        SableDimensionPhysicsCompat.remove(key);
    }
}