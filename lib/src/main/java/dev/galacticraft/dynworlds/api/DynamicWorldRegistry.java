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

/**
 * The registry for dynamic worlds.
 * Cast {@link net.minecraft.server.MinecraftServer} to this class to access the registry.
 * It is not possible to access the registry from the client.
 *
 * @since 0.1.0
 */
public interface DynamicWorldRegistry {
    /**
     * Registers a new world and updates all clients with the new world.
     * NOTE: The world will not be loaded until the next tick.
     *
     * @param id      The ID of the world.
     *                This ID must be unique and unused in the {@link net.minecraft.core.Registry#DIMENSION_TYPE_REGISTRY} registry and the {@link WorldGenSettings#dimensions()} registry.
     * @param stem The dimension stem for the world.
     * @param type    The dimension type of the world.
     * @since 0.1.0
     */
    void addDynamicWorld(ResourceLocation id, LevelStem stem, DimensionType type);

    /**
     * Tests if a world with the given ID exists.
     *
     * @param id The ID of the world.
     * @return True if the world exists, false otherwise.
     * If the world exists, you should not call {@link #addDynamicWorld(ResourceLocation, LevelStem, DimensionType)} with the same ID.
     * @since 0.1.0
     */
    boolean worldExists(ResourceLocation id);

    /**
     * Returns whether a world with the given ID can be created.
     *
     * @param id The ID of the world.
     * @return {@code true} if the world can be created, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canCreateWorld(ResourceLocation id);

    /**
     * Returns whether a world with the given ID can be deleted.
     *
     * @param id The ID of the world.
     * @return {@code true} if the world can be deleted, {@code false} otherwise.
     * @since 0.1.0
     */
    boolean canDestroyWorld(ResourceLocation id);

    /**
     * Erases a dynamic world from existence.
     * Players will be removed from the world using the provided player remover.
     *
     * @param id      The ID of the world.
     * @param remover The method to remove players from the world.
     * @since 0.1.0
     */
    void removeDynamicWorld(ResourceLocation id, PlayerRemover remover);
}
