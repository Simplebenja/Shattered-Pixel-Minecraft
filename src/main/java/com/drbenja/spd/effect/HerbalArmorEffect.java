package com.drbenja.spd.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.SpdProgression;

/**
 * Earthroot.Armor (SPD):
 * - Pool of absorb; each hit blocks up to (depth+5)/2.
 * - Mother: infinite, hidden in client HUD (SpdClient).
 * - GUI: refreshed with forceAddEffect from pool (same pattern as healing GUIs).
 * - Lost only on intentional jump / sprint (see ArmorEnchantmentHandlers), not knockback.
 */
public class HerbalArmorEffect extends MobEffect {

    private static final String KEY_POOL = "SpdHerbalPool";
    /** Game time until which jump/sprint/walk will not break Herbal Armor (1 s grace). */
    private static final String KEY_GRACE_UNTIL = "SpdHerbalGraceUntil";

    public HerbalArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x663300);
    }

    public static float maxBlockPerHit(LivingEntity entity) {
        return SpdProgression.herbalArmorMaxBlock(entity);
    }

    /** True during the 1-second grace after apply so the player can stop moving. */
    public static boolean inGrace(LivingEntity entity) {
        long until = entity.getPersistentData().getLong(KEY_GRACE_UNTIL).orElse(0L);
        return entity.level().getGameTime() < until;
    }

    public static void apply(LivingEntity entity, float pool) {
        if (entity.level().isClientSide()) return;
        CompoundTag data = entity.getPersistentData();
        float existing = data.getFloat(KEY_POOL).orElse(0f);
        float neu = Math.max(existing, pool);
        data.putFloat(KEY_POOL, neu);
        // 1 second grace: can jump/sprint/walk without breaking the armor
        data.putLong(KEY_GRACE_UNTIL, entity.level().getGameTime() + 20L);

        // Mother: infinite, no icon/particles (client also hides via SpdClient)
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.HERBAL_ARMOR,
                MobEffectInstance.INFINITE_DURATION,
                0,
                true,
                false,
                false
        ), null);
        applyVisual(entity, neu);
    }

    public static float absorb(LivingEntity entity, float incoming) {
        if (!entity.hasEffect(ModEffects.HERBAL_ARMOR)) return 0f;
        CompoundTag data = entity.getPersistentData();
        float pool = data.getFloat(KEY_POOL).orElse(0f);
        if (pool <= 0f) {
            clear(entity);
            return 0f;
        }

        float cap = maxBlockPerHit(entity);
        float blocked = Math.min(cap, Math.min(pool, incoming));
        float remaining = pool - blocked;

        if (remaining <= 0.01f) {
            clear(entity);
        } else {
            data.putFloat(KEY_POOL, remaining);
            entity.forceAddEffect(new MobEffectInstance(
                    ModEffects.HERBAL_ARMOR,
                    MobEffectInstance.INFINITE_DURATION,
                    0,
                    true,
                    false,
                    false
            ), null);
            applyVisual(entity, remaining);
        }
        return blocked;
    }

    public static float getPool(LivingEntity entity) {
        return entity.getPersistentData().getFloat(KEY_POOL).orElse(0f);
    }

    /**
     * Same idea as RegenerativeHealingEffect.applyVisual:
     * duration of GUI ≈ remaining pool (seconds), forced without remove/readd flicker.
     */
    public static void applyVisual(LivingEntity entity, float pool) {
        int visualTicks = Math.max(20, (Math.round(pool) + 1) * 20);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.HERBAL_ARMOR_GUI,
                visualTicks,
                0,
                false,
                true,
                true
        ), null);
    }

    public static void clear(LivingEntity entity) {
        entity.getPersistentData().remove(KEY_POOL);
        entity.removeEffect(ModEffects.HERBAL_ARMOR);
        entity.removeEffect(ModEffects.HERBAL_ARMOR_GUI);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        float pool = getPool(entity);
        if (pool <= 0.01f) {
            clear(entity);
            return false;
        }
        // Keep GUI bar aligned with pool every second (pool does not decay with time)
        if (entity.tickCount % 20 == 0) {
            applyVisual(entity, pool);
        }
        return true;
    }
}
