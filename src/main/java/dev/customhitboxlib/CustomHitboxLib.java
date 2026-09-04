package dev.customhitboxlib;

import dev.customhitboxlib.api.ICustomMultipart;
import dev.customhitboxlib.datapack.PartDefinitionLoader;
import dev.customhitboxlib.network.CustomPartPositionSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CustomHitboxLib.MOD_ID)
public class CustomHitboxLib {
    public static final String MOD_ID = "customhitboxlib";
    public static final Logger LOGGER = LogManager.getLogger();

    public CustomHitboxLib(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                CustomPartPositionSyncPacket.TYPE,
                CustomPartPositionSyncPacket.STREAM_CODEC,
                CustomPartPositionSyncPacket::handle
        );
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PartDefinitionLoader());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        var datapackParts = PartDefinitionLoader.getLoadedParts();
        if (datapackParts == null || datapackParts.isEmpty()) return;

        for (PartDefinitionLoader.CustomPartDefinition def : datapackParts.values()) {
            if (def.matches(entity)) {
                if (entity instanceof ICustomMultipart mp) {
                    for (PartDefinitionLoader.PartEntry part : def.parts()) {
                        mp.addCustomPart(part.name(), part.toApiDefinition());
                    }
                    if (def.mainHitboxPickable() != null) mp.setMainHitboxPickable(def.mainHitboxPickable());
                    if (def.mainHitboxPushable() != null) mp.setMainHitboxPushable(def.mainHitboxPushable());
                    if (def.mainHitboxCollision() != null) mp.setMainHitboxCollision(def.mainHitboxCollision());
                }
            }
        }
    }
}
