package dev.galacticraft.dynamicdimensions.impl.compat;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionProperties;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

final class SableDimensionPhysicsCompat {
    private static final String DATA_CLASS =
            "dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData";

    private static final String PHYSICS_CLASS =
            "dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysics";

    private static Boolean loaded;

    private SableDimensionPhysicsCompat() {
    }

    private static boolean isLoaded() {
        if (loaded != null) {
            return loaded;
        }

        try {
            Class.forName(DATA_CLASS);
            Class.forName(PHYSICS_CLASS);
            loaded = true;
        } catch (Throwable ignored) {
            loaded = false;
        }

        return loaded;
    }

    static void apply(ResourceKey<Level> key, DynamicDimensionProperties properties) {
        if (!isLoaded()) {
            return;
        }

        try {
            Class<?> dataClass = Class.forName(DATA_CLASS);
            Class<?> physicsClass = Class.forName(PHYSICS_CLASS);

            Field mapField = dataClass.getDeclaredField("DIMENSION_PHYSICS_DATA");
            mapField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<ResourceKey<Level>, Object> map =
                    (Map<ResourceKey<Level>, Object>) mapField.get(null);

            Constructor<?> constructor = physicsClass.getConstructor(
                    ResourceLocation.class,
                    int.class,
                    Optional.class,
                    Optional.class,
                    Optional.class,
                    Optional.class,
                    Optional.class
            );

            Object physics = constructor.newInstance(
                    key.location(),
                    properties.priority(),
                    Optional.of(properties.universalDrag()),
                    Optional.of(copy(properties.baseGravity())),
                    Optional.of(properties.basePressure()),
                    Optional.empty(),
                    Optional.of(copy(properties.magneticNorth()))
            );

            Object existing = map.get(key);
            if (existing == null || priorityOf(existing) <= properties.priority()) {
                map.put(key, physics);
            }
        } catch (Throwable throwable) {
            Constants.LOGGER.warn("Failed to apply Sable physics properties for dynamic dimension '{}'", key.location(), throwable);
        }
    }

    static void remove(ResourceKey<Level> key) {
        if (!isLoaded()) {
            return;
        }

        try {
            Class<?> dataClass = Class.forName(DATA_CLASS);

            Field mapField = dataClass.getDeclaredField("DIMENSION_PHYSICS_DATA");
            mapField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<ResourceKey<Level>, Object> map =
                    (Map<ResourceKey<Level>, Object>) mapField.get(null);

            map.remove(key);
        } catch (Throwable throwable) {
            Constants.LOGGER.warn("Failed to remove Sable physics properties for dynamic dimension '{}'", key.location(), throwable);
        }
    }

    private static int priorityOf(Object physics) {
        try {
            Method method = physics.getClass().getMethod("priority");
            return (int) method.invoke(physics);
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static Vector3f copy(Vector3f vector) {
        return new Vector3f(vector.x(), vector.y(), vector.z());
    }
}