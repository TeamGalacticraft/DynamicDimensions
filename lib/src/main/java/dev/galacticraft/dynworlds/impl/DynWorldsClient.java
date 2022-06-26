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

package dev.galacticraft.dynworlds.impl;

import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dynworlds.impl.accessor.ImmutableRegistryAccessAccessor;
import dev.galacticraft.dynworlds.impl.mixin.MappedRegistryAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalInt;

public final class DynWorldsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Constant.id("create_world"), (client, handler, buf, responseSender) -> {
            ResourceLocation id = buf.readResourceLocation();
            int rawId = buf.readInt();
            DimensionType type = DimensionType.DIRECT_CODEC.decode(NbtOps.INSTANCE, buf.readNbt()).get().orThrow().getFirst();
            ((ImmutableRegistryAccessAccessor) handler.registryAccess()).unfreezeTypes(reg -> reg.registerOrOverride(OptionalInt.of(rawId), ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), type, Lifecycle.stable()));
            handler.levels().add(ResourceKey.create(Registry.DIMENSION_REGISTRY, id));
        });

        ClientPlayNetworking.registerGlobalReceiver(Constant.id("destroy_world"), (client, handler, buf, responseSender) -> {
            ResourceLocation id = buf.readResourceLocation();
            ((ImmutableRegistryAccessAccessor) handler.registryAccess()).unfreezeTypes(reg -> {
                DimensionType dimensionType = reg.get(id);
                int rawId = reg.getId(dimensionType);
                if (dimensionType != null) {
                    MappedRegistryAccessor<DimensionType> accessor = (MappedRegistryAccessor<DimensionType>) reg;
                    accessor.getLifecycles().remove(dimensionType);
                    accessor.getById().remove(rawId);
                    accessor.getToId().removeInt(dimensionType);
                    accessor.getByValue().remove(dimensionType);
                    accessor.setHoldersInOrder(null);
                    accessor.getByKey().remove(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id));
                    accessor.getByLocation().remove(id);
                    Lifecycle base = Lifecycle.stable();
                    for (Lifecycle value : accessor.getLifecycles().values()) {
                        base.add(value);
                    }
                    accessor.setElementsLifecycle(base);
                }
            });
            handler.levels().remove(ResourceKey.create(Registry.DIMENSION_REGISTRY, id));
        });
    }
}
