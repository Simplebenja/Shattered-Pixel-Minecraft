package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** SPD Weakness — reduced outgoing damage. */
public class WeaknessSpdEffect extends MobEffect {
    public WeaknessSpdEffect() {
        super(MobEffectCategory.HARMFUL, 0x666666);
    }
}
