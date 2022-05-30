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
import dev.galacticraft.dynworlds.impl.accessor.DynamicRegistryManagerImmutableImplAccessor;
import dev.galacticraft.dynworlds.impl.mixin.SimpleRegistryAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;

import java.util.OptionalInt;

public final class DynWorldsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Constant.id("create_world"), (client, handler, buf, responseSender) -> {
            Identifier id = buf.readIdentifier();
            int rawId = buf.readInt();
            DimensionType type = DimensionType.CODEC.decode(NbtOps.INSTANCE, buf.readNbt()).get().orThrow().getFirst();
            ((DynamicRegistryManagerImmutableImplAccessor) handler.getRegistryManager()).unfreezeTypes(reg -> reg.replace(OptionalInt.of(rawId), RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id), type, Lifecycle.stable()));
            handler.getWorldKeys().add(RegistryKey.of(Registry.WORLD_KEY, id));
        });

        ClientPlayNetworking.registerGlobalReceiver(Constant.id("destroy_world"), (client, handler, buf, responseSender) -> {
            Identifier id = buf.readIdentifier();
            ((DynamicRegistryManagerImmutableImplAccessor) handler.getRegistryManager()).unfreezeTypes(reg -> {
                DimensionType dimensionType = reg.get(id);
                if (dimensionType != null) {
                    SimpleRegistryAccessor<DimensionType> accessor = (SimpleRegistryAccessor<DimensionType>) reg;
                    accessor.getEntryToLifecycle().remove(dimensionType);
                    accessor.getRawIdToEntry().remove(reg.getRawId(dimensionType));
                    accessor.getRawIdToEntry().size(accessor.getRawIdToEntry().size() - 1);
                    accessor.getEntryToRawId().removeInt(dimensionType);
                    accessor.getValueToEntry().remove(dimensionType);
                    accessor.setCachedEntries(null);
                    accessor.getKeyToEntry().remove(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id));
                    accessor.getIdToEntry().remove(id);
                    Lifecycle base = Lifecycle.stable();
                    for (Lifecycle value : accessor.getEntryToLifecycle().values()) {
                        base.add(value);
                    }
                    accessor.setLifecycle(base);
                }
            });
            handler.getWorldKeys().remove(RegistryKey.of(Registry.WORLD_KEY, id));
        });
    }
}
