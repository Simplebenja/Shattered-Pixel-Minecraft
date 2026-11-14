package com.drbenja.spd.effect;

import com.drbenja.spd.entity.classy.EntityClassHelper;

import net.minecraft.sounds.SoundSource;

import net.minecraft.sounds.SoundEvents;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD Frost. Immobilización (velocidad / inputs) vía ImmobilizeHandlers.
 * Este efecto solo maneja: fusión por fuego, chill al expirar en agua,
 * gracia de daño, overlay de freeze y FX.
 */
public class FrozenEffect extends MobEffect {

    public static final int DEFAULT_DURATION_TICKS = 200;
    public static final int ALLOW_CHILL_TICKS = 10;

    public static final String TAG_SKIP_WATER_CHILL = "spd_frozen_skip_water_chill";
    public static final String TAG_PENDING_WATER_CHILL = "spd_frozen_pending_water_chill";
    /** Hasta este gameTime el daño no rompe Frozen (evita el mismo golpe que convierte desde Chill). */
    public static final String TAG_DAMAGE_GRACE_UNTIL = "spd_frozen_dmg_grace_until";
    /** Skip apply SFX when extending an existing Frozen (frost cloud refresh). */
    public static final String TAG_SILENT_FX = "spd_frozen_silent_fx";
    public static final int DAMAGE_GRACE_TICKS = 10;

    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0xB0E0E6);
    }

    /** Delegates to ImmobilizeHandlers (kept for call sites that thaw Frozen). */
    public static void applyModifiers(LivingEntity entity) {
        com.drbenja.spd.event.effect.ImmobilizeHandlers.applySpeedLock(entity);
    }

    public static void clearModifiers(LivingEntity entity) {
        com.drbenja.spd.event.effect.ImmobilizeHandlers.clearModifiers(entity);
    }

    public static boolean isMelting(LivingEntity entity) {
        if (entity.isInLava() || entity.isOnFire() || entity.getRemainingFireTicks() > 0) {
            return true;
        }
        BlockPos pos = entity.blockPosition();
        for (BlockPos p : new BlockPos[]{pos, pos.above(), pos.below()}) {
            BlockState st = entity.level().getBlockState(p);
            if (st.getBlock() instanceof BaseFireBlock
                    || st.is(Blocks.LAVA)
                    || st.is(Blocks.FIRE)
                    || st.is(Blocks.SOUL_FIRE)
                    || (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT)
                    && Boolean.TRUE.equals(st.getValue(CampfireBlock.LIT)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWet(LivingEntity entity) {
        if (entity.isInWater() || entity.isUnderWater()) {
            return true;
        }
        BlockPos pos = entity.blockPosition();
        FluidState feet = entity.level().getFluidState(pos);
        FluidState below = entity.level().getFluidState(pos.below());
        FluidState eyes = entity.level().getFluidState(
                BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()));
        return feet.is(FluidTags.WATER) || below.is(FluidTags.WATER) || eyes.is(FluidTags.WATER);
    }

    public static void markSkipWaterChill(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_SKIP_WATER_CHILL, true);
        entity.getPersistentData().remove(TAG_PENDING_WATER_CHILL);
    }

    public static void grantDamageGrace(LivingEntity entity) {
        long until = entity.level().getGameTime() + DAMAGE_GRACE_TICKS;
        entity.getPersistentData().putLong(TAG_DAMAGE_GRACE_UNTIL, until);
    }

    public static boolean hasDamageGrace(LivingEntity entity) {
        long until = entity.getPersistentData().getLongOr(TAG_DAMAGE_GRACE_UNTIL, 0L);
        return entity.level().getGameTime() < until;
    }

    public static boolean consumeSkipWaterChill(LivingEntity entity) {
        var tag = entity.getPersistentData();
        boolean skip = tag.getBooleanOr(TAG_SKIP_WATER_CHILL, false);
        if (skip) {
            tag.remove(TAG_SKIP_WATER_CHILL);
        }
        return skip;
    }

    public static boolean allowsChilled(LivingEntity entity) {
        MobEffectInstance frozen = entity.getEffect(ModEffects.FROZEN);
        if (frozen == null) {
            return true;
        }
        return frozen.getDuration() <= ALLOW_CHILL_TICKS
                || entity.getPersistentData().getBooleanOr(TAG_PENDING_WATER_CHILL, false);
    }

    public static void finish(LivingEntity entity, boolean naturalExpire) {
        boolean skip = consumeSkipWaterChill(entity);
        boolean wet = isWet(entity);
        com.drbenja.spd.event.effect.ImmobilizeHandlers.clearModifiers(entity);
        try {
            entity.setTicksFrozen(0);
        } catch (Throwable ignored) {
        }

        if (!naturalExpire || skip || !wet) {
            entity.getPersistentData().remove(TAG_PENDING_WATER_CHILL);
            return;
        }

        entity.getPersistentData().putBoolean(TAG_PENDING_WATER_CHILL, true);
        if (entity.level() instanceof ServerLevel server) {
            server.getServer().execute(() -> applyPendingWaterChill(entity));
        }
    }

    public static void applyPendingWaterChill(LivingEntity entity) {
        var tag = entity.getPersistentData();
        if (!tag.getBooleanOr(TAG_PENDING_WATER_CHILL, false)) {
            return;
        }
        if (!entity.isAlive()) {
            tag.remove(TAG_PENDING_WATER_CHILL);
            return;
        }
        MobEffectInstance frozen = entity.getEffect(ModEffects.FROZEN);
        if (frozen != null && frozen.getDuration() > ALLOW_CHILL_TICKS) {
            return;
        }
        if (isMelting(entity) || !isWet(entity)) {
            tag.remove(TAG_PENDING_WATER_CHILL);
            return;
        }
        tag.remove(TAG_PENDING_WATER_CHILL);
        entity.addEffect(new MobEffectInstance(
                ModEffects.CHILLED,
                DEFAULT_DURATION_TICKS / 2,
                0,
                false,
                true,
                true));
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        entity.clearFire();
        entity.setRemainingFireTicks(0);
        if (entity.hasEffect(ModEffects.BURNING)) {
            entity.removeEffect(ModEffects.BURNING);
        }
        if (entity.hasEffect(ModEffects.CHILLED)) {
            ChilledEffect.clearModifier(entity);
            entity.removeEffect(ModEffects.CHILLED);
        }
        entity.getPersistentData().remove(TAG_PENDING_WATER_CHILL);
        grantDamageGrace(entity);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (isMelting(entity)) {
            markSkipWaterChill(entity);
            com.drbenja.spd.event.effect.ImmobilizeHandlers.clearModifiers(entity);
            entity.removeEffect(ModEffects.FROZEN);
            return false;
        }
        // Powder-snow style freeze overlay + freeze hearts while Frozen
        try {
            // Stay just at fully-frozen threshold so HUD frost hearts show; no powder-snow damage source
            int need = entity.getTicksRequiredToFreeze();
            entity.setTicksFrozen(need); // isFullyFrozen() == true for overlay/hearts
        } catch (Throwable ignored) {
        }

        MobEffectInstance self = entity.getEffect(ModEffects.FROZEN);
        if (self != null && self.getDuration() <= ALLOW_CHILL_TICKS && isWet(entity)) {
            entity.getPersistentData().putBoolean(TAG_PENDING_WATER_CHILL, true);
        }
        return true;
    }

    public static void markSilentFx(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_SILENT_FX, true);
    }

    public static boolean consumeSilentFx(LivingEntity entity) {
        var data = entity.getPersistentData();
        if (data.getBooleanOr(TAG_SILENT_FX, false)) {
            data.remove(TAG_SILENT_FX);
            return true;
        }
        return false;
    }

    /** Apply or extend Frozen. Never shortens. SFX only when newly frozen. */
    public static void applyOrExtend(LivingEntity entity, int ticks) {
        if (EntityClassHelper.immuneToChillFrozen(entity)) {
            return;
        }
        if (entity.level().isClientSide()) return;
        var existing = entity.getEffect(ModEffects.FROZEN);
        boolean was = existing != null;
        if (was && existing.getDuration() >= ticks) {
            return;
        }
        // Suppress event-based SFX (forceAddEffect may or may not fire Added)
        markSilentFx(entity);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.FROZEN, ticks, 0, false, true, true), null);
        if (!was) {
            playApplyFx(entity);
        }
    }

    /** Top up duration without any apply SFX (frost cloud maintenance). */
    public static void extendSilent(LivingEntity entity, int ticks) {
        if (entity.level().isClientSide()) return;
        var existing = entity.getEffect(ModEffects.FROZEN);
        if (existing != null && existing.getDuration() >= ticks) {
            return;
        }
        markSilentFx(entity);
        entity.forceAddEffect(new MobEffectInstance(
                ModEffects.FROZEN, ticks, 0, false, true, true), null);
        // never playApplyFx
    }

    public static void playApplyFx(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        // Hard cooldown: never spam apply SFX more than once per 40 ticks
        long now = level.getGameTime();
        long last = entity.getPersistentData().getLongOr("SpdFrozenApplyFxAt", 0L);
        if (now - last < 40L) {
            return;
        }
        entity.getPersistentData().putLong("SpdFrozenApplyFxAt", now);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_HURT_FREEZE, SoundSource.NEUTRAL, 1.0F, 0.9F);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                24, 0.35, 0.5, 0.35, 0.02);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.4, entity.getZ(),
                8, 0.25, 0.35, 0.25, 0.05);
    }

    public static void playShatterFx(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.85F, 1.15F);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                16, 0.3, 0.4, 0.3, 0.08);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                12, 0.3, 0.4, 0.3, 0.02);
        try {
            entity.setTicksFrozen(0);
        } catch (Throwable ignored) {
        }
    }
}
