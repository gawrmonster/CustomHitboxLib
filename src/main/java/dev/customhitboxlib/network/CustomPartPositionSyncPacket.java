package dev.customhitboxlib.network;

import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class CustomPartPositionSyncPacket {
    private final float yRot;
    private final float xRot;
    private final float yBodyRot;
    private final float yHeadRot;

    public CustomPartPositionSyncPacket(float yRot, float xRot, float yBodyRot, float yHeadRot) {
        this.yRot = yRot;
        this.xRot = xRot;
        this.yBodyRot = yBodyRot;
        this.yHeadRot = yHeadRot;
    }

    public static void encode(CustomPartPositionSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.yRot);
        buf.writeFloat(msg.xRot);
        buf.writeFloat(msg.yBodyRot);
        buf.writeFloat(msg.yHeadRot);
    }

    public static CustomPartPositionSyncPacket decode(FriendlyByteBuf buf) {
        return new CustomPartPositionSyncPacket(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat()
        );
    }

    public static void handle(CustomPartPositionSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;
            if (!(serverPlayer instanceof ICustomMultipart mp)) return;

            mp.setSyncedRotation(msg.yRot, msg.xRot, msg.yBodyRot, msg.yHeadRot);
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send(CustomPartPositionSyncPacket packet) {
        dev.customhitboxlib.CustomHitboxLib.CHANNEL.sendToServer(packet);
    }
}
