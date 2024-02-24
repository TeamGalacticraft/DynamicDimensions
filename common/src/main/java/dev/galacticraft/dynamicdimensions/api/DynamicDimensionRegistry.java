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

package dev.galacticraft.dynamicdimensions.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The registry for dynamic dimensions.
 * It is not possible to access the registry from the client.
 *
 * @since 0.1.0
 * @see #from(MinecraftServer)
 */
public interface DynamicDimensionRegistry {
    /**
     * Converts a Minecraft server instance into a dynamic dimension registry.
     *
     * @param server the current Minecraft server instance.
     * @return the server's dynamic dimension registry.
     * @since 0.5.0
     */
    @Contract(value = "_ -> param1", pure = true)
    static @NotNull DynamicDimensionRegistry from(@NotNull MinecraftServer server) {
        return ((DynamicDimensionRegistry) server);
    }

    /**
     * Returns whether a dynamic dimension exists with the given id
     *
     * @param id the id of the dynamic dimension
     * @return whether a dynamic dimension exists with the given id
     */
    boolean dynamicDimensionExists(@NotNull ResourceLocation id);

    /**
     * Returns whether any dimension, dimension type, or level stem is registered with the given id
     *
     * @param id the id of the dimension
     * @return whether any dimension, dimension type, or level stem is registered with the given id
     */
    boolean anyDimensionExists(@NotNull ResourceLocation id);

    /**
     * Returns whether a level and dimension with the given ID can be deleted.
     *
     * @param id The ID of the dimension.
     * @return {@code true} if the dimension is dynamic and can be deleted, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canDeleteDimension(@NotNull ResourceLocation id);

    /**
     * Returns whether a level and dimension with the given ID can be created.
     *
     * @param id The ID of the level/dimension.
     * @return {@code true} if a dynamic dimension can be created with the given id, {@code false} otherwise.
     * @since 0.6.0
     */
    boolean canCreateDimension(@NotNull ResourceLocation id);

    /**
     * Registers a new dimension and updates all clients with the new dimension.
     * If world data already exists for this dimension it will be overwritten.
     * Note: The dimension may not be loaded until the next tick.
     *
     * @param chunkGenerator The chunk generator.
     * @param id             The ID of the dimension.
     *                       This ID must be unique and unused in the {@link net.minecraft.core.registries.Registries#DIMENSION_TYPE} registry and the {@link net.minecraft.world.level.levelgen.WorldDimensions#dimensions()} registry.
     * @param type           The dimension type.
     * @return the server level of the new dimension if successful, {@code null} otherwise.
     * @see #loadDynamicDimension(ResourceLocation, ChunkGenerator, DimensionType) if you want to load previous data
     * @since 0.6.0
     */
    @Nullable ServerLevel createDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type);

    /**
     * Registers a new dimension and updates all clients with the new dimension.
     * If world data already exists for this dimension it will be used, otherwise it will be created.
     * Note: The dimension may not be loaded until the next tick.
     *
     * @param chunkGenerator The chunk generator.
     * @param id             The ID of the dimension.
     *                       This ID must be unique and unused in the {@link net.minecraft.core.registries.Registries#DIMENSION_TYPE dimension type} registry
     *                       and the {@link net.minecraft.world.level.levelgen.WorldDimensions#dimensions() dimensions} registry.
     * @param type           The dimension type.
     * @return the server level of the new dimension if successful, {@code null} otherwise.
     * @since 0.6.0
     */
    @Nullable ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type);

    /**
     * Deletes a dynamic dimension from the server.
     * This may delete the dimension files permanently.
     * Remaining players will be removed from the dimension using the provided player remover.
     * Note: The dimension may not be deleted until the next tick.
     *
     * @param id      The ID of the dimension.
     * @param remover The method to remove players from the dimension.
     * @return whether a dimension with the given id was deleted
     * @since 0.7.0
     */
    boolean deleteDynamicDimension(@NotNull ResourceLocation id, @Nullable PlayerRemover remover);

    /**
     * Removes a dynamic dimension from the server, saving the level to disk.
     * Remaining players will be removed from the dimension using the provided player remover.
     * Note: The dimension may not be unloaded until the next tick.
     *
     * @param id      The ID of the dimension.
     * @param remover The method to remove players from the dimension.
     * @return whether a dimension with the given id was unloaded
     * @since 0.7.0
     */
    boolean unloadDynamicDimension(@NotNull ResourceLocation id, @Nullable PlayerRemover remover);
}
