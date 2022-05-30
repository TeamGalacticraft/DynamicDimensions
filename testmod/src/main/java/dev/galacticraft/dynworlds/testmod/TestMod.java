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
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.world.dimension.DimensionOptions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class TestMod implements ModInitializer {
    public static final String MOD_ID = "dynworlds-test";
    SimpleCommandExceptionType ID_EXISTS = new SimpleCommandExceptionType(new LiteralText("A world with that ID already exists!"));

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("dynworlds:create").requires(s -> s.hasPermissionLevel(2)).then(CommandManager.argument("id", IdentifierArgumentType.identifier()).then(CommandManager.argument("options", NbtCompoundArgumentType.nbtCompound()).executes(ctx -> {
                Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
                DimensionOptions options = DimensionOptions.CODEC.decode(RegistryOps.of(NbtOps.INSTANCE, ctx.getSource().getRegistryManager()), NbtCompoundArgumentType.getNbtCompound(ctx, "options")).get().orThrow().getFirst();
                if (!((DynamicWorldRegistry) ctx.getSource().getServer()).canCreateWorld(id)) {
                    throw ID_EXISTS.create();
                }
                ((DynamicWorldRegistry) ctx.getSource().getServer()).addDynamicWorld(id, options, options.getDimensionTypeSupplier().getKeyOrValue().right().get());
                return 1;
            }))));

            dispatcher.register(CommandManager.literal("dynworlds:remove").requires(s -> s.hasPermissionLevel(2)).then(CommandManager.argument("id", IdentifierArgumentType.identifier()).executes(ctx -> {
                Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
                if (!((DynamicWorldRegistry) ctx.getSource().getServer()).canDestroyWorld(id)) {
                    throw ID_EXISTS.create();
                }
                ((DynamicWorldRegistry) ctx.getSource().getServer()).removeDynamicWorld(id, (server, player) -> {
                    player.sendMessage(new LiteralText("World " + id.toString() + " was removed."), MessageType.SYSTEM, Util.NIL_UUID);
                    player.giveItemStack(new ItemStack(Items.DIRT));
                    ServerWorld overworld = server.getOverworld();
                    player.teleport(overworld, player.getX(), 512, player.getZ(), player.getYaw(), player.getPitch());
                    player.setVelocity((overworld.random.nextDouble() - 0.5) * 10.0, -overworld.random.nextDouble() * 10.0, (overworld.random.nextDouble() - 0.5) * 10.0);
                    player.giveItemStack(new ItemStack(Items.APPLE));
//                    player.fallDistance = 1000;
                });
                return 1;
            })));
        });
    }

    @Contract("_ -> new")
    public static @NotNull Identifier id(@NotNull String id) {
        return new Identifier(MOD_ID, id);
    }
}
