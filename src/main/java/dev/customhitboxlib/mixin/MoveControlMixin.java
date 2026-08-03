package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MoveControl.class)
public abstract class MoveControlMixin {

    @Shadow
    protected Mob mob;

    @Shadow
    protected double wantedX;

    @Shadow
    protected double wantedY;

    @Shadow
    protected double wantedZ;

    @Shadow
    protected double speedModifier;

    @Shadow
    protected abstract float rotlerp(float p_23438_, float p_23439_, float p_23440_);

    @Shadow
    protected abstract boolean isWalkable(float p_23441_, float p_23442_);

    @Shadow
    private float strafeForwards;

    @Shadow
    private float strafeRight;

    @Inject(
        method = "tick()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hitboxlib$tick(CallbackInfo ci) {
        if (!(this.mob instanceof ICustomMultipart mp) || mp.isMainHitboxCollision()) {
            return;
        }

        MoveControl self = (MoveControl)(Object)this;
        int op = hitboxlib$getOperation(self);

        if (op == STRAFE) {
            float f = (float)this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float f1 = (float)this.speedModifier * f;
            float f2 = this.strafeForwards;
            float f3 = this.strafeRight;
            float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
            if (f4 < 1.0F) {
                f4 = 1.0F;
            }

            f4 = f1 / f4;
            f2 *= f4;
            f3 *= f4;
            float f5 = Mth.sin(this.mob.getYRot() * ((float)Math.PI / 180F));
            float f6 = Mth.cos(this.mob.getYRot() * ((float)Math.PI / 180F));
            float f7 = f2 * f6 - f3 * f5;
            float f8 = f3 * f6 + f2 * f5;
            if (!this.isWalkable(f7, f8)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }

            this.mob.setSpeed(f1);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            hitboxlib$setOperation(self, WAIT);
        } else if (op == MOVE_TO) {
            hitboxlib$setOperation(self, WAIT);
            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedZ - this.mob.getZ();

            double partMinY = hitboxlib$getLowestCollisionMinY();
            double mobY;
            if (mp.isMainHitboxCollision()) {
                mobY = partMinY != Double.MAX_VALUE ? Math.min(partMinY, this.mob.getY()) : this.mob.getY();
            } else {
                mobY = partMinY != Double.MAX_VALUE ? partMinY : this.mob.getY();
            }

            double adjustedWantedY = this.wantedY;
            double moveY = this.wantedY - mobY;
            if (partMinY != Double.MAX_VALUE && hitboxlib$partsCollideAtWantedY(moveY)) {
                if (!hitboxlib$partsCollideAtWantedY(moveY + 1.0D)) {
                    adjustedWantedY = this.wantedY + 1.0D;
                }
            }

            double closestPartWidth = hitboxlib$getClosestPartWidth(d0, d1);
            double closestDistSq = hitboxlib$getClosestPartDistSq(d0, d1);

            double d2 = adjustedWantedY - mobY;
            double d3 = d0 * d0 + d2 * d2 + d1 * d1;
            if (d3 < (double)2.5000003E-7F) {
                this.mob.setZza(0.0F);
                ci.cancel();
                return;
            }

            float f9 = (float)(Mth.atan2(d1, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f9, 90.0F));
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            BlockPos blockpos = this.mob.blockPosition();
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            if (d2 > this.mob.getAttributeValue(Attributes.STEP_HEIGHT) && closestDistSq < (double)Math.max(1.0F, closestPartWidth)
                    || !voxelshape.isEmpty() && mobY < voxelshape.max(Direction.Axis.Y) + (double)blockpos.getY()
                    && !blockstate.is(BlockTags.DOORS) && !blockstate.is(BlockTags.FENCES)) {
                this.mob.getJumpControl().jump();
                hitboxlib$setOperation(self, JUMPING);
            }
        } else if (op == JUMPING) {
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.onGround()) {
                hitboxlib$setOperation(self, WAIT);
            }
        } else {
            this.mob.setZza(0.0F);
        }

