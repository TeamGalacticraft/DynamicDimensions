/*
 * Copyright (c) 2021-2022 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.dynworlds.impl.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.OptionalDynamic;
import dev.galacticraft.dynworlds.impl.accessor.SavePropertiesAccessor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.timers.TimerQueue;
import org.jetbrains.annotations.NotNull;
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

@Mixin(PrimaryLevelData.class)
public abstract class LevelPropertiesMixin implements SavePropertiesAccessor {
    // Since we can't pass values to the constructor when reading the properties, we need to store them externally
    private static final ThreadLocal<Map<ResourceLocation, LevelStem>> MAP = new ThreadLocal<>();
    /**
     * Map of all the worlds that have been registered.
     */
    private @Unique Map<ResourceLocation, LevelStem> dynamicWorlds;

    @Inject(method = "parse", at = @At(value = "HEAD"))
    private static void parseDynamicWorlds(@NotNull Dynamic<Tag> dynamic, DataFixer dataFixer, int dataVersion, @Nullable CompoundTag playerData, LevelSettings levelInfo, LevelVersion saveVersionInfo, WorldGenSettings generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<PrimaryLevelData> cir) {
        OptionalDynamic<Tag> dynWorlds = dynamic.get("DynamicWorlds");
        if (dynWorlds.result().isPresent()) {
            MAP.set(new HashMap<>(dynWorlds.asMap(e -> new ResourceLocation(e.asString().get().orThrow()), e -> e.decode(LevelStem.CODEC).get().orThrow().getFirst())));
        }
    }

    @Inject(method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;ZIIIFJJIIIZIZZZLnet/minecraft/world/level/border/WorldBorder$Settings;IILjava/util/UUID;Ljava/util/Set;Lnet/minecraft/world/level/timers/TimerQueue;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/world/level/levelgen/WorldGenSettings;Lcom/mojang/serialization/Lifecycle;)V", at = @At("RETURN"))
    private void init(DataFixer dataFixer, int dataVersion, CompoundTag playerData, boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle, long time, long timeOfDay, int version, int clearWeatherTime, int rainTime, boolean raining, int thunderTime, boolean thundering, boolean initialized, boolean difficultyLocked, WorldBorder.Settings worldBorder, int wanderingTraderSpawnDelay, int wanderingTraderSpawnChance, UUID wanderingTraderId, Set<String> serverBrands, TimerQueue<MinecraftServer> scheduledEvents, CompoundTag customBossEvents, CompoundTag dragonFight, LevelSettings levelInfo, WorldGenSettings generatorOptions, Lifecycle lifecycle, CallbackInfo ci) {
        this.dynamicWorlds = MAP.get(); // get the map from the thread local
        MAP.remove(); // make sure we don't leave the map for other level properties to use
        if (this.dynamicWorlds == null) {
            this.dynamicWorlds = new HashMap<>(0); // if the world is new, there won't be any properties to read
        }
    }

    @Inject(method = "setTagData", at = @At("RETURN"))
    private void writeDynamicWorlds(RegistryAccess registryManager, @NotNull CompoundTag levelNbt, CompoundTag playerNbt, CallbackInfo ci) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryManager);
        CompoundTag compound = new CompoundTag();
        this.dynamicWorlds.forEach((id, options) -> compound.put(id.toString(), LevelStem.CODEC.encode(options, ops, new CompoundTag()).get().orThrow()));
        levelNbt.put("DynamicWorlds", compound);
    }

    @Override
    public void addDynamicWorld(ResourceLocation id, @NotNull LevelStem options) {
        if (options.typeHolder().unwrap().right().isEmpty()) { // if the dimension type is a reference, we can't guarantee it will exist later
            throw new IllegalArgumentException("Cannot add a dynamic world with reference dimension type");
        }
        this.dynamicWorlds.put(id, options);
    }

    @Override
    public void removeDynamicWorld(ResourceLocation key) {
        this.dynamicWorlds.remove(key);
    }

    @Override
    public Map<ResourceLocation, LevelStem> getDynamicWorlds() {
        return this.dynamicWorlds;
    }
}
