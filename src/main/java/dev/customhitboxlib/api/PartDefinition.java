package dev.customhitboxlib.api;

import net.minecraft.world.entity.EntityDimensions;

public record PartDefinition(
    String name,
    EntityDimensions dimensions,
    PartPositioner positioner,
    boolean pickable,
    boolean pushable,
    boolean collision,
    boolean suffocate
) {
    public static PartDefinition of(String name, float width, float height, PartPositioner positioner) {
        return new PartDefinition(name, EntityDimensions.scalable(width, height), positioner, true, false, false, false);
    }

    public static PartDefinition of(String name, EntityDimensions dimensions, PartPositioner positioner) {
        return new PartDefinition(name, dimensions, positioner, true, false, false, false);
    }

    public static PartDefinition of(String name, float width, float height, PartPositioner positioner, boolean pickable) {
        return new PartDefinition(name, EntityDimensions.scalable(width, height), positioner, pickable, false, false, false);
    }

    public static PartDefinition of(String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable) {
        return new PartDefinition(name, dimensions, positioner, pickable, false, false, false);
    }

    public static PartDefinition of(String name, float width, float height, PartPositioner positioner, boolean pickable, boolean pushable) {
        return new PartDefinition(name, EntityDimensions.scalable(width, height), positioner, pickable, pushable, false, false);
    }

    public static PartDefinition of(String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable, boolean pushable) {
        return new PartDefinition(name, dimensions, positioner, pickable, pushable, false, false);
    }

    public static PartDefinition of(String name, float width, float height, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision) {
        return new PartDefinition(name, EntityDimensions.scalable(width, height), positioner, pickable, pushable, collision, false);
    }

    public static PartDefinition of(String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision) {
        return new PartDefinition(name, dimensions, positioner, pickable, pushable, collision, false);
    }

    public static PartDefinition of(String name, float width, float height, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision, boolean suffocate) {
        return new PartDefinition(name, EntityDimensions.scalable(width, height), positioner, pickable, pushable, collision, suffocate);
    }

    public static PartDefinition of(String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision, boolean suffocate) {
        return new PartDefinition(name, dimensions, positioner, pickable, pushable, collision, suffocate);
    }
}
