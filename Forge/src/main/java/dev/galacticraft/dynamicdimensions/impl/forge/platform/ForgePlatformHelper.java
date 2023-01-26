/*
 * Copyright (c) 2021-2023 Team Galacticraft
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

package dev.galacticraft.dynamicdimensions.impl.forge.platform;

import dev.galacticraft.dynamicdimensions.api.event.DimensionAddedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DimensionRemovedCallback;
import dev.galacticraft.dynamicdimensions.impl.config.DynamicDimensionsConfig;
import dev.galacticraft.dynamicdimensions.impl.forge.config.DynamicDimensionsConfigImpl;
import dev.galacticraft.dynamicdimensions.impl.platform.services.PlatformHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class ForgePlatformHelper implements PlatformHelper {
    private final List<DimensionAddedCallback> addedCallbacks = new ArrayList<>();
    private final List<DimensionRemovedCallback> removedCallbacks = new ArrayList<>(); //todo: use forge event system?

    @Override
    public @NotNull DynamicDimensionsConfig getConfig() {
        return DynamicDimensionsConfigImpl.INSTANCE;
    }

    @Override
    public void registerAddedEvent(DimensionAddedCallback listener) {
        this.addedCallbacks.add(listener);
    }

    @Override
    public void registerRemovedEvent(DimensionRemovedCallback listener) {
        this.removedCallbacks.add(listener);
    }

    @Override
    public void invokeRemovedEvent(@NotNull ResourceKey<Level> key, @NotNull ServerLevel level) {
        for (DimensionRemovedCallback removedCallback : this.removedCallbacks) {
            removedCallback.dimensionRemoved(key, level);
        }
    }

    @Override
    public void invokeAddedEvent(@NotNull ResourceKey<Level> key, @NotNull ServerLevel level) {
        for (DimensionAddedCallback addedCallback : this.addedCallbacks) {
            addedCallback.dimensionAdded(key, level);
        }
    }
}
