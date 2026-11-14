package com.drbenja.spd.event.splash;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.cloud.ParalyticGasCloudEntity;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.init.ModPotions;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ParalyticGasSplashHandler {

    private static final int AIR_SEARCH_LIMIT = 64;

    private ParalyticGasSplashHandler() {}

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
        if (!contents.potion().get().is(ModPotions.PARALYTIC_GAS)) {
            return;
        }
        HitResult hit = resolveHit(event);
        if (hit == null) {
            return;
        }
        ServerLevel level = (ServerLevel) proj.level();
        BlockPos seedPos = resolveOpenAirSeed(level, hit, proj);

        ParalyticGasCloudEntity cloud = new ParalyticGasCloudEntity(ModEntities.PARALYTIC_GAS.get(), level);
        cloud.setPos(seedPos.getX() + 0.5, seedPos.getY() + 0.5, seedPos.getZ() + 0.5);
        cloud.seed(seedPos, ParalyticGasCloudEntity.SEED_AMOUNT);
        level.addFreshEntity(cloud);

        Vec3 pos = Vec3.atCenterOf(seedPos);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6F, 0.6F);
    }

    /**
     * Si el impacto cae en sólido (pared/suelo), coloca el gas en el aire más cercano
     * (preferente: cara de impacto o celda anterior del proyectil).
     */
    private static BlockPos resolveOpenAirSeed(Level level, HitResult hit, Projectile proj) {
        BlockPos hitPos = BlockPos.containing(hit.getLocation());

        // Bloque golpeado + normal hacia el aire
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos against = blockHit.getBlockPos();
            BlockPos outside = against.relative(blockHit.getDirection());
            if (!isBlocking(level, outside)) {
                return outside.immutable();
            }
            // A veces la normal apunta mal; probar la celda del impacto si es aire
            if (!isBlocking(level, hitPos)) {
                return hitPos.immutable();
            }
        }

        if (!isBlocking(level, hitPos)) {
            return hitPos.immutable();
        }

        // Retroceder un poco por la trayectoria del proyectil
        Vec3 motion = proj.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6) {
            Vec3 back = hit.getLocation().subtract(motion.normalize().scale(0.6));
            BlockPos backPos = BlockPos.containing(back);
            if (!isBlocking(level, backPos)) {
                return backPos.immutable();
            }
        }

        // BFS al aire más cercano
        BlockPos found = findNearestOpenAir(level, hitPos);
        return found != null ? found : hitPos.immutable();
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
            if (!isBlocking(level, cur)) {
                return cur;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (seen.add(n.immutable())) {
                    q.add(n.immutable());
                }
            }
        }
        return null;
    }

    private static boolean isBlocking(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
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
