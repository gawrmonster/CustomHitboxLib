package dev.customhitboxlib.util;

import dev.customhitboxlib.api.CustomEntityPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class CustomPartTracker {
    private static final Set<CustomEntityPart> ACTIVE_PARTS = Collections.newSetFromMap(new WeakHashMap<>());

    public static synchronized void registerPart(CustomEntityPart part) {
        if (part != null) {
            ACTIVE_PARTS.add(part);
        }
    }

    public static synchronized void unregisterPart(CustomEntityPart part) {
        if (part != null) {
            ACTIVE_PARTS.remove(part);
        }
    }

    public static synchronized List<CustomEntityPart> getIntersectingParts(Level level, AABB area) {
        List<CustomEntityPart> matching = new ArrayList<>();
        Iterator<CustomEntityPart> iterator = ACTIVE_PARTS.iterator();

        while (iterator.hasNext()) {
            CustomEntityPart part = iterator.next();
            Entity parent = part.getParent();

            if (parent == null || parent.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (parent.level() == level && part.isPickable() && part.getBoundingBox().intersects(area)) {
                matching.add(part);
            }
        }
        return matching;
    }
}