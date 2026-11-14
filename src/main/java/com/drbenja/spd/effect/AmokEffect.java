package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Attacks nearby creatures including other mobs. */
public class AmokEffect extends MobEffect {
    public AmokEffect() {
        super(MobEffectCategory.HARMFUL, 0xcc2222);
    }
}
