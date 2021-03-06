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

package dev.galacticraft.dyndims.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.jetbrains.annotations.NotNull;

/**
 * The registry for dynamic levels.
 * Cast {@link net.minecraft.server.MinecraftServer} to this class to access the registry.
 * It is not possible to access the registry from the client.
 *
 * @since 0.1.0
 */
public interface DynamicLevelRegistry {
    /**
     * Registers a new dimension and updates all clients with the new dimension.
     * NOTE: The dimension will not be loaded until the next tick.
     *
     * @param id      The ID of the dimension.
     *                This ID must be unique and unused in the {@link net.minecraft.core.Registry#DIMENSION_TYPE_REGISTRY} registry and the {@link WorldGenSettings#dimensions()} registry.
     * @param stem The dimension stem.
     * @param type    The dimension type.
     * @since 0.1.0
     */
    boolean addDynamicDimension(@NotNull ResourceLocation id, @NotNull LevelStem stem, @NotNull DimensionType type);

    /**
     * Tests if a level or dimension with the given ID exists.
     *
     * @param id The ID of the dimension.
     * @return {@code true} if the dimension exists, false otherwise.
     * If the dimension exists, you should not call {@link #addDynamicDimension(ResourceLocation, LevelStem, DimensionType)} with the same ID.
     * @since 0.1.0
     */
    boolean dimensionExists(@NotNull ResourceLocation id);

    /**
     * Returns whether a level and dimension with the given ID can be created.
     *
     * @param id The ID of the level/dimension.
     * @return {@code true} if the level and dimension can be created, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canCreateDimension(@NotNull ResourceLocation id);

    /**
     * Returns whether a level and dimension with the given ID can be deleted.
     *
     * @param id The ID of the level/dimension.
     * @return {@code true} if the level and dimension can be deleted, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canDeleteDimension(@NotNull ResourceLocation id);

    /**
     * Removes a dynamic dimension from the server.
     * This may delete the dimension files permanently.
     * Players will be removed from the dimension using the provided player remover.
     *
     * @param id      The ID of the dimension.
     * @param remover The method to remove players from the dimension.
     * @since 0.1.0
     */
    boolean removeDynamicDimension(@NotNull ResourceLocation id, @NotNull PlayerRemover remover);
}
