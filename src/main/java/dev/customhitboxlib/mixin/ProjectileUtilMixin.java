package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import dev.customhitboxlib.CustomHitboxLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(net.minecraft.world.entity.projectile.ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void hitboxlib$getEntityHitResult(Entity shooter, Vec3 start, Vec3 end, AABB area, Predicate<Entity> predicate, double maxDistance, CallbackInfoReturnable<EntityHitResult> cir) {
        Level level = shooter.level();
        EntityHitResult originalResult = cir.getReturnValue();

        CustomHitboxLib.LOGGER.info("=== getEntityHitResult called ===");
        CustomHitboxLib.LOGGER.info("Shooter: {}", shooter);
        CustomHitboxLib.LOGGER.info("Start: {}, End: {}", start, end);
        CustomHitboxLib.LOGGER.info("Area: {}", area);
        CustomHitboxLib.LOGGER.info("Original result: {}", originalResult);

        List<Entity> entities = level.getEntities(shooter, area, predicate);
        CustomHitboxLib.LOGGER.info("Entities from getEntities: {}", entities.size());
        for (Entity e : entities) {
            CustomHitboxLib.LOGGER.info("  Entity: {} class={} isPickable={} isMultipart={} hasCustomParts={}",
                e, e.getClass().getSimpleName(), e.isPickable(),
                e instanceof ICustomMultipart, e instanceof ICustomMultipart mp ? mp.hasCustomParts() : false);
        }

        double closestDistSq = Double.MAX_VALUE;
        EntityHitResult closestResult = null;
        boolean closestIsMainHitbox = false;

        if (originalResult != null) {
            closestDistSq = start.distanceToSqr(originalResult.getLocation());
            closestResult = originalResult;
            closestIsMainHitbox = true;
        }

        for (Entity entity : level.getEntities(shooter, area, predicate)) {
            if (!(entity instanceof ICustomMultipart mp) || !mp.hasCustomParts()) {
                CustomHitboxLib.LOGGER.info("  Skipping {} - not ICustomMultipart or no custom parts", entity);
                continue;
            }

            PartEntity<?>[] parts = entity.getParts();
            if (parts == null) {
                CustomHitboxLib.LOGGER.info("  Entity {} has null parts", entity);
                continue;
            }

            CustomHitboxLib.LOGGER.info("  Checking {} parts for entity {}", parts.length, entity);
            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp)) {
                    CustomHitboxLib.LOGGER.info("    Part {} is not CustomEntityPart", part);
                    continue;
                }
                CustomHitboxLib.LOGGER.info("    CustomEntityPart: pickable={} bbox={}", cp.isPickable(), cp.getBoundingBox());

                AABB partAABB = cp.getBoundingBox();
                Optional<Vec3> intercept = partAABB.clip(start, end);
                if (intercept.isPresent()) {
                    double distSq = start.distanceToSqr(intercept.get());
                    CustomHitboxLib.LOGGER.info("    CLIP HIT at {} distSq={} (closest was {})", intercept.get(), distSq, closestDistSq);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestResult = new EntityHitResult(entity, intercept.get());
                        closestIsMainHitbox = false;
                    }
                } else if (partAABB.contains(start)) {
                    CustomHitboxLib.LOGGER.info("    CONTAINS HIT - eye inside part bbox");
                    if (0.0D < closestDistSq) {
                        closestDistSq = 0.0D;
                        closestResult = new EntityHitResult(entity, start);
                        closestIsMainHitbox = false;
                    }
                } else {
                    CustomHitboxLib.LOGGER.info("    No hit on this part");
                }
            }
        }

        CustomHitboxLib.LOGGER.info("Final: closestResult={} closestIsMainHitbox={}", closestResult, closestIsMainHitbox);

        if (closestResult == null) {
            CustomHitboxLib.LOGGER.info("No result, returning");
            return;
        }

        if (!closestIsMainHitbox) {
            CustomHitboxLib.LOGGER.info("Custom part hit, setting return to {}", closestResult);
            cir.setReturnValue(closestResult);
        } else if (closestResult.getEntity() instanceof ICustomMultipart mp && !mp.isMainHitboxPickable()) {
            CustomHitboxLib.LOGGER.info("Main hitbox only + mainHitboxPickable=false, setting return to null");
            cir.setReturnValue(null);
        } else {
            CustomHitboxLib.LOGGER.info("Keeping original result");
        }
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void hitboxlib$getEntityHitResult(Level level, Entity shooter, Vec3 start, Vec3 end, AABB area, Predicate<Entity> predicate, float inflationAmount, CallbackInfoReturnable<EntityHitResult> cir) {
        EntityHitResult originalResult = cir.getReturnValue();

        double closestDistSq = Double.MAX_VALUE;
        EntityHitResult closestResult = null;
        boolean closestIsMainHitbox = false;

        if (originalResult != null) {
            closestResult = originalResult;
            closestIsMainHitbox = true;
            closestDistSq = Double.MAX_VALUE;
        }

        for (Entity entity : level.getEntities(shooter, area, predicate)) {
            if (!(entity instanceof ICustomMultipart mp) || !mp.hasCustomParts()) continue;

            PartEntity<?>[] parts = entity.getParts();
            if (parts == null) continue;

            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.isPickable()) continue;

                AABB partAABB = cp.getBoundingBox();
                Optional<Vec3> intercept = partAABB.clip(start, end);
                if (intercept.isPresent()) {
                    double distSq = start.distanceToSqr(intercept.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestResult = new EntityHitResult(entity, intercept.get());
                        closestIsMainHitbox = false;
                    }
                } else if (partAABB.contains(start)) {
                    if (0.0D < closestDistSq) {
                        closestDistSq = 0.0D;
                        closestResult = new EntityHitResult(entity, start);
                        closestIsMainHitbox = false;
                    }
                }
            }
        }

        if (closestResult == null) {
            return;
        }

        if (!closestIsMainHitbox) {
            cir.setReturnValue(closestResult);
        } else if (closestResult.getEntity() instanceof ICustomMultipart mp && !mp.isMainHitboxPickable()) {
            cir.setReturnValue(null);
        }
    }
}
