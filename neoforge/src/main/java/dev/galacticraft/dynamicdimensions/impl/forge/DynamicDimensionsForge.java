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

package dev.galacticraft.dynamicdimensions.impl.forge;

import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.client.network.DynamicDimensionsS2CPacketReceivers;
import dev.galacticraft.dynamicdimensions.impl.command.DynamicDimensionsCommands;
import dev.galacticraft.dynamicdimensions.impl.forge.config.DynamicDimensionsConfigImpl;
import dev.galacticraft.dynamicdimensions.impl.gametest.DynamicDimensionsGametest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
@Mod(Constants.MOD_ID)
public final class DynamicDimensionsForge {
    public DynamicDimensionsForge(IEventBus modEventBus, Dist dist, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DynamicDimensionsConfigImpl.SPEC);

        if (FMLEnvironment.dist.isClient()) {
            DynamicDimensionsS2CPacketReceivers.registerReceivers();
        }
        modEventBus.addListener(this::registerGametests);
    }

    public void registerGametests(@NotNull RegisterGameTestsEvent event) {
        event.register(DynamicDimensionsGametest.class);
    }

    private void registerCommands(@NotNull RegisterCommandsEvent event) {
        DynamicDimensionsCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }
}