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
import dev.galacticraft.dynworlds.impl.DynWorlds;
import dev.galacticraft.dynworlds.impl.accessor.DynamicRegistryManagerImmutableImplAccessor;
import dev.galacticraft.dynworlds.impl.accessor.SavePropertiesAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements DynamicWorldRegistry {
    @Shadow
    @Final
    protected SaveProperties saveProperties;

    @Shadow
    @Final
    private Executor workerExecutor;

    @Shadow
    @Final
    protected LevelStorage.Session session;

    @Shadow
    @Final
    private WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory;

    @Shadow
    @Final
    private Map<RegistryKey<World>, ServerWorld> worlds;

    @Shadow
    private PlayerManager playerManager;

    @Shadow
    public abstract ServerWorld getOverworld();

    @Shadow
    public abstract DynamicRegistryManager.Immutable getRegistryManager();

    private final Map<RegistryKey<World>, ServerWorld> enqueuedWorlds = new HashMap<>();

    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void addSatelliteWorlds(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        Registry<DimensionOptions> dimensions = this.saveProperties.getGeneratorOptions().getDimensions();
        assert dimensions instanceof MutableRegistry<DimensionOptions>;
        ((DynamicRegistryManagerImmutableImplAccessor) this.getRegistryManager()).unfreezeTypes(reg -> ((SavePropertiesAccessor) this.saveProperties).getDynamicWorlds().forEach((id, pair) -> {
            ((MutableRegistry<DimensionOptions>) dimensions).add(RegistryKey.of(Registry.DIMENSION_KEY, id), pair.getLeft(), Lifecycle.stable()); // dimension options is a mutable registry
            reg.add(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id), pair.getRight(), Lifecycle.stable()); // dimension type must be unfrozen
        }));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
    private void addWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.enqueuedWorlds.isEmpty()) {
            this.worlds.putAll(this.enqueuedWorlds);
            this.enqueuedWorlds.clear();
        }
    }

    @Override
    public void addDynamicWorld(Identifier id, DimensionOptions options, DimensionType type) {
        ((MutableRegistry<DimensionOptions>) this.saveProperties.getGeneratorOptions().getDimensions()).add(RegistryKey.of(Registry.DIMENSION_KEY, id), options, Lifecycle.stable());
        ((DynamicRegistryManagerImmutableImplAccessor) this.getRegistryManager()).unfreezeTypes(reg -> reg.add(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id), type, Lifecycle.stable()));

        RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, id);
        UnmodifiableLevelProperties properties = new UnmodifiableLevelProperties(this.saveProperties, this.saveProperties.getMainWorldProperties());
        ServerWorld world = new ServerWorld(
                (MinecraftServer) (Object) this,
                this.workerExecutor,
                this.session,
                properties,
                worldKey,
                options.getDimensionTypeSupplier(),
                worldGenerationProgressListenerFactory.create(10),
                options.getChunkGenerator(),
                this.saveProperties.getGeneratorOptions().isDebugWorld(),
                BiomeAccess.hashSeed(this.saveProperties.getGeneratorOptions().getSeed()),
                ImmutableList.of(),
                false
        );
        ServerWorld overworld = this.getOverworld();
        assert overworld != null;
        overworld.getWorldBorder().addListener(new WorldBorderListener.WorldBorderSyncer(world.getWorldBorder()));
        world.getChunkManager().applySimulationDistance(((ChunkTicketManagerAccessor) ((ServerChunkManagerAccessor) overworld.getChunkManager()).getTicketManager()).getSimulationDistance());
        world.getChunkManager().applyViewDistance(((ThreadedAnvilChunkStorageAccessor) overworld.getChunkManager().threadedAnvilChunkStorage).getRenderDistance());
        this.enqueuedWorlds.put(worldKey, world); //prevent comodification

        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeIdentifier(id);
        packetByteBuf.writeInt(this.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(type));
        packetByteBuf.writeNbt((NbtCompound) DimensionType.CODEC.encode(type, RegistryOps.of(NbtOps.INSTANCE, this.getRegistryManager()), new NbtCompound()).get().orThrow());
        this.playerManager.sendToAll(new CustomPayloadS2CPacket(DynWorlds.id("create_world"), packetByteBuf));
    }

    @Override
    public boolean worldExists(Identifier id) {
        return ((SavePropertiesAccessor) this.saveProperties).getDynamicWorlds().containsKey(id);
    }

    @Override
    public void removeDynamicWorld(Identifier id) { //todo!
        throw new UnsupportedOperationException("Removing a dynamic world is not supported yet.");
    }
}
