package dev.customhitboxlib.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface PartPositioner {
    Vec3 getPosition(Entity entity, float partialTick);
}
