package dev.galacticraft.dynworlds.impl.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.OptionalDynamic;
import dev.galacticraft.dynworlds.impl.accessor.SavePropertiesAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.SaveVersionInfo;
import net.minecraft.world.timer.Timer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(LevelProperties.class)
public abstract class LevelPropertiesMixin implements SavePropertiesAccessor {
    private static final ThreadLocal<Map<RegistryKey<DimensionOptions>, DimensionOptions>> MAP = new ThreadLocal<>();
    private @Unique Map<RegistryKey<DimensionOptions>, DimensionOptions> dynamicWorlds;

    @Inject(method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/NbtCompound;ZIIIFJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Properties;IILjava/util/UUID;Ljava/util/Set;Lnet/minecraft/world/timer/Timer;Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/gen/GeneratorOptions;Lcom/mojang/serialization/Lifecycle;)V", at = @At("RETURN"))
    private void init(DataFixer dataFixer, int dataVersion, NbtCompound playerData, boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle, long time, long timeOfDay, int version, int clearWeatherTime, int rainTime, boolean raining, int thunderTime, boolean thundering, boolean initialized, boolean difficultyLocked, WorldBorder.Properties worldBorder, int wanderingTraderSpawnDelay, int wanderingTraderSpawnChance, UUID wanderingTraderId, Set serverBrands, Timer scheduledEvents, NbtCompound customBossEvents, NbtCompound dragonFight, LevelInfo levelInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfo ci) {
        this.dynamicWorlds = MAP.get();
        MAP.set(null);
    }

    @Inject(method = "readProperties", at = @At(value = "NEW", target = "Lnet/minecraft/world/level/LevelProperties;<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/NbtCompound;ZIIIFJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Properties;IILjava/util/UUID;Ljava/util/Set;Lnet/minecraft/world/timer/Timer;Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/gen/GeneratorOptions;Lcom/mojang/serialization/Lifecycle;)V"))
    private static void readDynamicWorlds(Dynamic<NbtElement> dynamic, DataFixer dataFixer, int dataVersion, @Nullable NbtCompound playerData, LevelInfo levelInfo, SaveVersionInfo saveVersionInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<LevelProperties> cir) {
        HashMap<RegistryKey<DimensionOptions>, DimensionOptions> value = new HashMap<>();
        OptionalDynamic<NbtElement> dynWorlds = dynamic.get("DynamicWorlds");
        if (dynWorlds.result().isPresent()) {
            dynWorlds.asMap(e -> RegistryKey.of(Registry.DIMENSION_KEY, new Identifier(e.asString().getOrThrow(false, s -> {
                throw new RuntimeException(s);
            }))), e -> e.decode(DimensionOptions.CODEC)).forEach((dimensionOptionsRegistryKey, pairDataResult) ->{
                value.put(dimensionOptionsRegistryKey, pairDataResult.getOrThrow(false, s -> {
                    throw new RuntimeException(s);
                }).getFirst());
            });
        }
        MAP.set(value);
    }

    @Inject(method = "updateProperties", at = @At("RETURN"))
    private void writeDynamicWorlds(DynamicRegistryManager registryManager, NbtCompound levelNbt, NbtCompound playerNbt, CallbackInfo ci) {
        NbtCompound compound = new NbtCompound();
        this.dynamicWorlds.forEach((dimensionOptionsRegistryKey, dimensionOptions) -> {
            compound.put(dimensionOptionsRegistryKey.getValue().toString(), DimensionOptions.CODEC.encode(dimensionOptions, NbtOps.INSTANCE, new NbtCompound()).get().orThrow());
        });
        levelNbt.put("DynamicWorlds", compound);
    }

    @Override
    public void addDynamicWorld(RegistryKey<DimensionOptions> key, DimensionOptions dimension) {
        this.dynamicWorlds.put(key, dimension);
    }

    @Override
    public void removeDynamicWorld(RegistryKey<DimensionOptions> key) {
        this.dynamicWorlds.remove(key);
    }
}
