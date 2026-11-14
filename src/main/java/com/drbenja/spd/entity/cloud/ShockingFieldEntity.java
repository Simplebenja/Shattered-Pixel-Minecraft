package com.drbenja.spd.entity.cloud;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.effect.ParalyzedEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Shocking Brew: área fija 7×7×3, 20 s.
 * Cada segundo: daño de rayo + Paralyzed. (sin expansión por agua de momento)
 */
public class ShockingFieldEntity extends Entity {

    public static final int DURATION_TICKS = 20 * 20;
    public static final int RADIUS = 3; // 7x7
    public static final int HEIGHT = 3;

    private int life = DURATION_TICKS;
    private BlockPos origin = BlockPos.ZERO;

    public ShockingFieldEntity(EntityType<? extends ShockingFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOrigin(BlockPos origin) {
        this.origin = origin.immutable();
        setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", DURATION_TICKS);
        origin = new BlockPos(input.getIntOr("ox", 0), input.getIntOr("oy", 0), input.getIntOr("oz", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putInt("ox", origin.getX());
        output.putInt("oy", origin.getY());
        output.putInt("oz", origin.getZ());
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide()) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        getX() + (random.nextDouble() - 0.5) * 6,
                        getY() + random.nextDouble() * 2,
                        getZ() + (random.nextDouble() - 0.5) * 6,
                        0, 0.05, 0);
            }
            return;
        }
        if (life-- <= 0) { discard(); return; }
        if (life % 20 == 0) {
            shock((ServerLevel) level);
        }
        if (life % 5 == 0) {
            ((ServerLevel) level).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                    12, 3.0, 1.0, 3.0, 0.05);
        }
    }

    private void shock(ServerLevel level) {
        AABB box = new AABB(
                origin.getX() - RADIUS, origin.getY(), origin.getZ() - RADIUS,
                origin.getX() + RADIUS + 1, origin.getY() + HEIGHT, origin.getZ() + RADIUS + 1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
                continue; // Potion of Purity: immune to gases/clouds
            }
            e.addEffect(new MobEffectInstance(ModEffects.PARALYZED, ParalyzedEffect.DEFAULT_DURATION_TICKS, 0, false, true, true));
            float dmg = 2.0F + level.getRandom().nextFloat() * 1.5F;
            e.hurtServer(level, e.damageSources().lightningBolt(), dmg);
        }
        level.playSound(null, origin, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.35F, 1.4F);
    }
}
