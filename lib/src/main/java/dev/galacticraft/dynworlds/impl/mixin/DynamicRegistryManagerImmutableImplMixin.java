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

import com.google.common.collect.ImmutableMap;
import dev.galacticraft.dynworlds.impl.RegistryAppender;
import dev.galacticraft.dynworlds.impl.accessor.DynamicRegistryManagerImmutableImplAccessor;
import net.minecraft.util.registry.*;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(DynamicRegistryManager.ImmutableImpl.class)
public abstract class DynamicRegistryManagerImmutableImplMixin implements DynamicRegistryManagerImmutableImplAccessor {
    @Shadow
    @Final
    private Map<? extends RegistryKey<? extends Registry<?>>, ? extends Registry<?>> registries;

    @Override
    public void unfreezeTypes(RegistryAppender<DimensionType> appender) {
        dynworlds_addToRegistry(Registry.DIMENSION_TYPE_KEY, appender);
    }

    @Unique
    private <T> void dynworlds_addToRegistry(RegistryKey<Registry<T>> registryKey, RegistryAppender<T> appender) { // prefix as this is hacky
        Registry<T> registry = (Registry<T>) this.registries.get(registryKey);
        ImmutableMap.Builder<RegistryKey<? extends Registry<?>>, Registry<?>> builder = new ImmutableMap.Builder<>();
        this.registries.forEach((k, v) -> {
            if (k != registryKey) {
                builder.put(k, v);
            }
        });

        if (registry instanceof SimpleRegistry<T> simple && (simple.getClass() == SimpleRegistry.class || simple.getClass() == DefaultedRegistry.class)) {
            if (((SimpleRegistryAccessor<T>) simple).isFrozen()) {
                ((SimpleRegistryAccessor<T>) simple).setFrozen(false);  // safe as there should be no new intrusive holders of this registry as it was already frozen
                appender.register(simple);
                simple.freeze();                                        // freeze it again - everything is recalculated so there should be no leftover references
            } else {
                appender.register(simple);
            }
        } else {
            throw new IllegalStateException("DynWorlds: Registry is not vanilla! " + registry.getClass().getName());
        }
        builder.put(Registry.DIMENSION_KEY, registry);
    }
}
