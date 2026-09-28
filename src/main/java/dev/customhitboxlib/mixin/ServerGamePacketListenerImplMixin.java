package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader level, AABB oldBB, double x, double y, double z);

    @Shadow
    public ServerPlayer player;

    @Unique
    private Entity hitboxlib$interactTarget;

    @Inject(
        method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V",
        at = @At("HEAD")
    )
    private void hitboxlib$storeInteractTarget(ServerboundInteractPacket packet, CallbackInfo ci) {
        ServerLevel serverlevel = this.player.serverLevel();
        this.hitboxlib$interactTarget = packet.getTarget(serverlevel);
    }

    @Redirect(
        method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z")
    )
    private boolean hitboxlib$useEntityForInteract(ServerPlayer player, AABB aabb, double d) {
        if (this.hitboxlib$interactTarget != null) {
            return this.player.canInteractWithEntity(this.hitboxlib$interactTarget, d);
        }
        return this.player.canInteractWithEntity(aabb, d);
    }

    @Redirect(
        method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isPlayerCollidingWithAnythingNew(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/phys/AABB;DDD)Z")
    )
    private boolean hitboxlib$skipBlockValidation(ServerGamePacketListenerImpl listener, LevelReader level, AABB oldBB, double x, double y, double z) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();
        if (player instanceof ICustomMultipart mp) {
            if (mp.isMainHitboxCollision()) {
                if (this.isPlayerCollidingWithAnythingNew(level, oldBB, x, y, z)) {
                    return true;
                }
            }

            PartEntity<?>[] parts = mp.getCustomParts();
            if (parts != null) {
                double deltaX = x - player.getX();
                double deltaY = y - player.getY();
                double deltaZ = z - player.getZ();
                java.util.Map<String, Vec3> synced = mp.getSyncedPartPositions();

                for (PartEntity<?> part : parts) {
                    if (!(part instanceof CustomEntityPart cp) || !cp.hasCollision())
                        continue;

                    Vec3 syncedPos = synced.get(cp.getCustomName() != null ? cp.getCustomName().getString() : null);
                    AABB oldPartBB = syncedPos != null
                            ? cp.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(syncedPos)
                            : cp.getBoundingBox();

                    AABB newPartBB = oldPartBB.move(deltaX, deltaY, deltaZ);

                    VoxelShape oldPartShape = Shapes.create(oldPartBB.deflate(1.0E-5F));
                    for (VoxelShape shape : level.getCollisions(player, newPartBB.deflate(1.0E-5F))) {
                        if (!Shapes.joinIsNotEmpty(shape, oldPartShape, BooleanOp.AND)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
        return this.isPlayerCollidingWithAnythingNew(level, oldBB, x, y, z);
    }

}
