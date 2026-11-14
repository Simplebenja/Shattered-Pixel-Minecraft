package com.drbenja.spd.config.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/spd/entity_classes.toml
 * Additive entity ids per SPD class. Hardcoded defaults in {@link com.drbenja.spd.entity.classy.EntityClassHelper}
 * always apply; tags ({@code #spd:<class>}) and this config are extra ways for modders/packs to opt in.
 */
public final class EntityClassesConfig {

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FIERY =
            list("fiery", "Fiery: immune fire/burning/blazing; vulnerable storm cloud & aqua brew.",
                    List.of("minecraft:blaze", "minecraft:magma_cube"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ICY =
            list("icy", "Icy: immune freeze damage, chilled, frozen.",
                    List.of("minecraft:snow_golem", "minecraft:stray"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ACIDIC =
            list("acidic", "Acidic: 50% corrosion resist; immune caustic ooze. (empty until first mob)",
                    List.of());
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ELECTRIC =
            list("electric", "Electric: 50% shocking/lightning resist. (empty until first mob)",
                    List.of());
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INORGANIC =
            list("inorganic", "Inorganic: immune bleeding, toxic gas, etc.",
                    List.of(
                            "minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton",
                            "minecraft:bogged", "minecraft:vex", "minecraft:iron_golem",
                            "minecraft:snow_golem", "minecraft:end_crystal", "minecraft:phantom",
                            "minecraft:ghast", "minecraft:blaze", "minecraft:creaking",
                            "minecraft:breeze", "minecraft:wither", "minecraft:armor_stand",
                            "minecraft:item_frame", "minecraft:glow_item_frame"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEMONIC =
            list("demonic", "Demonic: extra sacred damage later.",
                    List.of("minecraft:wither_skeleton", "minecraft:wither", "minecraft:vex"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IMMOVABLE =
            list("immovable", "Immovable: immune vertigo/ethereal chains/repulsion KB (future).",
                    List.of("minecraft:shulker", "minecraft:creaking"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STATIC =
            list("static", "Static: immune many control effects.",
                    List.of("minecraft:end_crystal"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINIBOSS =
            list("miniboss", "Miniboss: corruption immunities shared with boss.",
                    List.of("minecraft:elder_guardian", "minecraft:end_crystal"));
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOSS =
            list("boss", "Boss: miniboss immunities + Grim 50% + repulsion KB resist.",
                    List.of("minecraft:warden", "minecraft:wither", "minecraft:ender_dragon"));

    public static final ModConfigSpec SPEC = B.build();

    private static ModConfigSpec.ConfigValue<List<? extends String>> list(String path, String comment, List<String> def) {
        return B.comment(comment).defineListAllowEmpty(path, def, () -> "", o -> o instanceof String);
    }

    private static final ConcurrentHashMap<String, Set<String>> CACHE = new ConcurrentHashMap<>();

    private EntityClassesConfig() {}

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            reload();
        }
    }

    public static void reload() {
        put("fiery", FIERY);
        put("icy", ICY);
        put("acidic", ACIDIC);
        put("electric", ELECTRIC);
        put("inorganic", INORGANIC);
        put("demonic", DEMONIC);
        put("immovable", IMMOVABLE);
        put("static", STATIC);
        put("miniboss", MINIBOSS);
        put("boss", BOSS);
    }

    private static void put(String key, ModConfigSpec.ConfigValue<List<? extends String>> val) {
        try {
            Set<String> set = new HashSet<>();
            for (String s : val.get()) {
                if (s != null && !s.isBlank()) {
                    set.add(s.toLowerCase(Locale.ROOT));
                }
            }
            CACHE.put(key, set);
        } catch (Exception ignored) {
            CACHE.put(key, Set.of());
        }
    }

    public static Set<String> configIds(String classKey) {
        return CACHE.getOrDefault(classKey, Set.of());
    }
}
