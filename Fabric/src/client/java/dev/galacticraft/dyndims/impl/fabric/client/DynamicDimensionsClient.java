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

package dev.galacticraft.dyndims.impl.fabric.client;

import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dyndims.impl.Constants;
import dev.galacticraft.dyndims.impl.fabric.DynamicDimensions;
import dev.galacticraft.dyndims.impl.fabric.mixin.MappedRegistryAccessor;
import dev.galacticraft.dyndims.impl.fabric.util.UnfrozenRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalInt;

public final class DynamicDimensionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Constants.CREATE_WORLD_PACKET, (client, handler, buf, responseSender) -> {
            ResourceLocation id = buf.readResourceLocation();
            int rawId = buf.readInt();
            DimensionType type = DimensionType.DIRECT_CODEC.decode(NbtOps.INSTANCE, buf.readNbt()).get().orThrow().getFirst();
            client.execute(() -> {
                try (UnfrozenRegistry<DimensionType> unfrozen = UnfrozenRegistry.create(handler.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY))) {
                    unfrozen.registry().registerOrOverride(OptionalInt.of(rawId), ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id), type, Lifecycle.stable());
                }
                handler.levels().add(ResourceKey.create(Registry.DIMENSION_REGISTRY, id));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Constants.DELETE_WORLD_PACKET, (client, handler, buf, responseSender) -> {
            ResourceLocation id = buf.readResourceLocation();
            client.execute(() -> {
                try (UnfrozenRegistry<DimensionType> unfrozen = UnfrozenRegistry.create(handler.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY))) {
                    DimensionType dimensionType = unfrozen.registry().get(id);
                    int rawId = unfrozen.registry().getId(dimensionType);
                    if (dimensionType != null) {
                        MappedRegistryAccessor<DimensionType> accessor = (MappedRegistryAccessor<DimensionType>) unfrozen.registry();
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
                }
                handler.levels().remove(ResourceKey.create(Registry.DIMENSION_REGISTRY, id));
            });
        });
    }
}
