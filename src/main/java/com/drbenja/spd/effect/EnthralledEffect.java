package com.drbenja.spd.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * SPD Enthralled: ally without corruption DoT.
 * Hearts on apply + ambient while active. Team assignment is done by the applier
 * via {@link com.drbenja.spd.util.AllyTeamHelper} (Siren's Song, future sources).
 */
public class EnthralledEffect extends MobEffect {

    private static final String KEY_BURST_UNTIL = "SpdEnthralledBurstUntil";

    public EnthralledEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        CompoundTag data = entity.getPersistentData();
        // Dense heart burst ~1s
        data.putInt(KEY_BURST_UNTIL, entity.tickCount + 20);

        if (entity.level() instanceof ServerLevel server) {
            spawnHearts(server, entity, 16);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!entity.isAlive()) {
            return true;
        }
        CompoundTag data = entity.getPersistentData();
        int now = entity.tickCount;
        int burstUntil = data.getInt(KEY_BURST_UNTIL).orElse(0);

        if (now <= burstUntil) {
            spawnHearts(level, entity, 3);
        } else if (now % 12 == 0) {
            // Sparse ambient hearts while enthralled
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double y = entity.getY() + 0.3 + entity.getRandom().nextDouble() * entity.getBbHeight() * 0.8;
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            level.sendParticles(ParticleTypes.HEART, true, true, x, y, z, 1, 0.05, 0.08, 0.05, 0.0);
        }
        return true;
    }

    private static void spawnHearts(ServerLevel level, LivingEntity entity, int count) {
        double cx = entity.getX();
        double cy = entity.getY() + entity.getBbHeight() * 0.6;
        double cz = entity.getZ();
        double spread = Math.max(0.35, entity.getBbWidth() * 0.65);
        double hy = Math.max(0.35, entity.getBbHeight() * 0.4);
        level.sendParticles(ParticleTypes.HEART, true, true, cx, cy, cz, count, spread, hy, spread, 0.0);
    }

    public static void clearSessionFlags(LivingEntity entity) {
        entity.getPersistentData().remove(KEY_BURST_UNTIL);
    }
}
