package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgePlayer;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerMixin implements IForgePlayer {

    @Override
    public boolean isCloseEnough(Entity entity, double dist) {
        Player self = (Player) (Object) this;
        Vec3 eye = self.getEyePosition();

        AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
        if (aabb.distanceToSqr(eye) < dist * dist) {
            return true;
        }

        if (entity instanceof ICustomMultipart mp && mp.hasCustomParts()) {
            PartEntity<?>[] parts = mp.getCustomParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (!(part instanceof CustomEntityPart cp) || !cp.isPickable()) continue;

                    AABB partAABB = cp.getBoundingBox().inflate(cp.getPickRadius());
                    if (partAABB.distanceToSqr(eye) < dist * dist) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}