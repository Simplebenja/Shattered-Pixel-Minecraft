package com.drbenja.spd.config.effect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/spd/imbued_with_frost.toml
 * Efectos y tipos de daño bloqueados mientras Imbued with Frost está activo.
 * Chilled, Frozen y freeze vanilla siempre se bloquean aunque se borren de la lista.
 */
public final class ImbuedWithFrostConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * Always blocked in code even if removed from this list.
     * Listed here as documented examples for pack makers / modders.
     */
    public static final List<String> HARDCODED_BLOCKED_EFFECTS = List.of(
            "spd:chilled",
            "spd:frozen"
    );

    public static final List<String> HARDCODED_BLOCKED_DAMAGE = List.of(
            "minecraft:freeze"
    );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_EFFECTS =
            BUILDER.comment(
                            "Mob effect ids blocked while Imbued with Frost is active.",
                            "Always blocked (even if removed): spd:chilled, spd:frozen.",
                            "Examples / defaults below — add mod effects as needed.")
                    .defineListAllowEmpty(
                            "blockedEffects",
                            List.of(
                                    "spd:chilled",
                                    "spd:frozen"
                                    // pack makers may add e.g. "othermod:frostbite"
                            ),
                            () -> "",
                            o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_DAMAGE_TYPES =
            BUILDER.comment(
                            "Damage type ids blocked while Imbued with Frost is active.",
                            "Always blocked (even if removed): minecraft:freeze (powder snow / freeze).",
                            "Examples of related cold damage you may also want to block:")
                    .defineListAllowEmpty(
                            "blockedDamageTypes",
                            List.of(
                                    "minecraft:freeze"
                                    // optional extras pack makers can keep:
                                    // "minecraft:sweet_berry_bush" is NOT cold — do not add by mistake
                            ),
                            () -> "",
                            o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile Set<String> cachedEffects = Set.of();
    private static volatile Set<String> cachedDamage = Set.of();

    private ImbuedWithFrostConfig() {}

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloadCache();
        }
    }

    private static void reloadCache() {
        try {
            cachedEffects = new HashSet<>(BLOCKED_EFFECTS.get());
            cachedDamage = new HashSet<>(BLOCKED_DAMAGE_TYPES.get());
        } catch (Exception ignored) {
        }
    }

    public static boolean isExtraBlockedEffect(Identifier effectId) {
        String s = effectId.toString();
        if (HARDCODED_BLOCKED_EFFECTS.contains(s)) {
            return true;
        }
        return cachedEffects.contains(s);
    }

    public static boolean isFreezeOrBlockedDamage(DamageSource source) {
        var key = source.typeHolder().unwrapKey();
        if (key.isEmpty()) {
            return false;
        }
        Identifier id = key.get().identifier();
        String s = id.toString();
        if (HARDCODED_BLOCKED_DAMAGE.contains(s) || id.getPath().contains("freeze")) {
            return true;
        }
        return cachedDamage.contains(s);
    }
}
