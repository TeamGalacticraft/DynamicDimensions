/*
 * Copyright (c) 2021-2023 Team Galacticraft
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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import dev.galacticraft.dynamicdimensions.api.event.DimensionAddedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DimensionRemovedCallback;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.accessor.PrimaryLevelDataAccessor;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.RandomSource;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements DynamicDimensionRegistry {
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

    @Shadow public abstract LayeredRegistryAccess<RegistryLayer> registries();

    @Shadow @Final private RandomSource random;

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.BEFORE))
    private void addLevels(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.levelsAwaitingCreation.isEmpty()) {
            for (Map.Entry<ResourceKey<Level>, ServerLevel> entry : this.levelsAwaitingCreation.entrySet()) {
                DimensionAddedCallback.invoke(entry.getKey(), entry.getValue());
                this.levels.put(entry.getKey(), entry.getValue());
            }
            this.levelsAwaitingCreation.clear();
        }
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void removeLevels(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.levelsAwaitingDeletion.isEmpty()) {
            for (ResourceKey<Level> key : this.levelsAwaitingDeletion) {
                try (ServerLevel serverWorld = this.levels.remove(key)) {
                    DimensionRemovedCallback.invoke(key, serverWorld);
                    for (ServerPlayer player : serverWorld.players()) {
                        player.connection.disconnect(Component.translatable("dynamicdimensions.dimension_disconnect")); //TODO: what happens to these players?
                    }

                    serverWorld.save(null, true, false);
                } catch (IOException e) {
                    Constants.LOGGER.error("Failed to close level upon removal! Memory may have been leaked.", e);
                }

                Path worldDir = this.storageSource.getDimensionPath(key);
                if (Constants.CONFIG.deleteRemovedDimensions()) {
                    try {
                        FileUtils.deleteDirectory(worldDir.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete deleted world directory!", e);
                    }
                } else {
                    try {
                        Path resolved;
                        String id = key.location().toString().replace(":", ",");
                        if (worldDir.getParent().getFileName().toString().equals(key.location().getNamespace())) {
                            resolved = worldDir.getParent().resolveSibling("deleted").resolve(id);
                        } else {
                            resolved = worldDir.resolveSibling(id + "_deleted");
                        }
                        if (resolved.toFile().exists()) {
                            FileUtils.deleteDirectory(resolved.toFile());
                        }
                        resolved.toFile().mkdirs();
                        Files.move(worldDir, resolved, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Constants.LOGGER.error("Failed to move removed dimension's directory.", e);
                        try {
                            FileUtils.deleteDirectory(worldDir.toFile());
                        } catch (IOException ex) {
                            ex.addSuppressed(e);
                            throw new RuntimeException("Failed to delete removed dimension's directory!", ex); // TODO: is a throw necessary here? what happens if a world is re-created with the same id? is it exploitable?
                        }
                    }
                }

                RegistryUtil.unregister(this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM), key.location());
                RegistryUtil.unregister(this.registries().compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE), key.location());

                FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(Unpooled.buffer());
                packetByteBuf.writeResourceLocation(key.location());
                this.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(Constants.DELETE_WORLD_PACKET, packetByteBuf));
            }
            this.levelsAwaitingDeletion.clear();
        }
    }

    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void loadDynamicDimensions(CallbackInfo ci) {
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        final Registry<LevelStem> stemRegistry = this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        Map<ResourceLocation, Pair<ChunkGenerator, DimensionType>> dimensions = ((PrimaryLevelDataAccessor) this.getWorldData()).dynamicDimensions();
        for (Map.Entry<ResourceLocation, Pair<ChunkGenerator, DimensionType>> entry : dimensions.entrySet()) {
            Holder.Reference<DimensionType> ref = RegistryUtil.registerUnfreeze(typeRegistry, entry.getKey(), entry.getValue().getSecond());
            RegistryUtil.registerUnfreeze(stemRegistry, entry.getKey(), new LevelStem(ref, entry.getValue().getFirst()));
        }
    }

    @Override
    public @Nullable ResourceLocation addDynamicDimension(@NotNull ChunkGenerator generator, @NotNull DimensionType type) {
        if (!this.canCreateDimensions()) return null;
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        final Registry<LevelStem> stemRegistry = this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);

        assert typeRegistry.stream().noneMatch(t -> t == type);

        final DataResult<Tag> encodedType = DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag());
        if (encodedType.error().isPresent()) {
            Constants.LOGGER.error("Failed to encode dimension type! {}", encodedType.error().get().message());
            return null;
        }

        final CompoundTag serializedType = (CompoundTag) encodedType.get().orThrow();

        ResourceLocation id;
        ResourceKey<Level> key;
        do {
            id = new ResourceLocation(Constants.DEFAULT_NAMESPACE, Long.toHexString(this.random.nextLong()));
            key = ResourceKey.create(Registries.DIMENSION, id);
        } while (typeRegistry.containsKey(id) || stemRegistry.containsKey(id) || this.levels.containsKey(key));

        createDynamicWorld(id, generator, type, typeRegistry, stemRegistry, serializedType, key);
        return id;
    }

    @Override
    public boolean addDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type) {
        if (!this.canCreateDimensions()) return false;
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        final Registry<LevelStem> stemRegistry = this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);

        if (typeRegistry.stream().anyMatch(t -> t == type)) {
            return false;
        }

        final DataResult<Tag> encodedType = DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag());
        if (encodedType.error().isPresent()) {
            Constants.LOGGER.error("Failed to encode dimension type! {}", encodedType.error().get().message());
            return false;
        }

        final CompoundTag serializedType = (CompoundTag) encodedType.get().orThrow();
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);

        createDynamicWorld(id, generator, type, typeRegistry, stemRegistry, serializedType, key);
        return true;
    }

    private void createDynamicWorld(@NotNull ResourceLocation id, @NotNull ChunkGenerator generator, @NotNull DimensionType type, Registry<DimensionType> typeRegistry, Registry<LevelStem> stemRegistry, CompoundTag serializedType, ResourceKey<Level> key) {
        final WorldData worldData = this.getWorldData();
        final ServerLevel overworld = this.overworld();
        assert overworld != null;

        final Holder.Reference<DimensionType> typeHolder = RegistryUtil.registerUnfreeze(typeRegistry, id, type);
        assert typeHolder.isBound() : "Registered dimension type not bound?!";

        final LevelStem stem = new LevelStem(typeHolder, generator);
        RegistryUtil.registerUnfreeze(stemRegistry, id, stem);

        final DerivedLevelData data = new DerivedLevelData(worldData, worldData.overworldData()); //todo: do we want separate data?
        Path path = this.storageSource.getDimensionPath(key);
        File directory = path.toFile();
        if (directory.exists()) {
            try {
                FileUtils.moveDirectory(directory, path.resolveSibling(directory.getName() + ".deleted").toFile());
            } catch (IOException e) {
                try {
                    FileUtils.deleteDirectory(directory);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }
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
                false
        );
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getChunkSource().setSimulationDistance(((DistanceManagerAccessor) ((ServerChunkCacheAccessor) overworld.getChunkSource()).getDistanceManager()).getSimulationDistance());
        level.getChunkSource().setViewDistance(((ChunkMapAccessor) overworld.getChunkSource().chunkMap).getViewDistance());
        ((PrimaryLevelDataAccessor) worldData).addDynamicDimension(id, generator, typeHolder.value());
        this.levelsAwaitingCreation.put(key, level); //prevent co-modification

        FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        packetByteBuf.writeResourceLocation(id);
        packetByteBuf.writeInt(typeRegistry.getId(type));
        packetByteBuf.writeNbt(serializedType);
        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            PacketSender.s2c(player).send(Constants.CREATE_WORLD_PACKET, new FriendlyByteBuf(packetByteBuf.copy()));
        }
        this.reloadTags();
    }

    @Override
    public boolean dynamicDimensionExists(@NotNull ResourceLocation id) {
        return ((PrimaryLevelDataAccessor) this.getWorldData()).dynamicDimensionExists(id);
    }

    @Override
    public boolean anyDimensionExists(@NotNull ResourceLocation id) {
        return this.levels.containsKey(ResourceKey.create(Registries.DIMENSION, id)) || this.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).containsKey(id) || this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM).containsKey(id);
    }

    private boolean canCreateDimensions() {
        return Constants.CONFIG.allowDimensionCreation();
    }

    @Override
    public boolean canDeleteDimension(@NotNull ResourceLocation id) {
        return this.dynamicDimensionExists(id) && (Constants.CONFIG.deleteDimensionsWithPlayers() || this.levels.get(ResourceKey.create(Registries.DIMENSION, id)).players().size() == 0);
    }

    @Override
    public boolean removeDynamicDimension(@NotNull ResourceLocation id, @NotNull PlayerRemover remover) {
        if (!this.canDeleteDimension(id)) return false;

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        List<ServerPlayer> players = new ArrayList<>(this.levels.get(key).players()); // prevent co-modification
        if (!players.isEmpty()) {
            if (Constants.CONFIG.deleteDimensionsWithPlayers()) {
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
