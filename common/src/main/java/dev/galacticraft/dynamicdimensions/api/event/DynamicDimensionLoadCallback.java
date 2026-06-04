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

package dev.galacticraft.dynamicdimensions.api.event;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionProperties;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.impl.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Called when dynamic dimensions should be loaded from the server.
 *
 * @since 0.5.0
 */
public interface DynamicDimensionLoadCallback {
    static void register(DynamicDimensionLoadCallback callback) {
        Services.PLATFORM.registerLoadEvent(callback);
    }

    @ApiStatus.Internal
    static void invoke(@NotNull MinecraftServer server, @NotNull DynamicDimensionLoader loader) {
        Services.PLATFORM.invokeLoadEvent(server, loader);
    }

    void loadDimensions(@NotNull MinecraftServer server, @NotNull DynamicDimensionLoader loader);

    @FunctionalInterface
    interface DynamicDimensionLoader {
        /**
         * Creates/loads a new dynamic dimension with the specified properties.
         * @param id the id of the dimension. Must be free in the level stem, dimension type, and level registries.
         * @param chunkGenerator the chunk generator to generate the dimension with
         * @param type the dimension type
         * @return the newly loaded level
         */
        @NotNull ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type);

        /**
         * Creates/loads a new dynamic dimension and applies the given properties before level construction,
         * so that compatible backends (e.g. Sable) receive the correct physics data from the start.
         * @param id the id of the dimension. Must be free in the level stem, dimension type, and level registries.
         * @param chunkGenerator the chunk generator to generate the dimension with
         * @param type the dimension type
         * @param properties the dimension properties to apply
         * @param server the current minecraft server
         * @return the newly loaded level
         */
        default @NotNull ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type, @NotNull DynamicDimensionProperties properties, @NotNull MinecraftServer server) {
            DynamicDimensionRegistry.from(server).setDimensionProperties(ResourceKey.create(Registries.DIMENSION, id), properties);
            return this.loadDynamicDimension(id, chunkGenerator, type);
        }
    }
}
