package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Applied to bosses instead of Corrupted.
 * Takes +67% damage from all sources (final incoming amount × 1.67).
 */
public class DoomedEffect extends MobEffect {

    public static final float DAMAGE_MULTIPLIER = 1.67f;

    public DoomedEffect() {
        super(MobEffectCategory.HARMFUL, 0x2A0040);
    }
}
