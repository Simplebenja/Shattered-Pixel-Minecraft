package com.drbenja.spd.effect;

import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * SPD-style Poison ("Poisoned"):
 * - Tracks remaining "left" charges (turns/seconds).
 * - Each second while left &gt; 0 and delay elapsed: deal toxic damage = floor(left/3)+1, then left -= 1.
 * - extend(amount) adds to left; set(duration) takes max.
 * - delaySeconds: first application can postpone damage (Venomous uses 3s).
 * - Inorganic entities are immune.
 */
public class PoisonedEffect extends MobEffect {

    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spd", "toxic"));

    private static final Map<LivingEntity, Float> LEFT = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> DELAY_UNTIL_TICK = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> LAST_DAMAGE_TICK = new WeakHashMap<>();

    public PoisonedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF8844AA);
    }

    /** Apply or extend poison. delaySeconds only applies when target had no active left. */
    public static void apply(LivingEntity target, float addLeft, int delaySeconds) {
        if (target == null || !target.isAlive()) return;
        if (EntityClassHelper.immuneToToxicGas(target) || EntityClassHelper.isInorganic(target)) {
            return;
        }
        float cur = LEFT.getOrDefault(target, 0f);
        boolean wasEmpty = cur <= 0.01f;
        float next = cur + addLeft;
        LEFT.put(target, next);
        if (wasEmpty && delaySeconds > 0) {
            DELAY_UNTIL_TICK.put(target, target.tickCount + delaySeconds * 20);
        }
        // Visual duration ~ remaining left (+ delay) in seconds
        int delayLeft = 0;
        Integer until = DELAY_UNTIL_TICK.get(target);
        if (until != null) {
            delayLeft = Math.max(0, until - target.tickCount);
        }
        int ticks = Math.max(20, (int) Math.ceil(next) * 20 + delayLeft);
        target.addEffect(new MobEffectInstance(ModEffects.POISONED, ticks, Math.max(0, (int) next - 1), false, true, true));
    }

    public static float getLeft(LivingEntity entity) {
        return LEFT.getOrDefault(entity, 0f);
    }

    public static void clear(LivingEntity entity) {
        LEFT.remove(entity);
        DELAY_UNTIL_TICK.remove(entity);
        LAST_DAMAGE_TICK.remove(entity);
        if (entity.hasEffect(ModEffects.POISONED)) {
            entity.removeEffect(ModEffects.POISONED);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (EntityClassHelper.immuneToToxicGas(entity) || EntityClassHelper.isInorganic(entity)) {
            clear(entity);
            return false;
        }

        float left = LEFT.getOrDefault(entity, 0f);
        if (left <= 0.01f) {
            clear(entity);
            return false;
        }

        Integer delayUntil = DELAY_UNTIL_TICK.get(entity);
        if (delayUntil != null && entity.tickCount < delayUntil) {
            // Still delayed — keep effect icon, no damage yet
            return true;
        }
        if (delayUntil != null) {
            DELAY_UNTIL_TICK.remove(entity);
        }

        int now = entity.tickCount;
        Integer last = LAST_DAMAGE_TICK.get(entity);
        if (last != null && now - last < 20) {
            return true;
        }
        LAST_DAMAGE_TICK.put(entity, now);

        int dmg = (int) (left / 3f) + 1;
        if (dmg > 0) {
            entity.hurtServer(level, toxicSource(level), (float) dmg);
        }

        left -= 1f;
        if (left <= 0.01f || !entity.isAlive()) {
            clear(entity);
            return false;
        }
        LEFT.put(entity, left);
        // Refresh visual amp/duration
        int ticks = Math.max(20, (int) Math.ceil(left) * 20);
        entity.addEffect(new MobEffectInstance(ModEffects.POISONED, ticks, Math.max(0, (int) left - 1), false, true, true));
        return true;
    }

    public static DamageSource toxicSource(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}
