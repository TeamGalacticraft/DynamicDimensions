package dev.galacticraft.dyndims.impl.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin {
    @Inject(method = "readWorldGenSettings", at = @At("HEAD"))
    private static <T> void readDynamicDimensions(@NotNull Dynamic<T> dynamic, DataFixer dataFixer, int i, CallbackInfoReturnable<Pair<WorldGenSettings, Lifecycle>> cir) {
        if (dynamic.get("DynamicDimensions").result().isPresent()) {
            if (dynamic.getOps() instanceof RegistryOps<T> registryOps) {
                Map<ResourceLocation, DimensionType> map = dynamic.get("DynamicDimensions").asMap(e -> new ResourceLocation(e.asString().get().orThrow()), e -> e.get("dimension_type").decode(DimensionType.DIRECT_CODEC).get().orThrow().getFirst());
                for (Map.Entry<ResourceLocation, DimensionType> entry : map.entrySet()) {
                    ((MappedRegistry<DimensionType>) registryOps.registry(Registry.DIMENSION_TYPE_REGISTRY).get()).register(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, entry.getKey()), entry.getValue(), Lifecycle.stable());
                }
            }
        }
    }
}
