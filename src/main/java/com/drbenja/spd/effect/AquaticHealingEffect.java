package com.drbenja.spd.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

import com.drbenja.spd.init.ModEffects;

/**
 * Aquatic Healing
 *
 * Pool: HT×1.5 × (amp+1) × timeFactor (MAX 4)
 *   → potenciadas (duración corta) mantienen pool similar a la básica
 *   → extendidas (duración larga) tienen más pool
 *
 * Ritmo: (HT/50) × (amp+1)² × min(1, 3600/ticksIniciales)
 *   → potenciadas regeneran más rápido; extendidas no bajan del ritmo base
 */
public class AquaticHealingEffect extends MobEffect {

    public static final String KEY_LEFT = "spd_aquatic_healing_left";
    public static final String KEY_JUST_CONVERTED = "spd_aquatic_just_converted";
    public static final String KEY_INCOMING_DURATION = "spd_aquatic_incoming_duration";
    public static final String KEY_INCOMING_AMP = "spd_aquatic_incoming_amp";
    public static final String KEY_RATE_MULT = "spd_aquatic_rate_mult";

    public static final int BASE_DURATION_TICKS = 3600;

    public AquaticHealingEffect() {
        super(MobEffectCategory.BENEFICIAL, -10079233);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }

        CompoundTag data = entity.getPersistentData();

        if (data.getBoolean(KEY_JUST_CONVERTED).orElse(false)) {
            data.remove(KEY_JUST_CONVERTED);
            float left = data.getFloat(KEY_LEFT).orElse(0f);
            if (left >= 1f) {
                applyVisual(entity, left, amplifier);
            }
            return;
        }

        int incomingDuration = data.contains(KEY_INCOMING_DURATION)
                ? data.getInt(KEY_INCOMING_DURATION).orElse(-1)
                : -1;
        int incomingAmp = data.contains(KEY_INCOMING_AMP)
                ? data.getInt(KEY_INCOMING_AMP).orElse(amplifier)
                : amplifier;

        data.remove(KEY_INCOMING_DURATION);
        data.remove(KEY_INCOMING_AMP);

        MobEffectInstance current = entity.getEffect(ModEffects.AQUATIC_HEALING);
        int oldAmp = current != null ? current.getAmplifier() : -1;
        int finalAmp = Math.max(oldAmp, incomingAmp);

        int duration = incomingDuration;
        if (duration == -1 && current != null) {
            duration = current.getDuration();
        }

        // Pool: timeFactor MAX 4; amp como (potencia+1)
        float timeFactor = poolFactorFromDuration(duration);
        float ampMult = finalAmp + 1;
        float base = entity.getMaxHealth() * 1.5f;
        float grant = Math.max(1f, base * ampMult * timeFactor);

        // Ritmo: (amp+1)^2 * min(1, 3600/ticks)
        float rateMult = rateMultFromDuration(duration);
        float ampRate = ampMult * ampMult; // (potencia+1)^2

        float existing = data.getFloat(KEY_LEFT).orElse(0f);
        boolean hasPool = existing >= 1f;

        float pool;
        float factorForMsg = timeFactor;

        if (!hasPool) {
            pool = grant;
        } else if (duration < 0) {
            pool = existing;
            factorForMsg = base > 0 ? existing / (base * ampMult) : 1f;
        } else if (grant <= existing) {
            pool = existing;
            factorForMsg = base > 0 ? existing / (base * ampMult) : 1f;
        } else {
            pool = grant;
        }

        data.putFloat(KEY_LEFT, pool);
        data.putFloat(KEY_RATE_MULT, rateMult * ampRate);

        applyVisual(entity, pool, finalAmp);
        msg(entity, pool, factorForMsg, finalAmp, base * ampMult, rateMult * ampRate);

        data.putBoolean(KEY_JUST_CONVERTED, true);
        entity.removeEffect(ModEffects.AQUATIC_HEALING);
        entity.addEffect(new MobEffectInstance(
                ModEffects.AQUATIC_HEALING,
                MobEffectInstance.INFINITE_DURATION,
                finalAmp,
                false,
                false,
                false
        ));
    }
/** MAX 4 */
    public static float poolFactorFromDuration(int duration) {
        if (duration < 0) {
            return 1.0f;
        }
        return Math.max(0.01f, Math.min(4.0f, (float) duration / (float) BASE_DURATION_TICKS));
    }

    /** min(1, 3600/ticks) — extendidas más lentas, cortas no superan 1 por el tiempo */
    public static float rateMultFromDuration(int duration) {
        if (duration < 0) {
            return 1.0f;
        }
        float r = (float) BASE_DURATION_TICKS / (float) Math.max(1, duration);
        return Math.min(1.0f, r);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();
        float left = data.getFloat(KEY_LEFT).orElse(0f);

        if (left < 1f) {
            endEffect(entity, data);
            return true;
        }

        if (entity.tickCount % 20 != 0) {
            return true;
        }

        boolean inWaterOrRain = entity.isInWater()
                || entity.isInWaterOrRain()
                || level.isRainingAt(entity.blockPosition())
                || level.isRainingAt(entity.blockPosition().above());

        float maxHp = entity.getMaxHealth();
        float currentHp = entity.getHealth();

        if (inWaterOrRain && currentHp < maxHp) {
            float rateStored = data.getFloat(KEY_RATE_MULT).orElse(1f);
            float baseHeal = maxHp / 50.0f;
            float healAmt = baseHeal * rateStored;
            healAmt = Math.max(1.0f, healAmt);
            healAmt = Math.min(healAmt, left);
            healAmt = Math.min(healAmt, maxHp - currentHp);

            if (healAmt > 0f) {
                entity.heal(healAmt);
                left -= healAmt;
                data.putFloat(KEY_LEFT, left);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
                    // player.sendSystemMessage(Component.literal(
                            // "§bAquatic Healing§r: restante §a" + round1(left)
                                    // + "§r (curó §a" + round1(healAmt) + "§r)"
                    // ));
        // }
            }
        }

        if (left < 1f) {
            endEffect(entity, data);
            return true;
        }

        applyVisual(entity, left, amplifier);
        return true;
    }

    public static void applyVisual(LivingEntity entity, float left, int amplifier) {
        int visualTicks = Math.max(20, (Math.round(left) + 1) * 20);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.AQUATIC_HEALING_GUI,
                visualTicks,
                amplifier,
                false,
                true,
                true
        ), null);
    }

    private void endEffect(LivingEntity entity, CompoundTag data) {
        data.remove(KEY_LEFT);
        data.remove(KEY_JUST_CONVERTED);
        data.remove(KEY_INCOMING_DURATION);
        data.remove(KEY_INCOMING_AMP);
        data.remove(KEY_RATE_MULT);
        entity.removeEffect(ModEffects.AQUATIC_HEALING);
        entity.removeEffect(ModEffects.AQUATIC_HEALING_GUI);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§bAquatic Healing§r: §cpool agotado§r"
            // ));
        // }
    }

    private void msg(LivingEntity entity, float pool, float factor, int amplifier, float fullCap, float rate) {
        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§bAquatic Healing§r: pool §a" + round1(pool)
                            // + "§r / tope §a" + round1(fullCap)
                            // + "§r | dosis §e" + Math.round(factor * 100) + "%"
                            // + "§r | nivel §e" + (amplifier + 1)
                            // + "§r | ritmo× §e" + round1(rate) + "§r"
            // ));
        // }
    }

    private static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }
}
