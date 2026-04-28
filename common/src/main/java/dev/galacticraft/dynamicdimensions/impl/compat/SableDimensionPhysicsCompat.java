/*
 * Copyright (c) 2021-2025 Team Galacticraft
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
import java.util.Map;
import java.util.Optional;

final class SableDimensionPhysicsCompat {
    static final String SABLE_MOD_ID = "sable";

    private SableDimensionPhysicsCompat() {
    }

    static void apply(ResourceKey<Level> key, DynamicDimensionProperties properties) {
        if (!Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            return;
        }

        try {
            Map<ResourceKey<Level>, DimensionPhysics> map = physicsMap();

            DimensionPhysics physics = new DimensionPhysics(
                    key.location(),
                    properties.priority(),
                    Optional.of(properties.universalDrag()),
                    Optional.of(copy(properties.baseGravity())),
                    Optional.of(properties.basePressure()),
                    Optional.empty(),
                    Optional.of(copy(properties.magneticNorth()))
            );

            DimensionPhysics existing = map.get(key);

            if (existing == null || existing.priority() <= properties.priority()) {
                map.put(key, physics);
            }
        } catch (Throwable throwable) {
            Constants.LOGGER.warn(
                    "Failed to apply Sable physics properties for dynamic dimension '{}'",
                    key.location(),
                    throwable
            );
        }
    }

    static void remove(ResourceKey<Level> key) {
        if (!Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            return;
        }

        try {
            physicsMap().remove(key);
        } catch (Throwable throwable) {
            Constants.LOGGER.warn(
                    "Failed to remove Sable physics properties for dynamic dimension '{}'",
                    key.location(),
                    throwable
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceKey<Level>, DimensionPhysics> physicsMap() throws ReflectiveOperationException {
        Field field = DimensionPhysicsData.class.getDeclaredField("DIMENSION_PHYSICS_DATA");
        field.setAccessible(true);
        return (Map<ResourceKey<Level>, DimensionPhysics>) field.get(null);
    }

    private static Vector3f copy(Vector3f vector) {
        return new Vector3f(vector.x(), vector.y(), vector.z());
    }
}