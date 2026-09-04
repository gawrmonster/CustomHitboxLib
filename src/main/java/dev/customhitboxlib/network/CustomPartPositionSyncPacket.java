package dev.customhitboxlib.network;

import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CustomPartPositionSyncPacket {

    private final String partName;
    private final double x;
    private final double y;
    private final double z;

    public CustomPartPositionSyncPacket(String partName, Vec3 pos) {
        this.partName = partName;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    public static void encode(CustomPartPositionSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.partName);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static CustomPartPositionSyncPacket decode(FriendlyByteBuf buf) {
        String partName = buf.readUtf();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        return new CustomPartPositionSyncPacket(partName, new Vec3(x, y, z));
    }

    public static void handle(CustomPartPositionSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;
            if (!(serverPlayer instanceof ICustomMultipart mp)) return;

            Map<String, Vec3> synced = mp.getSyncedPartPositions();
            synced.put(msg.partName, new Vec3(msg.x, msg.y, msg.z));
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send(CustomPartPositionSyncPacket packet) {
        dev.customhitboxlib.CustomHitboxLib.CHANNEL.sendToServer(packet);
    }
}
