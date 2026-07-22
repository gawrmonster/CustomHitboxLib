package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotationMixin {

    @Shadow
    public float yHeadRot;

    @Shadow
    public float yBodyRot;

    @Inject(method = "setYHeadRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventHeadYawCollision(float yHeadRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!hitboxlib$shouldCheck(self)) return;

        float oldYHeadRot = this.yHeadRot;
        this.yHeadRot = yHeadRot;

        if (hitboxlib$partsCollide(self)) {
            this.yHeadRot = oldYHeadRot;
            ci.cancel();
        }
    }

    @Inject(method = "setYBodyRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventBodyRenderYawCollision(float yBodyRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!hitboxlib$shouldCheck(self)) return;

        float oldYBodyRot = this.yBodyRot;
        this.yBodyRot = yBodyRot;

        if (hitboxlib$partsCollide(self)) {
            this.yBodyRot = oldYBodyRot;
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hitboxlib$snapRotationOnCollision(CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!hitboxlib$shouldCheck(self)) return;

        PartEntity<?>[] parts = ((ICustomMultipart) self).getCustomParts();
        if (parts == null) return;

        ((ICustomMultipart) self).tickCustomParts();
        if (!hitboxlib$partsCollideWithBlocks(self, parts)) return;

        float savedYHeadRot = this.yHeadRot;
        float savedYBodyRot = this.yBodyRot;

        this.yHeadRot = self.getYRot();
        hitboxlib$repositionParts(self, parts);
        if (!hitboxlib$partsCollideWithBlocks(self, parts)) return;

        this.yHeadRot = savedYHeadRot;
        this.yBodyRot = self.getYRot();
        hitboxlib$repositionParts(self, parts);
        if (!hitboxlib$partsCollideWithBlocks(self, parts)) return;

        this.yHeadRot = savedYHeadRot;
        this.yBodyRot = savedYBodyRot;
    }

    @Unique
    private static boolean hitboxlib$shouldCheck(Entity self) {
        if (!(self instanceof ICustomMultipart mp)) return false;
        if (mp.isMainHitboxCollision()) return false;
        PartEntity<?>[] parts = mp.getCustomParts();
        return parts != null && parts.length > 0;
    }

    @Unique
    private static void hitboxlib$repositionParts(Entity self, PartEntity<?>[] parts) {
        for (PartEntity<?> part : parts) {
            if (!(part instanceof CustomEntityPart cp)) continue;
            if (cp.getPositioner() == null) continue;

            Vec3 pos = cp.getPositioner().getPosition(self, 1.0F);
            cp.setPos(pos.x, pos.y, pos.z);
        }
    }

    @Unique
    private static boolean hitboxlib$partsCollideWithBlocks(Entity self, PartEntity<?>[] parts) {
        for (PartEntity<?> part : parts) {
            if (!(part instanceof CustomEntityPart cp)) continue;
            if (!cp.hasCollision()) continue;

            AABB partBox = cp.getBoundingBox();
            for (VoxelShape shape : self.level().getBlockCollisions(null, partBox)) {
                if (!shape.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
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
