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

package dev.galacticraft.dynamicdimensions.api;

import dev.galacticraft.dynamicdimensions.impl.accessor.DynamicDimensionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
     * Obtains a dynamic dimension registry from a Minecraft server instance.
     *
     * @param server the current Minecraft server instance.
     * @return the server's dynamic dimension registry.
     * @since 0.5.0
     */
    @Contract(value = "_ -> param1", pure = true)
    static @NotNull DynamicDimensionRegistry from(@NotNull MinecraftServer server) {
        return ((DynamicDimensionProvider) server).dynamicdimensions$registry();
    }

    /**
     * @param id the id of the dynamic dimension
     * {@return whether a dynamic dimension exists with the given id}
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    default boolean dynamicDimensionExists(@NotNull ResourceLocation id) {
        return dynamicDimensionExists(ResourceKey.create(Registries.DIMENSION, id));
    }

    /**
     * @param key the id of the dynamic dimension
     * {@return whether a dynamic dimension exists with the given id}
     * @since 0.9.0
     */
    boolean dynamicDimensionExists(@NotNull ResourceKey<Level> key);

    /**
     * @param id the id of the dimension
     * {@return whether any dimension, dimension type, or level stem is registered with the given id}
     */
    boolean anyDimensionExists(@NotNull ResourceLocation id);

    /**
     * Returns whether a level and dimension with the given ID can be deleted.
     *
     * @param id The ID of the dimension.
     * @return {@code true} if the dimension is dynamic and can be deleted, {@code false} otherwise.
     * @since 0.1.0
     */
    @Deprecated(since = "0.9.0", forRemoval = true)
    default boolean canDeleteDimension(@NotNull ResourceLocation id) {
        return this.canDeleteDimension(ResourceKey.create(Registries.DIMENSION, id));
    }

    /**
     * Returns whether a level and dimension with the given ID can be deleted.
     *
     * @param key The ID of the dimension.
     * @return {@code true} if the dimension is dynamic and can be deleted, {@code false} otherwise.
     * @since 0.9.0
     */
    boolean canDeleteDimension(@NotNull ResourceKey<Level> key);

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
     * Registers a new dimension and applies optional dynamic-dimension properties.
     *
     * @since 0.10.0
     */
    @Nullable ServerLevel createDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type, @NotNull DynamicDimensionProperties properties);

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
     * Loads a dynamic dimension and applies optional dynamic-dimension properties.
     *
     * @since 0.10.0
     */
    @Nullable ServerLevel loadDynamicDimension(@NotNull ResourceLocation id, @NotNull ChunkGenerator chunkGenerator, @NotNull DimensionType type, @NotNull DynamicDimensionProperties properties);

    /**
     * Sets properties for a dynamic dimension.
     *
     * <p>If the dimension is already loaded, compatible backends such as Sable
     * may apply these immediately.</p>
     *
     * @since 0.10.0
     */
    void setDimensionProperties(@NotNull ResourceKey<Level> key, @NotNull DynamicDimensionProperties properties);

    /**
     * Sets properties for a dynamic dimension.
     *
     * @since 0.10.0
     */
    default void setDimensionProperties(@NotNull ResourceLocation id, @NotNull DynamicDimensionProperties properties) {
        this.setDimensionProperties(ResourceKey.create(Registries.DIMENSION, id), properties);
    }

    /**
     * Gets the currently stored properties for a dynamic dimension.
     *
     * @since 0.10.0
     */
    @Nullable DynamicDimensionProperties getDimensionProperties(@NotNull ResourceKey<Level> key);

    /**
     * Gets the currently stored properties for a dynamic dimension.
     *
     * @since 0.10.0
     */
    default @Nullable DynamicDimensionProperties getDimensionProperties(@NotNull ResourceLocation id) {
        return this.getDimensionProperties(ResourceKey.create(Registries.DIMENSION, id));
    }

    /**
     * Clears stored properties for a dynamic dimension.
     *
     * <p>If the dimension has been applied to a compatible backend such as Sable,
     * this should also remove those backend properties.</p>
     *
     * @since 0.10.0
     */
    void clearDimensionProperties(@NotNull ResourceKey<Level> key);

    /**
     * Clears stored properties for a dynamic dimension.
     *
     * @since 0.10.0
     */
    default void clearDimensionProperties(@NotNull ResourceLocation id) {
        this.clearDimensionProperties(ResourceKey.create(Registries.DIMENSION, id));
    }

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
