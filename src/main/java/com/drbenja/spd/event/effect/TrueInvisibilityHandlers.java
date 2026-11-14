package com.drbenja.spd.event.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.TrueInvisibilityEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * SPD-style invis:
 * - Se rompe al atacar (melee) o al DISPARAR arco/ballesta/tridente.
 * - NO se rompe al cargar arco/ballesta, ni al beber/lanzar pociones.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class TrueInvisibilityHandlers {

    /** Ticks de gracia tras aplicar (evita que el propio consumo de la poción lo rompa). */
    private static final int GRACE_TICKS = 10;
    private static final String KEY_APPLIED_AT = "spd_true_invis_applied_at";

    private TrueInvisibilityHandlers() {}

    private static boolean hasInvis(LivingEntity e) {
        return e.hasEffect(ModEffects.TRUE_INVISIBILITY);
    }

    private static boolean inGrace(LivingEntity e) {
        long applied = e.getPersistentData().getLongOr(KEY_APPLIED_AT, -1000L);
        return e.level().getGameTime() - applied < GRACE_TICKS;
    }

    private static void breakInvis(LivingEntity e) {
        if (!hasInvis(e) || inGrace(e)) {
            return;
        }
        e.removeEffect(ModEffects.TRUE_INVISIBILITY);
    }

    public static void markApplied(LivingEntity e) {
        e.getPersistentData().putLong(KEY_APPLIED_AT, e.level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity aboutTo = event.getNewAboutToBeSetTarget();
        if (aboutTo != null && hasInvis(aboutTo)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent
    public static void onCritical(CriticalHitEvent event) {
        if (hasInvis(event.getEntity())) {
            event.setCriticalHit(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        breakInvis(event.getEntity());
    }

    @SubscribeEvent
    public static void onDealDamage(LivingIncomingDamageEvent event) {
        Entity src = event.getSource().getEntity();
        if (src instanceof LivingEntity attacker) {
            breakInvis(attacker);
        }
    }

    /** Solo al spawnear la flecha/tridente (disparo real), no al cargar. */
    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (!(e instanceof Projectile proj)) {
            return;
        }
        Entity owner = proj.getOwner();
        if (!(owner instanceof LivingEntity living) || !hasInvis(living)) {
            return;
        }
        if (proj instanceof AbstractArrow || proj instanceof ThrownTrident) {
            breakInvis(living);
        }
    }

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst != null && inst.getEffect().is(ModEffects.TRUE_INVISIBILITY)) {
            markApplied(event.getEntity());
        }
    }

    /**
     * forceAddEffect / duration refresh can fire Remove+Add on the same tick.
     * Clearing invisibility immediately causes a 1-tick visible flash (vanilla
     * INVISIBILITY avoids this because the engine owns that flag).
     * Defer clear and only drop the flag if the effect is truly gone.
     */
    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null || !inst.getEffect().is(ModEffects.TRUE_INVISIBILITY)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        scheduleInvisibilityReconcile(entity);
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null || !inst.getEffect().is(ModEffects.TRUE_INVISIBILITY)) {
            return;
        }
        scheduleInvisibilityReconcile(event.getEntity());
    }

    private static void scheduleInvisibilityReconcile(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        // Next server tick: if true-invis was only refreshed, keep vanilla visual;
        // if truly gone, drop the visual support we attached (no setInvisible toggle).
        entity.level().getServer().execute(() -> {
            if (entity.hasEffect(ModEffects.TRUE_INVISIBILITY)) {
                MobEffectInstance self = entity.getEffect(ModEffects.TRUE_INVISIBILITY);
                if (self != null) {
                    TrueInvisibilityEffect.syncVanillaVisual(entity, self.getDuration());
                }
            } else {
                TrueInvisibilityEffect.clearVisualSupport(entity);
            }
        });
    }
}
