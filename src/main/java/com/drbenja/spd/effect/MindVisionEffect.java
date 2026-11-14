package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

/**
 * Mind Vision: aplica Glowing vanilla a hostiles en radio
 * (3 chunks amp0 / 6 chunks amp1) alrededor del portador.
 */
public class MindVisionEffect extends MobEffect {

    /** 3 chunks = 48 bloques; 6 chunks = 96. */
    public static final double RANGE_BASE = 48.0D;
    public static final double RANGE_POWERED = 96.0D;

    public MindVisionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC77DFF);
    }

    public static double rangeForAmplifier(int amplifier) {
        return amplifier >= 1 ? RANGE_POWERED : RANGE_BASE;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Cada segundo
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        double range = rangeForAmplifier(amplifier);
        double rangeSq = range * range;
        AABB box = entity.getBoundingBox().inflate(range);

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e != entity && e instanceof Enemy && e.isAlive())) {
            if (target.distanceToSqr(entity) > rangeSq) {
                continue;
            }
            // Duración corta que se renueva; sin partículas ni icono en el objetivo
            target.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    40,
                    0,
                    true,
                    false,
                    false));
        }
        return true;
    }
}
