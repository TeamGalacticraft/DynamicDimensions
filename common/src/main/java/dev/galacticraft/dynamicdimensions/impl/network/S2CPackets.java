/*
 * Copyright (c) 2021-2026 Team Galacticraft
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

package dev.galacticraft.dynamicdimensions.impl.network;

import dev.galacticraft.dynamicdimensions.impl.Constants;
import io.netty.buffer.Unpooled;
import lol.bai.badpackets.api.PacketSender;
import lol.bai.badpackets.api.play.PlayPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class S2CPackets {
    public static void registerChannels() {
        PlayPackets.registerClientChannel(Constants.CREATE_DIMENSION_PACKET);
        PlayPackets.registerClientChannel(Constants.REMOVE_DIMENSION_PACKET);
    }

    public static void sendCreateDimension(ServerPlayer player, ResourceLocation id, CompoundTag serializedType) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(id);
        buf.writeNbt(serializedType);

        PacketSender.s2c(player).send(Constants.CREATE_DIMENSION_PACKET, buf);
    }

    public static void sendRemoveDimension(ServerPlayer player, ResourceLocation id) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(id);
        PacketSender.s2c(player).send(Constants.REMOVE_DIMENSION_PACKET, buf);
    }
}
