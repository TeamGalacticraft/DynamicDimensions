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

package dev.galacticraft.dynamicdimensions.impl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

/**
 * Commands for creating, unloading, and deleting dynamic dimensions
 */
@ApiStatus.Internal
public final class DynamicDimensionsCommands {
    private static final SimpleCommandExceptionType CANNOT_CREATE = new SimpleCommandExceptionType(Component.translatable("command.dynamicdimensions.create.error"));
    private static final SimpleCommandExceptionType CANNOT_DELETE = new SimpleCommandExceptionType(Component.translatable("command.dynamicdimensions.delete.error"));

    /**
     * Registers debug commands
     * @param dispatcher the server command dispatcher
     * @param registryAccess registry lookup for dynamic registries
     * @param environment the command environment
     */
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        if (Constants.CONFIG.enableCommands()) {
            dispatcher.register(Commands.literal("dynamicdimension")
                    .requires(s -> s.hasPermission(Constants.CONFIG.commandPermissionLevel()))
                    .then(Commands.literal("create")
                            .then(Commands.argument("id", ResourceLocationArgument.id())
                                    .then(Commands.argument("chunk_generator", CompoundTagArgument.compoundTag())
                                            .then(Commands.argument("dimension_type", CompoundTagArgument.compoundTag())
                                                    .executes(ctx -> {
                                                        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                                        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getSource().registryAccess());
                                                        ChunkGenerator generator = ChunkGenerator.CODEC.decode(ops, CompoundTagArgument.getCompoundTag(ctx, "chunk_generator")).get().orThrow().getFirst();
                                                        DimensionType type = DimensionType.DIRECT_CODEC.decode(ops, CompoundTagArgument.getCompoundTag(ctx, "dimension_type")).get().orThrow().getFirst();
                                                        DynamicDimensionRegistry from = DynamicDimensionRegistry.from(ctx.getSource().getServer());
                                                        if (from.anyDimensionExists(id)) {
                                                            throw CANNOT_CREATE.create();
                                                        }
                                                        if (from.createDynamicDimension(id, generator, type) == null) {
                                                            throw CANNOT_CREATE.create();
                                                        }
                                                        return 1;
                                                    })))
                                    .executes(ctx -> {
                                        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                        RegistryAccess access = ctx.getSource().registryAccess();
                                        ChunkGenerator generator = new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(access.lookupOrThrow(Registries.BIOME), access.lookupOrThrow(Registries.STRUCTURE_SET), access.lookupOrThrow(Registries.PLACED_FEATURE)));
                                        DimensionType type = new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0D, true, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
                                        DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(ctx.getSource().getServer());
                                        if (registry.anyDimensionExists(id)) {
                                            throw CANNOT_CREATE.create();
                                        }
                                        if (registry.createDynamicDimension(id, generator, type) == null) {
                                            throw CANNOT_CREATE.create();
                                        }
                                        return 1;
                                    })))
                    .then(Commands.literal("load")
                            .then(Commands.argument("id", ResourceLocationArgument.id())
                                    .then(Commands.argument("chunk_generator", CompoundTagArgument.compoundTag())
                                            .then(Commands.argument("dimension_type", CompoundTagArgument.compoundTag())
                                                    .executes(ctx -> {
                                                        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                                        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getSource().registryAccess());
                                                        ChunkGenerator generator = ChunkGenerator.CODEC.decode(ops, CompoundTagArgument.getCompoundTag(ctx, "chunk_generator")).get().orThrow().getFirst();
                                                        DimensionType type = DimensionType.DIRECT_CODEC.decode(ops, CompoundTagArgument.getCompoundTag(ctx, "dimension_type")).get().orThrow().getFirst();
                                                        DynamicDimensionRegistry from = DynamicDimensionRegistry.from(ctx.getSource().getServer());
                                                        if (from.anyDimensionExists(id)) {
                                                            throw CANNOT_CREATE.create();
                                                        }
                                                        if (from.loadDynamicDimension(id, generator, type) == null) {
                                                            throw CANNOT_CREATE.create();
                                                        }
                                                        return 1;
                                                    }))).executes(ctx -> {
                                        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                        RegistryAccess access = ctx.getSource().registryAccess();
                                        ChunkGenerator generator = new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(access.lookupOrThrow(Registries.BIOME), access.lookupOrThrow(Registries.STRUCTURE_SET), access.lookupOrThrow(Registries.PLACED_FEATURE)));
                                        DimensionType type = new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0D, true, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
                                        DynamicDimensionRegistry from = DynamicDimensionRegistry.from(ctx.getSource().getServer());
                                        if (from.anyDimensionExists(id)) {
                                            throw CANNOT_CREATE.create();
                                        }
                                        if (from.loadDynamicDimension(id, generator, type) == null) {
                                            throw CANNOT_CREATE.create();
                                        }
                                        return 1;
                                    })))
                    .then(Commands.literal("unload")
                            .then(Commands.argument("id", DimensionArgument.dimension())
                                    .executes(ctx -> {
                                        ServerLevel levelToDelete = DimensionArgument.getDimension(ctx, "id");
                                        ResourceKey<Level> key = levelToDelete.dimension();
                                        ResourceLocation id = key.location();
                                        if (!((DynamicDimensionRegistry) ctx.getSource().getServer()).canDeleteDimension(id)) {
                                            throw CANNOT_DELETE.create();
                                        }
                                        ((DynamicDimensionRegistry) ctx.getSource().getServer()).unloadDynamicDimension(id, null);
                                        return 1;
                                    })))
                    .then(Commands.literal("delete")
                            .then(Commands.argument("id", DimensionArgument.dimension())
                                    .executes(ctx -> {
                                        ServerLevel levelToDelete = DimensionArgument.getDimension(ctx, "id");
                                        ResourceKey<Level> key = levelToDelete.dimension();
                                        ResourceLocation id = key.location();
                                        if (!((DynamicDimensionRegistry) ctx.getSource().getServer()).canDeleteDimension(id)) {
                                            throw CANNOT_DELETE.create();
                                        }
                                        ((DynamicDimensionRegistry) ctx.getSource().getServer()).deleteDynamicDimension(id, null);
                                        return 1;
                                    }))));
            dispatcher.register(Commands.literal("dyndim").redirect(dispatcher.getRoot().getChild("dynamicdimension")));
        }
    }
}
