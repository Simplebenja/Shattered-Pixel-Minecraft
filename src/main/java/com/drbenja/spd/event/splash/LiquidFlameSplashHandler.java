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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.BurningEffect;
import com.drbenja.spd.effect.ChilledEffect;
import com.drbenja.spd.effect.FrozenEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModPotions;

/**
 * Splash Potion of Liquid Flame: cubo 3×3×3, Burning + incendio de terreno.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class LiquidFlameSplashHandler {

    public static final double HALF = 1.5D;

    private LiquidFlameSplashHandler() {}

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

        // Cualquier splash: apaga Burning en el radio de salpicadura vanilla (~4)
        boolean isLiquidFlame = contents.potion().get().is(ModPotions.LIQUID_FLAME);
        HitResult hit = resolveHit(event);
        if (hit == null) {
            return;
        }
        Vec3 pos = hit.getLocation();
        ServerLevel level = (ServerLevel) proj.level();

        if (!isLiquidFlame) {
            // Extinguir Burning por salpicadura de cualquier poción splash
            AABB splash = new AABB(pos.x - 4, pos.y - 2, pos.z - 4, pos.x + 4, pos.y + 2, pos.z + 4);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, splash, Entity::isAlive)) {
                if (e.hasEffect(ModEffects.BURNING)) {
                    e.clearFire();
                    e.removeEffect(ModEffects.BURNING);
                }
            }
            return;
        }

        AABB box = new AABB(
                pos.x - HALF, pos.y - HALF, pos.z - HALF,
                pos.x + HALF, pos.y + HALF, pos.z + HALF);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            entity.removeEffect(ModEffects.CHILLED);
            entity.removeEffect(ModEffects.FROZEN);
            ChilledEffect.clearModifier(entity);
            FrozenEffect.clearModifiers(entity);
            entity.addEffect(new MobEffectInstance(
                    ModEffects.BURNING, BurningEffect.DEFAULT_DURATION_TICKS, 0, false, true, true));
        }

        BurningEffect.affectTerrain(level, BlockPos.containing(pos));

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5, pos.z, 40, 1.0, 1.0, 1.0, 0.05);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.3, pos.z, 8, 0.6, 0.4, 0.6, 0.02);
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
}
