/*
 * Copyright (c) 2021-2026 Team Galacticraft
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

package dev.galacticraft.dynamicdimensions.impl;

import com.google.common.collect.ImmutableList;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionProperties;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback;
import dev.galacticraft.dynamicdimensions.impl.accessor.DynamicDimensionProvider;
import dev.galacticraft.dynamicdimensions.impl.accessor.PrimaryLevelDataAccessor;
import dev.galacticraft.dynamicdimensions.impl.compat.DynamicDimensionPhysicsCompat;
import dev.galacticraft.dynamicdimensions.impl.mixin.*;
import dev.galacticraft.dynamicdimensions.impl.network.S2CPackets;
import dev.galacticraft.dynamicdimensions.impl.registry.RegistryUtil;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamicDimensionRegistryImpl implements DynamicDimensionRegistry {
    private final @NotNull List<ResourceKey<Level>> dynamicDimensions;
    private final MinecraftServer server;
    private final Registry<DimensionType> dimTypes;
    private final Registry<LevelStem> stems;

    private final Map<ResourceKey<Level>, DynamicDimensionProperties> dimensionProperties = new HashMap<>();

    public DynamicDimensionRegistryImpl(MinecraftServer server) {
        this.server = server;
        this.dimTypes = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        this.stems = server.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        this.dynamicDimensions = ((PrimaryLevelDataAccessor) server.getWorldData()).dynamicDimensions$getDynamicDimensions();
    }

    public void loadDynamicDimensions() {
        DynamicDimensionLoadCallback.invoke(server, (id, chunkGenerator, type) -> {
            Constants.LOGGER.debug("Loading dynamic dimension '{}'", id);
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);

            // If properties were already registered before this callback fires
            // (e.g. by the caller calling setDimensionProperties first), stage them now.
            DynamicDimensionProperties properties = this.dimensionProperties.get(key);
            if (properties != null) {
                DynamicDimensionPhysicsCompat.stage(key, properties);
            }

            return this.createDynamicLevel(id, chunkGenerator, type, key);
        });

        Constants.LOGGER.info("Loaded {} dynamic dimensions", this.dynamicDimensions.size());
    }

    @Override
    public @Nullable ServerLevel createDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type) {
        return this.createDynamicLevel(id, generator, type, true);
    }

    @Override
    public @Nullable ServerLevel createDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type, @NotNull DynamicDimensionProperties properties) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        this.setDimensionProperties(key, properties);

        ServerLevel level = this.createDynamicLevel(id, generator, type, true);
        if (level == null) {
            this.clearDimensionProperties(key);
        }

        return level;
    }

    @Override
    public @Nullable ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type) {
        return this.createDynamicLevel(id, generator, type, false);
    }

    @Override
    public @Nullable ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type, @NotNull DynamicDimensionProperties properties) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        this.setDimensionProperties(key, properties);

        ServerLevel level = this.createDynamicLevel(id, generator, type, false);
        if (level == null) {
            this.clearDimensionProperties(key);
        }

        return level;
    }

    @Override
    public void setDimensionProperties(@NotNull ResourceKey<Level> key, @NotNull DynamicDimensionProperties properties) {
        this.dimensionProperties.put(key, properties);

        if (this.server.getLevel(key) != null) {
            DynamicDimensionPhysicsCompat.apply(key, properties);
        }
    }

    @Override
    public @Nullable DynamicDimensionProperties getDimensionProperties(@NotNull ResourceKey<Level> key) {
        return this.dimensionProperties.get(key);
    }


    @Override
    public void clearDimensionProperties(@NotNull ResourceKey<Level> key) {
        this.dimensionProperties.remove(key);
        DynamicDimensionPhysicsCompat.remove(key);
    }

    @Override
    public boolean dynamicDimensionExists(@NotNull ResourceKey<Level> key) {
        return this.dynamicDimensions.contains(key) || ((DynamicDimensionProvider) this.server).dynamicdimensions$isIdPendingCreation(key);
    }

    @Override
    public boolean anyDimensionExists(@NotNull ResourceLocation id) {
        return this.server.levelKeys().contains(ResourceKey.create(Registries.DIMENSION, id))
                || this.dimTypes.containsKey(id)
                || this.stems.containsKey(id);
    }

    @Override
    public boolean canDeleteDimension(@NotNull ResourceKey<Level> key) {
        return this.dynamicDimensionExists(key);
    }

    @Override
    public boolean canCreateDimension(@NotNull ResourceLocation id) {
        return !this.anyDimensionExists(id) && !this.dynamicDimensionExists(ResourceKey.create(Registries.DIMENSION, id));
    }

    @Override
    public boolean deleteDynamicDimension(@NotNull ResourceLocation id, @Nullable PlayerRemover remover) {
        if (remover == null) remover = PlayerRemover.DEFAULT;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canDeleteDimension(key)) return false;

        DynamicDimensionPhysicsCompat.remove(key);
        ((DynamicDimensionProvider) this.server).dynamicdimensions$removeLevel(key, remover, true);
        this.dimensionProperties.remove(key);

        return true;
    }

    @Override
    public boolean unloadDynamicDimension(@NotNull ResourceLocation id, @Nullable PlayerRemover remover) {
        if (remover == null) remover = PlayerRemover.DEFAULT;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canDeleteDimension(key)) return false;

        DynamicDimensionPhysicsCompat.remove(key);
        ((DynamicDimensionProvider) this.server).dynamicdimensions$removeLevel(key, remover, false);

        return true;
    }

    private @Nullable ServerLevel createDynamicLevel(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type, boolean deleteData) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canCreateDimension(id)) return null;
        Constants.LOGGER.debug("Attempting to create dynamic dimension '{}'", id);

        if (this.dimTypes.stream().anyMatch(t -> t == type)) {
            return null;
        }

        if (deleteData) ((DynamicDimensionProvider) this.server).dynamicdimensions$deleteLevelData(key);
        return this.createDynamicLevel(id, generator, type, key);
    }

    private @NotNull ServerLevel createDynamicLevel(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type, ResourceKey<Level> key) {
        Holder.Reference<DimensionType> typeHolder = RegistryUtil.registerUnfreeze(this.dimTypes, id, type);
        LevelStem stem = new LevelStem(typeHolder, generator);
        RegistryUtil.registerUnfreeze(this.stems, id, stem); // todo: look into whether stem registration is necessary

        return this.createDynamicLevel(key, this.server.getWorldData(), stem, this.server.overworld());
    }

    private @NotNull ServerLevel createDynamicLevel(ResourceKey<Level> key, WorldData worldData, LevelStem stem, ServerLevel overworld) {
        // Stage physics properties before ServerLevel construction so Sable's
        // SubLevelPhysicsSystem.initialize() mixin can flush them before reading gravity.
        DynamicDimensionProperties pendingProperties = this.dimensionProperties.get(key);
        if (pendingProperties != null) {
            DynamicDimensionPhysicsCompat.stage(key, pendingProperties);
        }

        final DerivedLevelData data = new DerivedLevelData(worldData, worldData.overworldData());
        final ServerLevel level = new ServerLevel(
                this.server,
                ((MinecraftServerAccessor) this.server).getExecutor(),
                ((MinecraftServerAccessor) this.server).getStorageSource(),
                data,
                key,
                stem,
                ((MinecraftServerAccessor) this.server).getProgressListenerFactory().create(10),
                worldData.isDebugWorld(),
                BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()),
                ImmutableList.of(),
                false,
                null
        );
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));

        level.getChunkSource().setSimulationDistance(((DistanceManagerAccessor) ((ServerChunkCacheAccessor) overworld.getChunkSource()).getDistanceManager()).getSimulationDistance());
        level.getChunkSource().setViewDistance(((ChunkMapAccessor) overworld.getChunkSource().chunkMap).getViewDistance());

        ForcedChunksSavedData forcedChunksSavedData = level.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        if (forcedChunksSavedData != null) {
            LongIterator longIterator = forcedChunksSavedData.getChunks().iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.nextLong();
                ChunkPos chunkPos = new ChunkPos(l);
                level.getChunkSource().updateChunkForced(chunkPos, true);
            }
        }

        level.setSpawnSettings(this.server.isSpawningMonsters(), this.server.isSpawningAnimals());

        ((DynamicDimensionProvider) this.server).dynamicdimensions$registerLevel(level);

        // Belt-and-suspenders apply after level exists, covers setDimensionProperties called post-creation.
        if (pendingProperties != null) {
            DynamicDimensionPhysicsCompat.apply(key, pendingProperties);
        }

        final var serializedType = ((CompoundTag) DimensionType.DIRECT_CODEC.encode(stem.type().value(), RegistryOps.create(NbtOps.INSTANCE, this.server.registryAccess()), new CompoundTag()).getOrThrow());
        for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
            S2CPackets.sendCreateDimension(player, key.location(), serializedType);
        }
        this.reloadDimensionTags();
        return level;
    }

    @SuppressWarnings("unchecked") // we know that the registry is a registry of dimension types as the key is correct
    private void reloadDimensionTags() {
        for (TagManager.LoadResult<?> result : ((ReloadableServerResourcesAccessor) ((MinecraftServerAccessor)this.server).getResources().managers()).getTagManager().getResult()) {
            if (result.key() == Registries.DIMENSION_TYPE) {
                this.dimTypes.resetTags();
                this.dimTypes.bindTags(((TagManager.LoadResult<DimensionType>) result).tags().entrySet()
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(entry -> TagKey.create(Registries.DIMENSION_TYPE, entry.getKey()), entry -> entry.getValue().stream().toList())));
                break;
            }
        }
        this.server.getPlayerList().broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.server.registries())));
    }

    public void remove(ResourceKey<Level> key) {
        this.dynamicDimensions.remove(key);
    }

    public void add(ResourceKey<Level> key) {
        this.dynamicDimensions.add(key);
    }
}
