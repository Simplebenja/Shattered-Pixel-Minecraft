package com.drbenja.spd.event.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/**
 * Drowsy + Magical Sleep.
 * Drowsy al expirar → Magical Sleep; daño interrumpe ambos.
 * Inmovilización del sueño: {@link ImmobilizeHandlers}.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class SleepHandlers {

    private SleepHandlers() {}

    // ---- Drowsy ----

    @SubscribeEvent
    public static void onDrowsyExpired(MobEffectEvent.Expired event) {
        // Holder comparison: use .is(...), never == / != with .get()
        if (!event.getEffectInstance().getEffect().is(ModEffects.DROWSY)) return;
        tryApplyMagicalSleep(event.getEntity());
    }

    /** Also catch natural end paths that fire Remove instead of Expired on some versions. */
    @SubscribeEvent
    public static void onDrowsyRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (!event.getEffectInstance().getEffect().is(ModEffects.DROWSY)) return;
        // Only when it actually expired (duration ran out), not when cleared by damage/command
        if (event.getEffectInstance().getDuration() > 1) return;
        tryApplyMagicalSleep(event.getEntity());
    }

    private static void tryApplyMagicalSleep(LivingEntity living) {
        if (living == null || living.level().isClientSide()) return;
        if (!(living.level() instanceof ServerLevel level)) return;
        if (living.hasEffect(ModEffects.MAGICAL_SLEEP)) return;

        boolean playerOrAlly = living instanceof Player
                || living instanceof OwnableEntity
                || isAlliedToNearbyPlayer(level, living);

        if (playerOrAlly) {
            if (living.getHealth() < living.getMaxHealth()) {
                living.addEffect(new MobEffectInstance(ModEffects.MAGICAL_SLEEP,
                        MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
            }
        } else {
            living.addEffect(new MobEffectInstance(ModEffects.MAGICAL_SLEEP,
                    MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
        }
    }

    // ---- Magical Sleep ----

    @SubscribeEvent
    public static void onSleepTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (!(living.level() instanceof ServerLevel level)) return;
        if (!living.hasEffect(ModEffects.MAGICAL_SLEEP)) return;

        if (living.tickCount % 30 == 0) {
            level.sendParticles(ParticleTypes.NOTE,
                    living.getX(), living.getY() + living.getBbHeight() + 0.35, living.getZ(),
                    1, 0.2, 0.1, 0.2, 0.0);
            level.sendParticles(ParticleTypes.CLOUD,
                    living.getX(), living.getY() + living.getBbHeight() * 0.7, living.getZ(),
                    2, 0.12, 0.08, 0.12, 0.005);
        }

        boolean healable = living instanceof Player || living instanceof OwnableEntity;
        if (healable && living.tickCount % 20 == 0) {
            if (living.getHealth() < living.getMaxHealth()) {
                living.heal(1.0f);
            } else {
                living.removeEffect(ModEffects.MAGICAL_SLEEP);
            }
        }
    }

    /**
     * Mark this tick as successfully shield-blocked (NeoForge LivingShieldBlockEvent).
     */
    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.hasEffect(ModEffects.DROWSY) && !e.hasEffect(ModEffects.MAGICAL_SLEEP)) return;
        // Only when the event actually blocked something
        try {
            if (!event.getBlocked()) return;
        } catch (Throwable ignored) {
            // older API without getBlocked() — still mark
        }
        e.getPersistentData().putLong("spd_sleep_shield_tick", e.level().getGameTime());
    }

    /**
     * NeoForge 26.x: LivingDamageEvent.Post uses getHealthDamage() / getInflictedDamage()
     * (getNewDamage was removed). Only real HP loss breaks sleep; pure shield blocks do not.
     */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity e = event.getEntity();
        if (!e.hasEffect(ModEffects.DROWSY) && !e.hasEffect(ModEffects.MAGICAL_SLEEP)) return;

        long now = e.level().getGameTime();
        if (e.getPersistentData().getLongOr("spd_sleep_shield_tick", -1L) == now) {
            return;
        }

        float healthLost = 0f;
        float inflicted = 0f;
        float blocked = 0f;
        try { healthLost = event.getHealthDamage(); } catch (Throwable ignored) {}
        try { inflicted = event.getInflictedDamage(); } catch (Throwable ignored) {}
        try { blocked = event.getBlockedDamage(); } catch (Throwable ignored) {}
        // Legacy 1.21 name if present on some builds
        try {
            if (healthLost <= 0f && inflicted <= 0f) {
                var m = event.getClass().getMethod("getNewDamage");
                Object v = m.invoke(event);
                if (v instanceof Float f) healthLost = f;
            }
        } catch (Throwable ignored) {}

        // Fully absorbed by shield / zero HP loss
        if (healthLost <= 0f && inflicted <= 0f) return;
        if (blocked > 0f && healthLost <= 0f && inflicted <= 0f) return;

        float dmg = Math.max(healthLost, inflicted);
        if (dmg <= 0f) return;

        if (e.hasEffect(ModEffects.DROWSY)) {
            e.removeEffect(ModEffects.DROWSY);
        }
        if (e.hasEffect(ModEffects.MAGICAL_SLEEP)) {
            e.removeEffect(ModEffects.MAGICAL_SLEEP);
        }
    }

    /**
     * Fallback for cases where Post is not fired (0 damage sequences).
     * Does not wake while this tick was shield-blocked.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageIncoming(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) return;
        LivingEntity e = event.getEntity();
        if (!e.hasEffect(ModEffects.DROWSY) && !e.hasEffect(ModEffects.MAGICAL_SLEEP)) return;
        long now = e.level().getGameTime();
        if (e.getPersistentData().getLongOr("spd_sleep_shield_tick", -1L) == now) {
            return;
        }
        // Prefer Post for players; for mobs Incoming is the reliable path
        if (e instanceof Player) {
            return;
        }
        if (event.getAmount() <= 0.0F) return;
        if (e.hasEffect(ModEffects.DROWSY)) {
            e.removeEffect(ModEffects.DROWSY);
        }
        if (e.hasEffect(ModEffects.MAGICAL_SLEEP)) {
            e.removeEffect(ModEffects.MAGICAL_SLEEP);
        }
    }

    /**
     * Magical Sleep: mute ambient / step / attack noise; keep hurt & death feedback.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!living.hasEffect(ModEffects.MAGICAL_SLEEP)) return;
        if (!(living instanceof Mob)) return;
        var sound = event.getSound();
        if (sound == null) return;
        String path = "";
        try {
            path = sound.unwrapKey()
                    .map(k -> k.identifier().getPath())
                    .orElseGet(() -> {
                        try {
                            return sound.value().location().getPath();
                        } catch (Exception ex) {
                            return "";
                        }
                    });
        } catch (Exception e) {
            path = "";
        }
        path = path == null ? "" : path.toLowerCase();
        // Always allow pain / death / drowning
        if (path.contains("hurt") || path.contains("death") || path.contains("die")
                || path.contains("drown")) {
            return;
        }
        // Ambient / idle only, and rarely (≈1/10)
        boolean ambient = path.contains("ambient") || path.contains("idle")
                || path.contains("breathe") || path.contains("snore");
        if (ambient && living.getRandom().nextInt(10) == 0) {
            return;
        }
        // Cancel steps, attack, celebrate, raid horns, etc.
        event.setCanceled(true);
    }

    private static boolean isAlliedToNearbyPlayer(ServerLevel level, LivingEntity living) {
        Player nearest = level.getNearestPlayer(living, 48);
        return nearest != null && living.isAlliedTo(nearest);
    }
}
