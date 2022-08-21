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

package dev.galacticraft.dyndims.impl.fabric;

import dev.galacticraft.dyndims.impl.Constants;
import dev.galacticraft.dyndims.impl.command.DynamicDimensionsCommands;
import dev.galacticraft.dyndims.impl.fabric.config.DynamicDimensionsConfigImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class DynamicDimensions implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            registerCommandCallback();
        } else {
            if (Constants.CONFIG.enableCommands()) {
                Constants.LOGGER.warn("Unable to register commands as fabric api (fabric-command-api-v2) is not installed.");
            }
        }
    }

    private static void registerCommandCallback() {
        CommandRegistrationCallback.EVENT.register(DynamicDimensionsCommands::register);
    }
}
