package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Visual tiers for Kinetic conserved damage (SPD ConservedDamage tint breakpoints):
 *   &lt; 5  → low  (white/yellow)  kinetic_charge
 *   5–9  → mid  (orange)        kinetic_charge_mid
 *   ≥ 10 → high (red)           kinetic_charge_high
 *
 * Duration seconds = ceil(conserved damage). Amplifier unused (0).
 */
public class KineticChargeEffect extends MobEffect {

    public enum Tier {
        LOW,
        MID,
        HIGH
    }

    private final Tier tier;

    public KineticChargeEffect(Tier tier) {
        super(MobEffectCategory.BENEFICIAL, colorFor(tier));
        this.tier = tier;
    }

    /** No-arg kept only if something still references it; defaults to LOW. */
    public KineticChargeEffect() {
        this(Tier.LOW);
    }

    public Tier tier() {
        return tier;
    }

    private static int colorFor(Tier tier) {
        return switch (tier) {
            case LOW -> 0xFFFFCC;
            case MID -> 0xFFAA00;
            case HIGH -> 0xFF2200;
        };
    }

    /** SPD thresholds: ≥10 red, ≥5 orange, else pale. */
    public static Tier tierFor(float conserved) {
        if (conserved >= 10.0f) {
            return Tier.HIGH;
        }
        if (conserved >= 5.0f) {
            return Tier.MID;
        }
        return Tier.LOW;
    }
}