        ci.cancel();
    }

    private static final int WAIT = 0;
    private static final int MOVE_TO = 1;
    private static final int STRAFE = 2;
    private static final int JUMPING = 3;

    @Unique
    private static int hitboxlib$getOperation(MoveControl self) {
        try {
            java.lang.reflect.Field f = MoveControl.class.getDeclaredField("f_24981_");
            f.setAccessible(true);
            Object val = f.get(self);
            if (val == null) return 0;
            return ((Enum<?>)val).ordinal();
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f = MoveControl.class.getDeclaredField("operation");
                f.setAccessible(true);
                Object val = f.get(self);
                if (val == null) return 0;
                return ((Enum<?>)val).ordinal();
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    @Unique
    private static void hitboxlib$setOperation(MoveControl self, int ordinal) {
        try {
            java.lang.reflect.Field f = MoveControl.class.getDeclaredField("f_24981_");
            f.setAccessible(true);
            Object[] constants = f.getType().getEnumConstants();
            if (constants != null && ordinal < constants.length) {
                f.set(self, constants[ordinal]);
            }
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f = MoveControl.class.getDeclaredField("operation");
                f.setAccessible(true);
                Object[] constants = f.getType().getEnumConstants();
                if (constants != null && ordinal < constants.length) {
                    f.set(self, constants[ordinal]);
                }
            } catch (Exception e2) {
            }
        }
    }

    @Unique
    private double hitboxlib$getLowestCollisionMinY() {
        PartEntity<?>[] parts = this.mob.getParts();
        if (parts == null) {
            return Double.MAX_VALUE;
        }
        double lowest = Double.MAX_VALUE;
        for (PartEntity<?> part : parts) {
            if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                double minY = cp.getBoundingBox().minY;
                if (minY < lowest) {
                    lowest = minY;
                }
            }
        }
        return lowest;
    }

    @Unique
    private boolean hitboxlib$partsCollideAtWantedY(double moveY) {
        PartEntity<?>[] parts = this.mob.getParts();
        if (parts == null) return false;
        double moveX = this.wantedX - this.mob.getX();
        double moveZ = this.wantedZ - this.mob.getZ();
        for (PartEntity<?> part : parts) {
            if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                AABB targetBox = cp.getBoundingBox().move(moveX, moveY, moveZ);
                for (VoxelShape shape : this.mob.level().getBlockCollisions(null, targetBox)) {
                    if (!shape.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Unique
    private double hitboxlib$getClosestPartWidth(double d0, double d1) {
        PartEntity<?>[] parts = this.mob.getParts();
        double closestPartWidth = this.mob.getBbWidth();
        double closestPartDistSq = d0 * d0 + d1 * d1;
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    double pDx = this.wantedX - (cp.getBoundingBox().minX + cp.getBoundingBox().maxX) * 0.5;
                    double pDz = this.wantedZ - (cp.getBoundingBox().minZ + cp.getBoundingBox().maxZ) * 0.5;
                    double pDistSq = pDx * pDx + pDz * pDz;
                    if (pDistSq < closestPartDistSq) {
                        closestPartDistSq = pDistSq;
                        closestPartWidth = Math.max(cp.getBoundingBox().getXsize(), cp.getBoundingBox().getZsize());
                    }
                }
            }
        }
        return closestPartWidth;
    }

    @Unique
    private double hitboxlib$getClosestPartDistSq(double d0, double d1) {
        PartEntity<?>[] parts = this.mob.getParts();
        double closest = d0 * d0 + d1 * d1;
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    double pDx = this.wantedX - (cp.getBoundingBox().minX + cp.getBoundingBox().maxX) * 0.5;
                    double pDz = this.wantedZ - (cp.getBoundingBox().minZ + cp.getBoundingBox().maxZ) * 0.5;
                    double pDistSq = pDx * pDx + pDz * pDz;
                    if (pDistSq < closest) {
                        closest = pDistSq;
                    }
                }
            }
        }
        return closest;
    }
}
