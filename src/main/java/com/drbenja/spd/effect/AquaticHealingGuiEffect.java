package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Solo visual + particulas (textura aquatic_healing_gui.png = copia de aquatic_healing). */
public class AquaticHealingGuiEffect extends MobEffect {
    public AquaticHealingGuiEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF00A3FF);
    }
}
