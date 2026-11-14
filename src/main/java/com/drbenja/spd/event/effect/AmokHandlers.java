package com.drbenja.spd.event.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AmokPathfinding;

/**
 * Amok: limpia agro al aplicar, busca el vivo más cercano como target,
 * peligros de daño ignorados en pathfinding (fuego/lava/etc.).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class AmokHandlers {

    private AmokHandlers() {}

    @SubscribeEvent
    public static void onApplied(MobEffectEvent.Added event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.AMOK.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        AmokPathfinding.apply(mob);

        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        if (mob.level() instanceof ServerLevel level) {
            LivingEntity nearest = findNearestLiving(level, mob, 16.0);
            if (nearest != null) {
                mob.setTarget(nearest);
                mob.getNavigation().moveTo(nearest, 1.25);
            }
        }
    }

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.AMOK.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!mob.hasEffect(ModEffects.AMOK)) {
            AmokPathfinding.restore(mob);
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.AMOK.get()) return;
        if (event.getEntity() instanceof Mob mob) {
            AmokPathfinding.restore(mob);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (!mob.hasEffect(ModEffects.AMOK)) {
            if (AmokPathfinding.isTracked(mob)) {
                AmokPathfinding.restore(mob);
            }
            return;
        }

        if (mob.tickCount % 40 == 0) {
            AmokPathfinding.apply(mob);
        }

        LivingEntity cur = mob.getTarget();
        if (cur == null || !cur.isAlive() || cur == mob) {
            if (mob.tickCount % 10 == 0) {
                LivingEntity nearest = findNearestLiving(level, mob, 24.0);
                if (nearest != null) {
                    mob.setTarget(nearest);
                    mob.getNavigation().moveTo(nearest, 1.2);
                }
            }
        } else if (mob.tickCount % 25 == 0) {
            LivingEntity nearest = findNearestLiving(level, mob, 24.0);
            if (nearest != null && nearest != cur
                    && nearest.distanceToSqr(mob) + 4.0 < cur.distanceToSqr(mob)) {
                mob.setTarget(nearest);
                mob.getNavigation().moveTo(nearest, 1.2);
            } else if (mob.getNavigation().isDone()) {
                // Solo re-path si ya terminó la ruta actual (evita cancelar rodeos a medias)
                mob.getNavigation().moveTo(cur, 1.2);
            }
        }
    }

    private static LivingEntity findNearestLiving(ServerLevel level, Mob self, double range) {
        AABB box = self.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == self) continue;
            double d = e.distanceToSqr(self);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }
}
