package com.drbenja.spd.item.stone;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SPD Stone of Blast: daño de explosión SIN romper bloques.
 * Afecta a todas las entidades vivas en el radio, incluido el lanzador.
 */
public class StoneOfBlastItem extends SpdStoneItem {
    private static final double RADIUS = 2.5;

    public StoneOfBlastItem(Properties props) {
        super(props);
    }

    @Override
    public void onImpact(ServerLevel level, Player thrower, HitResult hit, Vec3 impactPos) {
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.55F, 1.15F);

        level.sendParticles(ParticleTypes.EXPLOSION,
                impactPos.x, impactPos.y + 0.2, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SMOKE,
                impactPos.x, impactPos.y + 0.3, impactPos.z, 8, RADIUS * 0.35, 0.2, RADIUS * 0.35, 0.01);
        level.sendParticles(ParticleTypes.POOF,
                impactPos.x, impactPos.y + 0.2, impactPos.z, 6, RADIUS * 0.25, 0.15, RADIUS * 0.25, 0.02);
        for (int i = 0; i < 12; i++) {
            double a = (Math.PI * 2 * i) / 12.0;
            double px = impactPos.x + Math.cos(a) * RADIUS * 0.85;
            double pz = impactPos.z + Math.sin(a) * RADIUS * 0.85;
            level.sendParticles(ParticleTypes.SMOKE, px, impactPos.y + 0.15, pz, 1, 0.02, 0.02, 0.02, 0.0);
        }

        // Fuente neutra: explosion(lanzador, lanzador) en vanilla no daña al propio lanzador
        DamageSource src = level.damageSources().magic();

        AABB box = new AABB(impactPos, impactPos).inflate(RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            double dist = e.distanceToSqr(impactPos.x, impactPos.y, impactPos.z);
            if (dist > RADIUS * RADIUS) continue;
            float falloff = 1.0f - (float) (Math.sqrt(dist) / RADIUS);
            float dmg = 2.0f + 8.0f * falloff;
            e.hurtServer(level, src, dmg);
        }
    }
}
