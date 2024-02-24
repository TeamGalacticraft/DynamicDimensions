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

package dev.galacticraft.dynamicdimensions.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;

/**
 * Removes players from a {@link net.minecraft.world.level.Level}.
 */
@FunctionalInterface
public interface PlayerRemover {
    /**
     * Attempts to bring players to their personal spawn point, otherwise to the default (overworld) spawn point.
     */
    PlayerRemover DEFAULT = (server, player) -> {
        player.sendSystemMessage(Component.translatable("command.dynamicdimensions.delete.removed", player.serverLevel().dimension().location()), true);
        ServerLevel level = server.getLevel(player.getRespawnDimension());
        if (level != null && level != player.serverLevel()) {
            BlockPos pos = player.getRespawnPosition();
            if (pos != null) {
                player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
            } else {
                LevelData levelData = level.getLevelData();
                player.teleportTo(level, levelData.getXSpawn() + 0.5, levelData.getYSpawn(), levelData.getZSpawn() + 0.5, player.getYRot(), player.getXRot());
            }
        } else {
            level = server.overworld();
            LevelData levelData = level.getLevelData();
            player.teleportTo(level, levelData.getXSpawn() + 0.5, levelData.getYSpawn(), levelData.getZSpawn() + 0.5, player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(0.0, 0.0, 0.0);
    };

    /**
     * Called when a player must be removed from the level.
     * May cause unexpected behaviour if the player is not actually removed from the level.
     *
     * @param server The server instance
     * @param player The player to be removed
     */
    void removePlayer(MinecraftServer server, ServerPlayer player);
}
