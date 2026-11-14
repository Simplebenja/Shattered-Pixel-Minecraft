package com.drbenja.spd.event.effect;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.CleansedEffect;
import com.drbenja.spd.effect.FixedHoverEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Purification Barrier (Purity): block environmental harm, mitigate slow terrain/fluids.
 * Split from former PurityCleansingHandlers.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class PurityHandlers {

    public static final TagKey<DamageType> PURIFICATION_BLOCKED = TagKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("spd", "purification_blocked"));

    /** entityId -> gameTime until which splash/lingering environmental effects are blocked */
    private static final Map<Integer, Long> SPLASH_HIT_UNTIL = new ConcurrentHashMap<>();

    private PurityHandlers() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Entity proj = event.getProjectile();
        if (!(proj instanceof AbstractThrownPotion) && !isThrownPotionClass(proj)) {
            return;
        }
        Level level = proj.level();
        if (level.isClientSide()) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        Vec3 pos = hit != null ? hit.getLocation() : proj.position();
        double r = 4.0D;
        AABB box = new AABB(pos.x - r, pos.y - 2.0D, pos.z - r, pos.x + r, pos.y + 2.0D, pos.z + r);
        long until = level.getGameTime() + 10L;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            SPLASH_HIT_UNTIL.put(e.getId(), until);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        long now = event.getLevel().getGameTime();
        if ((now & 31L) != 0L) {
            return;
        }
        Iterator<Map.Entry<Integer, Long>> it = SPLASH_HIT_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < now) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return;
        }
        if (event.getSource().is(PURIFICATION_BLOCKED)
                || com.drbenja.spd.config.effect.PurityConfig.isDamageBlocked(event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * Mitiga ralentización por bloques (telaraña, soul sand, honey, slime, powder snow).
     */
    @SubscribeEvent
    public static void onEntityTickPuritySlow(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        if (!entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return;
        }
        try {
            entity.getClass().getMethod("setIsInPowderSnow", boolean.class).invoke(entity, false);
        } catch (ReflectiveOperationException ignored) {
        }
        BlockPos pos = entity.blockPosition();
        var level = entity.level();
        var feet = level.getBlockState(pos);
        var below = level.getBlockState(pos.below());
        if (feet.is(Blocks.COBWEB) || level.getBlockState(pos.above()).is(Blocks.COBWEB)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(3.5, 3.5, 3.5));
        }
        if (below.is(Blocks.SOUL_SAND) || below.is(Blocks.SOUL_SOIL)
                || below.is(Blocks.HONEY_BLOCK) || below.is(Blocks.SLIME_BLOCK)
                || feet.is(Blocks.HONEY_BLOCK) || feet.is(Blocks.SLIME_BLOCK)) {
            Vec3 m = entity.getDeltaMovement();
            if (m.horizontalDistanceSqr() > 1.0E-6) {
                entity.setDeltaMovement(m.x * 1.35, m.y, m.z * 1.35);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null) {
            return;
        }
        // Cleansed handled in CleansingHandlers
        if (entity.hasEffect(ModEffects.CLEANSED)) {
            return;
        }
        if (!entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return;
        }
        if (!isHarmful(incoming)) {
            return;
        }
        if (isEnvironmentalPotionSource(event.getEffectSource()) || isRecentlySplashHit(entity)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return;
        }
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null || !isHarmful(inst)) {
            return;
        }
        if (isEnvironmentalPotionSource(event.getEffectSource()) || isRecentlySplashHit(entity)) {
            if (entity.hasEffect(inst.getEffect())) {
                entity.removeEffect(inst.getEffect());
            }
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        if (inst.getEffect().value() instanceof FixedHoverEffect) {
            FixedHoverEffect.clearHover(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        if (inst.getEffect().value() instanceof FixedHoverEffect) {
            FixedHoverEffect.clearHover(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        if (!entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return;
        }

        entity.makeStuckInBlock(Blocks.AIR.defaultBlockState(), new Vec3(1.0D, 1.0D, 1.0D));
        entity.setIsInPowderSnow(false);
        if (entity.getTicksFrozen() > 0) {
            entity.setTicksFrozen(0);
        }

        if (intersectsWitherRose(entity) && entity.hasEffect(MobEffects.WITHER)) {
            entity.removeEffect(MobEffects.WITHER);
        }

        BlockPos below = entity.getBlockPosBelowThatAffectsMyMovement();
        var floor = entity.level().getBlockState(below);
        if (floor.is(Blocks.SOUL_SAND) || floor.is(Blocks.SOUL_SOIL) || floor.is(Blocks.HONEY_BLOCK)) {
            Vec3 v = entity.getDeltaMovement();
            if (v.horizontalDistanceSqr() > 1.0E-6) {
                entity.setDeltaMovement(v.x * 1.35D, v.y, v.z * 1.35D);
            }
        }
    }

    @SubscribeEvent
    public static void onPurityLavaTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (!living.hasEffect(ModEffects.PURIFICATION_BARRIER)) return;
        boolean inLava = living.isInLava();
        boolean inWater = living.isInWater() && !living.isSwimming();
        if (!inLava && !inWater) return;
        var m = living.getDeltaMovement();
        double hx = m.x;
        double hz = m.z;
        double h = Math.sqrt(hx * hx + hz * hz);
        if (h > 1.0E-5 && h < 0.22) {
            double target = inLava ? 0.12 : 0.10;
            double scale = Math.min(inLava ? 3.5 : 2.0, target / h);
            living.setDeltaMovement(hx * scale, m.y, hz * scale);
        }
    }

    static boolean isRecentlySplashHit(LivingEntity entity) {
        Long until = SPLASH_HIT_UNTIL.get(entity.getId());
        return until != null && entity.level().getGameTime() <= until;
    }

    private static boolean isHarmful(MobEffectInstance incoming) {
        return incoming.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                || CleansedEffect.shouldAffect(incoming.getEffect());
    }

    private static boolean isEnvironmentalPotionSource(Entity source) {
        if (source == null) {
            return false;
        }
        if (source instanceof AreaEffectCloud || source instanceof AbstractThrownPotion) {
            return true;
        }
        return isThrownPotionClass(source) || source.getClass().getName().contains("AreaEffectCloud");
    }

    private static boolean isThrownPotionClass(Entity source) {
        String name = source.getClass().getName();
        return name.contains("ThrownSplashPotion")
                || name.contains("ThrownLingeringPotion")
                || name.contains("AbstractThrownPotion")
                || name.contains("ThrownPotion");
    }

    private static boolean intersectsWitherRose(LivingEntity entity) {
        BlockPos min = entity.blockPosition().offset(-1, -1, -1);
        BlockPos max = entity.blockPosition().offset(1, 1, 1);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (entity.level().getBlockState(pos).is(Blocks.WITHER_ROSE)) {
                return true;
            }
        }
        return false;
    }
}
