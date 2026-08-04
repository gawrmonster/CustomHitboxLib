package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.monster.Zombie;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    private static final Logger LOGGER = LogManager.getLogger();

    @Shadow
    protected Mob mob;

    @Shadow
    protected float maxDistanceToWaypoint;

    @Shadow
    protected Path path;

    @Shadow
    protected abstract void doStuckDetection(Vec3 p_26539_);

    @Shadow
    protected abstract boolean shouldTargetNextNodeInDirection(Vec3 p_26560_);

    @Shadow
    public abstract boolean canCutCorner(BlockPathTypes p_265292_);

    @Shadow
    protected abstract Vec3 getTempMobPos();

    @Overwrite
    protected void followThePath() {
        Vec3 vec3 = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F ? this.mob.getBbWidth() / 2.0F : 0.75F - this.mob.getBbWidth() / 2.0F;
        Vec3i vec3i = this.path.getNextNodePos();
        double d0 = Math.abs(this.mob.getX() - ((double)vec3i.getX() + (this.mob.getBbWidth() + 1) / 2D));
        double d2 = Math.abs(this.mob.getZ() - ((double)vec3i.getZ() + (this.mob.getBbWidth() + 1) / 2D));
        double d1;
        if (this.mob instanceof ICustomMultipart mp) {
            double partMinY = hitboxlib$getLowestCollisionMinY(this.mob);
            double effectiveY = partMinY != Double.MAX_VALUE ? partMinY : this.mob.getY();
            d1 = Math.abs(effectiveY - (double)vec3i.getY());
        } else {
            d1 = Math.abs(this.mob.getY() - (double)vec3i.getY());
        }
        boolean flag = d0 <= (double)this.maxDistanceToWaypoint && d2 <= (double)this.maxDistanceToWaypoint && d1 < 1.0D;

        if (flag || this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(vec3)) {
            this.path.advance();
        }

        this.doStuckDetection(vec3);
    }

    @Unique
    private static double hitboxlib$getLowestCollisionMinY(Mob entity) {
        PartEntity<?>[] parts = entity.getParts();
        double lowest = Double.MAX_VALUE;
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    double minY = cp.getBoundingBox().minY;
                    if (minY < lowest) {
                        lowest = minY;
                    }
                }
            }
        }
        if (entity instanceof ICustomMultipart mp && mp.isMainHitboxCollision()) {
            double mainMinY = entity.getY();
            if (mainMinY < lowest) {
                lowest = mainMinY;
            }
        }
        return lowest;
    }
}
