package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * Blinded (Shrouding Fog):
 * - Mobs: rango de detección efectivo 1 bloque; al aplicarse pierden el target actual
 *   pero mantienen pathfinding hacia la última posición conocida.
 * - Jugadores: niebla de visión ~1 bloque (cliente); pueden seguir corriendo.
 */
public class BlindedEffect extends MobEffect {

    /** Distancia máxima para mantener/adquirir target (bloques). */
    public static final double DETECT_RANGE = 1.25D;

    public BlindedEffect() {
        super(MobEffectCategory.HARMFUL, 0x555555);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        // Guardar último punto y soltar agro visual/target
        Vec3 last = target.position();
        mob.setTarget(null);
        PathNavigation nav = mob.getNavigation();
        if (nav != null) {
            nav.moveTo(last.x, last.y, last.z, 1.0);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof Mob mob)) {
            return true;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && mob.distanceTo(target) > DETECT_RANGE) {
            Vec3 last = target.position();
            mob.setTarget(null);
            PathNavigation nav = mob.getNavigation();
            if (nav != null) {
                nav.moveTo(last.x, last.y, last.z, 1.0);
            }
        }
        return true;
    }
}
