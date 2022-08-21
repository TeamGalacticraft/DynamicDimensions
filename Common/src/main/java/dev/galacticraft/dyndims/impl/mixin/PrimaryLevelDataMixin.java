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

package dev.galacticraft.dyndims.impl.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.OptionalDynamic;
import dev.galacticraft.dyndims.impl.accessor.PrimaryLevelDataAccessor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.timers.TimerQueue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
public abstract class PrimaryLevelDataMixin implements PrimaryLevelDataAccessor {
    // Since we can't pass values to the constructor when reading the properties, we need to store them externally
    private static final ThreadLocal<Map<ResourceLocation, Pair<ChunkGenerator, DimensionType>>> MAP = new ThreadLocal<>();
    /**
     * Map of all the worlds that have been registered.
     */
    private @Unique Map<ResourceLocation, Pair<ChunkGenerator, DimensionType>> dynamicDimensions;

    @Inject(method = "parse", at = @At(value = "HEAD"))
    private static void parseDynamicDimensions(Dynamic<Tag> dynamic, DataFixer dataFixer, int i, CompoundTag compoundTag, LevelSettings levelSettings, LevelVersion levelVersion, WorldGenSettings worldGenSettings, Lifecycle lifecycle, CallbackInfoReturnable<PrimaryLevelData> cir) {
        OptionalDynamic<Tag> dyn = dynamic.get("DynamicDimensions");
        if (dyn.result().isPresent()) {
            MAP.set(new HashMap<>(dyn.asMap(e -> new ResourceLocation(e.asString().get().orThrow()), e -> new Pair<>(e.get("chunk_generator").decode(ChunkGenerator.CODEC).get().orThrow().getFirst(), e.get("dimension_type").decode(DimensionType.DIRECT_CODEC).get().orThrow().getFirst()))));
        }
    }

    @Inject(method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;ZIIIFJJIIIZIZZZLnet/minecraft/world/level/border/WorldBorder$Settings;IILjava/util/UUID;Ljava/util/Set;Lnet/minecraft/world/level/timers/TimerQueue;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/world/level/levelgen/WorldGenSettings;Lcom/mojang/serialization/Lifecycle;)V", at = @At("RETURN"))
    private void init(DataFixer dataFixer, int dataVersion, CompoundTag playerData, boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle, long time, long timeOfDay, int version, int clearWeatherTime, int rainTime, boolean raining, int thunderTime, boolean thundering, boolean initialized, boolean difficultyLocked, WorldBorder.Settings worldBorder, int wanderingTraderSpawnDelay, int wanderingTraderSpawnChance, UUID wanderingTraderId, Set<String> serverBrands, TimerQueue<MinecraftServer> scheduledEvents, CompoundTag customBossEvents, CompoundTag dragonFight, LevelSettings levelSettings, WorldGenSettings worldGenSettings, Lifecycle lifecycle, CallbackInfo ci) {
        this.dynamicDimensions = MAP.get(); // get the map from the thread local
        MAP.remove(); // make sure we don't leave the map for other level properties to use
        if (this.dynamicDimensions == null) {
            this.dynamicDimensions = new HashMap<>(0); // if the world is new, there won't be any properties to read
        }
    }

    @Inject(method = "setTagData", at = @At("RETURN"))
    private void writeDynamicDimensions(RegistryAccess registryAccess, @NotNull CompoundTag levelNbt, CompoundTag playerNbt, CallbackInfo ci) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        CompoundTag compound = new CompoundTag();
        this.dynamicDimensions.forEach((id, options) -> {
            CompoundTag pair = new CompoundTag();
            pair.put("chunk_generator", ChunkGenerator.CODEC.encode(options.getFirst(), ops, new CompoundTag()).get().orThrow());
            pair.put("dimension_type", DimensionType.DIRECT_CODEC.encode(options.getSecond(), ops, new CompoundTag()).get().orThrow());
            compound.put(id.toString(), pair);
        });
        levelNbt.put("DynamicDimensions", compound);
    }

    @Override
    public void addDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type) {
        this.dynamicDimensions.put(id, new Pair<>(chunkGenerator, type));
    }

    @Contract("null -> false")
    @Override
    public boolean removeDynamicDimension(ResourceLocation key) {
        return this.dynamicDimensions.remove(key) != null;
    }

    @Override
    public Map<ResourceLocation, Pair<ChunkGenerator, DimensionType>> getDynamicDimensions() {
        return this.dynamicDimensions;
    }
}
