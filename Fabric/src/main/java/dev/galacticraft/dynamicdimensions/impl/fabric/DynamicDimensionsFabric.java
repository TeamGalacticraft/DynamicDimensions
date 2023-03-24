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

package dev.galacticraft.dynamicdimensions.impl.fabric;

import dev.galacticraft.dynamicdimensions.api.event.DimensionAddedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DimensionRemovedCallback;
import dev.galacticraft.dynamicdimensions.api.event.DynamicDimensionLoadCallback;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.command.DynamicDimensionsCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class DynamicDimensionsFabric implements ModInitializer {
    public static final Event<DimensionAddedCallback> DIMENSION_ADDED_EVENT = EventFactory.createArrayBacked(DimensionAddedCallback.class, t -> (key, level) -> {
        for (DimensionAddedCallback callback : t) {
            callback.dimensionAdded(key, level);
        }
    });
    public static final Event<DimensionRemovedCallback> DIMENSION_REMOVED_EVENT = EventFactory.createArrayBacked(DimensionRemovedCallback.class, t -> (key, level) -> {
        for (DimensionRemovedCallback callback : t) {
            callback.dimensionRemoved(key, level);
        }
    });
    public static final Event<DynamicDimensionLoadCallback> DIMENSION_LOAD_EVENT = EventFactory.createArrayBacked(DynamicDimensionLoadCallback.class, t -> (server, loader) -> {
        for (DynamicDimensionLoadCallback callback : t) {
            callback.loadDimensions(server, loader);
        }
    });

    @Override
    public void onInitialize() {
        if (Constants.CONFIG.enableCommands()) {
            if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
                registerCommandCallback();
            } else {
                Constants.LOGGER.warn("Unable to register commands as the fabric command api module (fabric-command-api-v2) is not installed.");
            }
        }
    }

    private static void registerCommandCallback() {
        CommandRegistrationCallback.EVENT.register(DynamicDimensionsCommands::register);
    }
}
