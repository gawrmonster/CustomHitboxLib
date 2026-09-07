package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.customhitboxlib.CustomHitboxLib;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotationMixin {

    @Shadow
    public float yHeadRot;

    @Shadow
    public float yHeadRotO;

    @Shadow
    public float yBodyRot;

    @Shadow
    protected abstract float getYHeadRot();

    @Inject(method = "setYHeadRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventHeadYawCollision(float yHeadRot, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!hitboxlib$shouldCheck(self))
            return;

        if(self.noPhysics) return;
        boolean wasColliding = hitboxlib$partsCollide(self);

        float oldYHeadRot = this.yHeadRot;
        this.yHeadRot = yHeadRot;

        boolean nowColliding = hitboxlib$partsCollide(self);

        if (!wasColliding && nowColliding) {
            this.yHeadRot = oldYHeadRot;
            ci.cancel();
            return;
        }

        if (self instanceof Player living) {
            float diff = Mth.wrapDegrees(yHeadRot - living.yBodyRot);
            if (Math.abs(diff) > 90.0F) {
                float oldBodyRot = living.yBodyRot;
                float targetBodyRot = yHeadRot - Math.copySign(90.0F, diff);

                living.yBodyRot = targetBodyRot;
                boolean bodyBlocked = hitboxlib$partsCollide(self);
                living.yBodyRot = oldBodyRot;

                if (bodyBlocked) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "setYBodyRot", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$preventBodyRenderYawCollision(float yBodyRot, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!hitboxlib$shouldCheck(self))
            return;

        if(self.noPhysics) return;
        boolean wasColliding = hitboxlib$partsCollide(self);

        float oldYBodyRot = this.yBodyRot;
        this.yBodyRot = yBodyRot;

        boolean nowColliding = hitboxlib$partsCollide(self);

        if (!wasColliding && nowColliding) {
            this.yBodyRot = oldYBodyRot;
            ci.cancel();
        }
    }


    @Inject(method = "tickHeadTurn", at = @At("TAIL"))
    private void hitboxlib$preventTickHeadTurnCollision(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!hitboxlib$shouldCheck(self))
            return;
        if(self.noPhysics) return;

        if (!hitboxlib$partsCollide(self))
            return;

        this.yBodyRot = hitboxlib$prevYBodyRot;
        //this.yHeadRot = this.yHeadRotO;
    }

    @Inject(method = "tickHeadTurn", at = @At("HEAD"))
    private void hitboxlib$capturePreRotation(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!hitboxlib$shouldCheck(self))
            return;

        hitboxlib$prevYBodyRot = this.yBodyRot;
    }


    @Unique
    private float hitboxlib$prevYBodyRot;

    @Unique
    private static boolean hitboxlib$shouldCheck(Entity self) {
        if (!(self instanceof ICustomMultipart mp))
            return false;
        PartEntity<?>[] parts = mp.getCustomParts();
        return parts != null && parts.length > 0;
    }

    @Unique
    private static boolean hitboxlib$partsCollide(Entity self) {
        PartEntity<?>[] parts = ((ICustomMultipart) self).getCustomParts();
        if (parts == null)
            return false;

        for (PartEntity<?> part : parts) {
            if (!(part instanceof CustomEntityPart cp))
                continue;
            if (!cp.hasCollision())
                continue;
            if (cp.getPositioner() == null)
                continue;

            Vec3 newPos = cp.getPositioner().getPosition(self, 1.0F);
            float halfWidth = cp.getBbWidth() / 2.0F;
            float height = cp.getBbHeight();
            AABB partBox = new AABB(
                    newPos.x - halfWidth, newPos.y, newPos.z - halfWidth,
                    newPos.x + halfWidth, newPos.y + height, newPos.z + halfWidth);

            for (VoxelShape shape : self.level().getBlockCollisions(null, partBox)) {
                if (!shape.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
