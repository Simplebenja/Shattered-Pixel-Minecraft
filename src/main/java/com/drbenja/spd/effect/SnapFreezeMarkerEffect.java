package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Solo para tooltip de la poción Snap Freeze ("Frozen (00:10)").
 * No tiene lógica: el handler aplica {@link FrozenEffect} real en 3×3×3
 * y cualquier instancia de este marcador se elimina al aplicar.
 */
public class SnapFreezeMarkerEffect extends MobEffect {

    public SnapFreezeMarkerEffect() {
        super(MobEffectCategory.HARMFUL, 0xB2D6FF);
    }
}
