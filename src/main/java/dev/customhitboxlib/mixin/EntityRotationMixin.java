package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

@Mixin(Entity.class)
public abstract class EntityRotationMixin {

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Inject(method = "setYRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventBodyYawCollision(float yRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!hitboxlib$shouldCheck(self)) return;

        float oldYRot = this.yRot;
        this.yRot = yRot;

        if (hitboxlib$partsCollide(self)) {
            this.yRot = oldYRot;
            ci.cancel();
        }
    }

    @Inject(method = "setXRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventHeadPitchCollision(float xRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!hitboxlib$shouldCheck(self)) return;

        float oldXRot = this.xRot;
        this.xRot = xRot;

        if (hitboxlib$partsCollide(self)) {
            this.xRot = oldXRot;
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
