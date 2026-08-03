package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

@Mixin(Entity.class)
public abstract class EntityRotationMixin {

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Inject(method = "setYRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventBodyYawCollision(float newYRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (self == null || !hitboxlib$shouldCheck(self)) return;

        if (!(self instanceof LivingEntity living)) return;

        float oldYRot = living.getYRot();
        if (oldYRot == newYRot) return;

        // Check collision at old state
        boolean oldCollides = hitboxlib$partsCollide(self);

        // Check collision at new state
        this.yRot = newYRot;
        boolean newCollides = hitboxlib$partsCollide(self);

        // Rollback
        this.yRot = oldYRot;

        if (!oldCollides && newCollides) {
            ci.cancel();
            return;
        }

        float headBodyDiff = Mth.wrapDegrees(newYRot - living.yBodyRot);

        if (Math.abs(headBodyDiff) > 75.0F) {
            ci.cancel();
        }
    }

    @Inject(method = "setXRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventHeadPitchCollision(float newXRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!hitboxlib$shouldCheck(self)) return;

        float oldXRot = this.xRot;
        if (oldXRot == newXRot) return;

        // Check collision at old state
        boolean oldCollides = hitboxlib$partsCollide(self);

        // Check collision at new state
        this.xRot = newXRot;
        boolean newCollides = hitboxlib$partsCollide(self);

        // Rollback
        this.xRot = oldXRot;

        if (!oldCollides && newCollides) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean hitboxlib$shouldCheck(Entity self) {
        if (!(self instanceof ICustomMultipart mp)) return false;
        if (mp.isMainHitboxCollision()) return false;
        PartEntity<?>[] parts = mp.getCustomParts();
        return parts != null && parts.length > 0;
    }

    @Unique
    private static boolean hitboxlib$partsCollide(Entity self) {
        PartEntity<?>[] parts = ((ICustomMultipart) self).getCustomParts();
        if (parts == null) return false;

        for (PartEntity<?> part : parts) {
            if (!(part instanceof CustomEntityPart cp)) continue;
            if (!cp.hasCollision()) continue;
            if (cp.getPositioner() == null) continue;

            Vec3 newPos = cp.getPositioner().getPosition(self, 1.0F);
            float halfWidth = cp.getBbWidth() / 2.0F;
            float height = cp.getBbHeight();
            AABB partBox = new AABB(
                newPos.x - halfWidth, newPos.y, newPos.z - halfWidth,
                newPos.x + halfWidth, newPos.y + height, newPos.z + halfWidth
            );

            for (VoxelShape shape : self.level().getBlockCollisions(null, partBox)) {
                if (!shape.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
