package com.drbenja.spd.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

import com.drbenja.spd.init.ModEffects;

/**
 * Regenerative Healing (estilo SPD).
 *
 * Pool: (0.8×maxHP+14) × min(amp+1, timeFactor×(amp+1)²)
 *   timeFactor MAX 1 → más duración no sube la pool
 *   tope amp+1 → II Ext no supera a III en pool
 *
 * Ritmo: 25% × (amp+1) × rateMult del restante / segundo
 *   rateMult = min(1, 3600/duración) → Ext más lento; cortas NO aceleran
 *
 * Tras aplicar: duración infinita mientras quede pool (≥1).
 * Datos en persistent entity NBT (keys abajo). Más adelante se puede migrar a Attachment.
 */
public class RegenerativeHealingEffect extends MobEffect {

    public static final String KEY_LEFT = "spd_regen_healing_left";
    public static final String KEY_JUST_CONVERTED = "spd_regen_just_converted";
    public static final String KEY_INCOMING_DURATION = "spd_regen_incoming_duration";
    public static final String KEY_INCOMING_AMP = "spd_regen_incoming_amp";
    public static final String KEY_RATE_MULT = "spd_regen_rate_mult";

    public static final int BASE_DURATION_TICKS = 3600;

    public RegenerativeHealingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF33AACC); // -13395712
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }

        CompoundTag data = entity.getPersistentData();

        if (data.getBooleanOr(KEY_JUST_CONVERTED, false)) {
            data.remove(KEY_JUST_CONVERTED);
            return;
        }

        int incomingDuration = data.contains(KEY_INCOMING_DURATION)
                ? data.getIntOr(KEY_INCOMING_DURATION, -1)
                : -1;
        int incomingAmp = data.contains(KEY_INCOMING_AMP)
                ? data.getIntOr(KEY_INCOMING_AMP, amplifier)
                : amplifier;

        data.remove(KEY_INCOMING_DURATION);
        data.remove(KEY_INCOMING_AMP);

        MobEffectInstance current = entity.getEffect(ModEffects.REGENERATIVE_HEALING);
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
        float ampPlus = finalAmp + 1;
        float mult = Math.min(ampPlus, timeFactor * ampPlus * ampPlus);
        float basePool = 0.8f * entity.getMaxHealth() + 14.0f;
        float grant = Math.max(1f, basePool * mult);

        float rateMult = rateMultFromDuration(duration);

        float existing = data.getFloatOr(KEY_LEFT, 0f);
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

        data.putFloat(KEY_LEFT, pool);
        data.putFloat(KEY_RATE_MULT, rateMult);

        cure(entity);
        msg(entity, pool, timeFactor, finalAmp, grant, rateMult, mult);

        data.putBoolean(KEY_JUST_CONVERTED, true);
        entity.removeEffect(ModEffects.REGENERATIVE_HEALING);
        entity.addEffect(new MobEffectInstance(
                ModEffects.REGENERATIVE_HEALING,
                MobEffectInstance.INFINITE_DURATION,
                finalAmp,
                false,
                false,
                false
        ));
        applyVisual(entity, pool, finalAmp);
    }
public static void cure(LivingEntity entity) {
        // Solo quitar si está activo: removeEffect sin efecto dispara MobEffectEvent.Remove con instance null
        removeIfPresent(entity, MobEffects.POISON);
        removeIfPresent(entity, MobEffects.WEAKNESS);
        removeIfPresent(entity, MobEffects.BLINDNESS);
        removeIfPresent(entity, MobEffects.SLOWNESS);
        removeIfPresent(entity, MobEffects.WITHER);
        removeIfPresent(entity, ModEffects.BLEEDING);
    }

    private static void removeIfPresent(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (entity.hasEffect(effect)) {
            entity.removeEffect(effect);
        }
    }

    /** MAX 1 */
    public static float factorFromDuration(int duration) {
        if (duration < 0 || duration >= BASE_DURATION_TICKS) {
            return 1.0f;
        }
        return Math.max(0.01f, Math.min(1.0f, (float) duration / (float) BASE_DURATION_TICKS));
    }

    /** MAX 1: Ext ralentiza; cortas no aceleran el gasto */
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
        float left = data.getFloatOr(KEY_LEFT, 0f);

        if (left < 1f) {
            endEffect(entity, data);
            return false;
        }
        applyVisual(entity, left, amplifier);

        if (entity.tickCount % 20 != 0) {
            return true;
        }

        float rateMult = data.getFloatOr(KEY_RATE_MULT, 1f);
        float percent = 0.25f * (amplifier + 1) * rateMult;
        float healAmt = Math.round(left * percent);
        healAmt = Math.max(1f, healAmt);
        healAmt = Math.min(healAmt, left);

        float maxHp = entity.getMaxHealth();
        float currentHp = entity.getHealth();
        float actualHeal = Math.min(healAmt, Math.max(0f, maxHp - currentHp));

        if (actualHeal > 0f) {
            entity.heal(actualHeal);
        }
        left -= healAmt;
        data.putFloat(KEY_LEFT, left);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§aRegenerative Healing§r: restante §a" + round1(left)
                            // + "§r (curó §a" + round1(actualHeal)
                            // + "§r / intentó §e" + round1(healAmt) + "§r)"
            // ));
        // }

        if (left < 1f) {
            endEffect(entity, data);
            return false;
        }
        applyVisual(entity, left, amplifier);
        return true;
    }

    private void endEffect(LivingEntity entity, CompoundTag data) {
        data.remove(KEY_LEFT);
        data.remove(KEY_JUST_CONVERTED);
        data.remove(KEY_INCOMING_DURATION);
        data.remove(KEY_INCOMING_AMP);
        data.remove(KEY_RATE_MULT);
        entity.removeEffect(ModEffects.REGENERATIVE_HEALING);
        entity.removeEffect(ModEffects.REGENERATIVE_HEALING_GUI);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§aRegenerative Healing§r: §cpool agotado§r"
            // ));
        // }
    }

    private void msg(LivingEntity entity, float pool, float timeFactor, int amplifier,
                     float grant, float rateMult, float mult) {
        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§aRegenerative Healing§r: pool §a" + round1(pool)
                            // + "§r | grant §a" + round1(grant)
                            // + "§r | mult §e" + round1(mult)
                            // + "§r | tiempo §e" + Math.round(timeFactor * 100) + "%"
                            // + "§r | nivel §e" + (amplifier + 1)
                            // + "§r | ritmo× §e" + round1(rateMult * (amplifier + 1)) + "§r"
            // ));
        // }
    }

    private static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    public static void applyVisual(LivingEntity entity, float left, int amplifier) {
        int visualTicks = Math.max(20, (Math.round(left) + 1) * 20);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.REGENERATIVE_HEALING_GUI,
                visualTicks,
                amplifier,
                false,
                true,
                true
        ), null);
    }
}
