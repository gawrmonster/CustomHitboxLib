package dev.customhitboxlib.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;

        EntityDimensions dims = self.getDimensions(pose);
        float halfWidth = dims.width() / 2.0F;
        AABB boundingBox = new AABB(
                self.getX() - halfWidth, self.getY(), self.getZ() - halfWidth,
                self.getX() + halfWidth, self.getY() + dims.height(), self.getZ() + halfWidth);

        if (self.level().noCollision(self, boundingBox.deflate(1.0E-7D))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "canInteractWithEntity(Lnet/minecraft/world/entity/Entity;D)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void customHitbox$canInteractWithEntity(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;

        if (entity == null || entity.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }

        Vec3 eye = self.getEyePosition();
        double d0 = self.entityInteractionRange() + distance;
        double maxDistSqr = d0 * d0;

        AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
        if (aabb.distanceToSqr(eye) < maxDistSqr) {
            cir.setReturnValue(true);
            return;
        }

        if (entity instanceof ICustomMultipart mp && mp.hasCustomParts()) {
            PartEntity<?>[] parts = mp.getCustomParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (!(part instanceof CustomEntityPart cp) || !cp.isPickable()) continue;

                    AABB partAABB = cp.getBoundingBox().inflate(cp.getPickRadius());
                    if (partAABB.distanceToSqr(eye) < maxDistSqr) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }

        cir.setReturnValue(false);
    }
}
