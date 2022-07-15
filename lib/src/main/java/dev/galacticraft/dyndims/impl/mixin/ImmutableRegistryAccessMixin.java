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

package dev.galacticraft.dyndims.impl.mixin;

import dev.galacticraft.dyndims.impl.accessor.DimensionTypeRegistryAccessor;
import dev.galacticraft.dyndims.impl.util.UnfrozenRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(RegistryAccess.ImmutableRegistryAccess.class)
public abstract class ImmutableRegistryAccessMixin implements DimensionTypeRegistryAccessor {
    @Shadow
    @Final
    private Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

    @Override
    public UnfrozenRegistry<DimensionType> unfreeze() {
        Registry<DimensionType> registry = (Registry<DimensionType>) this.registries.get(Registry.DIMENSION_TYPE_REGISTRY);
        if (registry instanceof MappedRegistry<DimensionType> simple
                // if the registry is not a vanilla registry type,
                // we cannot guarantee that unfreezing the registry won't break stuff.
                && (simple.getClass() == MappedRegistry.class || simple.getClass() == DefaultedRegistry.class)
        ) {
            MappedRegistryAccessor<DimensionType> accessor = ((MappedRegistryAccessor<DimensionType>) simple);
            if (accessor.isFrozen()) {
                accessor.setFrozen(false);
                return new UnfrozenRegistry<>(simple, true);
            } else {
                return new UnfrozenRegistry<>(simple, false);
            }
        } else {
            throw new IllegalStateException("Dynamic Dimensions: Non-vanilla DimensionType registry! " + registry.getClass().getName());
        }
    }
}
