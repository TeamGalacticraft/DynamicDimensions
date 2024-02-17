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

package dev.galacticraft.dynamicdimensions.impl.gametest;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.mixin.MinecraftServerAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicDimensionsGametest {
    private static final String EMPTY_STRUCTURE = "empty"; // in minecraft namespace because forge.
    private static final ResourceLocation TEST_LEVEL_0 = new ResourceLocation("dyndims_test", "level_0");
    private static final ResourceLocation TEST_LEVEL_1 = new ResourceLocation("dyndims_test", "level_1");
    private static final ResourceLocation TEST_LEVEL_2 = new ResourceLocation("dyndims_test", "level_2");
    private static final ResourceLocation TEST_LEVEL_3 = new ResourceLocation("dyndims_test", "level_3");
    private static final ResourceLocation TEST_LEVEL_4 = new ResourceLocation("dyndims_test", "level_4");
    private static final AtomicBoolean LOCK = new AtomicBoolean(false);
    private static final int TIMEOUT_TICKS = 50;

    private synchronized void awaitLock(GameTestHelper context, Runnable runnable) { //todo: better way of doing this.
        if (LOCK.compareAndExchange(false, true)) {
            context.runAfterDelay(1, () -> awaitLock(context, runnable));
        } else {
            runnable.run();
        }
    }

    public static void resetLock() {
        LOCK.set(false);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = TIMEOUT_TICKS, batch = "dyndims/default")
    public void createDynamicDimension(@NotNull GameTestHelper context) {
        awaitLock(context, () -> {
            Constants.CONFIG.applyDefaultValues();
            final MinecraftServer server = context.getLevel().getServer();
            final ServerLevel overworld = server.overworld();
            final DimensionType dimensionType = createDimensionType();
            Assertions.assertNotNull(overworld);
            Assertions.assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_0));

            Assertions.assertTrue(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_0, overworld.getChunkSource().getGenerator(), dimensionType));
            context.runAfterDelay(1, () -> {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_0));
                Assertions.assertNotNull(level);
                Assertions.assertTrue(((DynamicDimensionRegistry) server).dynamicDimensionExists(TEST_LEVEL_0));
                Assertions.assertTrue(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_0));
                ((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_0, (server1, player) -> player.disconnect()); // Forge loads the dimension even though it shouldn't
                resetLock();
                context.runAfterDelay(1, context::succeed); // assert that the dimension is actually removed
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = TIMEOUT_TICKS, batch = "dyndims/no_create_dim")
    public void disableDimensionCreation(@NotNull GameTestHelper context) {
        awaitLock(context, () -> {
            Constants.CONFIG.applyDefaultValues();
            Constants.CONFIG.allowDimensionCreation(false);
            Constants.CONFIG.deleteRemovedDimensions(true);
            final MinecraftServer server = context.getLevel().getServer();
            final ServerLevel overworld = server.overworld();
            final DimensionType dimensionType = createDimensionType();
            Assertions.assertNotNull(overworld);
            Assertions.assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_2));

            Assertions.assertFalse(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_2, overworld.getChunkSource().getGenerator(), dimensionType));
            context.runAfterDelay(1, () -> {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_2));
                Assertions.assertNull(level);
                Assertions.assertFalse(((DynamicDimensionRegistry) server).dynamicDimensionExists(TEST_LEVEL_0));
                Assertions.assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_0));
                context.succeed();
                resetLock();
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = TIMEOUT_TICKS, batch = "dyndims/default")
    public void removeDynamicDimension(@NotNull GameTestHelper context) {
        awaitLock(context, () -> {
            Constants.CONFIG.applyDefaultValues();
            final MinecraftServer server = context.getLevel().getServer();
            final ServerLevel overworld = server.overworld();
            final DimensionType dimensionType = createDimensionType();
            Assertions.assertNotNull(overworld);
            Assertions.assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_1));

            Assertions.assertTrue(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_1, overworld.getChunkSource().getGenerator(), dimensionType));
            context.runAfterDelay(1, () -> {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_1));
                Assertions.assertNotNull(level);
                Assertions.assertTrue(((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_1, (server1, player) -> player.changeDimension(overworld)));
                context.runAfterDelay(1, () -> {
                    ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_1));
                    Assertions.assertNull(level2);
                    context.succeed();
                    resetLock();
                });
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = TIMEOUT_TICKS, batch = "dyndims/dim_delete")
    public void removedDimensionsDelete(@NotNull GameTestHelper context) {
        awaitLock(context, () -> {
            Constants.CONFIG.applyDefaultValues();
            Constants.CONFIG.deleteRemovedDimensions(true);
            final MinecraftServer server = context.getLevel().getServer();
            final Path worldDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_3));
            final File file = worldDir.toFile();
            final ServerLevel overworld = server.overworld();
            final DimensionType dimensionType = createDimensionType();

            Path resolved;
            String id = TEST_LEVEL_3.toString().replace(':', ',');
            if (worldDir.getParent().getFileName().toString().equals(TEST_LEVEL_3.getNamespace())) {
                resolved = worldDir.getParent().resolveSibling("deleted").resolve(id);
            } else {
                resolved = worldDir.resolveSibling(id + "_deleted");
            }

            try {
                FileUtils.deleteDirectory(file); // make sure there is no directory in the first place
                FileUtils.deleteDirectory(resolved.toFile());
            } catch (IOException e) {
                GameTestAssertException ex = new GameTestAssertException("Failed to delete dimension folder!");
                ex.addSuppressed(e);
                throw ex;
            }

            Assertions.assertNotNull(overworld);
            Assertions.assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_3));

            Assertions.assertTrue(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_3, overworld.getChunkSource().getGenerator(), dimensionType));
            context.runAfterDelay(1, () -> {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_3));
                Assertions.assertNotNull(level);

                level.save(null, true, false);
                Assertions.assertTrue(file.isDirectory());

                Assertions.assertTrue(((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_3, (server1, player) -> player.changeDimension(overworld)));
                context.runAfterDelay(1, () -> {
                    ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_3));
                    Assertions.assertNull(level2);
                    Assertions.assertFalse(file.isDirectory());
                    Assertions.assertFalse(resolved.toFile().exists());
                    resetLock();
                    context.succeed();
                });
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = TIMEOUT_TICKS, batch = "dyndims/default")
    public void removedDimensionsMove(@NotNull GameTestHelper context) {
        awaitLock(context, () -> {
            Constants.CONFIG.applyDefaultValues();
            final MinecraftServer server = context.getLevel().getServer();
            final Path worldDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
            final File file = worldDir.toFile();
            final ServerLevel overworld = server.overworld();
            final DimensionType dimensionType = createDimensionType();

            Path resolved;
            String id = TEST_LEVEL_4.toString().replace(':', ',');
            if (worldDir.getParent().getFileName().toString().equals(TEST_LEVEL_4.getNamespace())) {
                resolved = worldDir.getParent().resolveSibling("deleted").resolve(id);
            } else {
                resolved = worldDir.resolveSibling(id + "_deleted");
            }

            try {
                FileUtils.deleteDirectory(file); // make sure there is no directory in the first place
                FileUtils.deleteDirectory(resolved.toFile());
            } catch (IOException e) {
                GameTestAssertException ex = new GameTestAssertException("Failed to delete dimension folder!");
                ex.addSuppressed(e);
                throw ex;
            }

            Assertions.assertNotNull(overworld);
            Assertions.assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_4));

            Assertions.assertTrue(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_4, overworld.getChunkSource().getGenerator(), dimensionType));
            context.runAfterDelay(1, () -> {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
                Assertions.assertNotNull(level);

                level.save(null, true, false);
                Assertions.assertTrue(file.isDirectory());

                Assertions.assertTrue(((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_4, (server1, player) -> player.changeDimension(overworld)));
                context.runAfterDelay(1, () -> {
                    ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
                    Assertions.assertNull(level2);
                    Assertions.assertFalse(file.isDirectory());
                    Assertions.assertTrue(resolved.toFile().exists());

                    context.succeed();
                    resetLock();
                });
            });
        });
    }

    @Contract(" -> new")
    private static @NotNull DimensionType createDimensionType() {
        return new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, false, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
    }
}
