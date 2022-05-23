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

package dev.galacticraft.dynworlds.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class DynworldsGametest implements FabricGameTest {
    private static final String EMPTY_STRUCTURE = "dynworlds-test:empty";

    void beforeEach(@NotNull TestContext context) {
    }

    void afterEach(@NotNull TestContext context) {
    }

    @GameTest(structureName = EMPTY_STRUCTURE, tickLimit = 0)
    void testEmptyStructure(@NotNull TestContext context) {
        // no-op
    }

    @Override
    public void invokeTestMethod(@NotNull TestContext context, @NotNull Method method) {
        method.setAccessible(true);
        GameTest annotation = method.getAnnotation(GameTest.class);
        if (annotation == null) throw new AssertionError("Test method without gametest annotation?!");
        if (annotation.tickLimit() == 0) {
            context.addInstantFinalTask(() -> {
                beforeEach(context);
                FabricGameTest.super.invokeTestMethod(context, method);
                afterEach(context);
            });
        } else {
            beforeEach(context);
            FabricGameTest.super.invokeTestMethod(context, method);
            afterEach(context);
        }
    }
}
