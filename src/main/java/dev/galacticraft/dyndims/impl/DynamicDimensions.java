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

package dev.galacticraft.dyndims.impl;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.galacticraft.dyndims.api.DynamicDimensionRegistry;
import dev.galacticraft.dyndims.api.config.DynamicDimensionsConfig;
import dev.galacticraft.dyndims.impl.config.DynamicDimensionsConfigImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DynamicDimensions implements ModInitializer {
    public static final String MOD_ID = "dyndims";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final DynamicDimensionsConfig CONFIG = DynamicDimensionsConfigImpl.create();

    public static final ResourceLocation CREATE_WORLD_PACKET = new ResourceLocation(MOD_ID, "create_world");
    public static final ResourceLocation DELETE_WORLD_PACKET = new ResourceLocation(MOD_ID, "delete_world");

    private static final SimpleCommandExceptionType CANNOT_CREATE = new SimpleCommandExceptionType(Component.translatable("command.dyndims.create.error"));
    private static final SimpleCommandExceptionType CANNOT_DELETE = new SimpleCommandExceptionType(Component.translatable("command.dyndims.delete.error"));

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            if (CONFIG.enableCommands()) {
                dispatcher.register(Commands.literal("dimension")
                        .requires(s -> s.hasPermission(CONFIG.commandPermissionLevel()))
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .then(Commands.argument("chunk_generator", CompoundTagArgument.compoundTag())
                                                .then(Commands.argument("dimension_type", CompoundTagArgument.compoundTag())
                                                        .executes(ctx -> {
                                                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                                            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getSource().registryAccess());
                                                            ChunkGenerator generator = ChunkGenerator.CODEC.decode(ops, CompoundTagArgument.getCompoundTag(ctx, "chunk_generator")).get().orThrow().getFirst();
                                                            DimensionType type = DimensionType.DIRECT_CODEC.decode(ops, CompoundTagArgument.getCompoundTag(ctx, "dimension_type")).get().orThrow().getFirst();
                                                            if (!((DynamicDimensionRegistry) ctx.getSource().getServer()).canCreateDimension(id)) {
                                                                throw CANNOT_CREATE.create();
                                                            }
                                                            ((DynamicDimensionRegistry) ctx.getSource().getServer()).addDynamicDimension(id, generator, type);
                                                            return 1;
                                                        })))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(ctx -> {
                                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                            if (!((DynamicDimensionRegistry) ctx.getSource().getServer()).canDeleteDimension(id)) {
                                                throw CANNOT_DELETE.create();
                                            }
                                            ((DynamicDimensionRegistry) ctx.getSource().getServer()).removeDynamicDimension(id, (server, player) -> {
                                                player.sendSystemMessage(Component.translatable("command.dyndims.delete.removed", id), true);
                                                ServerLevel level = server.getLevel(player.getRespawnDimension());
                                                if (level != null) {
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
                                            });
                                            return 1;
                                        }))));
            }
        });
    }
}
