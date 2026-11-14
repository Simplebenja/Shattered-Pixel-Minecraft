package com.drbenja.spd.event.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.SpdVision;

/**
 * Aggression: mobs hostiles en radio de visión redirigen target a la entidad marcada.
 * Al acabar: limpian agro hacia ella y el agro de ella hacia otros (SPD detach).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class AggressionHandlers {

    private AggressionHandlers() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity marked)) return;
        if (marked.level().isClientSide()) return;
        if (!(marked.level() instanceof ServerLevel level)) return;
        if (!marked.hasEffect(ModEffects.AGGRESSION)) return;
        if (marked.tickCount % 10 != 0) return;

        double r = SpdVision.BASE_RADIUS;
        AABB box = marked.getBoundingBox().inflate(r);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m != marked)) {
            if (!isHostileish(mob)) continue;
            if (mob.distanceToSqr(marked) > r * r) continue;
            // No forzar bosses a atacar si están inmunes a la marca (ya no se aplica a bosses)
            if (mob.getTarget() != marked) {
                mob.setTarget(marked);
            }
        }
    }

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.AGGRESSION.get()) return;
        clearAggro(event.getEntity());
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.AGGRESSION.get()) return;
        clearAggro(event.getEntity());
    }

    private static void clearAggro(LivingEntity marked) {
        if (!(marked.level() instanceof ServerLevel level)) return;
        // Enemigos que tenían a marked como target
        AABB box = marked.getBoundingBox().inflate(SpdVision.ALERT_RADIUS);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (m.getTarget() == marked) {
                m.setTarget(null);
                m.setLastHurtByMob(null);
            }
        }
        // La propia entidad marcada limpia su target (evita pelea entre mobs residual)
        if (marked instanceof Mob self) {
            self.setTarget(null);
            self.setLastHurtByMob(null);
        }
    }

    private static boolean isHostileish(Mob mob) {
        return mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER;
    }
}
