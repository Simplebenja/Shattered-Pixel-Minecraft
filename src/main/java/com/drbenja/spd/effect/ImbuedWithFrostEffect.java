package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD FrostImbue: inmunidad a Chill/Frozen/freeze; melee aplica Chilled.
 * amp 0 → 3 s chilled al golpear; amp 1 → 6 s.
 */
public class ImbuedWithFrostEffect extends MobEffect {

    public static final int CHILL_ON_HIT_BASE = 60;   // 3 s
    public static final int CHILL_ON_HIT_POWERED = 120; // 6 s

    public ImbuedWithFrostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88CCFF);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        entity.removeEffect(ModEffects.CHILLED);
        entity.removeEffect(ModEffects.FROZEN);
        ChilledEffect.clearModifier(entity);
        FrozenEffect.clearModifiers(entity);
        entity.setTicksFrozen(0);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return true;
    }

    public static int chillTicksForAmplifier(int amplifier) {
        return amplifier > 0 ? CHILL_ON_HIT_POWERED : CHILL_ON_HIT_BASE;
    }
}
