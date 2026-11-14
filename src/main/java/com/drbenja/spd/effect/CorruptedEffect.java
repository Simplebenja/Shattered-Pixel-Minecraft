package com.drbenja.spd.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * SPD Corruption:
 * - On apply: full heal + strip harmful effects (except itself) + conversion sound + particle burst.
 * - DoT: maxHP/100 accumulated per second.
 * - AI (ConvertedAllyAiHandlers): ignore players/pets/animals/villagers; hunt monsters.
 * - Ambient particles while active (server-synced).
 */
public class CorruptedEffect extends MobEffect {

    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spd", "corruption"));

    private static final String KEY_PARTIAL = "SpdCorruptionPartial";
    private static final String KEY_LAST_TICK = "SpdCorruptionLastTick";
    private static final String KEY_HEALED = "SpdCorruptionHealed";
    private static final String KEY_BURST_UNTIL = "SpdCorruptionBurstUntil";

    public CorruptedEffect() {
        super(MobEffectCategory.HARMFUL, 0x2A0A3A);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        CompoundTag data = entity.getPersistentData();
        // Only once per application wave
        if (data.getBooleanOr(KEY_HEALED, false)) {
            return;
        }
        data.putBoolean(KEY_HEALED, true);
        data.putFloat(KEY_PARTIAL, 0f);
        // Dense particle burst for ~1 second
        data.putInt(KEY_BURST_UNTIL, entity.tickCount + 20);

        // Full heal (SPD corruptionHeal)
        entity.setHealth(entity.getMaxHealth());

        // Strip harmful effects except Corrupted
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance inst : entity.getActiveEffects()) {
            Holder<MobEffect> holder = inst.getEffect();
            if (holder.is(ModEffects.CORRUPTED)) continue;
            if (holder.value().getCategory() == MobEffectCategory.HARMFUL) {
                toRemove.add(holder);
            }
        }
        for (Holder<MobEffect> h : toRemove) {
            entity.removeEffect(h);
        }

        // Conversion cue (SPD-style "something changed")
        entity.level().playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                SoundSource.HOSTILE,
                0.9F,
                0.75F + entity.getRandom().nextFloat() * 0.15F
        );
        entity.level().playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SOUL_ESCAPE,
                SoundSource.HOSTILE,
                0.7F,
                0.6F
        );

        // Immediate conversion particle burst (server-synced)
        if (entity.level() instanceof ServerLevel server) {
            spawnBurst(server, entity, 24);
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

        // Particle visuals every tick during burst, then sparse ambient
        int burstUntil = data.getInt(KEY_BURST_UNTIL).orElse(0);
        if (now <= burstUntil) {
            spawnBurst(level, entity, 4);
        } else if (now % 10 == 0) {
            // Sparse ambient souls while corrupted (alwaysShow so they are not culled)
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double y = entity.getY() + 0.2 + entity.getRandom().nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            level.sendParticles(ParticleTypes.SOUL, true, true, x, y, z, 2, 0.08, 0.15, 0.08, 0.02);
            level.sendParticles(ParticleTypes.SMOKE, true, true, x, y, z, 1, 0.06, 0.08, 0.06, 0.0);
        }

        // DoT once per second
        int last = data.getInt(KEY_LAST_TICK).orElse(0);
        if (last != 0 && now - last < 20) {
            return true;
        }
        data.putInt(KEY_LAST_TICK, now);

        float partial = data.getFloat(KEY_PARTIAL).orElse(0f);
        partial += entity.getMaxHealth() / 100f;
        int damage = (int) partial;
        partial -= damage;
        data.putFloat(KEY_PARTIAL, partial);

        if (damage > 0) {
            entity.hurtServer(level, damageSource(level), (float) damage);
        }
        return true;
    }

    private static void spawnBurst(ServerLevel level, LivingEntity entity, int count) {
        double cx = entity.getX();
        double cy = entity.getY() + entity.getBbHeight() * 0.5;
        double cz = entity.getZ();
        double spread = Math.max(0.4, entity.getBbWidth() * 0.7);
        double hy = Math.max(0.4, entity.getBbHeight() * 0.45);
        // alwaysShow=true so particles are not distance-culled
        level.sendParticles(ParticleTypes.SOUL, true, true, cx, cy, cz, count, spread, hy, spread, 0.03);
        level.sendParticles(ParticleTypes.SMOKE, true, true, cx, cy, cz, Math.max(3, count / 2), spread, hy * 0.8, spread, 0.01);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, true, true, cx, cy, cz, Math.max(2, count / 3), spread * 0.5, hy * 0.6, spread * 0.5, 0.015);
    }

    /** Call when effect fully ends so next apply can heal again. */
    public static void clearSessionFlags(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(KEY_HEALED);
        data.remove(KEY_PARTIAL);
        data.remove(KEY_LAST_TICK);
        data.remove(KEY_BURST_UNTIL);
    }

    private static DamageSource damageSource(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}
