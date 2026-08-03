package dev.customhitboxlib.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
