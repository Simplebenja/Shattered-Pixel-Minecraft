package com.drbenja.spd.effect;

import com.drbenja.spd.util.SpdProgression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD Barkskin: reducción física = amplifier + 1.
 * Cada escalón dura el intervalo guardado (p. ej. 50 s).
 * Unos ticks antes de expirar se fuerza la bajada de potencia con forceAddEffect
 * (evita perder la cascada al expirar del todo).
 */
public class BarkskinEffect extends MobEffect {

    public static final int DEFAULT_INTERVAL_TICKS = 50 * 20;
    /** Forzar cascada cuando queden este número de ticks (antes de que el juego borre el efecto). */
    public static final int CASCADE_LEAD_TICKS = 5;
    private static final String KEY_INTERVAL = "spd_barkskin_interval";

    public BarkskinEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B5A2B);
    }

    public static int computeProtection(LivingEntity entity) {
        return SpdProgression.barkskinLevel(entity);
    }

    public static void storeInterval(LivingEntity entity, int ticks) {
        entity.getPersistentData().putInt(KEY_INTERVAL, Math.max(CASCADE_LEAD_TICKS + 1, ticks));
    }

    public static int getStoredInterval(LivingEntity entity) {
        int v = entity.getPersistentData().getIntOr(KEY_INTERVAL, DEFAULT_INTERVAL_TICKS);
        return v > 0 ? v : DEFAULT_INTERVAL_TICKS;
    }

    public static void applyOrStack(LivingEntity entity, int protection, int intervalTicks) {
        int protectionClamped = Math.max(1, protection);
        int amp = protectionClamped - 1;
        int interval = Math.max(CASCADE_LEAD_TICKS + 1, intervalTicks);
        MobEffectInstance cur = entity.getEffect(ModEffects.BARKSKIN);
        if (cur != null) {
            amp = Math.max(amp, cur.getAmplifier());
        }
        storeInterval(entity, interval);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.BARKSKIN, interval, amp, false, true, true), null);
    }

    public static void applyFromEntityStats(LivingEntity entity, int intervalTicks) {
        applyOrStack(entity, computeProtection(entity), intervalTicks);
    }

    public static int currentReduction(LivingEntity entity) {
        MobEffectInstance inst = entity.getEffect(ModEffects.BARKSKIN);
        if (inst == null) {
            return 0;
        }
        return inst.getAmplifier() + 1;
    }

    private static void forceUpdate(LivingEntity entity, int duration, int amplifier) {
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.BARKSKIN, duration, amplifier, false, true, true), null);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Solo cuando estamos en la ventana previa a expirar
        return duration > 0 && duration <= CASCADE_LEAD_TICKS;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        MobEffectInstance inst = entity.getEffect(ModEffects.BARKSKIN);
        if (inst == null) {
            return false;
        }
        int duration = inst.getDuration();
        if (duration <= 0 || duration > CASCADE_LEAD_TICKS) {
            return true;
        }

        // Últimos ticks del escalón actual
        if (amplifier <= 0) {
            // Potencia I: dejar que expire de forma natural
            return false;
        }

        int interval = getStoredInterval(entity);
        // Reaplicar potencia inferior con la duración inicial completa
        forceUpdate(entity, interval, amplifier - 1);
        return true;
    }
}
