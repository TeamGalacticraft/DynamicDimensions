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

package dev.galacticraft.dynworlds.testmod;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.galacticraft.dynworlds.api.DynamicWorldRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class TestMod implements ModInitializer {
    public static final String MOD_ID = "dynworlds-test";
    SimpleCommandExceptionType ID_EXISTS = new SimpleCommandExceptionType(new TextComponent("A world with that ID already exists!"));

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(Commands.literal("dynworlds:create").requires(s -> s.hasPermission(2)).then(Commands.argument("id", ResourceLocationArgument.id()).then(Commands.argument("options", CompoundTagArgument.compoundTag()).executes(ctx -> {
                ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                LevelStem options = LevelStem.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, ctx.getSource().registryAccess()), CompoundTagArgument.getCompoundTag(ctx, "options")).get().orThrow().getFirst();
                if (!((DynamicWorldRegistry) ctx.getSource().getServer()).canCreateWorld(id)) {
                    throw ID_EXISTS.create();
                }
                ((DynamicWorldRegistry) ctx.getSource().getServer()).addDynamicWorld(id, options, options.typeHolder().unwrap().right().get());
                return 1;
            }))));

            dispatcher.register(Commands.literal("dynworlds:remove").requires(s -> s.hasPermission(2)).then(Commands.argument("id", ResourceLocationArgument.id()).executes(ctx -> {
                ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                if (!((DynamicWorldRegistry) ctx.getSource().getServer()).canDestroyWorld(id)) {
                    throw ID_EXISTS.create();
                }
                ((DynamicWorldRegistry) ctx.getSource().getServer()).removeDynamicWorld(id, (server, player) -> {
                    player.sendMessage(new TextComponent("World " + id.toString() + " was removed."), ChatType.SYSTEM, Util.NIL_UUID);
                    player.addItem(new ItemStack(Items.DIRT));
                    ServerLevel overworld = server.overworld();
                    player.teleportTo(overworld, player.getX(), 512, player.getZ(), player.getYRot(), player.getXRot());
                    player.setDeltaMovement((overworld.random.nextDouble() - 0.5) * 10.0, -overworld.random.nextDouble() * 10.0, (overworld.random.nextDouble() - 0.5) * 10.0);
                    player.addItem(new ItemStack(Items.APPLE));
//                    player.fallDistance = 1000;
                });
                return 1;
            })));
        });
    }

    @Contract("_ -> new")
    public static @NotNull ResourceLocation id(@NotNull String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
