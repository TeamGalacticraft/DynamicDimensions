/*
 * Copyright (c) 2021-2026 Team Galacticraft
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

package dev.galacticraft.dynamicdimensions.impl.compat;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionProperties;
import dev.galacticraft.dynamicdimensions.impl.platform.Services;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import static dev.galacticraft.dynamicdimensions.impl.compat.SableDimensionPhysicsCompat.SABLE_MOD_ID;

public final class DynamicDimensionPhysicsCompat {
    private DynamicDimensionPhysicsCompat() {
    }

    public static void apply(ResourceKey<Level> key, DynamicDimensionProperties properties) {
        if (Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            SableDimensionPhysicsCompat.apply(key, properties);
        }
    }

    public static void remove(ResourceKey<Level> key) {
        if (Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            SableDimensionPhysicsCompat.remove(key);
        }
    }

    public static void stage(final ResourceKey<Level> key, final DynamicDimensionProperties properties) {
        if (Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            SableDimensionPhysicsCompat.stage(key, properties);
        }
    }
}