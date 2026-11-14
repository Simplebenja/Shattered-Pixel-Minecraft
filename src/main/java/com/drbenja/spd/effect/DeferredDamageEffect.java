package com.drbenja.spd.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * Viscosity deferred pool. Tick damage bypasses armor/enchants (damage type tags)
 * and is never re-deferred by Viscosity.
 */
public class DeferredDamageEffect extends MobEffect {

    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spd", "deferred_damage"));

    private static final String KEY_POOL = "SpdDeferredPool";
    private static final String KEY_LAST = "SpdDeferredLast";

    public DeferredDamageEffect() {
        super(MobEffectCategory.HARMFUL, 0x8844CC);
    }

    public static void addDeferred(LivingEntity entity, int amount) {
        if (amount <= 0 || entity.level().isClientSide()) return;
        CompoundTag data = entity.getPersistentData();
        float pool = data.getFloat(KEY_POOL).orElse(0f) + amount;
        data.putFloat(KEY_POOL, pool);
        int seconds = Math.max(1, (int) Math.ceil(pool));
        entity.addEffect(new MobEffectInstance(ModEffects.DEFERRED_DAMAGE, seconds * 20, 0, false, true, true));
    }

    public static boolean isDeferredSource(DamageSource source) {
        return source.is(DAMAGE_TYPE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();
        int now = entity.tickCount;
        int last = data.getInt(KEY_LAST).orElse(0);
        if (last != 0 && now - last < 20) {
            return true;
        }
        data.putInt(KEY_LAST, now);

        float pool = data.getFloat(KEY_POOL).orElse(0f);
        if (pool <= 0.01f) {
            data.remove(KEY_POOL);
            data.remove(KEY_LAST);
            return false;
        }

        int tickDmg;
        if (pool < 1f) {
            // SPD: min 1 unless remaining < 1 → clear without hit
            data.putFloat(KEY_POOL, 0f);
            data.remove(KEY_LAST);
            return false;
        }
        tickDmg = Math.max(1, (int) (pool * 0.1f));
        if (tickDmg > pool) tickDmg = (int) pool;

        // Ignore invuln frames briefly so DoT lands every second
        int inv = entity.getInvulnerableTime();
        entity.setInvulnerableTime(0);
        boolean hit = entity.hurtServer(level, damageSource(level), (float) tickDmg);
        entity.setInvulnerableTime(Math.min(inv, 10));

        if (hit || tickDmg > 0) {
            pool -= tickDmg;
        }
        if (pool <= 0.01f) {
            data.remove(KEY_POOL);
            data.remove(KEY_LAST);
            return false;
        }
        data.putFloat(KEY_POOL, pool);
        int seconds = Math.max(1, (int) Math.ceil(pool));
        entity.addEffect(new MobEffectInstance(ModEffects.DEFERRED_DAMAGE, seconds * 20, 0, false, true, true));
        return true;
    }

    private static DamageSource damageSource(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}
