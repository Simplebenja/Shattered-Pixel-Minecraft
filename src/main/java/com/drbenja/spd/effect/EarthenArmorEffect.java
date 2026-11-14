package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * Al aplicarse (beber / splash / lingering), convierte su duración en el intervalo
 * de cada escalón de Barkskin y calcula la potencia según max HP.
 */
public class EarthenArmorEffect extends MobEffect {

    public EarthenArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B5A2B);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        MobEffectInstance self = entity.getEffect(ModEffects.EARTHEN_ARMOR);
        int interval = BarkskinEffect.DEFAULT_INTERVAL_TICKS;
        if (self != null && self.getDuration() > 1) {
            interval = self.getDuration();
        }
        BarkskinEffect.applyFromEntityStats(entity, interval);
        entity.removeEffect(ModEffects.EARTHEN_ARMOR);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return false;
    }
}
