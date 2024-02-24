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
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalLong;

import static dev.galacticraft.dynamicdimensions.impl.gametest.Assertions.*;

/**
 * GameTests for DynamicDimensions
 */
@ApiStatus.Internal
public class DynamicDimensionsGametest {
    private static final String EMPTY_STRUCTURE = "empty"; // in minecraft namespace because forge.
    private static final ResourceLocation TEST_LEVEL_0 = new ResourceLocation(Constants.MOD_ID, "level_0");
    private static final ResourceLocation TEST_LEVEL_1 = new ResourceLocation(Constants.MOD_ID, "level_1");
    private static final ResourceLocation TEST_LEVEL_2 = new ResourceLocation(Constants.MOD_ID, "level_2");
    private static final ResourceLocation TEST_LEVEL_3 = new ResourceLocation(Constants.MOD_ID, "level_3");
    private static final ResourceLocation TEST_LEVEL_4 = new ResourceLocation(Constants.MOD_ID, "level_4");
    private static final ResourceLocation TEST_LEVEL_5 = new ResourceLocation(Constants.MOD_ID, "level_5");

    /**
     * Checks if dimensions can be created.
     * @param context GameTest context
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3)
    public void createDynamicDimension(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        assertNotNull(overworld);
        assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_0));

        assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_0, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_0));
            assertNotNull(level);
            assertTrue(((DynamicDimensionRegistry) server).dynamicDimensionExists(TEST_LEVEL_0));
            assertTrue(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_0));
            ((DynamicDimensionRegistry) server).deleteDynamicDimension(TEST_LEVEL_0, (server1, player) -> player.disconnect()); // Forge loads the dimension even though it shouldn't
            context.runAfterDelay(1, context::succeed); // assert that the dimension is actually removed
        });
    }

    /**
     * Checks if dimensions can be unloaded.
     * @param context GameTest context
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3)
    public void unloadDynamicDimension(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        assertNotNull(overworld);
        assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_1));

        assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_1, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_1));
            assertNotNull(level);
            assertTrue(((DynamicDimensionRegistry) server).unloadDynamicDimension(TEST_LEVEL_1, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_1));
                assertNull(level2);
                context.succeed();
            });
        });
    }

    /**
     * Checks if deleted dimensions actually have their files deleted.
     * @param context GameTest context
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3)
    public void removedDimensionsDelete(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final Path worldDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_3));
        final File file = worldDir.toFile();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        
        try {
            FileUtils.deleteDirectory(file); // make sure there is no directory in the first place
        } catch (IOException e) {
            GameTestAssertException ex = new GameTestAssertException("Failed to delete dimension folder!");
            ex.addSuppressed(e);
            throw ex;
        }

        assertNotNull(overworld);
        assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_3));

        assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_3, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_3));
            assertNotNull(level);

            level.save(null, true, false);
            assertTrue(file.isDirectory());

            assertTrue(((DynamicDimensionRegistry) server).deleteDynamicDimension(TEST_LEVEL_3, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_3));
                assertNull(level2);
                assertFalse(file.isDirectory());
                context.succeed();
            });
        });
    }

    /**
     * Checks that unloaded dimensions are saved and are not deleted.
     * @param context GameTest context
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 3)
    public void unloadedDynamicDimensionSaved(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final Path levelDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_2));
        final File file = levelDir.toFile();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        
        try {
            FileUtils.deleteDirectory(file); // make sure there is no directory in the first place
        } catch (IOException e) {
            GameTestAssertException ex = new GameTestAssertException("Failed to delete dimension folder!");
            ex.addSuppressed(e);
            throw ex;
        }

        assertNotNull(overworld);
        assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_2));

        assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_2, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_2));
            assertNotNull(level);

            level.save(null, true, false);
            assertTrue(file.isDirectory());

            assertTrue(((DynamicDimensionRegistry) server).unloadDynamicDimension(TEST_LEVEL_2, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_2));
                assertNull(level2);
                assertTrue(file.isDirectory());
                try {
                    FileUtils.deleteDirectory(file); // cleanup
                } catch (IOException ignored) {}
                
                context.succeed();
            });
        });
    }

    /**
     * Checks that unloaded dimensions are saved and can be reloaded.
     * @param context GameTest context
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 4)
    public void reloadDynamicDimension(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final Path levelDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
        final File file = levelDir.toFile();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();

        try {
            FileUtils.deleteDirectory(file); // make sure there is no directory in the first place
        } catch (IOException e) {
            GameTestAssertException ex = new GameTestAssertException("Failed to delete dimension folder!");
            ex.addSuppressed(e);
            throw ex;
        }

        assertNotNull(overworld);
        assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_4));

        assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_4, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
            assertNotNull(level);

            level.setBlock(BlockPos.ZERO, Blocks.REDSTONE_LAMP.defaultBlockState(), 0);

            level.save(null, true, false);
            assertTrue(file.isDirectory());

            assertTrue(((DynamicDimensionRegistry) server).unloadDynamicDimension(TEST_LEVEL_4, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
                assertNull(level2); // dimension was deleted
                assertTrue(file.isDirectory());

                // re-load dimension
                assertNotNull(((DynamicDimensionRegistry) server).loadDynamicDimension(TEST_LEVEL_4, overworld.getChunkSource().getGenerator(), dimensionType));

                context.runAfterDelay(1, () -> {
                    ServerLevel level3 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_4));
                    assertNotNull(level3);
                    assertEquals(level3.getBlockState(BlockPos.ZERO), Blocks.REDSTONE_LAMP.defaultBlockState());

                    context.succeed();
                    ((DynamicDimensionRegistry) server).deleteDynamicDimension(TEST_LEVEL_4, (server1, player) -> player.changeDimension(overworld));
                });
            });
        });
    }

    /**
     * Checks that creating a new dynamic dimension does not load previous dimension data.
     * @param context GameTest context
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 4)
    public void recreateDynamicDimension(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final Path levelDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_5));
        final File file = levelDir.toFile();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();

        try {
            FileUtils.deleteDirectory(file); // make sure there is no directory in the first place
        } catch (IOException e) {
            GameTestAssertException ex = new GameTestAssertException("Failed to delete dimension folder!");
            ex.addSuppressed(e);
            throw ex;
        }

        assertNotNull(overworld);
        assertFalse(((DynamicDimensionRegistry) server).anyDimensionExists(TEST_LEVEL_5));

        assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_5, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_5));
            assertNotNull(level);

            level.setBlock(BlockPos.ZERO, Blocks.REDSTONE_LAMP.defaultBlockState(), 0);

            level.save(null, true, false);
            assertTrue(file.isDirectory());

            assertTrue(((DynamicDimensionRegistry) server).unloadDynamicDimension(TEST_LEVEL_5, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_5));
                assertNull(level2); // dimension was deleted
                assertTrue(file.isDirectory());

                // re-load dimension
                assertNotNull(((DynamicDimensionRegistry) server).createDynamicDimension(TEST_LEVEL_5, overworld.getChunkSource().getGenerator(), dimensionType));

                context.runAfterDelay(1, () -> {
                    ServerLevel level3 = server.getLevel(ResourceKey.create(Registries.DIMENSION, TEST_LEVEL_5));
                    assertNotNull(level3);
                    assertNotEquals(level3.getBlockState(BlockPos.ZERO), Blocks.REDSTONE_LAMP.defaultBlockState());

                    context.succeed();
                    ((DynamicDimensionRegistry) server).deleteDynamicDimension(TEST_LEVEL_5, (server1, player) -> player.changeDimension(overworld));
                });
            });
        });
    }

    /**
     * Constructs an arbitrary dimension type for testing.
     * @return a new, unregistered dimension type
     */
    @Contract(" -> new")
    private static @NotNull DimensionType createDimensionType() {
        return new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, false, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
    }
}
