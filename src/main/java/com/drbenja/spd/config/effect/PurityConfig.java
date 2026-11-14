package com.drbenja.spd.config.effect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/spd/purity.toml
 * Damage types and effects blocked by Purification Barrier.
 * Hard defaults match current gameplay; lists are extensible.
 */
public final class PurityConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_DAMAGE_TYPES =
            BUILDER.comment(
                            "Damage type ids blocked while Purification Barrier is active.",
                            "Also blocked via #spd:purification_blocked tag when present.")
                    .defineListAllowEmpty(
                            "blockedDamageTypes",
                            List.of(
                                    // Environmental / status-like damage Purification Barrier blocks by default
                                    "minecraft:on_fire",
                                    "minecraft:in_fire",
                                    "minecraft:hot_floor",
                                    "minecraft:campfire",
                                    "minecraft:freeze",
                                    "minecraft:lightning_bolt",
                                    "minecraft:drown",
                                    "minecraft:stalagmite",
                                    "minecraft:sweet_berry_bush",
                                    "minecraft:cactus"),
                            () -> "",
                            o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> NEVER_BLOCKED_DAMAGE_TYPES =
            BUILDER.comment(
                            "Damage type ids that Purification Barrier must never block",
                            "(examples: lava, fall, player/mob attacks).")
                    .defineListAllowEmpty(
                            "neverBlockedDamageTypes",
                            List.of(
                                    "minecraft:lava",
                                    "minecraft:fall",
                                    "minecraft:fly_into_wall",
                                    "minecraft:player_attack",
                                    "minecraft:mob_attack",
                                    "minecraft:arrow",
                                    "minecraft:trident",
                                    "minecraft:explosion",
                                    "minecraft:player_explosion"),
                            () -> "",
                            o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_ENVIRONMENTAL_EFFECTS =
            BUILDER.comment(
                            "Extra harmful effect ids blocked when the source is splash/lingering/environmental.",
                            "Empty = all harmful effects from those sources are blocked.")
                    .defineListAllowEmpty("blockedEnvironmentalEffects", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile Set<String> blockedDamage = Set.of();
    private static volatile Set<String> neverDamage = Set.of();
    private static volatile Set<String> blockedEffects = Set.of();

    private PurityConfig() {}

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloadCache();
        }
    }

    private static void reloadCache() {
        try {
            blockedDamage = new HashSet<>(BLOCKED_DAMAGE_TYPES.get());
            neverDamage = new HashSet<>(NEVER_BLOCKED_DAMAGE_TYPES.get());
            blockedEffects = new HashSet<>(BLOCKED_ENVIRONMENTAL_EFFECTS.get());
        } catch (Exception ignored) {
        }
    }

    public static boolean isDamageBlocked(DamageSource source) {
        var key = source.typeHolder().unwrapKey();
        if (key.isEmpty()) {
            return false;
        }
        String id = key.get().identifier().toString();
        if (neverDamage.contains(id)) {
            return false;
        }
        return blockedDamage.contains(id);
    }

    public static boolean isEnvironmentalEffectBlocked(MobEffect effect) {
        if (blockedEffects.isEmpty()) {
            return true; // default: all harmful handled by category in handler
        }
        Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id != null && blockedEffects.contains(id.toString());
    }
}
