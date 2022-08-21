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

package dev.galacticraft.dyndims.impl.forge.registry;

import dev.galacticraft.dyndims.impl.mixin.MappedRegistryAccessor;
import dev.galacticraft.dyndims.impl.registry.UnfrozenRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.NotNull;

public final class UnfrozenRegistryImpl<T> implements UnfrozenRegistry<T> {
    private final MappedRegistry<T> registry;
    private final boolean refreeze;
    private boolean open = true;

    public UnfrozenRegistryImpl(@NotNull MappedRegistry<T> registry, boolean refreeze) {
        this.registry = registry;
        this.refreeze = refreeze;
    }

    /**
     * Creates an unfrozen registry.
     * @param registry The registry to unfreeze. Must be an exact vanilla type (MappedRegistry or DefaultedRegistry)
     *                otherwise the registry will not be unfrozen
     * @return A new {@link UnfrozenRegistryImpl} with a mutable registry wrapped inside
     * @param <T> The registry's type
     */
    public static <T> @NotNull UnfrozenRegistryImpl<T> create(@NotNull Registry<T> registry) {
        if (registry instanceof MappedRegistry<T> mapped) {
            boolean frozen = ((MappedRegistryAccessor<T>) mapped).isFrozen();
            if (frozen) mapped.unfreeze();
            return new UnfrozenRegistryImpl<>(mapped, frozen);
        } else {
            throw new IllegalArgumentException("Invalid non-mapped registry: " + registry.getClass() + " (" + registry.key() + ")");
        }
    }

    @Override
    public @NotNull MappedRegistry<T> registry() {
        if (!this.open) throw new IllegalStateException("Registry has been re-frozen!");
        return this.registry;
    }

    @Override
    public boolean open() {
        return this.open;
    }

    @Override
    public void close() {
        if (this.open) {
            this.open = false;
            if (this.refreeze) this.registry.freeze();
        }
    }
}
