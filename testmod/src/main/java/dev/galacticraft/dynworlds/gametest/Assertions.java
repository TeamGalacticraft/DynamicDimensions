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

import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Assertions {
    public static void assertTrue(boolean b) {
        if (!b) {
            throw new GameTestException(format(true, false, 1));
        }
    }

    public static void assertFalse(boolean b) {
        if (b) {
            throw new GameTestException(format(false, true, 1));
        }
    }

    public static void assertEquals(boolean a, boolean b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(byte a, byte b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(short a, short b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(int a, int b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(long a, long b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(float a, float b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(double a, double b) {
        if (a != b) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertEquals(Object a, Object b) {
        if (!Objects.equals(a, b)) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    //apparently itemstack does not implement Object#equals()
    public static void assertEquals(ItemStack a, ItemStack b) {
        if (a == null || b == null || !ItemStack.canCombine(a, b) || a.getCount() != b.getCount()) {
            throw new GameTestException(format(a, b, 1));
        }
    }

    public static void assertSame(Object a, Object b) {
        if (a != b) {
            String aStr = "null";
            String bStr = "null";
            if (a != null) {
                aStr = a.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(a)) + "[" + a + "]";
            }
            if (b != null) {
                bStr = b.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(b)) + "[" + b + "]";
            }
            throw new GameTestException(format(aStr, bStr, 1));
        }
    }

    public static void assertThrows(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            return;
        }
        throw new GameTestException(format("<any exception>", "<none>", 1));
    }

    public static <T extends Throwable> void assertThrows(Class<T> clazz, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (!clazz.isInstance(throwable)) {
                GameTestException gameTestException = new GameTestException(format(clazz.getName(), throwable.getClass().getName(), 1));
                gameTestException.addSuppressed(throwable);
                throw gameTestException;
            } else {
                return;
            }
        }
        throw new GameTestException(format(clazz.getName(), "<none>", 1));
    }

    public static <T extends Throwable> void assertThrowsExactly(Class<T> clazz, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (!clazz.equals(throwable.getClass())) {
                GameTestException gameTestException = new GameTestException(format(clazz.getName(), throwable.getClass().getName(), 1));
                gameTestException.addSuppressed(throwable);
                throw gameTestException;
            }
        }
        throw new GameTestException(format(clazz.getName(), "<none>", 1));
    }

    private static @NotNull String format(@Nullable Object expected, @Nullable Object found, int depth) {
        return "[Expected: " + expected + ", Found: " + found + "] (Line: " + StackWalker.getInstance().walk(s -> s.skip(depth + 1).findFirst().map(StackWalker.StackFrame::getLineNumber).orElse(-1)) + ")";
    }
}
