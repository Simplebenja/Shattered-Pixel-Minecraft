package com.drbenja.spd.event.splash;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModPotions;

/**
 * Splash Snap Freeze: Frozen instantáneo en cubo 3×3×3 (10 s tierra / 30 s agua). Sin nube.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class SnapFreezeSplashHandler {

    public static final int FROZEN_DRY = 200;   // 10 s
    public static final int FROZEN_WET = 600;   // 30 s
    public static final double HALF = 1.5D;

    private SnapFreezeSplashHandler() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide()) {
            return;
        }
        if (!isThrownPotion(proj)) {
            return;
        }
        ItemStack item = getPotionItem(proj);
        if (item.isEmpty()) {
            return;
        }
        PotionContents contents = item.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return;
        }
        if (!contents.potion().get().is(ModPotions.SNAP_FREEZE)) {
            return;
        }

        HitResult hit = resolveHit(event);
        if (hit == null) {
            return;
        }
        Vec3 pos = hit.getLocation();
        ServerLevel level = (ServerLevel) proj.level();
        AABB box = new AABB(
                pos.x - HALF, pos.y - HALF, pos.z - HALF,
                pos.x + HALF, pos.y + HALF, pos.z + HALF);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (ChillImmuneEntitiesConfig.isChillImmune(entity)) {
                continue;
            }
            int ticks = entity.isInWater() || entity.isUnderWater() ? FROZEN_WET : FROZEN_DRY;
            entity.removeEffect(ModEffects.CHILLED);
            entity.removeEffect(ModEffects.SNAP_FREEZE_MARKER);
            // applyOrExtend: SFX only if newly frozen; never shortens
            com.drbenja.spd.effect.FrozenEffect.applyOrExtend(entity, ticks);
        }

        extinguish(level, BlockPos.containing(pos));
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.5, pos.z, 40, 1.0, 1.0, 1.0, 0.05);
    }

    private static void extinguish(ServerLevel level, BlockPos center) {
        int r = 1;
        for (BlockPos p : BlockPos.betweenClosed(
                center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r, center.getY() + r, center.getZ() + r)) {
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof BaseFireBlock || st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) {
                level.removeBlock(p, false);
            } else if (st.getBlock() instanceof CampfireBlock
                    && st.hasProperty(CampfireBlock.LIT)
                    && Boolean.TRUE.equals(st.getValue(CampfireBlock.LIT))) {
                level.setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
            }
        }
    }

    private static HitResult resolveHit(ProjectileImpactEvent event) {
        try {
            return (HitResult) event.getClass().getMethod("getHitResult").invoke(event);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            return (HitResult) event.getClass().getMethod("getRayTraceResult").invoke(event);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean isThrownPotion(Entity e) {
        String n = e.getClass().getName();
        return n.contains("ThrownPotion") || n.contains("AbstractThrownPotion")
                || n.contains("throwableitemprojectile");
    }

    private static ItemStack getPotionItem(Projectile proj) {
        try {
            Object o = proj.getClass().getMethod("getItem").invoke(proj);
            if (o instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return ItemStack.EMPTY;
    }

    /** Tooltip marker must not stay on entities; strip every tick. */
    @SubscribeEvent
    public static void onSnapFreezeTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (living.hasEffect(ModEffects.SNAP_FREEZE_MARKER)) {
            living.removeEffect(ModEffects.SNAP_FREEZE_MARKER);
        }
    }

}
