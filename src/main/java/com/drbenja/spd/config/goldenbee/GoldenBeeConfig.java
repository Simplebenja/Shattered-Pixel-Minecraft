package com.drbenja.spd.config.goldenbee;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Config: config/spd/goldenbee/
 * - hive_entity_blacklist.json
 * - hive_entity_whitelist.json
 * - hive_block_whitelist.json
 *
 * Vanilla beehive/bee_nest hard-blocked. Golden Nest always allowed for bees.
 * Golden Bee always allowed even if not in whitelist.
 */
public final class GoldenBeeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<Identifier> ENTITY_BLACKLIST = new HashSet<>();
    private static final Set<Identifier> ENTITY_WHITELIST = new HashSet<>();
    private static final Set<Identifier> BLOCK_WHITELIST = new HashSet<>();
    private static boolean loaded;

    private GoldenBeeConfig() {}

    public static void ensureLoaded() {
        if (loaded) return;
        Path dir = FMLPaths.CONFIGDIR.get().resolve("spd").resolve("goldenbee");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        loadIdSet(dir.resolve("hive_entity_blacklist.json"), ENTITY_BLACKLIST, defaultEntityBlacklist());
        loadIdSet(dir.resolve("hive_entity_whitelist.json"), ENTITY_WHITELIST, defaultEntityWhitelist());
        loadIdSet(dir.resolve("hive_block_whitelist.json"), BLOCK_WHITELIST, defaultBlockWhitelist());
        loaded = true;
    }

    public static void reload() {
        loaded = false;
        ENTITY_BLACKLIST.clear();
        ENTITY_WHITELIST.clear();
        BLOCK_WHITELIST.clear();
        ensureLoaded();
    }

    public static boolean isEntityBlacklisted(Identifier id) {
        ensureLoaded();
        return id != null && ENTITY_BLACKLIST.contains(id);
    }

    public static boolean isEntityWhitelisted(Identifier id) {
        ensureLoaded();
        return id != null && ENTITY_WHITELIST.contains(id);
    }

    public static boolean isHiveBlockAllowed(Identifier id) {
        ensureLoaded();
        if (id == null) return false;
        // hard block vanilla hives
        if (id.equals(Identifier.parse("minecraft:beehive")) || id.equals(Identifier.parse("minecraft:bee_nest"))) {
            return false;
        }
        if (id.equals(Identifier.parse("spd:golden_beehive"))) {
            return true;
        }
        return BLOCK_WHITELIST.contains(id);
    }

    /** Puede entrar al Golden Nest: bee vanilla, golden bee, o whitelist (si no blacklist). */
    public static boolean canEnterNest(Identifier entityId) {
        ensureLoaded();
        if (entityId == null) return false;
        if (ENTITY_BLACKLIST.contains(entityId)) return false;
        if (entityId.equals(Identifier.parse("minecraft:bee"))) return true;
        if (entityId.equals(Identifier.parse("spd:golden_bee"))) return true;
        return ENTITY_WHITELIST.contains(entityId);
    }

    private static JsonObject defaultEntityBlacklist() {
        JsonObject o = new JsonObject();
        o.addProperty("_comment", "Entity IDs that can NEVER enter a Golden Nest. Example: minecraft:creeper");
        JsonArray a = new JsonArray();
        a.add("minecraft:creeper");
        o.add("ids", a);
        return o;
    }

    private static JsonObject defaultEntityWhitelist() {
        JsonObject o = new JsonObject();
        o.addProperty("_comment", "Extra entity IDs allowed into Golden Nest (minecraft:bee and spd:golden_bee always allowed). Example: spd:golden_bee");
        JsonArray a = new JsonArray();
        a.add("spd:golden_bee");
        o.add("ids", a);
        return o;
    }

    private static JsonObject defaultBlockWhitelist() {
        JsonObject o = new JsonObject();
        o.addProperty("_comment", "Blocks Golden Bee may ENTER. spd:golden_beehive is always allowed. Vanilla beehive/bee_nest are hard-blocked in code even if listed.");
        JsonArray a = new JsonArray();
        a.add("spd:golden_beehive");
        o.add("ids", a);
        return o;
    }

    private static void loadIdSet(Path file, Set<Identifier> target, JsonObject defaults) {
        if (!Files.exists(file)) {
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(defaults, w);
            } catch (IOException ignored) {}
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj != null && obj.has("ids") && obj.get("ids").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("ids")) {
                    if (el.isJsonPrimitive()) {
                        try {
                            target.add(Identifier.parse(el.getAsString()));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
