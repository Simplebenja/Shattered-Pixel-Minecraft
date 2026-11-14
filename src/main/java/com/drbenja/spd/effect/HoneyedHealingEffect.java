package com.drbenja.spd.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.nbt.CompoundTag;

import com.drbenja.spd.init.ModEffects;

/**
 * Honeyed Healing — misma pool/ritmo que Regenerative + comida/saturación.
 *
 * Pool: (0.8×HT+14) × min(amp+1, timeFactor×(amp+1)²)
 * Ritmo: 25% × (amp+1) × min(1, 3600/duración)
 * Saturación: 1 × (amp+1) / s
 */
public class HoneyedHealingEffect extends MobEffect {

    public static final String KEY_LEFT = "spd_honeyed_healing_left";
    public static final String KEY_JUST_CONVERTED = "spd_honeyed_just_converted";
    public static final String KEY_INCOMING_DURATION = "spd_honeyed_incoming_duration";
    public static final String KEY_INCOMING_AMP = "spd_honeyed_incoming_amp";
    public static final String KEY_RATE_MULT = "spd_honeyed_rate_mult";

    public static final int BASE_DURATION_TICKS = 3600;
    private static final int INITIAL_FOOD = 6;

    public HoneyedHealingEffect() {
        super(MobEffectCategory.BENEFICIAL, -13312);
        this.withSoundOnAdded(BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse("entity.bee.loop")));
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

        MobEffectInstance current = entity.getEffect(ModEffects.HONEYED_HEALING);
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

        data.putFloat(KEY_LEFT, pool);
        data.putFloat(KEY_RATE_MULT, rateMult);

        cure(entity);
        applyInitialHunger(entity);
        msg(entity, pool, timeFactor, finalAmp, grant, rateMult, mult);

        data.putBoolean(KEY_JUST_CONVERTED, true);
        entity.removeEffect(ModEffects.HONEYED_HEALING);
        entity.addEffect(new MobEffectInstance(
                ModEffects.HONEYED_HEALING,
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
        removeIfPresent(entity, ModEffects.POISONED);
        com.drbenja.spd.effect.PoisonedEffect.clear(entity);
        com.drbenja.spd.effect.BleedingEffect.clear(entity);
    }

    private static void removeIfPresent(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (entity.hasEffect(effect)) {
            entity.removeEffect(effect);
        }
    }

    private static void applyInitialHunger(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        FoodData food = player.getFoodData();
        int newLevel = Math.min(20, food.getFoodLevel() + INITIAL_FOOD);
        food.setFoodLevel(newLevel);
        float sat = Math.min(newLevel, food.getSaturationLevel() + 2.0f);
        food.setSaturation(sat);
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

        float rateMult = data.getFloat(KEY_RATE_MULT).orElse(1f);
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

        if (entity instanceof Player player) {
            FoodData food = player.getFoodData();
            float satGain = 1.0f * (amplifier + 1);
            float newSat = Math.min(food.getFoodLevel(), food.getSaturationLevel() + satGain);
            food.setSaturation(newSat);
        }

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§6Honeyed Healing§r: restante §a" + round1(left)
                            // + "§r (curó §a" + round1(actualHeal)
                            // + "§r / intentó §e" + round1(healAmt)
                            // + "§r / sat+ §e" + (amplifier + 1) + "§r)"
            // ));
        // }

        if (left < 1f) {
            endEffect(entity, data);
            return true;
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
        entity.removeEffect(ModEffects.HONEYED_HEALING);
        entity.removeEffect(ModEffects.HONEYED_HEALING_GUI);

        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§6Honeyed Healing§r: §cpool agotado§r"
            // ));
        // }
    }

    private void msg(LivingEntity entity, float pool, float timeFactor, int amplifier,
                     float grant, float rateMult, float mult) {
        // DEBUG chat (descomentar para pruebas)
        // if (entity instanceof ServerPlayer player) {
            // player.sendSystemMessage(Component.literal(
                    // "§6Honeyed Healing§r: pool §a" + round1(pool)
                            // + "§r | grant §a" + round1(grant)
                            // + "§r | mult §e" + round1(mult)
                            // + "§r | tiempo §e" + Math.round(timeFactor * 100) + "%"
                            // + "§r | nivel §e" + (amplifier + 1)
                            // + "§r | ritmo× §e" + round1(rateMult * (amplifier + 1))
                            // + "§r | §e+" + INITIAL_FOOD + " comida§r"
                            // + " | sat/s §e" + (amplifier + 1) + "§r"
            // ));
        // }
    }

    private static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    public static void applyVisual(LivingEntity entity, float left, int amplifier) {
        int visualTicks = Math.max(20, (Math.round(left) + 1) * 20);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.HONEYED_HEALING_GUI,
                visualTicks,
                amplifier,
                false,
                true,
                true
        ), null);
    }
}

