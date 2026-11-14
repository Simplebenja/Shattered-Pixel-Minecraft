package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Visual-only: duration seconds ≈ remaining herbal armor pool. */
public class HerbalArmorGuiEffect extends MobEffect {
    public HerbalArmorGuiEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88AA44);
    }
}
