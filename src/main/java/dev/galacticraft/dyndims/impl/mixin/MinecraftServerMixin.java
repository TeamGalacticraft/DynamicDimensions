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

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dyndims.api.DynamicLevelRegistry;
import dev.galacticraft.dyndims.api.PlayerRemover;
import dev.galacticraft.dyndims.impl.DynamicDimensions;
import dev.galacticraft.dyndims.impl.accessor.PrimaryLevelDataAccessor;
import dev.galacticraft.dyndims.impl.util.UnfrozenRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements DynamicLevelRegistry {
    private final Map<ResourceKey<Level>, ServerLevel> levelsAwaitingCreation = new HashMap<>();
    private final List<ResourceKey<Level>> levelsAwaitingDeletion = new ArrayList<>();
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

    @Inject(method = "createLevels", at = @At("HEAD"))
    private void createDynamicLevels(ChunkProgressListener listener, CallbackInfo ci) {
        try (UnfrozenRegistry<DimensionType> unfrozen = UnfrozenRegistry.create(this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY))) {
            try (UnfrozenRegistry<LevelStem> unfrozenLevelStem = UnfrozenRegistry.create(this.getWorldData().worldGenSettings().dimensions())) {
                ((PrimaryLevelDataAccessor) this.getWorldData()).getDynamicDimensions().forEach((id, pair) -> {
                    Holder<DimensionType> dimHolder = unfrozen.registry().register(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), pair.getSecond(), Lifecycle.stable()); // dimension type must be unfrozen
                    unfrozenLevelStem.registry().register(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id), new LevelStem(dimHolder, pair.getFirst()), Lifecycle.stable());
                });
            }
        }
        this.reloadTags();
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void addLevels(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.levelsAwaitingCreation.isEmpty()) {
            this.levels.putAll(this.levelsAwaitingCreation);
            this.levelsAwaitingCreation.clear();
        }
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void removeLevels(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.levelsAwaitingDeletion.isEmpty()) {
            for (ResourceKey<Level> key : this.levelsAwaitingDeletion) {
                ServerLevel serverWorld = this.levels.remove(key);

                for (ServerPlayer player : serverWorld.players()) {
                    player.connection.disconnect(Component.translatable("dyndims.dimension_disconnect"));
                }

                try {
                    serverWorld.save(null, true, false);
                    serverWorld.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e); // oh no.
                }

                Path worldDir = storageSource.getDimensionPath(key);
                if (DynamicDimensions.CONFIG.deleteRemovedDimensions()) {
                    try {
                        FileUtils.deleteDirectory(worldDir.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete deleted world directory!", e);
                    }
                } else {
                    try {
                        Path resolved;
                        if (worldDir.getParent().getFileName().toString().equals(key.location().getNamespace())) {
                            resolved = worldDir.getParent().resolveSibling("deleted").resolve(key.location().toString());
                        } else {
                            resolved = worldDir.resolveSibling(key.location().toString() + "_deleted");
                        }
                        if (resolved.toFile().exists()) {
                            FileUtils.deleteDirectory(resolved.toFile());
                        }
                        Files.move(worldDir, resolved, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        DynamicDimensions.LOGGER.error("Failed to move deleted world directory.", e);
                        try {
                            FileUtils.deleteDirectory(worldDir.toFile());
                        } catch (IOException ex) {
                            ex.addSuppressed(e);
                            throw new RuntimeException("Failed to delete deleted world directory!", ex);
                        }
                    }
                }

                try (UnfrozenRegistry<LevelStem> unfrozen = UnfrozenRegistry.create(this.getWorldData().worldGenSettings().dimensions())) {
                    MappedRegistry<LevelStem> reg = unfrozen.registry();
                    LevelStem stem = reg.get(key.location());
                    int rawId = reg.getId(stem);
                    MappedRegistryAccessor<LevelStem> accessor = (MappedRegistryAccessor<LevelStem>) unfrozen.registry();
                    accessor.getLifecycles().remove(stem);
                    accessor.getToId().removeInt(stem);
                    accessor.getByValue().remove(stem);
                    accessor.setHoldersInOrder(null);
                    accessor.getByKey().remove(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, key.location()));
                    accessor.getByLocation().remove(key.location());
                    accessor.getById().remove(rawId);
                    Lifecycle base = Lifecycle.stable();
                    for (Lifecycle value : accessor.getLifecycles().values()) {
                        base.add(value);
                    }
                    accessor.setElementsLifecycle(base);
                }

                try (UnfrozenRegistry<DimensionType> unfrozen = UnfrozenRegistry.create(this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY))) {
                    DimensionType dimensionType = unfrozen.registry().get(key.location());
                    int rawId = unfrozen.registry().getId(dimensionType);
                    MappedRegistryAccessor<DimensionType> accessor = (MappedRegistryAccessor<DimensionType>) unfrozen.registry();
                    accessor.getLifecycles().remove(dimensionType);
                    accessor.getToId().removeInt(dimensionType);
                    accessor.getByValue().remove(dimensionType);
                    accessor.setHoldersInOrder(null);
                    accessor.getByKey().remove(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, key.location()));
                    accessor.getByLocation().remove(key.location());
                    accessor.getById().remove(rawId);
                    Lifecycle base1 = Lifecycle.stable();
                    for (Lifecycle value : accessor.getLifecycles().values()) {
                        base1.add(value);
                    }
                    accessor.setElementsLifecycle(base1);
                    FriendlyByteBuf packetByteBuf = PacketByteBufs.create();
                    packetByteBuf.writeResourceLocation(key.location());
                    this.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(DynamicDimensions.DELETE_WORLD_PACKET, packetByteBuf));
                }
            }
            reloadTags();
            this.levelsAwaitingDeletion.clear();
            System.gc(); // destroy everything
        }
    }

    @Override
    public boolean addDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type) {
        if (!this.canCreateDimension(id)) return false;
        Holder<DimensionType> typeHolder;
        try (UnfrozenRegistry<DimensionType> unfrozen = UnfrozenRegistry.create(this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY))) {
            typeHolder = unfrozen.registry().register(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), type, Lifecycle.stable());
        }

        LevelStem stem = new LevelStem(typeHolder, chunkGenerator);

        try (UnfrozenRegistry<LevelStem> unfrozen = UnfrozenRegistry.create(this.getWorldData().worldGenSettings().dimensions())) {
            unfrozen.registry().register(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id), stem, Lifecycle.stable());
        }

        ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
        DerivedLevelData data = new DerivedLevelData(this.getWorldData(), this.getWorldData().overworldData());
        ServerLevel level = new ServerLevel(
                (MinecraftServer) (Object) this,
                this.executor,
                this.storageSource,
                data,
                key,
                stem,
                progressListenerFactory.create(10),
                this.getWorldData().worldGenSettings().isDebug(),
                BiomeManager.obfuscateSeed(this.getWorldData().worldGenSettings().seed()),
                ImmutableList.of(),
                false
        );
        ServerLevel overworld = this.overworld();
        assert overworld != null;
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getChunkSource().setSimulationDistance(((DistanceManagerAccessor) ((ServerChunkCacheAccessor) overworld.getChunkSource()).getDistanceManager()).getSimulationDistance());
        level.getChunkSource().setViewDistance(((ChunkMapAccessor) overworld.getChunkSource().chunkMap).getViewDistance());
        assert stem.typeHolder().isBound();
        ((PrimaryLevelDataAccessor) this.getWorldData()).addDynamicDimension(id, chunkGenerator, typeHolder.value());
        this.levelsAwaitingCreation.put(key, level); //prevent co-modification

        FriendlyByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeResourceLocation(id);
        packetByteBuf.writeInt(this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getId(type));
        packetByteBuf.writeNbt((CompoundTag) DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag()).get().orThrow());
        this.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(DynamicDimensions.CREATE_WORLD_PACKET, packetByteBuf));
        this.reloadTags();
        return true;
    }

    @Override
    public boolean dimensionExists(@NotNull ResourceLocation id) {
        return this.levels.containsKey(ResourceKey.create(Registry.DIMENSION_REGISTRY, id))
                && this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).containsKey(id)
                || this.getWorldData().worldGenSettings().dimensions().containsKey(id)
                || ((PrimaryLevelDataAccessor) this.getWorldData()).getDynamicDimensions().containsKey(id);
    }

    @Override
    public boolean canCreateDimension(@NotNull ResourceLocation id) {
        return DynamicDimensions.CONFIG.allowDimensionCreation() && !this.dimensionExists(id);
    }

    @Override
    public boolean canDeleteDimension(@NotNull ResourceLocation id) {
        return this.dimensionExists(id) && (DynamicDimensions.CONFIG.deleteDimensionsWithPlayers() || this.levels.get(ResourceKey.create(Registry.DIMENSION_REGISTRY, id)).players().size() == 0);
    }

    @Override
    public boolean removeDynamicDimension(@NotNull ResourceLocation id, @NotNull PlayerRemover remover) {
        if (!this.canDeleteDimension(id)) return false;

        ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
        List<ServerPlayer> players = new ArrayList<>(this.levels.get(key).players()); // prevent co-modification
        if (!players.isEmpty()) {
            if (DynamicDimensions.CONFIG.deleteDimensionsWithPlayers()) {
                for (ServerPlayer player : players) {
                    if (player.level.dimension().equals(key)) {
                        remover.removePlayer((MinecraftServer) (Object) this, player);
                    }
                }
            } else {
                return false;
            }
        }

        ((PrimaryLevelDataAccessor) this.getWorldData()).removeDynamicDimension(id);
        this.levelsAwaitingDeletion.add(key);
        return true;
    }

    private void reloadTags() {
        for (TagManager.LoadResult<?> result : ((ReloadableServerResourcesAccessor) this.resources.managers()).getTagManager().getResult()) {
            if (result.key() == Registry.DIMENSION_TYPE_REGISTRY) {
                Registry<DimensionType> types = this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
                types.resetTags();
                //noinspection unchecked - we know that the registry is a registry of dimension types as the key is correct
                types.bindTags(((TagManager.LoadResult<DimensionType>) result).tags().entrySet()
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(entry -> TagKey.create(Registry.DIMENSION_TYPE_REGISTRY, entry.getKey()), entry -> entry.getValue().stream().toList())));
                break;
            }
        }
        this.getPlayerList().broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registryAccess())));
    }
}
