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
                double dx = x - player.getX();
                double dy = y - player.getY();
                double dz = z - player.getZ();
                for (PartEntity<?> part : parts) {
                    if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                        AABB currentPartBox = cp.getBoundingBox();
                        AABB targetPartBox = currentPartBox.move(dx, dy, dz);
                        VoxelShape currentPartShape = Shapes.create(currentPartBox.deflate(1.0E-5F));
                        for (VoxelShape shape : level.getCollisions(player, targetPartBox.deflate(1.0E-5F))) {
                            if (!Shapes.joinIsNotEmpty(shape, currentPartShape, BooleanOp.AND)) {
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
