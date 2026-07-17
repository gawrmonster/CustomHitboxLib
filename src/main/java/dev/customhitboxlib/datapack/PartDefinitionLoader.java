package dev.customhitboxlib.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.customhitboxlib.CustomHitboxLib;
import dev.customhitboxlib.api.PartDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PartDefinitionLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static Map<String, CustomPartDefinition> LOADED_PARTS = new LinkedHashMap<>();

    public PartDefinitionLoader() {
        super(GSON, "custom_parts");
    }

    public static Map<String, CustomPartDefinition> getLoadedParts() {
        return LOADED_PARTS;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] apply() called with {} resources", pObject.size());
        Map<String, CustomPartDefinition> newParts = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement element = entry.getValue();
            CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] Processing resource: {}", location);

            try {
                CustomPartDefinition def = parseDefinition(location.toString(), element);
                if (def != null) {
                    CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] Successfully parsed: {}", location);
                    newParts.put(location.toString(), def);
                } else {
                    CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] parseDefinition returned null for: {}", location);
                }
            } catch (Exception e) {
                CustomHitboxLib.LOGGER.error("Failed to parse custom part definition: {}", location, e);
            }
        }

        LOADED_PARTS = newParts;
        CustomHitboxLib.LOGGER.info("Loaded {} custom part definitions from datapacks", LOADED_PARTS.size());
    }

    private static CustomPartDefinition parseDefinition(String key, JsonElement element) {
        try {
            List<SelectorEntry> selectors = new ArrayList<>();
            List<PartEntry> partsList = new ArrayList<>();
            CustomHitboxFields fields = new CustomHitboxFields();

            if (element.isJsonArray()) {
                for (JsonElement item : element.getAsJsonArray()) {
                    parseEntry(item, selectors, partsList, fields);
                }
            } else if (element.isJsonObject()) {
                parseEntry(element, selectors, partsList, fields);
            } else {
                return null;
            }

            if (selectors.isEmpty() || partsList.isEmpty()) {
                CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] Skipping {} - selectors: {}, parts: {}", key, selectors.size(), partsList.size());
                return null;
            }

            return new CustomPartDefinition(selectors, partsList, fields.mainHitboxPickable, fields.mainHitboxPushable, fields.mainHitboxCollision);
        } catch (Exception e) {
            CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] Exception in parseDefinition for {}: {}", key, e.getMessage());
            return null;
        }
    }

    private static void parseEntry(JsonElement element, List<SelectorEntry> selectors, List<PartEntry> partsList, CustomHitboxFields outFields) {
        JsonObject obj = element.getAsJsonObject();
        String entityId = null;
        String nbtString = null;

        if (obj.has("selector")) {
            JsonObject sel = obj.getAsJsonObject("selector");
            if (sel.has("id")) entityId = sel.get("id").getAsString();
            if (sel.has("nbt")) nbtString = sel.get("nbt").getAsString();
        } else if (obj.has("id")) {
            entityId = obj.get("id").getAsString();
            if (obj.has("nbt")) nbtString = obj.get("nbt").getAsString();
        }

        if (entityId == null && nbtString == null) {
                CustomHitboxLib.LOGGER.info("[PartDefinitionLoader] Skipping entry - no entityId or nbtString");
                return;
            }

        selectors.add(new SelectorEntry(entityId, nbtString));

        if (obj.has("main_hitbox_pickable")) outFields.mainHitboxPickable = obj.get("main_hitbox_pickable").getAsBoolean();
        if (obj.has("main_hitbox_pushable")) outFields.mainHitboxPushable = obj.get("main_hitbox_pushable").getAsBoolean();
        if (obj.has("main_hitbox_collision")) outFields.mainHitboxCollision = obj.get("main_hitbox_collision").getAsBoolean();

        if (!obj.has("parts")) return;
        for (JsonElement partEl : obj.getAsJsonArray("parts")) {
            JsonObject partObj = partEl.getAsJsonObject();
            String name = partObj.get("name").getAsString();
            float width = partObj.get("width").getAsFloat();
            float height = partObj.get("height").getAsFloat();

            float ox = 0, oy = 0, oz = 0;
            if (partObj.has("offset")) {
                var arr = partObj.getAsJsonArray("offset");
                ox = arr.get(0).getAsFloat();
                oy = arr.get(1).getAsFloat();
                oz = arr.get(2).getAsFloat();
            }

            boolean pickable = !partObj.has("pickable") || partObj.get("pickable").getAsBoolean();
            boolean pushable = partObj.has("pushable") && partObj.get("pushable").getAsBoolean();
            boolean collision = partObj.has("collision") && partObj.get("collision").getAsBoolean();
            boolean suffocate = partObj.has("suffocate") && partObj.get("suffocate").getAsBoolean();

            partsList.add(new PartEntry(name, width, height, ox, oy, oz, pickable, pushable, collision, suffocate));
        }
    }

    private static class CustomHitboxFields {
        Boolean mainHitboxPickable = null;
        Boolean mainHitboxPushable = null;
        Boolean mainHitboxCollision = null;
    }

    public record CustomPartDefinition(
        List<SelectorEntry> selectors,
        List<PartEntry> parts,
        Boolean mainHitboxPickable,
        Boolean mainHitboxPushable,
        Boolean mainHitboxCollision
    ) {
        public boolean matches(Entity entity) {
            if (entity == null) return false;
            for (SelectorEntry selector : selectors) {
                if (selector.matches(entity)) return true;
            }
            return false;
        }
    }

    public record SelectorEntry(String entityId, String nbtString) {
        public boolean matches(Entity entity) {
            if (entityId != null) {
                String currentId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                if (!entityId.equals(currentId)) return false;
            }

            if (nbtString != null && entity instanceof LivingEntity living) {
                try {
                    CompoundTag entityNbt = new CompoundTag();
                    living.save(entityNbt);
                    String nbtClean = nbtString.replace("{", "").replace("}", "").trim();
                    if (nbtClean.isEmpty()) return entityId != null;
                    String[] pairs = nbtClean.split(",");
                    for (String pair : pairs) {
                        String[] kv = pair.split(":");
                        if (kv.length != 2) continue;
                        String key = kv[0].trim();
                        String val = kv[1].trim();
                        if (!entityNbt.contains(key)) return false;
                        String entityVal = entityNbt.get(key).toString().replace("\"", "").trim();
                        if (!val.equalsIgnoreCase(entityVal)) return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }

            return entityId != null || nbtString != null;
        }
    }

    public record PartEntry(
        String name, float width, float height,
        float ox, float oy, float oz,
        boolean pickable, boolean pushable, boolean collision, boolean suffocate
    ) {
        public PartDefinition toApiDefinition() {
            float finalOx = ox, finalOy = oy, finalOz = oz;
            return PartDefinition.of(
                name, width, height,
                (entity, partialTick) -> new Vec3(finalOx, finalOy, finalOz),
                pickable, pushable, collision, suffocate
            );
        }
    }
}