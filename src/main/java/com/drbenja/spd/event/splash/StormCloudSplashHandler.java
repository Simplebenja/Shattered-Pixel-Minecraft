package com.drbenja.spd.event.splash;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.cloud.StormCloudEntity;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.init.ModPotions;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class StormCloudSplashHandler {

    private static final int AIR_SEARCH_LIMIT = 64;
    private static final Identifier STORM_ID =
            Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "potion_of_storm_clouds");

    private StormCloudSplashHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide()) return;
        if (!isThrownPotion(proj)) return;
        ItemStack item = getPotionItem(proj);
        if (item.isEmpty() || !isStormCloud(item)) return;

        HitResult hit = resolveHit(event);
        ServerLevel level = (ServerLevel) proj.level();
        BlockPos seedPos = hit != null
                ? resolveOpenAirSeed(level, hit, proj)
                : resolveOpenAirSeed(level, BlockPos.containing(proj.position()), proj);

        StormCloudEntity cloud = new StormCloudEntity(ModEntities.STORM_CLOUD.get(), level);
        cloud.setPos(seedPos.getX() + 0.5, seedPos.getY() + 0.5, seedPos.getZ() + 0.5);
        cloud.seed(seedPos, StormCloudEntity.SEED_AMOUNT);
        level.addFreshEntity(cloud);

        Vec3 pos = Vec3.atCenterOf(seedPos);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.5F, 1.0F);
    }

    private static boolean isStormCloud(ItemStack item) {
        PotionContents contents = item.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        try {
            var opt = contents.potion();
            if (opt != null && !opt.isEmpty()) {
                if (opt.get().is(STORM_ID)) return true;
                if (opt.get().is(ModPotions.STORM_CLOUDS.getKey())) return true;
                Potion potion = opt.get().value();
                Identifier id = BuiltInRegistries.POTION.getKey(potion);
                if (id != null && id.equals(STORM_ID)) return true;
            }
        } catch (Throwable ignored) {}
        String s = item.toString();
        return s.contains("potion_of_storm_clouds") || s.contains("storm_clouds");
    }

    private static BlockPos resolveOpenAirSeed(Level level, HitResult hit, Projectile proj) {
        BlockPos hitPos = BlockPos.containing(hit.getLocation());
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos outside = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (!isBlocking(level, outside)) return outside.immutable();
            if (!isBlocking(level, hitPos)) return hitPos.immutable();
        }
        if (!isBlocking(level, hitPos)) return hitPos.immutable();
        Vec3 motion = proj.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6) {
            BlockPos backPos = BlockPos.containing(hit.getLocation().subtract(motion.normalize().scale(0.6)));
            if (!isBlocking(level, backPos)) return backPos.immutable();
        }
        BlockPos found = findNearestOpenAir(level, hitPos);
        return found != null ? found : hitPos.immutable();
    }

    private static BlockPos resolveOpenAirSeed(Level level, BlockPos start, Projectile proj) {
        if (!isBlocking(level, start)) return start.immutable();
        BlockPos found = findNearestOpenAir(level, start);
        return found != null ? found : start.immutable();
    }

    private static BlockPos findNearestOpenAir(Level level, BlockPos start) {
        Queue<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        q.add(start.immutable());
        seen.add(start.immutable());
        int visited = 0;
        while (!q.isEmpty() && visited < AIR_SEARCH_LIMIT) {
            BlockPos cur = q.poll();
            visited++;
            if (!isBlocking(level, cur)) return cur;
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (seen.add(n.immutable())) q.add(n.immutable());
            }
        }
        return null;
    }

    private static boolean isBlocking(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static HitResult resolveHit(ProjectileImpactEvent event) {
        try { return (HitResult) event.getClass().getMethod("getHitResult").invoke(event); }
        catch (ReflectiveOperationException ignored) {}
        try { return (HitResult) event.getClass().getMethod("getRayTraceResult").invoke(event); }
        catch (ReflectiveOperationException ignored) {}
        return null;
    }

    private static boolean isThrownPotion(Entity e) {
        String n = e.getClass().getName().toLowerCase();
        return n.contains("thrownpotion") || n.contains("abstractthrownpotion")
                || n.contains("throwableitemprojectile");
    }

    private static ItemStack getPotionItem(Projectile proj) {
        try {
            Object o = proj.getClass().getMethod("getItem").invoke(proj);
            if (o instanceof ItemStack stack) return stack;
        } catch (ReflectiveOperationException ignored) {}
        return ItemStack.EMPTY;
    }
}
