package dev.customhitboxlib;

import dev.customhitboxlib.api.ICustomMultipart;
import dev.customhitboxlib.datapack.PartDefinitionLoader;
import dev.customhitboxlib.network.CustomPartPositionSyncPacket;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;

@Mod(CustomHitboxLib.MOD_ID)
public class CustomHitboxLib {
    public static final String MOD_ID = "customhitboxlib";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> "1",
            "1"::equals,
            "1"::equals
    );

    public CustomHitboxLib() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
    }

    static {
        int id = 0;
        CHANNEL.registerMessage(id++, CustomPartPositionSyncPacket.class, CustomPartPositionSyncPacket::encode, CustomPartPositionSyncPacket::decode, CustomPartPositionSyncPacket::handle);
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