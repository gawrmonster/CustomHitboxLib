package dev.customhitboxlib.mixin;

import com.google.common.collect.ImmutableList;
import dev.customhitboxlib.CustomHitboxLib;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private static Vec3 collideWithShapes(Vec3 pMovement, AABB pAabb, List<VoxelShape> pShapes) {
        return null;
    }

    @Shadow
    protected abstract void onInsideBlock(BlockState pState);

    @Inject(method = "tick", at = @At("HEAD"))
    private void hitboxlib$tickHead(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ICustomMultipart mp) {
            mp.tickCustomParts();
        }
    }

    @Inject(method = "canEnterPose", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$canEnterPose(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player))
            return;

        EntityDimensions dims = self.getDimensions(pose);
        float halfWidth = dims.width / 2.0F;
        AABB boundingBox = new AABB(
                self.getX() - halfWidth, self.getY(), self.getZ() - halfWidth,
                self.getX() + halfWidth, self.getY() + dims.height, self.getZ() + halfWidth);

        if (self.level().noCollision(player, boundingBox.deflate(1.0E-7D))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$overrideCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof LivingEntity) || !(self instanceof ICustomMultipart mp) || self.noPhysics) {
            return;
        }

        PartEntity<?>[] ownParts = mp.getCustomParts();
        if (ownParts == null || ownParts.length == 0) {
            return;
        }

        if (movement.lengthSqr() == 0.0D) {
            cir.setReturnValue(movement);
            return;
        }

        Vec3 vec3 = hitboxlib$resolveCompoundCollision(self, mp, movement, Vec3.ZERO);

        boolean flag = movement.x != vec3.x;
        boolean flag1 = movement.y != vec3.y;
        boolean flag2 = movement.z != vec3.z;
        boolean flag3 = self.onGround() || (flag1 && movement.y < 0.0D);
        float stepHeight = self.getStepHeight();

        if (stepHeight > 0.0F && flag3 && (flag || flag2)) {
            Vec3 vec31 = hitboxlib$resolveCompoundCollision(self, mp, new Vec3(movement.x, (double) stepHeight, movement.z), Vec3.ZERO);

            Vec3 vec32 = hitboxlib$resolveCompoundCollision(self, mp, new Vec3(0.0D, (double) stepHeight, 0.0D), new Vec3(movement.x, 0.0D, movement.z));

            if (vec32.y < (double) stepHeight) {
                Vec3 vec33 = hitboxlib$resolveCompoundCollision(self, mp, new Vec3(movement.x, 0.0D, movement.z), vec32).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                Vec3 dropVec = new Vec3(0.0D, -vec31.y + movement.y, 0.0D);
                Vec3 finalDrop = hitboxlib$resolveCompoundCollision(self, mp, dropVec, vec31);
                cir.setReturnValue(vec31.add(finalDrop));
                return;
            }
        }

        cir.setReturnValue(vec3);
    }

    private Vec3 hitboxlib$resolveCompoundCollision(Entity self, ICustomMultipart mp, Vec3 movement, Vec3 positionalOffset) {
        Vec3 result = movement;

        if (mp.isMainHitboxCollision()) {
            AABB mainBox = self.getBoundingBox().move(positionalOffset);
            List<VoxelShape> shapes = hitboxlib$collectShapesForBox(self, mainBox, result, positionalOffset);
            result = collideWithShapes(result, mainBox, shapes);
        }

        PartEntity<?>[] ownParts = mp.getCustomParts();
        if (ownParts != null) {
            for (PartEntity<?> part : ownParts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    AABB partBox = cp.getBoundingBox().move(positionalOffset);
                    List<VoxelShape> shapes = hitboxlib$collectShapesForBox(self, partBox, result, positionalOffset);
                    result = collideWithShapes(result, partBox, shapes);
                }
            }
        }

        return result;
    }

    private List<VoxelShape> hitboxlib$collectShapesForBox(Entity self, AABB box, Vec3 movement, Vec3 positionalOffset) {
        AABB area = box.expandTowards(movement);
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builder();

        builder.addAll(self.level().getEntityCollisions(self, area));

        List<Entity> nearbyEntities = self.level().getEntities(self, area.inflate(1.0E-7D));
        for (Entity e : nearbyEntities) {
            if (e == self || !(e instanceof ICustomMultipart otherMp) || !otherMp.hasCustomParts()) continue;
            PartEntity<?>[] parts = otherMp.getCustomParts();
            if (parts == null) continue;

            for (PartEntity<?> otherPart : parts) {
                if (otherPart instanceof CustomEntityPart otherCp && e.canCollideWith(self) && otherCp.hasCollision()) {
                    builder.add(Shapes.create(otherCp.getBoundingBox()));
                }
            }
        }

        WorldBorder border = self.level().getWorldBorder();
        if (border.isInsideCloseToBorder(self, area)) {
            builder.add(border.getCollisionShape());
        }

        self.level().getBlockCollisions(self, area).forEach(builder::add);

        return builder.build();
    }

    @Inject(method = "checkInsideBlocks()V", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$checkInsideBlocks(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.noPhysics) {
            return;
        }
        if (!(self instanceof ICustomMultipart mp)) {
            return;
        }

        Level level = self.level();
        PartEntity<?>[] parts = mp.getCustomParts();
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.hasCollision())
                    continue;

                AABB partBox = cp.getBoundingBox();
                int minX = (int) Math.floor(partBox.minX);
                int minY = (int) Math.floor(partBox.minY);
                int minZ = (int) Math.floor(partBox.minZ);
                int maxX = (int) Math.floor(partBox.maxX);
                int maxY = (int) Math.floor(partBox.maxY);
                int maxZ = (int) Math.floor(partBox.maxZ);

                BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            mutPos.set(x, y, z);
                            BlockState state = level.getBlockState(mutPos);
                            state.entityInside(level, mutPos, self);
                            onInsideBlock(state);
                        }
                    }
                }
            }
        }

        if (!mp.isMainHitboxCollision()) {
            ci.cancel();
        }
    }

    @Inject(method = "isInWall()Z", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$isInWall(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.noPhysics) {
            return;
        }
        if (!(self instanceof ICustomMultipart mp)) {
            return;
        }

        PartEntity<?>[] parts = mp.getCustomParts();
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.isSuffocate()) continue;

                float f = cp.getBbWidth() * 0.8F;
                Vec3 eyePos = cp.getEyePosition();
                AABB aabb = AABB.ofSize(eyePos, (double)f, 1.0E-6D, (double)f);
                if (BlockPos.betweenClosedStream(aabb).anyMatch((p_201942_) -> {
                    BlockState blockstate = self.level().getBlockState(p_201942_);
                    return !blockstate.isAir() && blockstate.isSuffocating(self.level(), p_201942_) && Shapes.joinIsNotEmpty(blockstate.getCollisionShape(self.level(), p_201942_).move((double)p_201942_.getX(), (double)p_201942_.getY(), (double)p_201942_.getZ()), Shapes.create(aabb), BooleanOp.AND);
                })) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        if (!mp.isMainHitboxCollision()) {
            cir.setReturnValue(false);
        }
    }
}
