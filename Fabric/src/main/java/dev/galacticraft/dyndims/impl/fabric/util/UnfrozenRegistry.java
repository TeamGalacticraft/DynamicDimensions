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

package dev.galacticraft.dyndims.impl.fabric.util;

import dev.galacticraft.dyndims.impl.fabric.mixin.MappedRegistryAccessor;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Wrapper for a registry that has been made mutable temporarily
 * @param <T> The registry's type
 */
@ApiStatus.Internal
public final class UnfrozenRegistry<T> implements AutoCloseable {
    private final @NotNull MappedRegistry<T> registry;
    private final boolean refreeze;
    private boolean open = true;

    /**
     * Creates an unfrozen registry
     * @param registry the mutable registry
     * @param refreeze whether to freeze the registry when this object is closed
     */
    private UnfrozenRegistry(@NotNull MappedRegistry<T> registry, boolean refreeze) {
        this.registry = registry;
        this.refreeze = refreeze;
    }

    /**
     * Creates an unfrozen registry.
     * @param registry The registry to unfreeze. Must be an exact vanilla type (MappedRegistry or DefaultedRegistry)
     *                otherwise the registry will not be unfrozen
     * @return A new {@link UnfrozenRegistry} with a mutable registry wrapped inside
     * @param <T> The registry's type
     */
    public static <T> @NotNull UnfrozenRegistry<T> create(@NotNull Registry<T> registry) {
        if (registry instanceof MappedRegistry<T> simple
                // if the registry is not a vanilla registry type,
                // we cannot guarantee that unfreezing the registry won't break stuff.
                && (simple.getClass() == MappedRegistry.class || simple.getClass() == DefaultedRegistry.class)) {

            MappedRegistryAccessor<T> accessor = (MappedRegistryAccessor<T>) registry;
            boolean frozen = accessor.isFrozen();
            accessor.setFrozen(false);
            return new UnfrozenRegistry<>(simple, frozen);
        } else {
            throw new IllegalStateException("Dynamic Dimensions: Non-vanilla '" + registry.key().location() + "' registry! " + registry.getClass().getName());
        }
    }

    @Override
    public void close() {
        this.open = false;
        if (this.refreeze) this.registry.freeze();
    }

    /**
     * Returns the stored registry. Will fail if the registry has already been re-frozen.
     * @return the stored registry
     */
    public @NotNull MappedRegistry<T> registry() {
        if (!this.open) throw new IllegalStateException("Registry has been re-frozen!");
        return this.registry;
    }

    /**
     * Returns whether the registry is still mutable
     * @return whether the registry is still mutable
     */
    public boolean open() {
        return this.open;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnfrozenRegistry<?> that = (UnfrozenRegistry<?>) o;
        return this.refreeze == that.refreeze && this.open == that.open && this.registry.equals(that.registry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.registry, this.refreeze, this.open);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "UnfrozenRegistry{" +
                "registry=" + this.registry +
                ", refreeze=" + this.refreeze +
                ", open=" + this.open +
                '}';
    }
}
