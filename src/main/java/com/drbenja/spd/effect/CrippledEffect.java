package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * SPD Cripple: -50% movement speed. Default 10 s; Dragon's Breath applies 10 s.
 */
public class CrippledEffect extends MobEffect {

    public static final Identifier SPEED_MOD_ID = Identifier.fromNamespaceAndPath("spd", "crippled_speed");
    public static final int DEFAULT_DURATION_TICKS = 200; // 10 s
    public static final int DRAGON_BREATH_DURATION = 200; // 10 s (SPD Cripple.DURATION)

    public CrippledEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B4513);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MOD_ID,
                -0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
