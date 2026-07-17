package dev.customhitboxlib;

import dev.customhitboxlib.api.ICustomMultipart;
import dev.customhitboxlib.datapack.PartDefinitionLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Map;

@Mod(CustomHitboxLib.MOD_ID)
public class CustomHitboxLib {
    public static final String MOD_ID = "customhitboxlib";
    public static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, PartDefinitionLoader.CustomPartDefinition> datapackParts;

    public CustomHitboxLib(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        datapackParts = PartDefinitionLoader.load(event.getServer());
        LOGGER.info("Loaded {} custom part definitions from datapacks", datapackParts.size());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (datapackParts == null || datapackParts.isEmpty()) return;
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;

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