package dev.customhitboxlib.datapack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.customhitboxlib.CustomHitboxLib;
import dev.customhitboxlib.api.PartDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PartDefinitionLoader {

    private static final Gson GSON = new Gson();

    public static Map<String, CustomPartDefinition> load(net.minecraft.server.MinecraftServer server) {
        var result = new LinkedHashMap<String, CustomPartDefinition>();
        String namespace = CustomHitboxLib.MOD_ID;
        String basePath = "custom_parts";

        Path serverDir = server.getServerDirectory().toPath();
        Path datapacksDir = serverDir.resolve("datapacks");

        if (!Files.exists(datapacksDir)) {
            return result;
        }

        try {
            try (DirectoryStream<Path> packDirs = Files.newDirectoryStream(datapacksDir)) {
                for (Path packDir : packDirs) {
                    if (!Files.isDirectory(packDir)) continue;
                    String packId = packDir.getFileName().toString();
                    if (!packId.equals(namespace)) continue;

                    Path dataDir = packDir.resolve("data").resolve(namespace).resolve(basePath);
                    if (!Files.exists(dataDir)) continue;

                    loadJsonFilesRecursive(dataDir, result);
                }
            }
        } catch (IOException e) {
            CustomHitboxLib.LOGGER.warn("Error loading datapack parts from disk", e);
        }

        return result;
    }

    private static void loadJsonFilesRecursive(Path dir, Map<String, CustomPartDefinition> result) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    loadJsonFilesRecursive(entry, result);
                } else if (entry.toString().endsWith(".json")) {
                    String fileName = entry.getFileName().toString();
                    String key = fileName.substring(0, fileName.length() - 5);

                    try (var is = Files.newInputStream(entry)) {
                        JsonElement element = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonElement.class);
                        if (element == null) continue;

                        CustomPartDefinition def = parseDefinition(key, element);
                        if (def != null) {
                            result.put(key, def);
                        }
                    } catch (JsonSyntaxException e) {
                        CustomHitboxLib.LOGGER.error("Invalid JSON in datapack file: {}", entry, e);
                    }
                }
            }
        }
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
                return null;
            }

            return new CustomPartDefinition(selectors, partsList, fields.mainHitboxPickable, fields.mainHitboxPushable, fields.mainHitboxCollision);
        } catch (Exception e) {
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

        if (entityId == null && nbtString == null) return;

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