package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Targeted / Aggression (SPD Stone of Aggression).
 * Mientras dura, enemigos cercanos (visión) priorizan a esta entidad como target.
 * Duración: 5 s hostil, 20 s aliado/uno mismo (aplicada por la piedra).
 */
public class AggressionEffect extends MobEffect {
    public static final int DURATION_HOSTILE_TICKS = 5 * 20;
    public static final int DURATION_ALLY_TICKS = 20 * 20;

    public AggressionEffect() {
        super(MobEffectCategory.HARMFUL, 0xcc4444);
    }
}
