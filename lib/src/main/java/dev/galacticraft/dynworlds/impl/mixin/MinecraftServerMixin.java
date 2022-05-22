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

package dev.galacticraft.dynworlds.impl.mixin;

import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dynworlds.impl.accessor.SavePropertiesAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final protected SaveProperties saveProperties;

    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void addSatelliteWorlds(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        Registry<DimensionOptions> dimensions = this.saveProperties.getGeneratorOptions().getDimensions();
        assert dimensions instanceof MutableRegistry<DimensionOptions>;
        ((SavePropertiesAccessor) this.saveProperties).getDynamicWorlds().forEach((key, dimensionOptions) -> ((MutableRegistry<DimensionOptions>) dimensions).add(key, dimensionOptions, Lifecycle.stable()));
    }
}
