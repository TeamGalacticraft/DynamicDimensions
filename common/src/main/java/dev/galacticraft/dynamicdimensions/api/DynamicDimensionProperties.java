package dev.galacticraft.dynamicdimensions.api;

import org.joml.Vector3f;

/**
 * Generic physics/environment properties for a dynamic dimension.
 *
 * DynamicDimensions does not decide what these values mean.
 * Optional compat layers, such as Sable compat, may consume them.
 */
public abstract class DynamicDimensionProperties {
    public abstract int priority();

    public abstract Vector3f baseGravity();

    public abstract double basePressure();

    public abstract float universalDrag();

    public abstract Vector3f magneticNorth();
}