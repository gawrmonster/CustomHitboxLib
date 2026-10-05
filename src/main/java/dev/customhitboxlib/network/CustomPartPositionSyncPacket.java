package dev.customhitboxlib.network;

import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

public class CustomPartPositionSyncPacket implements CustomPacketPayload {

    private final float yRot;
    private final float xRot;
    private final float yBodyRot;
    private final float yHeadRot;

    public static final CustomPacketPayload.Type<CustomPartPositionSyncPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("customhitboxlib", "sync_part_position"));
    public static final StreamCodec<FriendlyByteBuf, CustomPartPositionSyncPacket> STREAM_CODEC = CustomPacketPayload.codec(CustomPartPositionSyncPacket::write, CustomPartPositionSyncPacket::new);

    public CustomPartPositionSyncPacket(float yRot, float xRot, float yBodyRot, float yHeadRot) {
        this.yRot = yRot;
        this.xRot = xRot;
        this.yBodyRot = yBodyRot;
        this.yHeadRot = yHeadRot;
    }

    private CustomPartPositionSyncPacket(FriendlyByteBuf buf) {
        this.yRot = buf.readFloat();
        this.xRot = buf.readFloat();
        this.yBodyRot = buf.readFloat();
        this.yHeadRot = buf.readFloat();
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeFloat(yRot);
        buf.writeFloat(xRot);
        buf.writeFloat(yBodyRot);
        buf.writeFloat(yHeadRot);
    }

    @Override
    public CustomPacketPayload.Type<CustomPartPositionSyncPacket> type() {
        return TYPE;
    }

    public static void handle(CustomPartPositionSyncPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) ctx.player();
            if (serverPlayer == null) return;
            if (!(serverPlayer instanceof ICustomMultipart mp)) return;

            mp.setSyncedRotation(msg.yRot, msg.xRot, msg.yBodyRot, msg.yHeadRot);
        });
    }

    public static void send(float yRot, float xRot, float yBodyRot, float yHeadRot) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        var connection = mc.getConnection();
        if (connection == null) return;
        var conn = connection.getConnection();
        if (conn == null) return;
        conn.send(new CustomPartPositionSyncPacket(yRot, xRot, yBodyRot, yHeadRot).toVanillaServerbound());
    }
}
