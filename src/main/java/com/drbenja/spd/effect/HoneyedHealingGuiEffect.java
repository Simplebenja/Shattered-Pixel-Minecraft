package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Solo visual + particulas de honeyed_healing. */
public class HoneyedHealingGuiEffect extends MobEffect {
    public HoneyedHealingGuiEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFCC00);
    }
}
