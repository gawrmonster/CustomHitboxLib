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


    @Redirect(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 hitboxlib$collideBoundingBox(Entity entity, Vec3 movement, AABB aabb, Level level,
            List<VoxelShape> entityShapes) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof LivingEntity) || !(self instanceof ICustomMultipart mp)) {
            return Entity.collideBoundingBox(entity, movement, aabb, level, entityShapes);
        }

        ImmutableList.Builder<VoxelShape> entityShapeBuilder = ImmutableList
                .builderWithExpectedSize(entityShapes.size() + 10);
        entityShapeBuilder.addAll(entityShapes);

        PartEntity<?>[] ownParts = self.getParts();

        AABB searchBox = self.getBoundingBox().inflate(1.0E-7D);
        List<Entity> nearbyEntities = level.getEntities(self, searchBox);

        for (Entity e : nearbyEntities) {
            if (e == self)
                continue;
            if (!(e instanceof ICustomMultipart otherMp) || !otherMp.hasCustomParts())
                continue;

            PartEntity<?>[] parts = e.getParts();
            if (parts == null)
                continue;

            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp))
                    continue;
                if (cp.isPushable())
                    continue;

                entityShapeBuilder.add(Shapes.create(cp.getBoundingBox()));
            }
        }

        WorldBorder worldborder = level.getWorldBorder();
        boolean inBorder = entity != null && worldborder.isInsideCloseToBorder(entity, aabb.expandTowards(movement));
        if (inBorder) {
            entityShapeBuilder.add(worldborder.getCollisionShape());
        }

        Vec3 result = collideWithShapes(movement, aabb, entityShapeBuilder.build());

        if (mp.isMainHitboxCollision()) {
            AABB blockArea = aabb.expandTowards(result);
            List<VoxelShape> mainBlockShapes = new ArrayList<>();
            level.getBlockCollisions(entity, blockArea).forEach(mainBlockShapes::add);
            if (!mainBlockShapes.isEmpty()) {
                result = collideWithShapes(result, aabb, mainBlockShapes);
            }
        }

        if (ownParts != null && !self.noPhysics) {
            for (PartEntity<?> part : ownParts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    AABB partBox = cp.getBoundingBox();
                    AABB partBlockArea = partBox.expandTowards(result);
                    List<VoxelShape> partBlockShapes = new ArrayList<>();
                    level.getBlockCollisions(entity, partBlockArea).forEach(partBlockShapes::add);
                    if (!partBlockShapes.isEmpty()) {
                        result = collideWithShapes(result, partBox, partBlockShapes);
                    }
                }
            }
        }

        return result;
    }

    @Inject(method = "checkInsideBlocks()V", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$checkInsideBlocks(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.noPhysics) {
            ci.cancel();
            return;
        }
        if (!(self instanceof ICustomMultipart mp) || mp.isMainHitboxCollision()) {
            return;
        }

        Level level = self.level();
        PartEntity<?>[] parts = self.getParts();
        if (parts == null)
            return;

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

        ci.cancel();
    }

    @Inject(method = "isInWall()Z", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$isInWall(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.noPhysics) {
            cir.setReturnValue(false);
            return;
        }
        if (!(self instanceof ICustomMultipart mp)) {
            return;
        }
        if (mp.isMainHitboxCollision()) {
            return;
        }

        PartEntity<?>[] parts = self.getParts();
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (part instanceof CustomEntityPart cp && cp.isSuffocate() && cp.isInWall()) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            cir.setReturnValue(false);
        }

    }
}
