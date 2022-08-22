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

package dev.galacticraft.dyndims.gametest;

import dev.galacticraft.dyndims.api.DynamicDimensionRegistry;
import dev.galacticraft.dyndims.gametest.mixin.MinecraftServerAccessor;
import dev.galacticraft.dyndims.impl.Constants;
import net.minecraft.core.Registry;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalLong;

public class DynamicDimensionsGametest {
    private static final String EMPTY_STRUCTURE = "empty"; // in minecraft namespace because forge.
    private static final ResourceLocation TEST_LEVEL_0 = new ResourceLocation("dyndims_test", "level_0"); // tests that run in parallel
    private static final ResourceLocation TEST_LEVEL_1 = new ResourceLocation("dyndims_test", "level_1");
    private static final ResourceLocation TEST_LEVEL_REUSABLE = new ResourceLocation("dyndims_test", "reusable"); // for guaranteed non-parallel tests

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2, batch = "dyndims/t0")
    public void createDynamicDimension(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        Assertions.assertNotNull(overworld);
        Assertions.assertFalse(((DynamicDimensionRegistry) server).dimensionExists(TEST_LEVEL_0));

        Assertions.assertTrue(((DynamicDimensionRegistry) server).addDynamicDimension(TEST_LEVEL_0, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_0));
            Assertions.assertNotNull(level);
            ((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_0, (server1, player) -> player.disconnect()); // Forge loads the dimension even though it shouldn't
            context.runAfterDelay(1, context::succeed); // assert that the dimension is actually removed
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 1, batch = "dyndims/config_changing/0") // separate batch to avoid parallel execution
    public void disableDimensionCreation(@NotNull GameTestHelper context) {
        Constants.CONFIG.allowDimensionCreation(false);
        final MinecraftServer server = context.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        Assertions.assertNotNull(overworld);
        Assertions.assertFalse(((DynamicDimensionRegistry) server).dimensionExists(TEST_LEVEL_REUSABLE));

        Assertions.assertFalse(((DynamicDimensionRegistry) server).addDynamicDimension(TEST_LEVEL_REUSABLE, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
            Assertions.assertNull(level);
            context.succeed();
        });
        Constants.CONFIG.allowDimensionCreation(true);
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2, batch = "dyndims/t1")
    public void removeDynamicDimension(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();
        Assertions.assertNotNull(overworld);
        Assertions.assertFalse(((DynamicDimensionRegistry) server).dimensionExists(TEST_LEVEL_1));

        Assertions.assertTrue(((DynamicDimensionRegistry) server).addDynamicDimension(TEST_LEVEL_1, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_1));
            Assertions.assertNotNull(level);
            Assertions.assertTrue(((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_1, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_1));
                Assertions.assertNull(level2);
                context.succeed();
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2, batch = "dyndims/config_changing/1")
    public void removedDimensionsDelete(@NotNull GameTestHelper context) {
        Constants.CONFIG.deleteRemovedDimensions(true);
        final MinecraftServer server = context.getLevel().getServer();
        final Path worldDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
        final File file = worldDir.toFile();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();

        Path resolved;
        String id = TEST_LEVEL_REUSABLE.toString().replace(":", ",");
        if (worldDir.getParent().getFileName().toString().equals(TEST_LEVEL_REUSABLE.getNamespace())) {
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
        Assertions.assertFalse(((DynamicDimensionRegistry) server).dimensionExists(TEST_LEVEL_REUSABLE));

        Assertions.assertTrue(((DynamicDimensionRegistry) server).addDynamicDimension(TEST_LEVEL_REUSABLE, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
            Assertions.assertNotNull(level);

            level.save(null, true,  false);
            Assertions.assertTrue(file.isDirectory());

            Assertions.assertTrue(((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_REUSABLE, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
                Assertions.assertNull(level2);
                Assertions.assertFalse(file.isDirectory());
                Assertions.assertFalse(resolved.toFile().exists());

                context.succeed();
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2, batch = "dyndims/config_changing/2")
    public void removedDimensionsMove(@NotNull GameTestHelper context) {
        Constants.CONFIG.deleteRemovedDimensions(false);
        final MinecraftServer server = context.getLevel().getServer();
        final Path worldDir = ((MinecraftServerAccessor) server).getStorageSource().getDimensionPath(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
        final File file = worldDir.toFile();
        final ServerLevel overworld = server.overworld();
        final DimensionType dimensionType = createDimensionType();

        Path resolved;
        String id = TEST_LEVEL_REUSABLE.toString().replace(":", ",");
        if (worldDir.getParent().getFileName().toString().equals(TEST_LEVEL_REUSABLE.getNamespace())) {
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
        Assertions.assertFalse(((DynamicDimensionRegistry) server).dimensionExists(TEST_LEVEL_REUSABLE));

        Assertions.assertTrue(((DynamicDimensionRegistry) server).addDynamicDimension(TEST_LEVEL_REUSABLE, overworld.getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
            Assertions.assertNotNull(level);

            level.save(null, true,  false);
            Assertions.assertTrue(file.isDirectory());

            Assertions.assertTrue(((DynamicDimensionRegistry) server).removeDynamicDimension(TEST_LEVEL_REUSABLE, (server1, player) -> player.changeDimension(overworld)));
            context.runAfterDelay(1, () -> {
                ServerLevel level2 = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL_REUSABLE));
                Assertions.assertNull(level2);
                Assertions.assertFalse(file.isDirectory());
                Assertions.assertTrue(resolved.toFile().exists());

                context.succeed();
            });
        });
    }

    private static DimensionType createDimensionType() {
        return new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, false, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
    }
}
