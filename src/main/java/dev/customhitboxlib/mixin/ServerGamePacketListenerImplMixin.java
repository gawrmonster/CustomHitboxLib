package dev.customhitboxlib.mixin;

import dev.customhitboxlib.CustomHitboxLib;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader level, AABB oldBB, double x, double y, double z);

    @Redirect(
        method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isPlayerCollidingWithAnythingNew(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/phys/AABB;DDD)Z")
    )
    private boolean hitboxlib$skipBlockValidation(ServerGamePacketListenerImpl listener, LevelReader level, AABB oldBB, double x, double y, double z) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();

        if (player instanceof ICustomMultipart mp) {
            PartEntity<?>[] parts = player.getParts();
            if (parts != null) {
                // Compute movement delta directly from target coordinates vs oldBB position
                double dx = x - (oldBB.minX + (oldBB.maxX - oldBB.minX) / 2.0);
                double dy = y - oldBB.minY;
                double dz = z - (oldBB.minZ + (oldBB.maxZ - oldBB.minZ) / 2.0);

                for (PartEntity<?> part : parts) {
                    if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                        AABB currentPartBox = cp.getBoundingBox();
                        AABB targetPartBox = currentPartBox.move(dx, dy, dz);

                        // Deflate the old shape slightly to avoid edge-precision artifacts
                        VoxelShape currentPartShape = Shapes.create(currentPartBox.deflate(1.0E-5D));

                        // Fetch actual collision shapes in target area (do NOT deflate search box)
                        Iterable<VoxelShape> collisions = level.getCollisions(player, targetPartBox);

                        for (VoxelShape blockShape : collisions) {
                            // 1. Deflate target block shape slightly like vanilla does
                            VoxelShape targetShape = Shapes.joinUnoptimized(blockShape, Shapes.create(targetPartBox.deflate(1.0E-5D)), BooleanOp.AND);

                            // 2. Return true ONLY if there is a collision in the new target space that DID NOT exist in the original space
                            if (!targetShape.isEmpty() && !Shapes.joinIsNotEmpty(targetShape, currentPartShape, BooleanOp.AND)) {
                                return true;
                            }
                        }
                    }
                }
            }

            if (!mp.isMainHitboxCollision()) {
                return false;
            }
        }

        return this.isPlayerCollidingWithAnythingNew(level, oldBB, x, y, z);
    }

}
