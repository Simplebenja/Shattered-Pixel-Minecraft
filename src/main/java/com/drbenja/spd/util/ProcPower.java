package com.drbenja.spd.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.init.ModItems;

/**
 * Multiplier / shared proc formulas for SPD enchantments.
 * <p>
 * Offhand {@code ring_test} forces <b>proc chance = 100%</b> only.
 * It does <b>not</b> inflate {@link #multiplier} / damage scaling (powerMulti stays normal).
 */
public final class ProcPower {

    private ProcPower() {}

    /**
     * Damage / strength multiplier from rings/talents (normally 1.0).
     * ring_test does NOT change this — only chance().
     */
    public static float multiplier(LivingEntity beneficiary) {
        return 1.0f;
    }

    public static boolean hasRingTest(LivingEntity entity) {
        if (entity == null) return false;
        ItemStack off = entity.getOffhandItem();
        return !off.isEmpty() && off.is(ModItems.RING_TEST.get());
    }

    /** Proc chance after multiplier; ring_test → 1.0 (guaranteed). */
    public static float chance(LivingEntity beneficiary, float baseChance) {
        if (hasRingTest(beneficiary)) {
            return 1.0f;
        }
        return baseChance * multiplier(beneficiary);
    }

    public static float scale(LivingEntity beneficiary, float baseValue) {
        return baseValue * multiplier(beneficiary);
    }

    /**
     * Default weapon-enchant proc (Chilling style, SPD):
     * MC level 1 → 25%, 2 → 40%, 3 → 50%.
     * {@code enchantLevel} is Minecraft 1-based.
     */
    public static float standardWeaponProc(int enchantLevel) {
        int spdLvl = Math.max(0, enchantLevel - 1);
        return (spdLvl + 1f) / (spdLvl + 4f);
    }

    /** Standard weapon proc already scaled by {@link #chance}. */
    public static float standardWeaponProc(LivingEntity beneficiary, int enchantLevel) {
        return chance(beneficiary, standardWeaponProc(enchantLevel));
    }

    /**
     * SPD powerMulti = max(1, procChance).
     * With ring_test, chance is 1.0 → powerMulti stays 1 (no damage inflation).
     */
    public static float powerMulti(LivingEntity beneficiary, float procChance) {
        return Math.max(1f, procChance);
    }
}
