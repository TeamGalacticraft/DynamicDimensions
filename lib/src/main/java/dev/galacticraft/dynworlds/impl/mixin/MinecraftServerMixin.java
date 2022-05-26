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
import dev.galacticraft.dynworlds.api.DynamicWorldRegistry;
import dev.galacticraft.dynworlds.api.PlayerDestroyer;
import dev.galacticraft.dynworlds.impl.Constant;
import dev.galacticraft.dynworlds.impl.accessor.DynamicRegistryManagerImmutableImplAccessor;
import dev.galacticraft.dynworlds.impl.accessor.SavePropertiesAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.SynchronizeTagsS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.TagKey;
import net.minecraft.tag.TagManagerLoader;
import net.minecraft.tag.TagPacketSerializer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.*;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements DynamicWorldRegistry {
    private final Map<RegistryKey<World>, ServerWorld> enqueuedCreatedWorlds = new HashMap<>();
    private final List<RegistryKey<World>> enqueuedDestroyedWorlds = new ArrayList<>();
    @Shadow
    @Final
    protected LevelStorage.Session session;
    @Shadow
    private MinecraftServer.ResourceManagerHolder resourceManagerHolder;
    @Shadow
    @Final
    private Executor workerExecutor;
    @Shadow
    @Final
    private WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory;
    @Shadow
    @Final
    private Map<RegistryKey<World>, ServerWorld> worlds;

    @Shadow
    public abstract ServerWorld getOverworld();

    @Shadow
    public abstract PlayerManager getPlayerManager();

    @Shadow
    public abstract DynamicRegistryManager.Immutable getRegistryManager();

    @Shadow
    public abstract SaveProperties getSaveProperties();

    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void createDynamicWorlds(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        Registry<DimensionOptions> dimensions = this.getSaveProperties().getGeneratorOptions().getDimensions();
        assert dimensions instanceof MutableRegistry<DimensionOptions>;
        ((DynamicRegistryManagerImmutableImplAccessor) this.getRegistryManager()).unfreezeTypes(reg -> ((SavePropertiesAccessor) this.getSaveProperties()).getDynamicWorlds().forEach((id, pair) -> {
            ((MutableRegistry<DimensionOptions>) dimensions).add(RegistryKey.of(Registry.DIMENSION_KEY, id), pair, Lifecycle.stable()); // dimension options is a mutable registry
            reg.add(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id), pair.getDimensionTypeSupplier().value(), Lifecycle.stable()); // dimension type must be unfrozen
        }));
        reloadTags();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void addWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.enqueuedCreatedWorlds.isEmpty()) {
            this.worlds.putAll(this.enqueuedCreatedWorlds);
            this.enqueuedCreatedWorlds.clear();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void removeWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.enqueuedDestroyedWorlds.isEmpty()) {
            for (RegistryKey<World> key : this.enqueuedDestroyedWorlds) {
                ServerWorld serverWorld = this.worlds.remove(key);

                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    player.networkHandler.disconnect(new LiteralText("The world you were in has been deleted."));
                }

                try {
                    serverWorld.close();
                } catch (IOException e) {
                    throw new RuntimeException(e); // oh no.
                }

                try {
                    Files.move(session.getWorldDirectory(key), session.getWorldDirectory(key).resolveSibling(key.getValue().toString() + "_deleted"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
//                throw new RuntimeException(e);
                }

                SimpleRegistry<DimensionOptions> reg = ((SimpleRegistry<DimensionOptions>) this.getSaveProperties().getGeneratorOptions().getDimensions());
                DimensionOptions dimensionOptions = reg.get(key.getValue());
                int rawId = reg.getRawId(dimensionOptions);
                SimpleRegistryAccessor<DimensionOptions> accessor = (SimpleRegistryAccessor<DimensionOptions>) reg;
                accessor.getEntryToLifecycle().remove(dimensionOptions);
                accessor.getEntryToRawId().removeInt(dimensionOptions);
                accessor.getValueToEntry().remove(dimensionOptions);
                accessor.setCachedEntries(null);
                accessor.getKeyToEntry().remove(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, key.getValue()));
                accessor.getIdToEntry().remove(key.getValue());
                accessor.getRawIdToEntry().remove(rawId);
                Lifecycle base = Lifecycle.stable();
                for (Lifecycle value : accessor.getEntryToLifecycle().values()) {
                    base.add(value);
                }
                accessor.setLifecycle(base);

                ((DynamicRegistryManagerImmutableImplAccessor) this.getRegistryManager()).unfreezeTypes(reg1 -> {
                    DimensionType dimensionType = reg1.get(key.getValue());
                    int rawId1 = reg1.getRawId(dimensionType);
                    SimpleRegistryAccessor<DimensionType> accessor1 = (SimpleRegistryAccessor<DimensionType>) reg1;
                    accessor1.getEntryToLifecycle().remove(dimensionType);
                    accessor1.getEntryToRawId().removeInt(dimensionType);
                    accessor1.getValueToEntry().remove(dimensionType);
                    accessor1.setCachedEntries(null);
                    accessor1.getKeyToEntry().remove(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, key.getValue()));
                    accessor1.getIdToEntry().remove(key.getValue());
                    accessor1.getRawIdToEntry().remove(rawId1);
                    Lifecycle base1 = Lifecycle.stable();
                    for (Lifecycle value : accessor1.getEntryToLifecycle().values()) {
                        base1.add(value);
                    }
                    accessor1.setLifecycle(base1);
                    PacketByteBuf packetByteBuf = PacketByteBufs.create();
                    packetByteBuf.writeIdentifier(key.getValue());
                    this.getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Constant.id("destroy_world"), packetByteBuf));
                });
            }
            reloadTags();
            this.enqueuedDestroyedWorlds.clear();
            System.gc(); // destroy everything
        }
    }

    @Override
    public void addDynamicWorld(Identifier id, @NotNull DimensionOptions options, DimensionType type) {
        if (worldExists(id) || this.worlds.containsKey(RegistryKey.of(Registry.WORLD_KEY, id))) {
            throw new IllegalArgumentException("World already exists!?");
        }
        ((MutableRegistry<DimensionOptions>) this.getSaveProperties().getGeneratorOptions().getDimensions()).add(RegistryKey.of(Registry.DIMENSION_KEY, id), options, Lifecycle.stable());
        ((DynamicRegistryManagerImmutableImplAccessor) this.getRegistryManager()).unfreezeTypes(reg -> reg.add(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id), type, Lifecycle.stable()));

        RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, id);
        UnmodifiableLevelProperties properties = new UnmodifiableLevelProperties(this.getSaveProperties(), this.getSaveProperties().getMainWorldProperties());
        ServerWorld world = new ServerWorld(
                (MinecraftServer) (Object) this,
                this.workerExecutor,
                this.session,
                properties,
                worldKey,
                options.getDimensionTypeSupplier(),
                worldGenerationProgressListenerFactory.create(10),
                options.getChunkGenerator(),
                this.getSaveProperties().getGeneratorOptions().isDebugWorld(),
                BiomeAccess.hashSeed(this.getSaveProperties().getGeneratorOptions().getSeed()),
                ImmutableList.of(),
                false
        );
        ServerWorld overworld = this.getOverworld();
        assert overworld != null;
        overworld.getWorldBorder().addListener(new WorldBorderListener.WorldBorderSyncer(world.getWorldBorder()));
        world.getChunkManager().applySimulationDistance(((ChunkTicketManagerAccessor) ((ServerChunkManagerAccessor) overworld.getChunkManager()).getTicketManager()).getSimulationDistance());
        world.getChunkManager().applyViewDistance(((ThreadedAnvilChunkStorageAccessor) overworld.getChunkManager().threadedAnvilChunkStorage).getRenderDistance());
        if (options.getDimensionTypeSupplier() instanceof RegistryEntry.Reference<DimensionType>
                && (options.getDimensionTypeSupplier().getKeyOrValue().right().isEmpty()
                || options.getDimensionTypeSupplier().value() != type)) {
            ((RegistryEntryReferenceInvoker<DimensionType>) options.getDimensionTypeSupplier()).callSetKeyAndValue(options.getDimensionTypeSupplier().getKey().get(), type);
        }
        ((SavePropertiesAccessor) this.getSaveProperties()).addDynamicWorld(id, options);
        this.enqueuedCreatedWorlds.put(worldKey, world); //prevent comodification

        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeIdentifier(id);
        packetByteBuf.writeInt(this.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(type));
        packetByteBuf.writeNbt((NbtCompound) DimensionType.CODEC.encode(type, NbtOps.INSTANCE, new NbtCompound()).get().orThrow());
        this.getPlayerManager().sendToAll(new CustomPayloadS2CPacket(Constant.id("create_world"), packetByteBuf));
        reloadTags();
    }

    @Override
    public boolean worldExists(Identifier id) {
        return this.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).containsId(id)
                || this.getSaveProperties().getGeneratorOptions().getDimensions().containsId(id)
                || ((SavePropertiesAccessor) this.getSaveProperties()).getDynamicWorlds().containsKey(id);
    }

    @Override
    public void removeDynamicWorld(Identifier id, @Nullable PlayerDestroyer destroyer) {
        if (this.worldExists(id)) { //don't want to accidentally remove the overworld/nether/whatever

            ((SavePropertiesAccessor) this.getSaveProperties()).removeDynamicWorld(id); //worst case, it'll just be gone on reload
            RegistryKey<World> of = RegistryKey.of(Registry.WORLD_KEY, id);
            for (ServerPlayerEntity serverPlayerEntity : this.getPlayerManager().getPlayerList()) {
                if (serverPlayerEntity.world.getRegistryKey().equals(of)) {
                    if (destroyer == null) {
                        throw new IllegalArgumentException("Cannot remove world as it is currently loaded by a player!");
                    } else {
                        destroyer.destroyPlayer((MinecraftServer) (Object) this, serverPlayerEntity);
                    }
                }
            }

            this.enqueuedDestroyedWorlds.add(of);
        }
    }

    private void reloadTags() {
        for (TagManagerLoader.RegistryTags<?> registryTag : ((DataPackContentsAccessor) this.resourceManagerHolder.dataPackContents()).getRegistryTagManager().getRegistryTags()) {
            if (registryTag.key() == Registry.DIMENSION_TYPE_KEY) {
                Registry<DimensionType> dimensionTypes = this.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY);
                dimensionTypes.clearTags();
                //noinspection unchecked - we know that the registry is a registry of dimension types as the key is correct
                dimensionTypes.populateTags(((TagManagerLoader.RegistryTags<DimensionType>) registryTag).tags().entrySet()
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(entry -> TagKey.of(Registry.DIMENSION_TYPE_KEY, entry.getKey()), entry -> (entry.getValue()).values())));
                break;
            }
        }
        this.getPlayerManager().sendToAll(new SynchronizeTagsS2CPacket(TagPacketSerializer.serializeTags(this.getRegistryManager())));
    }
}
