package com.drbenja.spd.config.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.entity.classy.EntityClassHelper;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/spd/chill_immune_entities.toml
 * Entidades inmunes a Chilled y Frozen (fuentes del mod).
 * minecraft:snow_golem es siempre inmune aunque no esté en la lista.
 */
public final class ChillImmuneEntitiesConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final String ALWAYS_IMMUNE = "minecraft:snow_golem";

    /** Defaults align with planned Icy class; snow_golem always immune in code. */
    private static final List<String> DEFAULT_EXAMPLE = List.of(
            ALWAYS_IMMUNE,
            "minecraft:stray"
            // future Icy: frostbite zombie (26.4 id TBD)
    );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> IMMUNE_ENTITIES =
            BUILDER.comment(
                            "Entity type ids immune to Chilled and Frozen from SPD frost sources.",
                            "Always immune even if removed: minecraft:snow_golem.",
                            "Defaults also list Icy-class examples (stray). Will merge into #spd:icy later.")
                    .defineListAllowEmpty("immuneEntities", DEFAULT_EXAMPLE, () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile Set<String> cached = new HashSet<>(DEFAULT_EXAMPLE);

    private ChillImmuneEntitiesConfig() {}

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloadCache();
        }
    }

    private static void reloadCache() {
        try {
            cached = new HashSet<>(IMMUNE_ENTITIES.get());
        } catch (Exception ignored) {
        }
    }

    public static boolean isChillImmune(LivingEntity entity) {
        // Icy / Static classes (tags + entity_classes.toml + hardcoded)
        if (EntityClassHelper.immuneToChillFrozen(entity)) {
            return true;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return false;
        }
        String s = id.toString();
        if (ALWAYS_IMMUNE.equals(s) || "minecraft:snow_golem".equals(s)) {
            return true;
        }
        return cached.contains(s);
    }
}
