package com.drbenja.spd.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.drbenja.spd.init.ModEffects;

/**
 * Barrier (Potion of Shielding) — Opción B + decay SPD
 *
 * Pool: (0.6×HT+10) × min(amp+1, timeFactor×(amp+1)²)
 *   → II Ext máx ×2; III (corta) ×3; no deja II Ext > III en pool
 *
 * Decay estilo SPD (sobre pool RESTANTE, no la inicial):
 *   min(1, pool/20) × rateMult(tiempo)
 *   → al bajar de 20 el decay se ralentiza, como en SPD.
 *
 * Si la absorción sube (manzana dorada, etc.):
 *   abs = pool + absActual  (pool no cambia)
 *   max abs se sube si hace falta.
 */
public class BarrierEffect extends MobEffect {

    public static final String KEY_LEFT = "spd_barrier_left";
    public static final String KEY_JUST_CONVERTED = "spd_barrier_just_converted";
    public static final String KEY_INCOMING_DURATION = "spd_barrier_incoming_duration";
    public static final String KEY_INCOMING_AMP = "spd_barrier_incoming_amp";
    public static final String KEY_RATE_MULT = "spd_barrier_rate_mult";
    public static final String KEY_PARTIAL = "spd_barrier_partial";
    public static final String KEY_INIT_DUR = "spd_barrier_init_dur";
    public static final String KEY_LAST_ABS = "spd_barrier_last_abs";
    public static final String KEY_OUR_MAX = "spd_barrier_our_max";

    public static final int BASE_DURATION_TICKS = 3600;

    private static final Identifier MAX_ABS_MOD_ID =
            Identifier.fromNamespaceAndPath("spd", "barrier_max_absorption");

    public BarrierEffect() {
        super(MobEffectCategory.BENEFICIAL, -3355393);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }

        CompoundTag data = entity.getPersistentData();

        if (data.getBoolean(KEY_JUST_CONVERTED).orElse(false)) {
            data.remove(KEY_JUST_CONVERTED);
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

        MobEffectInstance current = entity.getEffect(ModEffects.BARRIER);
        int oldAmp = current != null ? current.getAmplifier() : -1;
        int finalAmp = Math.max(oldAmp, incomingAmp);

        int duration = incomingDuration;
        if (duration <= 0 && current != null) {
            duration = current.getDuration();
        }
        if (duration <= 0) {
            duration = BASE_DURATION_TICKS;
        }

        float timeFactor = factorFromDuration(duration);
        // mult = min(amp+1, timeFactor × (amp+1)²) → II Ext máx ×2, III ×3
        float ampPlus = finalAmp + 1;
        float rawMult = timeFactor * ampPlus * ampPlus;
        float mult = Math.min(ampPlus, rawMult);
        float basePool = 0.6f * entity.getMaxHealth() + 10.0f;
        float grant = Math.max(1f, basePool * mult);
        float rateMult = rateMultFromDuration(duration);

        float existing = data.getFloat(KEY_LEFT).orElse(0f);
        boolean hasPool = existing >= 1f;

        float pool;
        if (!hasPool) {
            pool = grant;
        } else if (incomingDuration < 0) {
            pool = existing;
        } else if (grant <= existing) {
            pool = existing;
        } else {
            pool = grant;
        }

        float n = entity.getAbsorptionAmount();

        data.putFloat(KEY_LEFT, pool);
        data.putFloat(KEY_RATE_MULT, rateMult);
        data.putFloat(KEY_PARTIAL, 0f);
        data.putInt(KEY_INIT_DUR, duration);
        data.putFloat(KEY_OUR_MAX, pool);

        // max y abs = lo existente + nuestra pool
        ensureMaxAtLeast(entity, n + pool);
        entity.setAbsorptionAmount(n + pool);
        data.putFloat(KEY_LAST_ABS, entity.getAbsorptionAmount());

        msgStart(entity, pool, timeFactor, finalAmp, rateMult, duration, basePool, grant, n,
                entity.getAbsorptionAmount());

        data.putBoolean(KEY_JUST_CONVERTED, true);
        entity.removeEffect(ModEffects.BARRIER);
        entity.addEffect(new MobEffectInstance(
                ModEffects.BARRIER,
                MobEffectInstance.INFINITE_DURATION,
                finalAmp,
                false,
                true,
                true
        ));
    }
public static float factorFromDuration(int duration) {
        if (duration < 0 || duration >= BASE_DURATION_TICKS) {
            return 1.0f;
        }
        return Math.max(0.01f, Math.min(1.0f, (float) duration / (float) BASE_DURATION_TICKS));
    }

