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

package dev.galacticraft.dynworlds.impl.client.network;

import com.mojang.serialization.Lifecycle;
import dev.galacticraft.dynworlds.impl.DynWorlds;
import dev.galacticraft.dynworlds.impl.accessor.DynamicRegistryManagerImmutableImplAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;

import java.util.OptionalInt;

public class DynWorldsS2CPacketReceivers {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DynWorlds.id("create_world"), (client, handler, buf, responseSender) -> {
            Identifier id = buf.readIdentifier();
            int rawId = buf.readInt();
            DimensionType type = DimensionType.CODEC.decode(RegistryOps.of(NbtOps.INSTANCE, handler.getRegistryManager()), buf.readNbt()).get().orThrow().getFirst();
            ((DynamicRegistryManagerImmutableImplAccessor) handler.getRegistryManager()).unfreezeTypes(reg -> {
                reg.replace(OptionalInt.of(rawId), RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id), type, Lifecycle.stable());
            });
            handler.getWorldKeys().add(RegistryKey.of(Registry.WORLD_KEY, id));
        });
    }
}
