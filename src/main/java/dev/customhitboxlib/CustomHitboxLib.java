package dev.customhitboxlib;

import dev.customhitboxlib.api.ICustomMultipart;
import dev.customhitboxlib.datapack.PartDefinitionLoader;
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

@Mod(CustomHitboxLib.MOD_ID)
public class CustomHitboxLib {
    public static final String MOD_ID = "customhitboxlib";
    public static final Logger LOGGER = LogManager.getLogger();

    public CustomHitboxLib(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Server-side: registers the datapack loader for server data reload.
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        LOGGER.info("[CustomHitboxLib] Registering PartDefinitionLoader listener");
        event.addListener(new PartDefinitionLoader());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        var datapackParts = PartDefinitionLoader.getLoadedParts();
        LOGGER.info("[CustomHitboxLib] Entity joined: {}, loaded parts: {}", event.getEntity().getType().toString(), datapackParts != null ? datapackParts.size() : "null");
        if (datapackParts == null || datapackParts.isEmpty()) return;
        Entity entity = event.getEntity();

        for (PartDefinitionLoader.CustomPartDefinition def : datapackParts.values()) {
            if (def.matches(entity)) {
                LOGGER.info("[CustomHitboxLib] Entity {} matched definition, ICustomMultipart: {}", entity.getType().toString(), entity instanceof ICustomMultipart);
                if (entity instanceof ICustomMultipart mp) {
                    LOGGER.info("[CustomHitboxLib] Applying {} custom parts to entity {}", def.parts().size(), entity.getType().toString());
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