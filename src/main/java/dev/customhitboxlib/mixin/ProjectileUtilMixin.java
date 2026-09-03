package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(net.minecraft.world.entity.projectile.ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void hitboxlib$getEntityHitResult(Entity shooter, Vec3 start, Vec3 end, AABB area, Predicate<Entity> predicate, double maxDistance, CallbackInfoReturnable<EntityHitResult> cir) {
        Level level = shooter.level();
        double closestDistSq = Double.MAX_VALUE;
        EntityHitResult closestResult = null;

        for (Entity entity : level.getEntities(shooter, area, predicate)) {
            boolean isMultipart = entity instanceof ICustomMultipart mp && mp.hasCustomParts();

            AABB mainAabb = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> mainIntercept = mainAabb.clip(start, end);
            if (mainIntercept.isPresent()) {
                double distSq = start.distanceToSqr(mainIntercept.get());
                if (distSq < closestDistSq) {
                    boolean skipMain = isMultipart && !((ICustomMultipart) entity).isMainHitboxPickable();
                    if (!skipMain) {
                        closestDistSq = distSq;
                        closestResult = new EntityHitResult(entity, mainIntercept.get());
                    }
                }
            } else if (mainAabb.contains(start)) {
                boolean skipMain = isMultipart && !((ICustomMultipart) entity).isMainHitboxPickable();
                if (!skipMain && 0.0D < closestDistSq) {
                    closestDistSq = 0.0D;
                    closestResult = new EntityHitResult(entity, start);
                }
            }

            if (!isMultipart) continue;
            PartEntity<?>[] parts = ((ICustomMultipart)entity).getCustomParts();
            if (parts == null) continue;

            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.isPickable()) continue;

                AABB partAABB = cp.getBoundingBox();
                Optional<Vec3> partIntercept = partAABB.clip(start, end);
                if (partIntercept.isPresent()) {
                    double distSq = start.distanceToSqr(partIntercept.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestResult = new EntityHitResult(entity, partIntercept.get());
                    }
                } else if (partAABB.contains(start)) {
                    if (0.0D < closestDistSq) {
                        closestDistSq = 0.0D;
                        closestResult = new EntityHitResult(entity, start);
                    }
                }
            }
        }

        cir.setReturnValue(closestResult);
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void hitboxlib$getEntityHitResult(Level level, Entity shooter, Vec3 start, Vec3 end, AABB area, Predicate<Entity> predicate, float inflationAmount, CallbackInfoReturnable<EntityHitResult> cir) {
        double closestDistSq = Double.MAX_VALUE;
        EntityHitResult closestResult = null;

        for (Entity entity : level.getEntities(shooter, area, predicate)) {
            boolean isMultipart = entity instanceof ICustomMultipart mp && mp.hasCustomParts();

            AABB mainAabb = entity.getBoundingBox().inflate(inflationAmount);
            Optional<Vec3> mainIntercept = mainAabb.clip(start, end);
            if (mainIntercept.isPresent()) {
                double distSq = start.distanceToSqr(mainIntercept.get());
                if (distSq < closestDistSq) {
                    boolean skipMain = isMultipart && !((ICustomMultipart) entity).isMainHitboxPickable();
                    if (!skipMain) {
                        closestDistSq = distSq;
                        closestResult = new EntityHitResult(entity, mainIntercept.get());
                    }
                }
            }

            if (!isMultipart) continue;
            PartEntity<?>[] parts = ((ICustomMultipart)entity).getCustomParts();
            if (parts == null) continue;

            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.isPickable()) continue;

                AABB partAABB = cp.getBoundingBox();
                Optional<Vec3> partIntercept = partAABB.clip(start, end);
                if (partIntercept.isPresent()) {
                    double distSq = start.distanceToSqr(partIntercept.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestResult = new EntityHitResult(entity, partIntercept.get());
                    }
                } else if (partAABB.contains(start)) {
                    if (0.0D < closestDistSq) {
                        closestDistSq = 0.0D;
                        closestResult = new EntityHitResult(entity, start);
                    }
                }
            }
        }

        cir.setReturnValue(closestResult);
    }
}
