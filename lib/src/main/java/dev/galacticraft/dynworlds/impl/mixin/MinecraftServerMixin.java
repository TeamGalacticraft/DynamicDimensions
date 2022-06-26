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

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dynworlds.api.DynamicLevelRegistry;
import dev.galacticraft.dynworlds.api.PlayerRemover;
import dev.galacticraft.dynworlds.impl.Constant;
import dev.galacticraft.dynworlds.impl.accessor.ImmutableRegistryAccessAccessor;
import dev.galacticraft.dynworlds.impl.accessor.PrimaryLevelDataAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
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
    private final Map<ResourceKey<Level>, ServerLevel> enqueuedCreatedWorlds = new HashMap<>();
    private final List<ResourceKey<Level>> enqueuedDestroyedWorlds = new ArrayList<>();
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
    private void createDynamicWorlds(ChunkProgressListener listener, CallbackInfo ci) {
        Registry<LevelStem> dimensions = this.getWorldData().worldGenSettings().dimensions();
        assert dimensions instanceof WritableRegistry<LevelStem>;
        ((ImmutableRegistryAccessAccessor) this.registryAccess()).unfreezeTypes(reg -> ((PrimaryLevelDataAccessor) this.getWorldData()).getDynamicWorlds().forEach((id, pair) -> {
            ((WritableRegistry<LevelStem>) dimensions).register(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id), pair, Lifecycle.stable()); // level stem is a mutable registry
            reg.register(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), pair.typeHolder().value(), Lifecycle.stable()); // dimension type must be unfrozen
        }));
        reloadTags();
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void addWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.enqueuedCreatedWorlds.isEmpty()) {
            this.levels.putAll(this.enqueuedCreatedWorlds);
            this.enqueuedCreatedWorlds.clear();
        }
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void removeWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.enqueuedDestroyedWorlds.isEmpty()) {
            for (ResourceKey<Level> key : this.enqueuedDestroyedWorlds) {
                ServerLevel serverWorld = this.levels.remove(key);

                for (ServerPlayer player : serverWorld.players()) {
                    player.connection.disconnect(new TextComponent("The world you were in has been deleted."));
                }

                try {
                    serverWorld.save(null, true, false);
                    serverWorld.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e); // oh no.
                }

                Path worldDir = storageSource.getDimensionPath(key);
                if (Constant.CONFIG.deleteRemovedWorlds()) {
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
                        Constant.LOGGER.error("Failed to move deleted world directory.", e);
                        try {
                            FileUtils.deleteDirectory(worldDir.toFile());
                        } catch (IOException ex) {
                            ex.addSuppressed(e);
                            throw new RuntimeException("Failed to delete deleted world directory!", ex);
                        }
                    }
                }

                MappedRegistry<LevelStem> reg = ((MappedRegistry<LevelStem>) this.getWorldData().worldGenSettings().dimensions());
                LevelStem stem = reg.get(key.location());
                int rawId = reg.getId(stem);
                MappedRegistryAccessor<LevelStem> accessor = (MappedRegistryAccessor<LevelStem>) reg;
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

                ((ImmutableRegistryAccessAccessor) this.registryAccess()).unfreezeTypes(reg1 -> {
                    DimensionType dimensionType = reg1.get(key.location());
                    int rawId1 = reg1.getId(dimensionType);
                    MappedRegistryAccessor<DimensionType> accessor1 = (MappedRegistryAccessor<DimensionType>) reg1;
                    accessor1.getLifecycles().remove(dimensionType);
                    accessor1.getToId().removeInt(dimensionType);
                    accessor1.getByValue().remove(dimensionType);
                    accessor1.setHoldersInOrder(null);
                    accessor1.getByKey().remove(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, key.location()));
                    accessor1.getByLocation().remove(key.location());
                    accessor1.getById().remove(rawId1);
                    Lifecycle base1 = Lifecycle.stable();
                    for (Lifecycle value : accessor1.getLifecycles().values()) {
                        base1.add(value);
                    }
                    accessor1.setElementsLifecycle(base1);
                    FriendlyByteBuf packetByteBuf = PacketByteBufs.create();
                    packetByteBuf.writeResourceLocation(key.location());
                    this.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(Constant.id("destroy_world"), packetByteBuf));
                });
            }
            reloadTags();
            this.enqueuedDestroyedWorlds.clear();
            System.gc(); // destroy everything
        }
    }

    @Override
    public void addDynamicLevel(ResourceLocation id, @NotNull LevelStem stem, DimensionType type) {
        if (!this.canCreateLevel(id)) {
            throw new IllegalArgumentException("World already exists!?");
        }
        ((WritableRegistry<LevelStem>) this.getWorldData().worldGenSettings().dimensions()).register(ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id), stem, Lifecycle.stable());
        ((ImmutableRegistryAccessAccessor) this.registryAccess()).unfreezeTypes(reg -> reg.register(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), type, Lifecycle.stable()));

        ResourceKey<Level> worldKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
        DerivedLevelData properties = new DerivedLevelData(this.getWorldData(), this.getWorldData().overworldData());
        ServerLevel world = new ServerLevel(
                (MinecraftServer) (Object) this,
                this.executor,
                this.storageSource,
                properties,
                worldKey,
                stem.typeHolder(),
                progressListenerFactory.create(10),
                stem.generator(),
                this.getWorldData().worldGenSettings().isDebug(),
                BiomeManager.obfuscateSeed(this.getWorldData().worldGenSettings().seed()),
                ImmutableList.of(),
                false
        );
        ServerLevel overworld = this.overworld();
        assert overworld != null;
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(world.getWorldBorder()));
        world.getChunkSource().setSimulationDistance(((DistanceManagerAccessor) ((ServerChunkCacheAccessor) overworld.getChunkSource()).getDistanceManager()).getSimulationDistance());
        world.getChunkSource().setViewDistance(((ChunkMapAccessor) overworld.getChunkSource().chunkMap).getViewDistance());
        if (stem.typeHolder() instanceof Holder.Reference<DimensionType>
                && (stem.typeHolder().unwrap().right().isEmpty()
                || stem.typeHolder().value() != type)) {
            ((HolderReferenceInvoker<DimensionType>) stem.typeHolder()).callBind(stem.typeHolder().unwrapKey().get(), type);
        }
        ((PrimaryLevelDataAccessor) this.getWorldData()).addDynamicLevel(id, stem);
        this.enqueuedCreatedWorlds.put(worldKey, world); //prevent comodification

        FriendlyByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeResourceLocation(id);
        packetByteBuf.writeInt(this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getId(type));
        packetByteBuf.writeNbt((CompoundTag) DimensionType.DIRECT_CODEC.encode(type, NbtOps.INSTANCE, new CompoundTag()).get().orThrow());
        this.getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(Constant.id("create_world"), packetByteBuf));
        this.reloadTags();
    }

    @Override
    public boolean levelExists(ResourceLocation id) {
        return this.levels.containsKey(ResourceKey.create(Registry.DIMENSION_REGISTRY, id))
                && this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).containsKey(id)
                || this.getWorldData().worldGenSettings().dimensions().containsKey(id)
                || ((PrimaryLevelDataAccessor) this.getWorldData()).getDynamicWorlds().containsKey(id);
    }

    @Override
    public boolean canCreateLevel(ResourceLocation id) {
        return Constant.CONFIG.allowWorldCreation() && !this.levelExists(id);
    }

    @Override
    public boolean canDestroyLevel(ResourceLocation id) {
        return this.levelExists(id) && (Constant.CONFIG.deleteWorldsWithPlayers() || this.levels.get(ResourceKey.create(Registry.DIMENSION_REGISTRY, id)).players().size() == 0);
    }

    @Override
    public void removeDynamicLevel(ResourceLocation id, @Nullable PlayerRemover remover) {
        if (!this.canDestroyLevel(id)) {
            throw new IllegalArgumentException("Cannot destroy world!");
        }

        ((PrimaryLevelDataAccessor) this.getWorldData()).removeDynamicLevel(id); //worst case, it'll just be gone on reload
        ResourceKey<Level> of = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
        for (ServerPlayer serverPlayerEntity : this.getPlayerList().getPlayers()) {
            if (serverPlayerEntity.level.dimension().equals(of)) {
                if (remover == null) {
                    throw new IllegalArgumentException("Cannot remove world as it is currently loaded by a player!");
                } else {
                    remover.removePlayer((MinecraftServer) (Object) this, serverPlayerEntity);
                }
            }
        }

        this.enqueuedDestroyedWorlds.add(of);
    }

    private void reloadTags() {
        for (TagManager.LoadResult<?> registryTag : ((ReloadableServerResourcesAccessor) this.resources.managers()).getTagManager().getResult()) {
            if (registryTag.key() == Registry.DIMENSION_TYPE_REGISTRY) {
                Registry<DimensionType> dimensionTypes = this.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
                dimensionTypes.resetTags();
                //noinspection unchecked - we know that the registry is a registry of dimension types as the key is correct
                dimensionTypes.bindTags(((TagManager.LoadResult<DimensionType>) registryTag).tags().entrySet()
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(entry -> TagKey.create(Registry.DIMENSION_TYPE_REGISTRY, entry.getKey()), entry -> (entry.getValue()).getValues())));
                break;
            }
        }
        this.getPlayerList().broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registryAccess())));
    }
}
