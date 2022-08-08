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

import dev.galacticraft.dyndims.api.DynamicLevelRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.OptionalLong;

public class DynamicDimensionsGametest implements FabricGameTest {
    private static final String EMPTY_STRUCTURE = "dyndims-gametest:empty";
    private static final ResourceLocation TEST_LEVEL = new ResourceLocation("dyndims-gametest", "level");

    void beforeEach(@NotNull GameTestHelper context) {
    }

    void afterEach(@NotNull GameTestHelper context) {
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2)
    void createDynamicDimension(@NotNull GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        DimensionType dimensionType = new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, false, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
        Assertions.assertTrue(((DynamicLevelRegistry) server).addDynamicDimension(TEST_LEVEL, server.overworld().getChunkSource().getGenerator(), dimensionType));
        context.runAfterDelay(1, () -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, TEST_LEVEL));
            Assertions.assertNotNull(level);
            context.succeed();
        });
    }

    @Override
    public void invokeTestMethod(@NotNull GameTestHelper context, @NotNull Method method) {
        method.setAccessible(true);
        GameTest annotation = method.getAnnotation(GameTest.class);
        if (annotation == null) throw new AssertionError("Test method without gametest annotation?!");
        beforeEach(context);
        FabricGameTest.super.invokeTestMethod(context, method);
        afterEach(context);
    }
}
