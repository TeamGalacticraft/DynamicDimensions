/*
 * Copyright (c) 2021-2024 Team Galacticraft
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

package dev.galacticraft.dynamicdimensions.impl.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import dev.galacticraft.dynamicdimensions.api.event.DimensionAddedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DimensionRemovedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.accessor.PrimaryLevelDataAccessor;
import dev.galacticraft.dynamicdimensions.impl.internal.DimensionRemovalTicket;
import dev.galacticraft.dynamicdimensions.impl.registry.RegistryUtil;
import io.netty.buffer.Unpooled;
import lol.bai.badpackets.api.PacketSender;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements DynamicDimensionRegistry {
    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;
    @Shadow
    private MinecraftServer.ReloadableResources resources;
    @Shadow
    @Final
    private Executor executor;
    @Shadow
    @Final
    private ChunkProgressListenerFactory progressListenerFactory;
    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Shadow
    public abstract ServerLevel overworld();

    @Shadow
    public abstract PlayerList getPlayerList();

    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();

    @Shadow
    public abstract WorldData getWorldData();

    @Shadow
    public abstract LayeredRegistryAccess<RegistryLayer> registries();

    @Unique
    private final @NotNull List<ServerLevel> pendingLevels = new ArrayList<>();
    @Unique
    private final @NotNull List<DimensionRemovalTicket> pendingDeletions = new ArrayList<>();
    @Unique
    private final @NotNull List<ResourceKey<Level>> dynamicDimensions = new ArrayList<>();
    @Unique
    private boolean tickingLevels = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initDynamicDimensions(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        ((PrimaryLevelDataAccessor) worldStem.worldData()).dynamicDimensions$setDynamicList(this.dynamicDimensions);
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.BEFORE))
    private void addLevels(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.pendingLevels.isEmpty()) {
            for (ServerLevel level : this.pendingLevels) {
                this.registerLevel(level);
            }
            this.pendingLevels.clear();
        }

        if (!this.pendingDeletions.isEmpty()) {
            for (DimensionRemovalTicket ticket : this.pendingDeletions) {
                this.unloadLevel(ticket.key(), ticket.removalMode());
                if (ticket.removeFiles()) {
                    this.deleteLevelData(ticket.key());
                }
            }
            this.pendingDeletions.clear();
        }
    }

    @Unique
    private void unloadLevel(ResourceKey<Level> key, PlayerRemover playerRemover) {
        try (ServerLevel level = this.levels.remove(key)) {
            if (level == null) {
                assert !this.dynamicDimensions.contains(key);
                return;
            }
            DimensionRemovedCallback.invoke(key, level);

            List<ServerPlayer> players = new ArrayList<>(level.players()); // prevent co-modification
            for (ServerPlayer player : players) {
                playerRemover.removePlayer((MinecraftServer) (Object) this, player);
            }

            level.save(null, true, false);
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to close level upon removal! Memory may have been leaked.", e);
        }

        RegistryUtil.unregister(this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM), key.location());
        RegistryUtil.unregister(this.registries().compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE), key.location());
        this.dynamicDimensions.remove(key);

        FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        packetByteBuf.writeResourceLocation(key.location());
        this.getPlayerList().getPlayers().forEach(player -> PacketSender.s2c(player).send(Constants.REMOVE_DIMENSION_PACKET, packetByteBuf));
    }

    @Unique
    private void deleteLevelData(ResourceKey<Level> key) {
        Path worldDir = this.storageSource.getDimensionPath(key);
        if (worldDir.toFile().exists()) {
            try {
                FileUtils.deleteDirectory(worldDir.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete deleted world directory!", e);
            }
        }
    }

    @Unique
    private void registerLevel(ServerLevel level) {
        DimensionAddedCallback.invoke(level.dimension(), level);
        this.levels.put(level.dimension(), level);
        this.dynamicDimensions.add(level.dimension());
        level.tick(() -> true);
    }

    @Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerFunctionManager;tick()V", shift = At.Shift.AFTER))
    private void markTickingLevels(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        this.tickingLevels = true;
    }

    @Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerConnectionListener;tick()V", shift = At.Shift.BEFORE))
    private void markNotTickingLevels(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        this.tickingLevels = false;
    }

    @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;entrySet()Ljava/util/Set;", shift = At.Shift.BEFORE))
    private void loadDynamicDimensions(CallbackInfo ci) {
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        final Registry<LevelStem> stemRegistry = this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);

        DynamicDimensionLoadCallback.invoke((MinecraftServer) (Object) this, (id, chunkGenerator, type) -> {
            Constants.LOGGER.debug("Loading dynamic dimension '{}'", id);
            Holder.Reference<DimensionType> ref = RegistryUtil.registerUnfreeze(typeRegistry, id, type);
            RegistryUtil.registerUnfreeze(stemRegistry, id, new LevelStem(ref, chunkGenerator));
            this.dynamicDimensions.add(ResourceKey.create(Registries.DIMENSION, id));
        });
    }

    @Override
    public @Nullable ServerLevel createDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canCreateDimension(id)) return null;
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        final Registry<LevelStem> stemRegistry = this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        Constants.LOGGER.debug("Attempting to create dynamic dimension '{}'", id);

        if (typeRegistry.stream().anyMatch(t -> t == type)) {
            return null;
        }

        final DataResult<Tag> encodedType = DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag());
        if (encodedType.error().isPresent()) {
            Constants.LOGGER.error("Failed to encode dimension type! {}", encodedType.error().get().message());
            return null;
        }

        final CompoundTag serializedType = (CompoundTag) encodedType.get().orThrow();

        return this.createDynamicLevel(id, generator, type, typeRegistry, stemRegistry, serializedType, key, true);
    }

    @Override
    public @Nullable ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canCreateDimension(id)) return null;
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        final Registry<LevelStem> stemRegistry = this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        Constants.LOGGER.debug("Attempting to create dynamic dimension '{}'", id);

        if (typeRegistry.stream().anyMatch(t -> t == type)) {
            return null;
        }

        final DataResult<Tag> encodedType = DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag());
        if (encodedType.error().isPresent()) {
            Constants.LOGGER.error("Failed to encode dimension type! {}", encodedType.error().get().message());
            return null;
        }

        final CompoundTag serializedType = (CompoundTag) encodedType.get().orThrow();

        return this.createDynamicLevel(id, generator, type, typeRegistry, stemRegistry, serializedType, key, false);
    }

    @Override
    public boolean dynamicDimensionExists(@NotNull ResourceLocation id) {
        return this.dynamicDimensions.contains(ResourceKey.create(Registries.DIMENSION, id));
    }

    @Override
    public boolean anyDimensionExists(@NotNull ResourceLocation id) {
        return this.levels.containsKey(ResourceKey.create(Registries.DIMENSION, id)) || this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).containsKey(id) || this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM).containsKey(id);
    }

    @Override
    public boolean canDeleteDimension(@NotNull ResourceLocation id) {
        return this.dynamicDimensionExists(id);
    }

    @Override
    public boolean canCreateDimension(@NotNull ResourceLocation id) {
        return !this.anyDimensionExists(id) && !this.dynamicDimensionExists(id) && !this.isIdPendingCreation(id);
    }

    @Override
    public boolean deleteDynamicDimension(@NotNull ResourceLocation id, @Nullable PlayerRemover remover) {
        if (remover == null) {
            remover = PlayerRemover.DEFAULT;
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canDeleteDimension(id)) return false;

        if (this.tickingLevels) {
            this.pendingDeletions.add(new DimensionRemovalTicket(key, remover, true));
        } else {
            this.unloadLevel(key, remover);
            this.deleteLevelData(key);
        }

        return true;
    }

    @Override
    public boolean unloadDynamicDimension(@NotNull ResourceLocation id, @Nullable PlayerRemover remover) {
        if (remover == null) {
            remover = PlayerRemover.DEFAULT;
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!this.canDeleteDimension(id)) return false;

        if (this.tickingLevels) {
            this.pendingDeletions.add(new DimensionRemovalTicket(key, remover, false));
        } else {
            this.unloadLevel(key, remover);
        }

        return true;
    }

    @Unique
    private ServerLevel createDynamicLevel(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type, Registry<DimensionType> typeRegistry, Registry<LevelStem> stemRegistry, CompoundTag serializedType, ResourceKey<Level> key, boolean deleteOldData) {
        final WorldData worldData = this.getWorldData();
        final ServerLevel overworld = this.overworld();
        assert overworld != null;

        final Holder.Reference<DimensionType> typeHolder = RegistryUtil.registerUnfreeze(typeRegistry, id, type);
        assert typeHolder.isBound() : "Registered dimension type not bound?!";

        final LevelStem stem = new LevelStem(typeHolder, generator);
        RegistryUtil.registerUnfreeze(stemRegistry, id, stem);

        final DerivedLevelData data = new DerivedLevelData(worldData, worldData.overworldData()); //todo: do we want separate data?
        if (deleteOldData) {
            this.deleteLevelData(key);
        }
        final ServerLevel level = new ServerLevel(
                (MinecraftServer) (Object) this,
                this.executor,
                this.storageSource,
                data,
                key,
                stem,
                this.progressListenerFactory.create(10),
                worldData.isDebugWorld(),
                BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()),
                ImmutableList.of(),
                false,
                null
        );
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getChunkSource().setSimulationDistance(((DistanceManagerAccessor) ((ServerChunkCacheAccessor) overworld.getChunkSource()).getDistanceManager()).getSimulationDistance());
        level.getChunkSource().setViewDistance(((ChunkMapAccessor) overworld.getChunkSource().chunkMap).getViewDistance());

        if (this.tickingLevels) {
            this.pendingLevels.add(level); //prevent co-modification
        } else {
            this.registerLevel(level);
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(id);
        buf.writeInt(typeRegistry.getId(type));
        buf.writeNbt(serializedType);
        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            PacketSender.s2c(player).send(Constants.CREATE_DIMENSION_PACKET, new FriendlyByteBuf(buf.copy()));
        }
        this.reloadDimensionTags();
        return level;
    }

    @Unique
    private boolean isIdPendingCreation(ResourceLocation id) {
        for (ServerLevel pendingLevel : this.pendingLevels) {
            if (pendingLevel.dimension().location().equals(id)) return true;
        }
        return false;
    }

    @Unique
    private void reloadDimensionTags() {
        for (TagManager.LoadResult<?> result : ((ReloadableServerResourcesAccessor) this.resources.managers()).getTagManager().getResult()) {
            if (result.key() == Registries.DIMENSION_TYPE) {
                Registry<DimensionType> types = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
                types.resetTags();
                //noinspection unchecked - we know that the registry is a registry of dimension types as the key is correct
                types.bindTags(((TagManager.LoadResult<DimensionType>) result).tags().entrySet()
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(entry -> TagKey.create(Registries.DIMENSION_TYPE, entry.getKey()), entry -> entry.getValue().stream().toList())));
                break;
            }
        }
        this.getPlayerList().broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries())));
    }
}
