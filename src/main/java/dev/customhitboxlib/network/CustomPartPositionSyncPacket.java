package dev.customhitboxlib.network;

import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public class CustomPartPositionSyncPacket implements CustomPacketPayload {

    private final String partName;
    private final double x;
    private final double y;
    private final double z;

    public static final CustomPacketPayload.Type<CustomPartPositionSyncPacket> TYPE = CustomPacketPayload.createType("customhitboxlib:sync_part_position");
    public static final StreamCodec<FriendlyByteBuf, CustomPartPositionSyncPacket> STREAM_CODEC = CustomPacketPayload.codec(CustomPartPositionSyncPacket::write, CustomPartPositionSyncPacket::new);

    public CustomPartPositionSyncPacket(String partName, Vec3 pos) {
        this.partName = partName;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    private CustomPartPositionSyncPacket(FriendlyByteBuf buf) {
        this.partName = buf.readUtf();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(partName);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
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

            Map<String, Vec3> synced = mp.getSyncedPartPositions();
            synced.put(msg.partName, new Vec3(msg.x, msg.y, msg.z));
        });
    }

    public static void send(String partName, Vec3 pos) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        var connection = mc.getConnection();
        if (connection == null) return;
        var conn = connection.getConnection();
        if (conn == null) return;
        conn.send(new CustomPartPositionSyncPacket(partName, pos).toVanillaServerbound());
    }
}
