package dev.customhitboxlib.mixin;

import com.google.common.collect.ImmutableList;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
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

    @Redirect(
        method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", 
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
            ordinal = 0
        )
    )
    private Vec3 hitboxlib$collideWithShapesStepUp(Vec3 movement, AABB aabb, List<VoxelShape> shapes) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof LivingEntity) || !(self instanceof ICustomMultipart mp)) {
            return collideWithShapes(movement, aabb, shapes);
        }

        Vec3 result = movement;

        // 1. Only collide main hitbox with shapes if main hitbox collision is enabled
        if (mp.isMainHitboxCollision()) {
            result = collideWithShapes(movement, aabb, shapes);
        }

        // 2. Custom parts block collision
        PartEntity<?>[] ownParts = self.getParts();
        if (ownParts != null && !self.noPhysics) {
            for (PartEntity<?> part : ownParts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    AABB partBox = cp.getBoundingBox();
                    AABB partBlockArea = partBox.expandTowards(result);
                    List<VoxelShape> partBlockShapes = new ArrayList<>();
                    self.level().getBlockCollisions(self, partBlockArea).forEach(partBlockShapes::add);
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
            return;
        }
        if (!(self instanceof ICustomMultipart mp)) {
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

        PartEntity<?>[] parts = self.getParts();
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart cp) || !cp.isSuffocate()) continue;

                Vec3 pos = cp.position();
                float halfWidth = cp.getBbWidth() * 0.5F;
                AABB partAabb = new AABB(
                    pos.x - halfWidth, pos.y, pos.z - halfWidth,
                    pos.x + halfWidth, pos.y + cp.getBbHeight(), pos.z + halfWidth
                );

                if (self.level().collidesWithSuffocatingBlock(cp, partAabb)) {
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
