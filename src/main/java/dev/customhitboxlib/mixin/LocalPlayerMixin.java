package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Shadow
    protected abstract boolean suffocatesAt(BlockPos pos);

    @Redirect(
        method = "moveTowardsClosestSpace",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;suffocatesAt(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean hitboxlib$skipPushWhenPartInsideBlock(LocalPlayer self, BlockPos pos) {
        if (!(self instanceof ICustomMultipart mp)) {
            return this.suffocatesAt(pos);
        }

        if (self.noPhysics) {
            return false;
        }

        PartEntity<?>[] parts = self.getParts();
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.hasCollision()) continue;

                AABB blockAabb = new AABB(
                    (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(),
                    (double) pos.getX() + 1.0D, (double) pos.getY() + 1.0D, (double) pos.getZ() + 1.0D
                ).deflate(1.0E-7D);

                if (self.level().collidesWithSuffocatingBlock(cp, blockAabb)) {
                    return false;
                }
            }
        }
        if(!mp.isMainHitboxCollision()) {
            return false;
        }

        return this.suffocatesAt(pos);
    }
}