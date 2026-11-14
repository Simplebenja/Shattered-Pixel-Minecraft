package com.drbenja.spd.event.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.SpdVision;

/**
 * Lógica compartida de Terror y Dread (huida, daño acorta duración, no atacar).
 * El encantamiento Eldritch solo aplica el efecto; este handler gestiona el comportamiento.
 * Dread añade despawn al salir de la visión del jugador.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class TerrorDreadHandlers {

    private TerrorDreadHandlers() {}

    public static boolean isTerrified(LivingEntity e) {
        return e.hasEffect(ModEffects.TERROR) || e.hasEffect(ModEffects.DREAD);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel server)) return;
        if (!isTerrified(mob)) return;

        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }
        mob.setLastHurtByMob(null);
        mob.setLastHurtMob(null);

        if (mob.tickCount % 5 == 0) {
            var uuid = TerrorEffect.getSource(mob);
            if (uuid != null) {
                Entity src = server.getEntity(uuid);
                if (src instanceof LivingEntity source && source.isAlive()) {
                    double dx = mob.getX() - source.getX();
                    double dz = mob.getZ() - source.getZ();
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len < 0.1) {
                        dx = mob.getRandom().nextGaussian();
                        dz = mob.getRandom().nextGaussian();
                        len = Math.sqrt(dx * dx + dz * dz);
                    }
                    dx /= len;
                    dz /= len;
                    double dist = mob.distanceTo(source);
                    double step = dist < 12.0 ? 10.0 : 6.0;
                    mob.getNavigation().moveTo(mob.getX() + dx * step, mob.getY(), mob.getZ() + dz * step, 1.3);
                }
            }
        }

        // Dread despawn fuera de visión / lejos del jugador
        if (mob.hasEffect(ModEffects.DREAD)) {
            Player nearest = server.getNearestPlayer(mob, SpdVision.ALERT_RADIUS);
            if (nearest == null) {
                if (!mob.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
                    mob.discard();
                }
            } else {
                double vr = SpdVision.visionRadius(nearest);
                double despawnR = Math.max(24.0, vr * 2.5);
                if (mob.distanceToSqr(nearest) > despawnR * despawnR) {
                    if (!mob.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
                        mob.discard();
                    }
                }
            }
        }
    }

    /** SPD: daño reduce 5 turns/segundos de Terror o Dread. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamaged(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        shorten(e, ModEffects.TERROR);
        shorten(e, ModEffects.DREAD);
    }

    private static void shorten(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (!e.hasEffect(effect)) return;
        var inst = e.getEffect(effect);
        if (inst == null) return;
        int neu = Math.max(0, inst.getDuration() - 5 * 20);
        e.removeEffect(effect);
        if (neu > 0) {
            e.addEffect(new MobEffectInstance(effect, neu, 0, false, true, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!isTerrified(event.getEntity())) return;
        if (event.getNewAboutToBeSetTarget() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectile(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (!(e instanceof Projectile proj)) return;
        Entity owner = proj.getOwner();
        if (owner instanceof LivingEntity living && isTerrified(living)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingIncomingDamageEvent event) {
        Entity atk = event.getSource().getEntity();
        if (atk instanceof LivingEntity living && isTerrified(living)) {
            event.setAmount(0f);
            event.setCanceled(true);
        }
    }
}
