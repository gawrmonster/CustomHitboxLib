package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobRotationMixin {

    @Inject(method = "tickHeadTurn", at = @At("HEAD"))
    private void hitboxlib$capturePreRotation(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (!hitboxlib$shouldCheck(self))
            return;

        hitboxlib$prevYBodyRot = ((LivingEntity) (Object) this).yBodyRot;
    }

    @Inject(method = "tickHeadTurn", at = @At("TAIL"))
    private void hitboxlib$preventTickHeadTurnCollision(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (!hitboxlib$shouldCheck(self))
            return;

        LivingEntity living = (LivingEntity) (Object) this;

        if (!hitboxlib$partsCollide(self)) {
            hitboxlib$prevYBodyRot = living.yBodyRot;
            return;
        }

        living.yBodyRot = hitboxlib$prevYBodyRot;
        hitboxlib$prevYBodyRot = living.yBodyRot;
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
