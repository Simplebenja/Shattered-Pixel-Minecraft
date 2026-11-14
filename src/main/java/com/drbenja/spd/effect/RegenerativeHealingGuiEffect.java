package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Solo visual + particulas de regenerative_healing. */
public class RegenerativeHealingGuiEffect extends MobEffect {
    public RegenerativeHealingGuiEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF33AACC);
    }
}