    public static float rateMultFromDuration(int duration) {
        if (duration < 0) {
            return 1.0f;
        }
        int d = Math.max(20, duration);
        float r = (float) BASE_DURATION_TICKS / (float) d;
        return Math.max(0.25f, Math.min(4.0f, r));
    }

    /**
     * Decay estilo SPD: min(1, shieldingActual/20) por turno.
     * Aquí shielding = pool restante (no el valor inicial).
     * rateMult solo aporta el factor de duración de la poción.
     */
    private static float decayPerSecond(float pool, float rateMult) {
        return Math.min(1f, pool / 20f) * rateMult;
    }

    /**
     * Asegura que MAX_ABSORPTION sea al menos 'needed'.
     * El modificador de Barrier aporta exactamente 'needed' como tope añadido
     * (se recalcula; no deja un valor obsoleto más bajo).
     */
    private static void ensureMaxAtLeast(LivingEntity entity, float needed) {
        AttributeInstance maxAbs = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbs == null) {
            return;
        }
        maxAbs.removeModifier(MAX_ABS_MOD_ID);
        float amount = Math.max(0f, needed);
        if (amount > 0f) {
            maxAbs.addTransientModifier(new AttributeModifier(
                    MAX_ABS_MOD_ID,
                    amount,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    private static void removeOurMaxModifier(LivingEntity entity) {
        AttributeInstance maxAbs = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbs != null) {
            maxAbs.removeModifier(MAX_ABS_MOD_ID);
        }
    }

    private static float getMaxAbsorption(LivingEntity entity) {
        AttributeInstance maxAbs = entity.getAttribute(Attributes.MAX_ABSORPTION);
        return maxAbs != null ? (float) maxAbs.getValue() : 0f;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 20 != 0) {
            return true;
        }

        CompoundTag data = entity.getPersistentData();
        float pool = data.getFloat(KEY_LEFT).orElse(0f);

        if (pool < 1f) {
            endEffect(entity, data);
            return true;
        }

        float absNow = entity.getAbsorptionAmount();
        float lastAbs = data.getFloat(KEY_LAST_ABS).orElse(absNow);
        float rateMult = data.getFloat(KEY_RATE_MULT).orElse(1f);
        int initDur = data.getInt(KEY_INIT_DUR).orElse(BASE_DURATION_TICKS);

        // --- Absorción SUBIÓ (manzana dorada, otro efecto, etc.) ---
        // abs = pool + absActual; pool no cambia.
        if (absNow > lastAbs + 0.05f) {
            float stacked = pool + absNow;
            ensureMaxAtLeast(entity, stacked);
            entity.setAbsorptionAmount(stacked);
            absNow = entity.getAbsorptionAmount();
            data.putFloat(KEY_LAST_ABS, absNow);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
                // player.sendSystemMessage(Component.literal(
                        // "§bBarrier§r: §a+absorción externa§r → abs §a" + round2(absNow)
                                // + "§r (pool §e" + round2(pool) + "§r + externa)"
                // ));
        // }
        }

        // --- Daño: la absorción BAJÓ → restar a la pool ---
        absNow = entity.getAbsorptionAmount();
        lastAbs = data.getFloat(KEY_LAST_ABS).orElse(absNow);
        if (absNow < lastAbs - 0.05f) {
            float absorbed = lastAbs - absNow;
            pool = Math.max(0f, pool - absorbed);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
                // player.sendSystemMessage(Component.literal(
                        // "§bBarrier§r: §cdaño → pool §e-" + round2(absorbed)
                                // + "§r | pool §a" + round2(pool)
                                // + "§r | abs §7" + round2(absNow)
                // ));
        // }
            data.putFloat(KEY_LEFT, pool);
            if (pool < 1f) {
                endEffect(entity, data);
                return true;
            }
        }

        // --- Decay SPD sobre pool RESTANTE ---
        float decay = decayPerSecond(pool, rateMult);
        float partial = data.getFloat(KEY_PARTIAL).orElse(0f) + decay;
        int lost = 0;
        if (partial >= 1f) {
            lost = (int) partial;
            partial -= lost;
            pool = Math.max(0f, pool - lost);
        }
        data.putFloat(KEY_PARTIAL, partial);
        data.putFloat(KEY_LEFT, pool);

        // Reducir absorción actual en 'lost' (no pisar con pool)
        absNow = entity.getAbsorptionAmount();
        if (lost > 0) {
            float reduced = Math.max(0f, absNow - lost);
            if (reduced < 0.05f && pool >= 1f) {
                // abs se fue a 0 pero aún hay pool → restaurar
                ensureMaxAtLeast(entity, pool);
                reduced = pool;
            }
            entity.setAbsorptionAmount(reduced);
            absNow = entity.getAbsorptionAmount();
        } else if (absNow < 0.05f && pool >= 1f) {
            ensureMaxAtLeast(entity, pool);
            entity.setAbsorptionAmount(pool);
            absNow = entity.getAbsorptionAmount();
        }

        data.putFloat(KEY_LAST_ABS, absNow);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§bBarrier§r: pool §a" + round2(pool)
                            // + "§r | abs §a" + round2(absNow)
                            // + "§r | decay §c" + round2(decay)
                            // + "§r | lost §c" + lost
                            // + "§r | rateMult §e" + round2(rateMult)
                            // + "§r | maxAbs §7" + round2(getMaxAbsorption(entity))
                            // + "§r | initDur §7" + initDur
            // ));
        // }

        if (pool < 1f) {
            endEffect(entity, data);
            return true;
        }

        return true;
    }

    private void endEffect(LivingEntity entity, CompoundTag data) {
        float last = data.getFloat(KEY_LEFT).orElse(0f);
        float rateMult = data.getFloat(KEY_RATE_MULT).orElse(1f);
        int initDur = data.getInt(KEY_INIT_DUR).orElse(-1);
        float absBefore = entity.getAbsorptionAmount();

        data.remove(KEY_LEFT);
        data.remove(KEY_JUST_CONVERTED);
        data.remove(KEY_INCOMING_DURATION);
        data.remove(KEY_INCOMING_AMP);
        data.remove(KEY_RATE_MULT);
        data.remove(KEY_PARTIAL);
        data.remove(KEY_INIT_DUR);
        data.remove(KEY_LAST_ABS);
        data.remove(KEY_OUR_MAX);

        // Solo nuestro modificador; no tocar absorption
        removeOurMaxModifier(entity);
        entity.removeEffect(ModEffects.BARRIER);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§bBarrier§r: §cescudo agotado§r (pool=" + round2(last)
                            // + " absQueda=" + round2(entity.getAbsorptionAmount())
                            // + " absAntes=" + round2(absBefore)
                            // + " rateMult=" + round2(rateMult)
                            // + " initDur=" + initDur + ")"
            // ));
        // }
    }

    private void msgStart(LivingEntity entity, float pool, float timeFactor, int amplifier,
                          float rateMult, int duration, float basePool, float grant,
                          float nBefore, float absAfter) {
        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§bBarrier§r: INICIO pool §a" + round2(pool)
                            // + "§r | grant §a" + round2(grant)
                            // + "§r | base §7" + round2(basePool)
                            // + "§r | mult §e" + round2(Math.min(amplifier + 1, timeFactor * (amplifier + 1) * (amplifier + 1)))
                            // + "§r | dosis §e" + Math.round(timeFactor * 100) + "%"
                            // + "§r | nivel §e" + (amplifier + 1)
                            // + "§r | rateMult §e" + round2(rateMult)
                            // + "§r | initDur §7" + duration
                            // + "§r | absAntes §7" + round2(nBefore)
                            // + "§r | absAhora §a" + round2(absAfter)
                            // + "§r | maxAbs §7" + round2(getMaxAbsorption(entity))
            // ));
        // }
    }

    private static float round2(float v) {
        return Math.round(v * 100f) / 100f;
    }
}
