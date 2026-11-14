package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.event.effect.ImmobilizeHandlers;

/**
 * SPD Magical Sleep: immobilize via ImmobilizeHandlers (shared speed lock).
 * Wake on damage (SleepHandlers); allies may heal while asleep.
 */
public class MagicalSleepEffect extends MobEffect {

    public static final Identifier SPEED_MOD_ID = ImmobilizeHandlers.SPEED_MOD_ID;

    public MagicalSleepEffect() {
        super(MobEffectCategory.HARMFUL, 0x2244aa);
    }

    public static void clearModifiers(LivingEntity entity) {
        ImmobilizeHandlers.clearModifiers(entity);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return true;
    }
}
