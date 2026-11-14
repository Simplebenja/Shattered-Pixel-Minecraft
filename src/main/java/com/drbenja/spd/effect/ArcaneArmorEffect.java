package com.drbenja.spd.effect;

import com.drbenja.spd.util.SpdProgression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD ArcaneArmor: reduce daño mágico de forma aleatoria 0..nivel.
 * Cascada de potencia igual que Barkskin (forceAddEffect ~5 ticks antes de expirar).
 * Intervalo por escalón: duración de la fuente (80 s por defecto del elixir).
 */
public class ArcaneArmorEffect extends MobEffect {

    public static final int DEFAULT_INTERVAL_TICKS = 80 * 20;
    public static final int CASCADE_LEAD_TICKS = 5;
    private static final String KEY_INTERVAL = "spd_arcane_armor_interval";

    public ArcaneArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9966FF);
    }

    /**
     * SPD: 5 + lvl/2. MC: lvl ≈ (maxHP - 15) / 5 → 5 + ((maxHP - 15) / 5) / 2
     */
    public static int computeProtection(LivingEntity entity) {
        return SpdProgression.arcaneArmorLevel(entity);
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
        MobEffectInstance cur = entity.getEffect(ModEffects.ARCANE_ARMOR);
        if (cur != null) {
            // SPD: prefiere mayor valor / menor intervalo vía comparación; aquí max potencia
            amp = Math.max(amp, cur.getAmplifier());
        }
        storeInterval(entity, interval);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.ARCANE_ARMOR, interval, amp, false, true, true), null);
    }

    public static void applyFromEntityStats(LivingEntity entity, int intervalTicks) {
        applyOrStack(entity, computeProtection(entity), intervalTicks);
    }

    /** Nivel actual de protección (máximo del rango aleatorio). */
    public static int currentLevel(LivingEntity entity) {
        MobEffectInstance inst = entity.getEffect(ModEffects.ARCANE_ARMOR);
        if (inst == null) {
            return 0;
        }
        return inst.getAmplifier() + 1;
    }

    /** Reducción aleatoria uniforme en [0, nivel]. */
    public static int rollReduction(LivingEntity entity) {
        int level = currentLevel(entity);
        if (level <= 0) {
            return 0;
        }
        return entity.getRandom().nextInt(level + 1);
    }

    private static void forceUpdate(LivingEntity entity, int duration, int amplifier) {
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.ARCANE_ARMOR, duration, amplifier, false, true, true), null);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration <= CASCADE_LEAD_TICKS;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        MobEffectInstance inst = entity.getEffect(ModEffects.ARCANE_ARMOR);
        if (inst == null) {
            return false;
        }
        int duration = inst.getDuration();
        if (duration <= 0 || duration > CASCADE_LEAD_TICKS) {
            return true;
        }
        if (amplifier <= 0) {
            return false;
        }
        int interval = getStoredInterval(entity);
        forceUpdate(entity, interval, amplifier - 1);
        return true;
    }
}
