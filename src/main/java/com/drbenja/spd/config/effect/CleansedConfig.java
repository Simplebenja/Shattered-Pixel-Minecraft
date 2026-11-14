package com.drbenja.spd.config.effect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/spd/cleansed.toml
 * Effects removed / blocked by Cleansed.
 * Corrupted and Doomed are always kept (cannot be cleansed).
 */
public final class CleansedConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_REMOVE =
            BUILDER.comment(
                            "Extra effect ids always removed even if not HARMFUL (mis-typed negatives).",
                            "All MobEffectCategory.HARMFUL are removed by default.")
                    .defineListAllowEmpty(
                            "alwaysRemove",
                            List.of(
                                    "minecraft:bad_omen",
                                    "minecraft:trial_omen",
                                    "minecraft:raid_omen"),
                            () -> "",
                            o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> NEVER_REMOVE =
            BUILDER.comment(
                            "Effect ids Cleansed must never remove or block.",
                            "spd:corrupted, spd:doomed and spd:deferred_damage are always kept.")
                    .defineListAllowEmpty(
                            "neverRemove",
                            List.of(
                                    "spd:corrupted",
                                    "spd:doomed",
                                    "spd:deferred_damage"),
                            () -> "",
                            o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile Set<String> alwaysRemove = Set.of();
    private static volatile Set<String> neverRemove = Set.of();

    private CleansedConfig() {}

    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloadCache();
        }
    }

    private static void reloadCache() {
        try {
            alwaysRemove = new HashSet<>(ALWAYS_REMOVE.get());
            neverRemove = new HashSet<>(NEVER_REMOVE.get());
            neverRemove.add("spd:corrupted");
            neverRemove.add("spd:doomed");
            neverRemove.add("spd:deferred_damage");
        } catch (Exception ignored) {
            neverRemove = Set.of("spd:corrupted", "spd:doomed", "spd:deferred_damage");
        }
    }

    public static boolean isNeverRemove(Holder<MobEffect> holder) {
        Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
        String key = id != null ? id.toString() : "";
        return neverRemove.contains(key)
                || "spd:corrupted".equals(key)
                || "spd:doomed".equals(key)
                || "spd:deferred_damage".equals(key);
    }

    public static boolean isAlwaysRemove(Holder<MobEffect> holder) {
        Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
        return id != null && alwaysRemove.contains(id.toString());
    }

    public static boolean shouldAffect(Holder<MobEffect> holder) {
        if (isNeverRemove(holder)) {
            return false;
        }
        if (isAlwaysRemove(holder)) {
            return true;
        }
        return holder.value().getCategory() == MobEffectCategory.HARMFUL;
    }
}
