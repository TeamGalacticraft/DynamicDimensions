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

package dev.galacticraft.dyndims.impl.util;

import net.minecraft.core.MappedRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class UnfrozenRegistry<T> implements AutoCloseable {
    private final @NotNull MappedRegistry<T> registry;
    private final boolean refreeze;
    private boolean open = true;

    public UnfrozenRegistry(@NotNull MappedRegistry<T> registry, boolean refreeze) {
        this.registry = registry;
        this.refreeze = refreeze;
    }

    @Override
    public void close() {
        this.open = false;
        if (this.refreeze) this.registry.freeze();
    }

    public @NotNull MappedRegistry<T> registry() {
        if (!this.open) throw new IllegalStateException("Registry has been re-frozen!");
        return this.registry;
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
