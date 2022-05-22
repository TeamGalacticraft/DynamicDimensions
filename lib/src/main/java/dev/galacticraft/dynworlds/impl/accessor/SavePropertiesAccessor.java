package dev.galacticraft.dynworlds.impl.accessor;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;

import java.util.Map;

public interface SavePropertiesAccessor {
    void addDynamicWorld(RegistryKey<DimensionOptions> key, DimensionOptions dimension);

    void removeDynamicWorld(RegistryKey<DimensionOptions> key);

    Map<RegistryKey<DimensionOptions>, DimensionOptions> getDynamicWorlds();
}
