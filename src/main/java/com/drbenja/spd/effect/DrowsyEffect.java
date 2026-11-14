package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** After duration, enters Magical Sleep unless damaged. */
public class DrowsyEffect extends MobEffect {
    public DrowsyEffect() {
        super(MobEffectCategory.NEUTRAL, 0x8899aa);
    }
}
