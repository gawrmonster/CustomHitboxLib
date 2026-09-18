package dev.customhitboxlib.mixin;

import com.google.common.collect.ImmutableList;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$overrideCollide(Vec3 vec, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof LivingEntity) || !(self instanceof ICustomMultipart mp) || self.noPhysics) {
            return;
        }

        PartEntity<?>[] ownParts = mp.getCustomParts();
        if ((ownParts == null || ownParts.length == 0) && !mp.isMainHitboxCollision()) {
            return;
        }

        if (vec.lengthSqr() == 0.0D) {
            cir.setReturnValue(vec);
            return;
        }

        Vec3 resolvedMovement = hitboxlib$resolveCompoundCollision(self, mp, vec);

        boolean blockedX = vec.x != resolvedMovement.x;
        boolean blockedY = vec.y != resolvedMovement.y;
        boolean blockedZ = vec.z != resolvedMovement.z;
        boolean falling = blockedY && vec.y < 0.0D;
        float maxUpStep = self.maxUpStep();

        if (maxUpStep > 0.0F && (falling || self.onGround()) && (blockedX || blockedZ)) {
            AABB primaryBox = mp.isMainHitboxCollision() ? self.getBoundingBox() : ownParts[0].getBoundingBox();
            AABB adjustedBox = falling ? primaryBox.move(0.0D, resolvedMovement.y, 0.0D) : primaryBox;
            AABB stepSearchArea = adjustedBox.expandTowards(vec.x, (double) maxUpStep, vec.z);
            if (!falling) {
                stepSearchArea = stepSearchArea.expandTowards(0.0D, -1.0E-5D, 0.0D);
            }

            List<VoxelShape> colliders = hitboxlib$collectAllColliders(self, mp, stepSearchArea);
            
            it.unimi.dsi.fastutil.floats.FloatSet candidateHeights = new it.unimi.dsi.fastutil.floats.FloatArraySet(4);
            for (VoxelShape voxelshape : colliders) {
                it.unimi.dsi.fastutil.doubles.DoubleListIterator iterator = voxelshape.getCoords(net.minecraft.core.Direction.Axis.Y).iterator();
                while (iterator.hasNext()) {
                    double d0 = (Double) iterator.next();
                    float stepOffset = (float) (d0 - adjustedBox.minY);
                    if (!(stepOffset < 0.0F) && stepOffset != maxUpStep) {
                        if (stepOffset > (float) resolvedMovement.y) {
                            break;
                        }
                        candidateHeights.add(stepOffset);
                    }
                }
            }

            float[] sortedHeights = candidateHeights.toFloatArray();
            it.unimi.dsi.fastutil.floats.FloatArrays.unstableSort(sortedHeights);

            for (float stepHeight : sortedHeights) {
                Vec3 stepAttempt = hitboxlib$resolveCompoundCollision(self, mp, new Vec3(vec.x, (double) stepHeight, vec.z));
                if (stepAttempt.horizontalDistanceSqr() > resolvedMovement.horizontalDistanceSqr()) {
                    double verticalCorrection = primaryBox.minY - adjustedBox.minY;
                    cir.setReturnValue(stepAttempt.add(0.0D, -verticalCorrection, 0.0D));
                    return;
                }
            }
        }

        cir.setReturnValue(resolvedMovement);
    }

    private Vec3 hitboxlib$resolveCompoundCollision(Entity self, ICustomMultipart mp, Vec3 movement) {
        Vec3 result = movement;

        if (mp.isMainHitboxCollision()) {
            AABB mainBox = self.getBoundingBox();
            List<VoxelShape> shapes = hitboxlib$collectShapesForArea(self, mainBox.expandTowards(result));
            result = collideWithShapes(result, mainBox, shapes);
        }

        PartEntity<?>[] ownParts = mp.getCustomParts();
        if (ownParts != null) {
            for (PartEntity<?> part : ownParts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    AABB partBox = cp.getBoundingBox();
                    List<VoxelShape> shapes = hitboxlib$collectShapesForArea(self, partBox.expandTowards(result));
                    result = collideWithShapes(result, partBox, shapes);
                }
            }
        }

        return result;
    }

    private List<VoxelShape> hitboxlib$collectAllColliders(Entity self, ICustomMultipart mp, AABB area) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builder();

        if (mp.isMainHitboxCollision()) {
            builder.addAll(hitboxlib$collectShapesForArea(self, area));
        }

        PartEntity<?>[] ownParts = mp.getCustomParts();
        if (ownParts != null) {
            for (PartEntity<?> part : ownParts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    builder.addAll(hitboxlib$collectShapesForArea(self, area));
                }
            }
        }

        return builder.build();
    }

    private List<VoxelShape> hitboxlib$collectShapesForArea(Entity self, AABB searchArea) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builder();

        builder.addAll(self.level().getEntityCollisions(self, searchArea));

        List<Entity> nearbyEntities = self.level().getEntities(self, searchArea.inflate(1.0E-7D));
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
        if (border.isInsideCloseToBorder(self, searchArea)) {
            builder.add(border.getCollisionShape());
        }

        self.level().getBlockCollisions(self, searchArea).forEach(builder::add);

        return builder.build();
    }

    @Inject(method = "checkInsideBlocks()V", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$checkInsideBlocks(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.noPhysics || !(self instanceof ICustomMultipart mp)) {
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
        if (self.noPhysics || !(self instanceof ICustomMultipart mp)) {
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
