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

package dev.galacticraft.dynamicdimensions.impl.mixin;

import com.mojang.datafixers.DataFixer;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import dev.galacticraft.dynamicdimensions.api.event.DimensionAddedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DimensionRemovedCallback;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.DynamicDimensionRegistryImpl;
import dev.galacticraft.dynamicdimensions.impl.accessor.DynamicDimensionProvider;
import dev.galacticraft.dynamicdimensions.impl.internal.DimensionRemovalTicket;
import dev.galacticraft.dynamicdimensions.impl.network.S2CPackets;
import dev.galacticraft.dynamicdimensions.impl.registry.RegistryUtil;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
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
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements DynamicDimensionProvider {
    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;
    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Shadow
    public abstract PlayerList getPlayerList();

    @Shadow
    public abstract LayeredRegistryAccess<RegistryLayer> registries();

    @Unique
    private final @NotNull List<ServerLevel> pendingLevels = new ArrayList<>();
    @Unique
    private final @NotNull List<DimensionRemovalTicket> pendingDeletions = new ArrayList<>();
    @Unique
    private DynamicDimensionRegistryImpl dynamicDimensions;
    @Unique
    private boolean tickingLevels = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initDynamicDimensions(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        this.dynamicDimensions = new DynamicDimensionRegistryImpl((MinecraftServer) (Object) this);
    }

    /**
     * Load dynamic dimensions AFTER all normal levels have been loaded,
     * but still during server load (so that it's not too late).
     * Hopefully hints that these dimension may be removed later.
     */
    @Inject(method = "prepareLevels", at = @At("RETURN"))
    private void loadDynamicDimensions(ChunkProgressListener chunkProgressListener, CallbackInfo ci) {
        this.dynamicDimensions.loadDynamicDimensions();
    }

    @Override
    public void dynamicdimensions$removeLevel(ResourceKey<Level> key, @Nullable PlayerRemover removalMode, boolean removeFiles) {
        if (this.tickingLevels) {
            this.pendingDeletions.add(new DimensionRemovalTicket(key, removalMode, removeFiles));
        } else {
            this.unloadLevel(key, removalMode);
            if (removeFiles) this.dynamicdimensions$deleteLevelData(key);
        }
    }

    @Override
    public void dynamicdimensions$deleteLevelData(ResourceKey<Level> key) {
        Path dimensionPath = this.storageSource.getDimensionPath(key);
        if (dimensionPath.toFile().exists()) {
            try {
                FileUtils.deleteDirectory(dimensionPath.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete deleted level directory!", e);
            }
        }
    }

    @Override
    public boolean dynamicdimensions$isIdPendingCreation(@NotNull ResourceKey<Level> key) {
        for (ServerLevel pendingLevel : this.pendingLevels) {
            if (pendingLevel.dimension().equals(key)) return true;
        }
        return false;
    }

    @Override
    public void dynamicdimensions$registerLevel(ServerLevel level) {
        if (this.tickingLevels) {
            this.pendingLevels.add(level); //prevent co-modification
        } else {
            this.registerLevel(level);
        }
    }

    @Override
    public @NotNull DynamicDimensionRegistry dynamicdimensions$registry() {
        return this.dynamicDimensions;
    }

    @Inject(method = "tickChildren", at = @At(value = "HEAD"))
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
                    this.dynamicdimensions$deleteLevelData(ticket.key());
                }
            }
            this.pendingDeletions.clear();
        }
    }

    @Unique
    private void unloadLevel(ResourceKey<Level> key, PlayerRemover playerRemover) {
        ResourceLocation dimType = null;

        try (ServerLevel level = this.levels.get(key)) {
            if (level == null) {
                Constants.LOGGER.error("Attempted to unload non-existent level {}", key);
                return;
            }
            DimensionRemovedCallback.invoke(key, level);

            List<ServerPlayer> players = new ArrayList<>(level.players()); // prevent co-modification
            for (ServerPlayer player : players) {
                playerRemover.removePlayer((MinecraftServer) (Object) this, player);
            }

            level.save(null, true, level.noSave);
            dimType = level.dimensionTypeRegistration().unwrapKey().get().location();
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to close level upon removal! Memory may have been leaked.", e);
        } finally {
            this.levels.remove(key);
        }
        assert dimType != null;

        RegistryUtil.unregister(this.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM), key.location());
        RegistryUtil.unregister(this.registries().compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE), dimType);
        this.dynamicDimensions.remove(key);

        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            S2CPackets.sendRemoveDimension(player, key.location());
        }
    }

    @Unique
    private void registerLevel(ServerLevel level) {
        DimensionAddedCallback.invoke(level.dimension(), level);
        this.levels.put(level.dimension(), level);
        this.dynamicDimensions.add(level.dimension());
        level.tick(() -> true);
    }

    @Inject(method = "tickChildren", at = @At(value = "HEAD"))
    private void markTickingLevels(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        this.tickingLevels = true;
    }

    @Inject(method = "tickChildren", at = @At(value = "RETURN"))
    private void markNotTickingLevels(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        this.tickingLevels = false;
    }
}
