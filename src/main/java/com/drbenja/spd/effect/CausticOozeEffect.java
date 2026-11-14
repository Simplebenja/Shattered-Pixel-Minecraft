package com.drbenja.spd.effect;

import com.drbenja.spd.util.SpdProgression;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Caustic Ooze (SPD Ooze) — faithful:
 * - depth &gt; 5 → 1 + depth/5 each second
 * - depth == 5 → 1
 * - depth &lt; 5 → 50% chance of 1 (sewers)
 * - Always at least 1 when it does hit
 * - Water / rain washes away; Purification Barrier blocks DoT
 */
public class CausticOozeEffect extends MobEffect {

    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spd", "caustic_ooze"));

    private static final Map<LivingEntity, Integer> LAST_DAMAGE_TICK = new WeakHashMap<>();

    public CausticOozeEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF006633);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.isInWater() || level.isRainingAt(entity.blockPosition().above())) {
            entity.removeEffect(ModEffects.CAUSTIC_OOZE);
            LAST_DAMAGE_TICK.remove(entity);
            return true;
        }

        if (entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return true;
        }

        Integer last = LAST_DAMAGE_TICK.get(entity);
        int now = entity.tickCount;
        if (last != null && now - last < 20) {
            return true;
        }

        int depth = SpdProgression.depth(entity);
        float damage;
        if (depth > 5) {
            damage = 1.0F + depth / 5.0F + amplifier;
        } else if (depth == 5) {
            damage = 1.0F + amplifier;
        } else {
            // SPD sewers: ~0.5 dmg/turn average → 50% chance of 1
            if (amplifier <= 0 && !entity.getRandom().nextBoolean()) {
                LAST_DAMAGE_TICK.put(entity, now);
                return true;
            }
            damage = 1.0F + amplifier;
        }
        damage = Math.max(1.0F, damage);

        entity.hurtServer(level, damageSource(level), damage);
        LAST_DAMAGE_TICK.put(entity, now);
        return true;
    }

    private static DamageSource damageSource(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}
