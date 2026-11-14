package com.drbenja.spd.event.splash;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.cloud.FrostCloudEntity;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.init.ModPotions;

/**
 * Splash Potion of Frost → nube 3×3×3 (blob Freezing).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class FrostSplashHandler {

    private FrostSplashHandler() {}

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
        if (!contents.potion().get().is(ModPotions.FROST)) {
            return;
        }

        HitResult hit = resolveHit(event);
        if (hit == null) {
            return;
        }
        Vec3 pos = hit.getLocation();
        double y = pos.y + 0.5D;

        ServerLevel level = (ServerLevel) proj.level();
        FrostCloudEntity cloud = new FrostCloudEntity(ModEntities.FROST_CLOUD.get(), level);
        cloud.setPos(pos.x, y, pos.z);
        level.addFreshEntity(cloud);

        level.playSound(null, pos.x, y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, y, pos.z, 24, 0.8, 0.8, 0.8, 0.02);
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
        return n.contains("ThrownPotion")
                || n.contains("AbstractThrownPotion")
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
