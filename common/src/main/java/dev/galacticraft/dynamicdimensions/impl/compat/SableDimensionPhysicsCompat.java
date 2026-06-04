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
import dev.galacticraft.dynamicdimensions.impl.Constants;
import dev.galacticraft.dynamicdimensions.impl.platform.Services;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysics;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SableDimensionPhysicsCompat {
    static final String SABLE_MOD_ID = "sable";

    /**
     * Properties staged before SubLevelPhysicsSystem.initialize() runs for a newly created dynamic dimension.
     * Flushed into DIMENSION_PHYSICS_DATA by the mixin at the HEAD of initialize(), before gravity is ever read.
     */
    private static final Map<ResourceKey<Level>, DynamicDimensionProperties> PENDING_PROPERTIES = new HashMap<>();

    private SableDimensionPhysicsCompat() {
    }

    /**
     * Call this synchronously at the very start of createDynamicDimension, before any deferred work is queued.
     * Ensures properties are present in PENDING_PROPERTIES before SubLevelPhysicsSystem.initialize() fires.
     */
    static void stage(final ResourceKey<Level> key, final DynamicDimensionProperties properties) {
        if (!Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            return;
        }
        PENDING_PROPERTIES.put(key, properties);
    }

    /**
     * Called from the mixin on SubLevelPhysicsSystem.initialize() at HEAD.
     * Drains the pending entry for this dimension into DIMENSION_PHYSICS_DATA before gravity is read.
     */
    public static void flushForLevel(final ResourceKey<Level> key) {
        final DynamicDimensionProperties pending = PENDING_PROPERTIES.remove(key);
        if (pending == null || !Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            return;
        }

        try {
            final Map<ResourceKey<Level>, DimensionPhysics> map = physicsMap();
            final DimensionPhysics physics = buildPhysics(key, pending);
            final DimensionPhysics existing = map.get(key);

            if (existing == null || existing.priority() <= pending.priority()) {
                map.put(key, physics);
                clearDefault(key);
            }
        } catch (final Throwable throwable) {
            Constants.LOGGER.warn(
                    "Failed to flush staged Sable physics for dynamic dimension '{}'",
                    key.location(),
                    throwable
            );
        }
    }

    /**
     * Directly writes properties into DIMENSION_PHYSICS_DATA.
     * Safe to call after the level exists, but on its own is insufficient — Rapier reads gravity only once
     * at initialize() time. Use stage() + the mixin flush to cover that window instead.
     */
    static void apply(final ResourceKey<Level> key, final DynamicDimensionProperties properties) {
        if (!Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            return;
        }

        try {
            final Map<ResourceKey<Level>, DimensionPhysics> map = physicsMap();
            final DimensionPhysics physics = buildPhysics(key, properties);
            final DimensionPhysics existing = map.get(key);

            if (existing == null || existing.priority() <= properties.priority()) {
                map.put(key, physics);
                clearDefault(key);
            }
        } catch (final Throwable throwable) {
            Constants.LOGGER.warn(
                    "Failed to apply Sable physics properties for dynamic dimension '{}'",
                    key.location(),
                    throwable
            );
        }
    }

    static void remove(final ResourceKey<Level> key) {
        if (!Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            return;
        }

        // Clear any staged-but-not-yet-flushed entry so it doesn't leak into a future dimension with the same key.
        PENDING_PROPERTIES.remove(key);

        try {
            physicsMap().remove(key);
        } catch (final Throwable throwable) {
            Constants.LOGGER.warn(
                    "Failed to remove Sable physics properties for dynamic dimension '{}'",
                    key.location(),
                    throwable
            );
        }
    }

    private static DimensionPhysics buildPhysics(final ResourceKey<Level> key, final DynamicDimensionProperties properties) {
        return new DimensionPhysics(
                key.location(),
                properties.priority(),
                Optional.of(properties.universalDrag()),
                Optional.of(copy(properties.baseGravity())),
                Optional.of(properties.basePressure()),
                Optional.empty(),
                Optional.of(copy(properties.magneticNorth()))
        );
    }

    private static void clearDefault(final ResourceKey<Level> key) throws ReflectiveOperationException {
        final Field field = DimensionPhysicsData.class.getDeclaredField("DEFAULT_DIMENSION_PHYSICS_DATA");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        final Map<ResourceKey<Level>, DimensionPhysics> defaults =
                (Map<ResourceKey<Level>, DimensionPhysics>) field.get(null);

        defaults.remove(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceKey<Level>, DimensionPhysics> physicsMap() throws ReflectiveOperationException {
        final Field field = DimensionPhysicsData.class.getDeclaredField("DIMENSION_PHYSICS_DATA");
        field.setAccessible(true);
        return (Map<ResourceKey<Level>, DimensionPhysics>) field.get(null);
    }

    private static Vector3f copy(final Vector3f vector) {
        return new Vector3f(vector.x(), vector.y(), vector.z());
    }
}