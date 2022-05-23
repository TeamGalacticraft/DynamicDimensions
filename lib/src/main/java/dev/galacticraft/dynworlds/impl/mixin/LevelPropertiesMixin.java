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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
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
    private static final ThreadLocal<Map<Identifier, Pair<DimensionOptions, DimensionType>>> MAP = new ThreadLocal<>();
    private @Unique Map<Identifier, Pair<DimensionOptions, DimensionType>> dynamicWorlds;

    @Inject(method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/NbtCompound;ZIIIFJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Properties;IILjava/util/UUID;Ljava/util/Set;Lnet/minecraft/world/timer/Timer;Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/gen/GeneratorOptions;Lcom/mojang/serialization/Lifecycle;)V", at = @At("RETURN"))
    private void init(DataFixer dataFixer, int dataVersion, NbtCompound playerData, boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle, long time, long timeOfDay, int version, int clearWeatherTime, int rainTime, boolean raining, int thunderTime, boolean thundering, boolean initialized, boolean difficultyLocked, WorldBorder.Properties worldBorder, int wanderingTraderSpawnDelay, int wanderingTraderSpawnChance, UUID wanderingTraderId, Set serverBrands, Timer scheduledEvents, NbtCompound customBossEvents, NbtCompound dragonFight, LevelInfo levelInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfo ci) {
        this.dynamicWorlds = MAP.get();
        MAP.set(null);
        if (this.dynamicWorlds == null) {
            this.dynamicWorlds = new HashMap<>(); // if the world is new, there won't be any properties to read
        }
    }

    @Inject(method = "readProperties", at = @At(value = "HEAD"))
    private static void readDynamicWorlds(Dynamic<NbtElement> dynamic, DataFixer dataFixer, int dataVersion, @Nullable NbtCompound playerData, LevelInfo levelInfo, SaveVersionInfo saveVersionInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<LevelProperties> cir) {
        Map<Identifier, Pair<DimensionOptions, DimensionType>> value = new HashMap<>();
        OptionalDynamic<NbtElement> dynWorlds = dynamic.get("DynamicWorlds");
        if (dynWorlds.result().isPresent()) {
            dynWorlds.asMap(e -> new Identifier(e.asString().get().orThrow()), e -> new Pair<>(e.get("options").decode(DimensionOptions.CODEC), e.get("type").decode(DimensionType.CODEC))).forEach((id, pair) -> {
                value.put(id, new Pair<>(pair.getLeft().get().orThrow().getFirst(), pair.getRight().get().orThrow().getFirst()));
            });
        }
        MAP.set(value);
    }

    @Inject(method = "updateProperties", at = @At("RETURN"))
    private void writeDynamicWorlds(DynamicRegistryManager registryManager, NbtCompound levelNbt, NbtCompound playerNbt, CallbackInfo ci) {
        NbtCompound compound = new NbtCompound();
        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, registryManager);
        this.dynamicWorlds.forEach((id, pair) -> {
            NbtCompound nbt = new NbtCompound();
            nbt.put("options", DimensionOptions.CODEC.encode(pair.getLeft(), ops, new NbtCompound()).get().orThrow());
            nbt.put("type", DimensionType.CODEC.encode(pair.getRight(), ops, new NbtCompound()).get().orThrow());
            compound.put(id.toString(), nbt);
        });
        levelNbt.put("DynamicWorlds", compound);
    }

    @Override
    public void addDynamicWorld(Identifier id, DimensionOptions options, DimensionType type) {
        this.dynamicWorlds.put(id, new Pair<>(options, type));
    }

    @Override
    public void removeDynamicWorld(Identifier key) {
        this.dynamicWorlds.remove(key);
    }

    @Override
    public Map<Identifier, Pair<DimensionOptions, DimensionType>> getDynamicWorlds() {
        return this.dynamicWorlds;
    }
}
