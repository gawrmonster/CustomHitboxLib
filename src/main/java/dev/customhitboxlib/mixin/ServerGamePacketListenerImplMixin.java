package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        if (player instanceof ICustomMultipart mp && !mp.isMainHitboxCollision()) {
            return false;
        }
        return this.isPlayerCollidingWithAnythingNew(level, oldBB, x, y, z);
    }
}
