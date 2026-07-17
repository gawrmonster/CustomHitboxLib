package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void hitboxlib$suppressPartJumping(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        if (self.level() != null && self instanceof ICustomMultipart mp && !mp.isMainHitboxCollision()) {
            // Force the jumping state to false if our custom parts aren't hitting a
            // physical wall.
            // This prevents vanilla AI tick inputs from overriding the physics engine.
            if (!self.horizontalCollision) {
                self.setJumping(false);
            }

            // Override the MoveControl state right before path navigation runs
            MoveControl moveControl = self.getMoveControl();
            if (moveControl != null && moveControl.hasWanted() && !self.horizontalCollision) {
                // If the AI is trying to move forward but forces a jump due to being "stuck" in
                // the floor,
                // we clear the jumping trigger from the path navigation module.
                self.setJumping(false);
            }
        }
    }
}