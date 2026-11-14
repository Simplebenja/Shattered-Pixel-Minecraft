package com.drbenja.spd.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD BlockBuff: timed absorption shield (5s).
 * Uses MAX_ABSORPTION attribute + absorption amount (same approach as BarrierEffect)
 * so yellow hearts actually appear. Icon texture: textures/mob_effect/blocking.png
 * (copy of barrier.png).
 */
public class BlockingEffect extends MobEffect {

    public static final int DURATION_TICKS = 100; // 5s
    public static final String KEY_SHIELD = "SpdBlockingShield";

    private static final Identifier MAX_ABS_MOD_ID =
            Identifier.fromNamespaceAndPath("spd", "blocking_max_absorption");

    public BlockingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4488FF);
    }

    public static void apply(LivingEntity entity, int shieldPoints) {
        int pts = Math.max(1, shieldPoints);
        clearAbsorptionContribution(entity);

        CompoundTag data = entity.getPersistentData();
        data.putInt(KEY_SHIELD, pts);

        float current = entity.getAbsorptionAmount();
        float needed = current + pts;
        ensureMaxAtLeast(entity, needed);
        entity.setAbsorptionAmount(needed);

        entity.addEffect(new MobEffectInstance(
                ModEffects.BLOCKING,
                DURATION_TICKS,
                Math.max(0, pts - 1),
                false,
                true,
                true
        ));
    }

    public static void clearAbsorptionContribution(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(KEY_SHIELD)) {
            removeOurMaxModifier(entity);
            return;
        }
        int pts = data.getInt(KEY_SHIELD).orElse(0);
        data.remove(KEY_SHIELD);

        float abs = entity.getAbsorptionAmount();
        entity.setAbsorptionAmount(Math.max(0f, abs - pts));
        removeOurMaxModifier(entity);

        // If barrier (or other) still needs max absorption, leave remaining abs alone
        float remaining = entity.getAbsorptionAmount();
        if (remaining > 0f) {
            ensureMaxAtLeast(entity, remaining);
        }
    }

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

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Keep absorption in sync while active (damage may consume hearts)
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();
        int pts = data.getInt(KEY_SHIELD).orElse(0);
        MobEffectInstance inst = entity.getEffect(ModEffects.BLOCKING);
        if (inst == null || inst.getDuration() <= 1 || pts <= 0) {
            clearAbsorptionContribution(entity);
            return true;
        }
        // Clamp: do not force absorption above remaining shield if damage already ate it
        float abs = entity.getAbsorptionAmount();
        if (abs > pts) {
            // Extra abs from other sources is fine; ensure max covers it
            ensureMaxAtLeast(entity, abs);
        } else if (abs < pts) {
            // Track reduced shield from damage so clear() doesn't oversubtract
            int remaining = Math.max(0, Math.round(abs));
            data.putInt(KEY_SHIELD, remaining);
            ensureMaxAtLeast(entity, abs);
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        // Applied via apply()
    }
}
