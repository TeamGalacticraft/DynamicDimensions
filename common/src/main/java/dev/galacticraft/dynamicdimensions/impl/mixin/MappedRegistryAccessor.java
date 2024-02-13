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

package dev.galacticraft.dynamicdimensions.impl.mixin;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor<T> {
    @Accessor("nextId")
    void setNextId(int nextId);

    @Accessor("tags")
    Map<TagKey<T>, HolderSet.Named<T>> tags();

    @Accessor("unregisteredIntrusiveHolders")
    @Nullable Map<T, Holder.Reference<T>> getUnregisteredIntrusiveHolders();

    @Accessor("byId")
    ObjectList<Holder.Reference<T>> getById();

    @Accessor("toId")
    Reference2IntMap<T> getToId();

    @Accessor("byLocation")
    Map<ResourceLocation, Holder.Reference<T>> getByLocation();

    @Accessor("byKey")
    Map<ResourceKey<T>, Holder.Reference<T>> getByKey();

    @Accessor("byValue")
    Map<T, Holder.Reference<T>> getByValue();

    @Accessor("lifecycles")
    Map<T, Lifecycle> getLifecycles();

    @Accessor("frozen")
    boolean isFrozen();

    @Accessor("frozen")
    void setFrozen(boolean frozen);

    @Accessor("holdersInOrder")
    void setHoldersInOrder(List<Holder.Reference<T>> o);

    @Accessor("registryLifecycle")
    void setRegistryLifecycle(Lifecycle base);
}
