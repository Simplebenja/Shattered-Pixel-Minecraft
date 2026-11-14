package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.event.effect.ImmobilizeHandlers;
import com.drbenja.spd.init.ModEffects;

/**
 * SPD Paralysis: shared immobilize speed lock via ImmobilizeHandlers.
 * Fire does not clear it; damage reduces duration.
 */
public class ParalyzedEffect extends MobEffect {

    public static final Identifier SPEED_MOD_ID = ImmobilizeHandlers.SPEED_MOD_ID;
    public static final int DEFAULT_DURATION_TICKS = 200;
    public static final int TICKS_PER_DAMAGE = 10;

    public ParalyzedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFDB65);
    }

    public static void clearModifiers(LivingEntity entity) {
        ImmobilizeHandlers.clearModifiers(entity);
    }

    public static void applyModifiers(LivingEntity entity) {
        ImmobilizeHandlers.applySpeedLock(entity);
    }

    public static void reduceByDamage(LivingEntity entity, float amount) {
        MobEffectInstance inst = entity.getEffect(ModEffects.PARALYZED);
        if (inst == null || amount <= 0) {
            return;
        }
        int reduce = Math.max(1, Math.round(amount * TICKS_PER_DAMAGE));
        int next = inst.getDuration() - reduce;
        if (next <= 0) {
            entity.removeEffect(ModEffects.PARALYZED);
            clearModifiers(entity);
        } else {
            entity.addEffect(new MobEffectInstance(
                    ModEffects.PARALYZED, next, inst.getAmplifier(),
                    inst.isAmbient(), inst.isVisible(), inst.showIcon()));
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return true;
    }
}
