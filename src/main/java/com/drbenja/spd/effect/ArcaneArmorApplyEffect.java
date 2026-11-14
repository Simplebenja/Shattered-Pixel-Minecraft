package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * Marcador del elixir: convierte su duración en el intervalo de cada escalón de Arcane Armor.
 */
public class ArcaneArmorApplyEffect extends MobEffect {

    public ArcaneArmorApplyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9966FF);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        MobEffectInstance self = entity.getEffect(ModEffects.ARCANE_ARMOR_APPLY);
        int interval = ArcaneArmorEffect.DEFAULT_INTERVAL_TICKS;
        if (self != null && self.getDuration() > 1) {
            interval = self.getDuration();
        }
        ArcaneArmorEffect.applyFromEntityStats(entity, interval);
        entity.removeEffect(ModEffects.ARCANE_ARMOR_APPLY);
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
