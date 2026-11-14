package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Terror that causes despawn when far from the player. */
public class DreadEffect extends MobEffect {
    public DreadEffect() {
        super(MobEffectCategory.HARMFUL, 0x1a0a0a);
    }
}
