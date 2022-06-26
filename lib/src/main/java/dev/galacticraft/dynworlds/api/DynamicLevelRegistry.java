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

package dev.galacticraft.dynworlds.api;

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
     * Registers a new level and updates all clients with the new level.
     * NOTE: The level will not be loaded until the next tick.
     *
     * @param id      The ID of the level.
     *                This ID must be unique and unused in the {@link net.minecraft.core.Registry#DIMENSION_TYPE_REGISTRY} registry and the {@link WorldGenSettings#dimensions()} registry.
     * @param stem The dimension stem for the level.
     * @param type    The dimension type of the level.
     * @since 0.1.0
     */
    boolean addDynamicLevel(@NotNull ResourceLocation id, @NotNull LevelStem stem, @NotNull DimensionType type);

    /**
     * Tests if a level with the given ID exists.
     *
     * @param id The ID of the level.
     * @return True if the level exists, false otherwise.
     * If the level exists, you should not call {@link #addDynamicLevel(ResourceLocation, LevelStem, DimensionType)} with the same ID.
     * @since 0.1.0
     */
    boolean levelExists(@NotNull ResourceLocation id);

    /**
     * Returns whether a level with the given ID can be created.
     *
     * @param id The ID of the level.
     * @return {@code true} if the level can be created, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canCreateLevel(@NotNull ResourceLocation id);

    /**
     * Returns whether a level with the given ID can be deleted.
     *
     * @param id The ID of the level.
     * @return {@code true} if the level can be deleted, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canDestroyLevel(@NotNull ResourceLocation id);

    /**
     * Erases a dynamic level from existence.
     * Players will be removed from the level using the provided player remover.
     *
     * @param id      The ID of the level.
     * @param remover The method to remove players from the level.
     * @since 0.1.0
     */
    boolean removeDynamicLevel(@NotNull ResourceLocation id, @NotNull PlayerRemover remover);
}
