package dev.customhitboxlib.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraftforge.entity.PartEntity;

public final class MultipartHelper {
    private MultipartHelper() {}

    public static void addPart(Entity entity, String name, EntityDimensions dimensions, PartPositioner positioner) {
        addPart(entity, name, dimensions, positioner, true, false, false);
    }

    public static void addPart(Entity entity, String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable) {
        addPart(entity, name, dimensions, positioner, pickable, false, false);
    }

    public static void addPart(Entity entity, String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable, boolean pushable) {
        addPart(entity, name, dimensions, positioner, pickable, pushable, false, false);
    }

    public static void addPart(Entity entity, String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision) {
        addPart(entity, name, dimensions, positioner, pickable, pushable, collision, false);
    }

    public static void addPart(Entity entity, String name, EntityDimensions dimensions, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision, boolean suffocate) {
        if (entity instanceof ICustomMultipart mp) {
            mp.addCustomPart(name, new PartDefinition(name, dimensions, positioner, pickable, pushable, collision, suffocate));
        }
    }

    public static void addPart(Entity entity, String name, float width, float height, PartPositioner positioner) {
        addPart(entity, name, width, height, positioner, true, false, false);
    }

    public static void addPart(Entity entity, String name, float width, float height, PartPositioner positioner, boolean pickable) {
        addPart(entity, name, width, height, positioner, pickable, false, false);
    }

    public static void addPart(Entity entity, String name, float width, float height, PartPositioner positioner, boolean pickable, boolean pushable) {
        addPart(entity, name, EntityDimensions.scalable(width, height), positioner, pickable, pushable, false, false);
    }

    public static void addPart(Entity entity, String name, float width, float height, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision) {
        addPart(entity, name, EntityDimensions.scalable(width, height), positioner, pickable, pushable, collision, false);
    }

    public static void addPart(Entity entity, String name, float width, float height, PartPositioner positioner, boolean pickable, boolean pushable, boolean collision, boolean suffocate) {
        addPart(entity, name, EntityDimensions.scalable(width, height), positioner, pickable, pushable, collision, suffocate);
    }

    public static void removePart(Entity entity, String name) {
        if (entity instanceof ICustomMultipart mp) {
            mp.removeCustomPart(name);
        }
    }

    public static boolean hasParts(Entity entity) {
        if (entity instanceof ICustomMultipart mp) {
            return mp.hasCustomParts();
        }
        return false;
    }

    public static void setMainHitboxPickable(Entity entity, boolean pickable) {
        if (entity instanceof ICustomMultipart mp) {
            mp.setMainHitboxPickable(pickable);
        }
    }

    public static boolean isMainHitboxPickable(Entity entity) {
        if (entity instanceof ICustomMultipart mp) {
            return mp.isMainHitboxPickable();
        }
        return true;
    }

    public static void setMainHitboxPushable(Entity entity, boolean pushable) {
        if (entity instanceof ICustomMultipart mp) {
            mp.setMainHitboxPushable(pushable);
        }
    }

    public static boolean isMainHitboxPushable(Entity entity) {
        if (entity instanceof ICustomMultipart mp) {
            return mp.isMainHitboxPushable();
        }
        return true;
    }

    public static void setMainHitboxCollision(Entity entity, boolean collision) {
        if (entity instanceof ICustomMultipart mp) {
            mp.setMainHitboxCollision(collision);
        }
    }

    public static boolean isMainHitboxCollision(Entity entity) {
        if (entity instanceof ICustomMultipart mp) {
            return mp.isMainHitboxCollision();
        }
        return true;
    }
}
