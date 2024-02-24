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

package dev.galacticraft.dynamicdimensions.impl.fabric.platform;

import dev.galacticraft.dynamicdimensions.api.event.DimensionAddedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DimensionRemovedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback;
import dev.galacticraft.dynamicdimensions.impl.config.DynamicDimensionsConfig;
import dev.galacticraft.dynamicdimensions.impl.fabric.DynamicDimensionsFabric;
import dev.galacticraft.dynamicdimensions.impl.fabric.config.DynamicDimensionsConfigImpl;
import dev.galacticraft.dynamicdimensions.impl.platform.services.PlatformHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class FabricPlatformHelper implements PlatformHelper {
    @Override
    public @NotNull DynamicDimensionsConfig getConfig() {
        return DynamicDimensionsConfigImpl.create();
    }

    @Override
    public void registerAddedEvent(DimensionAddedCallback callback) {
        DynamicDimensionsFabric.DIMENSION_ADDED_EVENT.register(callback);
    }

    @Override
    public void registerRemovedEvent(DimensionRemovedCallback callback) {
        DynamicDimensionsFabric.DIMENSION_REMOVED_EVENT.register(callback);
    }

    @Override
    public void registerLoadEvent(DynamicDimensionLoadCallback callback) {
        DynamicDimensionsFabric.DIMENSION_LOAD_EVENT.register(callback);
    }

    @Override
    public void invokeRemovedEvent(@NotNull ResourceKey<Level> key, @NotNull ServerLevel level) {
        DynamicDimensionsFabric.DIMENSION_REMOVED_EVENT.invoker().dimensionRemoved(key, level);
    }

    @Override
    public void invokeAddedEvent(@NotNull ResourceKey<Level> key, @NotNull ServerLevel level) {
        DynamicDimensionsFabric.DIMENSION_ADDED_EVENT.invoker().dimensionAdded(key, level);
    }

    @Override
    public void invokeLoadEvent(MinecraftServer server, DynamicDimensionLoadCallback.DynamicDimensionLoader loader) {
        DynamicDimensionsFabric.DIMENSION_LOAD_EVENT.invoker().loadDimensions(server, loader);
    }
}
