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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dyndims.api.DynamicDimensionRegistry;
import dev.galacticraft.dyndims.api.PlayerRemover;
import dev.galacticraft.dyndims.impl.Constants;
import dev.galacticraft.dyndims.impl.accessor.PrimaryLevelDataAccessor;
import dev.galacticraft.dyndims.impl.platform.Services;
import dev.galacticraft.dyndims.impl.registry.UnfrozenRegistry;
import io.netty.buffer.Unpooled;
import lol.bai.badpackets.api.PacketSender;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
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

    @Inject(method = "createLevels", at = @At("HEAD"))
    private void createDynamicLevels(ChunkProgressListener listener, CallbackInfo ci) {
        Registry<DimensionType> types = this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        try (UnfrozenRegistry<LevelStem> unfrozenLevelStem = Services.PLATFORM.unfreezeRegistry(this.getWorldData().worldGenSettings().dimensions())) {
            ((PrimaryLevelDataAccessor) this.getWorldData()).getDynamicDimensions().forEach((id, pair) -> {
                if (!unfrozenLevelStem.registry().containsKey(id)) {
                    Holder<DimensionType> dimHolder = types.getHolder(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id)).orElseThrow();
                    unfrozenLevelStem.registry().register(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id), new LevelStem(dimHolder, pair.getFirst()), Lifecycle.stable());
                }
            });
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
                try (ServerLevel serverWorld = this.levels.remove(key)) {
                    for (ServerPlayer player : serverWorld.players()) {
                        player.connection.disconnect(Component.translatable("dyndims.dimension_disconnect")); //TODO: what happens to these players?
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

                this.removeEntryFromRegistry(this.getWorldData().worldGenSettings().dimensions(), Registry.LEVEL_STEM_REGISTRY, key.location());
                this.removeEntryFromRegistry(this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), Registry.DIMENSION_TYPE_REGISTRY, key.location());
                FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(Unpooled.buffer());
                packetByteBuf.writeResourceLocation(key.location());
                this.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(Constants.DELETE_WORLD_PACKET, packetByteBuf));
            }
            this.reloadTags();
            this.levelsAwaitingDeletion.clear();
        }
    }

    @Override
    public boolean addDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type) {
        if (!this.canCreateDimension(id)) return false;

        final ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
        final WorldData worldData = this.getWorldData();
        final DataResult<Tag> encode = DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag());
        final Registry<DimensionType> typeRegistry = this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        final ServerLevel overworld = this.overworld();
        Holder<DimensionType> typeHolder;
        CompoundTag encodedDimensionType;

        assert overworld != null;

        if (encode.error().isPresent()) {
            Constants.LOGGER.error("Failed to encode dimension type! {}", encode.error().get().message());
            return false;
        } else {
            encodedDimensionType = (CompoundTag) encode.get().orThrow();
        }

        try (UnfrozenRegistry<DimensionType> unfrozen = Services.PLATFORM.unfreezeRegistry(typeRegistry)) {
            typeHolder = unfrozen.registry().register(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), type, Lifecycle.stable());
        }

        assert typeHolder.isBound() : "Registered dimension type not bound?!";
        LevelStem stem = new LevelStem(typeHolder, chunkGenerator);

        try (UnfrozenRegistry<LevelStem> unfrozen = Services.PLATFORM.unfreezeRegistry(worldData.worldGenSettings().dimensions())) {
            unfrozen.registry().register(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id), stem, Lifecycle.stable());
        }

        DerivedLevelData data = new DerivedLevelData(worldData, worldData.overworldData());
        ServerLevel level = new ServerLevel(
                (MinecraftServer) (Object) this,
                this.executor,
                this.storageSource,
                data,
                key,
                stem,
                this.progressListenerFactory.create(10),
                worldData.worldGenSettings().isDebug(),
                BiomeManager.obfuscateSeed(worldData.worldGenSettings().seed()),
                ImmutableList.of(),
                false
        );
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getChunkSource().setSimulationDistance(((DistanceManagerAccessor) ((ServerChunkCacheAccessor) overworld.getChunkSource()).getDistanceManager()).getSimulationDistance());
        level.getChunkSource().setViewDistance(((ChunkMapAccessor) overworld.getChunkSource().chunkMap).getViewDistance());
        ((PrimaryLevelDataAccessor) worldData).addDynamicDimension(id, chunkGenerator, typeHolder.value());
        this.levelsAwaitingCreation.put(key, level); //prevent co-modification

        FriendlyByteBuf packetByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        packetByteBuf.writeResourceLocation(id);
        packetByteBuf.writeInt(typeRegistry.getId(type));
        packetByteBuf.writeNbt(encodedDimensionType);
        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            PacketSender.s2c(player).send(Constants.CREATE_WORLD_PACKET, new FriendlyByteBuf(packetByteBuf.copy()));
        }
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
        return Constants.CONFIG.allowDimensionCreation() && !this.dimensionExists(id);
    }

    @Override
    public boolean canDeleteDimension(@NotNull ResourceLocation id) {
        return this.dimensionExists(id) && (Constants.CONFIG.deleteDimensionsWithPlayers() || this.levels.get(ResourceKey.create(Registry.DIMENSION_REGISTRY, id)).players().size() == 0);
    }

    @Override
    public boolean removeDynamicDimension(@NotNull ResourceLocation id, @NotNull PlayerRemover remover) {
        if (!this.canDeleteDimension(id)) return false;

        ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
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

    private <T> void removeEntryFromRegistry(Registry<T> registry, ResourceKey<Registry<T>> key, ResourceLocation id) {
        try (UnfrozenRegistry<T> unfrozen = Services.PLATFORM.unfreezeRegistry(registry)) {
            final T value = unfrozen.registry().get(id);
            final int rawId = unfrozen.registry().getId(value);
            final var accessor = (MappedRegistryAccessor<T>) unfrozen.registry();
            accessor.getLifecycles().remove(value);
            accessor.getToId().removeInt(value);
            accessor.getByValue().remove(value);
            accessor.setHoldersInOrder(null);
            accessor.getByKey().remove(ResourceKey.create(key, id));
            accessor.getByLocation().remove(id);
            accessor.getById().remove(rawId);
            Lifecycle base = Lifecycle.stable();
            for (Lifecycle lifecycle : accessor.getLifecycles().values()) {
                base.add(lifecycle);
            }
            accessor.setElementsLifecycle(base);
        }
    }
}
