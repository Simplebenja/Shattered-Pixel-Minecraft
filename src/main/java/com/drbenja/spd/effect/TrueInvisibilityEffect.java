package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.event.effect.TrueInvisibilityHandlers;

/**
 * SPD True Invisibility (logic). Visual stealth is delegated to vanilla
 * {@link MobEffects#INVISIBILITY} so duration refreshes do not flash the player
 * (vanilla owns the invisible flag; our custom setInvisible toggles caused flicker).
 */
public class TrueInvisibilityEffect extends MobEffect {

    /** NBT: we attached vanilla INVISIBILITY as visual support for this effect. */
    public static final String KEY_OWNS_VANILLA_VISUAL = "spd_true_invis_owns_vanilla_visual";

    public TrueInvisibilityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF0F0F0);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Keep vanilla visual in sync (handles edge cases where it was cleared)
        MobEffectInstance self = entity.getEffect(com.drbenja.spd.init.ModEffects.TRUE_INVISIBILITY);
        if (self != null) {
            syncVanillaVisual(entity, self.getDuration());
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        TrueInvisibilityHandlers.markApplied(entity);
        MobEffectInstance self = entity.getEffect(com.drbenja.spd.init.ModEffects.TRUE_INVISIBILITY);
        int dur = self != null ? self.getDuration() : 20 * 20;
        if (dur == MobEffectInstance.INFINITE_DURATION) {
            dur = 20 * 60; // safety; true invis potions are timed
        }
        boolean first = !entity.getPersistentData().getBooleanOr(KEY_OWNS_VANILLA_VISUAL, false);
        syncVanillaVisual(entity, dur);
        if (first) {
            clearAggro(entity);
        }
    }

    /**
     * Applies/refreshes vanilla INVISIBILITY for the visual only.
     * ambient=true, no particles — avoids potion-cloud look; no flag toggling on our side.
     */
    public static void syncVanillaVisual(LivingEntity entity, int durationTicks) {
        if (entity.level().isClientSide()) return;
        if (durationTicks < 0) {
            durationTicks = 40;
        }
        // Do not shorten a longer vanilla invis potion the player already has
        MobEffectInstance existing = entity.getEffect(MobEffects.INVISIBILITY);
        if (existing != null && existing.getDuration() > durationTicks
                && !entity.getPersistentData().getBooleanOr(KEY_OWNS_VANILLA_VISUAL, false)) {
            // Player has their own longer potion — leave it; still mark we rely on invis flag
            return;
        }
        entity.getPersistentData().putBoolean(KEY_OWNS_VANILLA_VISUAL, true);
        entity.forceAddEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                durationTicks,
                0,
                true,  // ambient
                false, // no particles
                false  // no icon (True Invisibility icon is enough)
        ), null);
    }

    public static void clearAggro(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        AABB box = entity.getBoundingBox().inflate(64.0D);
        for (Mob mob : entity.level().getEntitiesOfClass(Mob.class, box)) {
            if (mob.getTarget() == entity) {
                mob.setTarget(null);
            }
        }
    }

    /**
     * Called when True Invisibility is truly gone. Removes only the vanilla visual
     * we attached; does not call setInvisible (vanilla effect removal handles that).
     */
    public static void clearVisualSupport(LivingEntity entity) {
        if (!entity.getPersistentData().getBooleanOr(KEY_OWNS_VANILLA_VISUAL, false)) {
            return;
        }
        entity.getPersistentData().remove(KEY_OWNS_VANILLA_VISUAL);
        if (entity.hasEffect(MobEffects.INVISIBILITY)) {
            entity.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    /** @deprecated use {@link #clearVisualSupport} — no longer toggles setInvisible */
    @Deprecated
    public static void clearInvisibleFlag(LivingEntity entity) {
        clearVisualSupport(entity);
    }
}

