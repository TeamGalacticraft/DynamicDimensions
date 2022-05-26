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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(SimpleRegistry.class)
public interface SimpleRegistryAccessor<T> {
    @Accessor
    ObjectList<RegistryEntry.Reference<T>> getRawIdToEntry();

    @Accessor
    Object2IntMap<T> getEntryToRawId();

    @Accessor
    Map<Identifier, RegistryEntry.Reference<T>> getIdToEntry();

    @Accessor
    Map<RegistryKey<T>, RegistryEntry.Reference<T>> getKeyToEntry();

    @Accessor
    Map<T, RegistryEntry.Reference<T>> getValueToEntry();

    @Accessor
    Map<T, Lifecycle> getEntryToLifecycle();

    @Accessor("frozen")
    boolean isFrozen();

    @Accessor("frozen")
    void setFrozen(boolean frozen);

    @Accessor("cachedEntries")
    void setCachedEntries(List<RegistryEntry.Reference<T>> o);

    @Accessor("lifecycle")
    void setLifecycle(Lifecycle base);
}
