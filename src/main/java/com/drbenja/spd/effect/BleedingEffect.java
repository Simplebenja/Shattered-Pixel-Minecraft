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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * SPD-style Bleeding:
 * - Internal float {@code level}; each second deal {@code round(level)} damage,
 *   then set level to a random value in [level/2, level] until it rounds to 0.
 * - {@link #applyOrMax} sets level to max(current, newLevel) (non-stacking upward).
 * - Cleared by regeneration or a sudden HP heal spike.
 * - Inorganic immune.
 */
public class BleedingEffect extends MobEffect {

    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spd", "bleeding"));

    private static final Map<LivingEntity, Float> LEVEL = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> LAST_TICK = new WeakHashMap<>();
    private static final Map<LivingEntity, Float> LAST_HEALTH = new WeakHashMap<>();

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFCC0000);
    }

    /** Set bleed level to at least {@code level} (SPD non-stacking upward). */
    public static void applyOrMax(LivingEntity entity, float level) {
        if (entity == null || !entity.isAlive()) return;
        if (EntityClassHelper.immuneToBleeding(entity)) return;
        float cur = LEVEL.getOrDefault(entity, 0f);
        float next = Math.max(cur, level);
        if (next < 0.5f) return;
        LEVEL.put(entity, next);
        int amp = Math.max(0, Math.round(next) - 1);
        entity.addEffect(new MobEffectInstance(ModEffects.BLEEDING, MobEffectInstance.INFINITE_DURATION, amp, false, true, true));
    }

    public static float getLevel(LivingEntity entity) {
        return LEVEL.getOrDefault(entity, 0f);
    }

    public static void clear(LivingEntity entity) {
        LEVEL.remove(entity);
        LAST_TICK.remove(entity);
        LAST_HEALTH.remove(entity);
        if (entity.hasEffect(ModEffects.BLEEDING)) {
            entity.removeEffect(ModEffects.BLEEDING);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (EntityClassHelper.immuneToBleeding(entity)) {
            clear(entity);
            return false;
        }

        Float lastHealth = LAST_HEALTH.get(entity);
        float currentHealth = entity.getHealth();
        if (lastHealth != null && currentHealth > lastHealth + 2.0F) {
            clear(entity);
            return true;
        }
        LAST_HEALTH.put(entity, currentHealth);

        if (entity.hasEffect(MobEffects.REGENERATION)) {
            clear(entity);
            return true;
        }

        float bleed = LEVEL.getOrDefault(entity, 0f);
        if (bleed < 0.5f && Math.round(bleed) <= 0) {
            clear(entity);
            return false;
        }

        int now = entity.tickCount;
        Integer last = LAST_TICK.get(entity);
        if (last != null && now - last < 20) {
            return true;
        }
        LAST_TICK.put(entity, now);

        int dmg = Math.round(bleed);
        if (dmg > 0) {
            entity.hurtServer(level, damageSource(level), (float) dmg);
        }

        // SPD: level = Random.NormalFloat(level/2, level) ≈ uniform mid of [half, full]
        float lo = bleed / 2f;
        float hi = bleed;
        float next = lo + entity.getRandom().nextFloat() * (hi - lo);
        if (Math.round(next) <= 0 || !entity.isAlive()) {
            clear(entity);
            return false;
        }
        LEVEL.put(entity, next);
        int amp = Math.max(0, Math.round(next) - 1);
        entity.addEffect(new MobEffectInstance(ModEffects.BLEEDING, MobEffectInstance.INFINITE_DURATION, amp, false, true, true));
        return true;
    }

    private static DamageSource damageSource(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}

