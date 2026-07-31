package dev.customhitboxlib.api;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PartPositioners {
    private PartPositioners() {
    }

    public static PartPositioner atOffset(double x, double y, double z) {
        return (entity, partialTick) -> {
            return new Vec3(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
        };
    }

    public static PartPositioner rotating(double offsetX, double offsetY, double offsetZ) {
        return (entity, partialTick) -> {
            float bodyYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
            float yawRad = (float) Math.toRadians(bodyYaw);
            double sin = Mth.sin(yawRad);
            double cos = Mth.cos(yawRad);
            return new Vec3(
                    entity.getX() + offsetX * cos - offsetZ * sin,
                    entity.getY() + offsetY,
                    entity.getZ() + offsetX * sin + offsetZ * cos);
        };
    }

    public static PartPositioner relativeTo(Entity anchor, double x, double y, double z) {
        return (entity, partialTick) -> anchor.position().add(x, y, z);
    }

    public static PartPositioner following(double distanceBehind, double heightOffset) {
        return (entity, partialTick) -> {
            Vec3 look = entity.getLookAngle();
            Vec3 behind = entity.position().subtract(look.multiply(distanceBehind, 0, distanceBehind));
            return new Vec3(behind.x, entity.getY() + heightOffset, behind.z);
        };
    }

    public static PartPositioner lerpTo(Entity target, double x, double y, double z, double speed) {
        return (entity, partialTick) -> {
            Vec3 targetPos = target.position().add(x, y, z);
            Vec3 current = entity.position();
            return current.lerp(targetPos, (float) speed);
        };
    }
}
