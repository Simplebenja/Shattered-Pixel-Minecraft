package com.drbenja.spd.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Central SPD-style progression math for Minecraft.
 * <p>
 * <b>All</b> hero-level, depth/floor, and related scaling in potions, effects,
 * and enchantments should go through this class (or {@link EnchantLevels} for
 * enchantment 1→0 conversion, {@link ArmorEnchantHelper} for reading armor
 * enchant levels). When real hero levels / dungeon floors ship, change the
 * implementations here once — call sites need not be touched.
 * <p>
 * Hero level mapping: SPD starts HT 20 at level 1 and gains +5 HT per level.
 * {@code level ≈ (maxHP - 15) / 5}, so vanilla 20 HP → level 1.
 */
public final class SpdProgression {

    private SpdProgression() {}

    /**
     * Approximate SPD hero level from max health (integer, minimum 1).
     * Vanilla player (20 HP) → 1; +5 max HP ≈ +1 level.
     */
    public static int heroLevel(LivingEntity entity) {
        return Math.max(1, Math.round(heroLevelExact(entity)));
    }

    public static int heroLevel(Player player) {
        return heroLevel((LivingEntity) player);
    }

    /**
     * Same mapping as {@link #heroLevel} but as float (for proc rates that use
     * {@code (maxHp-15)/5} without rounding). Minimum 1.
     */
    public static float heroLevelExact(LivingEntity entity) {
        float maxHp = entity.getMaxHealth();
        return Math.max(1f, (maxHp - 15f) / 5f);
    }

    /**
     * Current dungeon depth / floor. Always 1 until floors are implemented.
     * Prefer {@link #depth(LivingEntity)} so entity context is available later.
     */
    public static int depth(LivingEntity entity) {
        return 1;
    }

    public static int depth() {
        return 1;
    }

    /** SPD {@code scalingDepth()} — same as {@link #depth} until floors exist. */
    public static int scalingDepth(LivingEntity entity) {
        return depth(entity);
    }

    // --- Common SPD-derived formulas (single place to tune) ---

    /** Potion of Earthen Armor / Barkskin initial level: {@code 2 + heroLevel/3}. */
    public static int barkskinLevel(LivingEntity entity) {
        return 2 + heroLevel(entity) / 3;
    }

    /** Elixir of Arcane Armor initial level: {@code 5 + heroLevel/2}. */
    public static int arcaneArmorLevel(LivingEntity entity) {
        return 5 + heroLevel(entity) / 2;
    }

    /**
     * Blocking enchant proc chance (SPD):
     * {@code (heroLevelExact + 0) related: ((maxHp-15)/5 + 1) / ((maxHp-15)/5 + 20)}.
     */
    public static float blockingProcChance(LivingEntity attacker) {
        float t = heroLevelExact(attacker);
        // heroLevelExact is already (maxHp-15)/5 with floor 1; SPD used t+1 over t+20
        // with t = (maxHp-15)/5 allowing 0 at low HP — we keep t>=1 so:
        // (t + 0) wait: original was ((maxHp-15)/5 + 1) / ((maxHp-15)/5 + 20)
        // = (t + 1) / (t + 20) where t = (maxHp-15)/5 without max(1,...)
        float raw = (attacker.getMaxHealth() - 15f) / 5f;
        return (raw + 1f) / (raw + 20f);
    }

    /** Herbal Armor (Entanglement) max block per hit: {@code (depth + 5) / 2}. */
    public static float herbalArmorMaxBlock(LivingEntity entity) {
        return (depth(entity) + 5) / 2f;
    }

    /** Toxic-style DoT: {@code 1 + depth/5}. */
    public static float toxicGasDamage(LivingEntity entity) {
        return 1.0F + depth(entity) / 5.0F;
    }

    /** Storm Cloud damage to Fiery: {@code 1 + depth/5}. */
    public static float stormCloudFieryDamage(LivingEntity entity) {
        return 1.0F + depth(entity) / 5.0F;
    }

    /** Aqua Brew damage to Fiery: random in {@code [5+depth, 10+2*depth]}. */
    public static float aquaBrewFieryDamage(LivingEntity entity, net.minecraft.util.RandomSource random) {
        int d = depth(entity);
        float min = 5.0F + d;
        float max = 10.0F + 2.0F * d;
        return min + random.nextFloat() * (max - min);
    }

    /**
     * Piso de boss de la mazmorra SPD (placeholder hasta implementar dungeons).
     * Cuando exista, devolver true en habitaciones/pisos de boss.
     */
    public static boolean isBossFloor(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        // TODO: dimension + floor data
        return false;
    }
}
