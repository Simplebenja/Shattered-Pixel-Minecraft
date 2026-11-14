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
import com.drbenja.spd.entity.cloud.ShroudingFogCloudEntity;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.init.ModPotions;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ShroudingFogSplashHandler {

    private static final int AIR_SEARCH_LIMIT = 64;
    private static final Identifier SHROUDING_ID =
            Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "potion_of_shrouding_fog");

    private ShroudingFogSplashHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
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
        // Solo splash / lingering (no bebible lanzado)
        if (!item.is(Items.SPLASH_POTION) && !item.is(Items.LINGERING_POTION)) {
            // Algunas implementaciones no marcan el item; seguir si el contenido coincide
        }
        if (!isShroudingFog(item)) {
            return;
        }

        HitResult hit = resolveHit(event);
        ServerLevel level = (ServerLevel) proj.level();
        BlockPos seedPos = hit != null
                ? resolveOpenAirSeed(level, hit, proj)
                : resolveOpenAirSeed(level, BlockPos.containing(proj.position()), proj);

        ShroudingFogCloudEntity cloud = new ShroudingFogCloudEntity(ModEntities.SHROUDING_FOG.get(), level);
        cloud.setPos(seedPos.getX() + 0.5, seedPos.getY() + 0.5, seedPos.getZ() + 0.5);
        cloud.seed(seedPos, ShroudingFogCloudEntity.SEED_AMOUNT);
        Entity owner = proj.getOwner();
        if (owner != null) {
            cloud.setOwner(owner.getUUID());
        }
        level.addFreshEntity(cloud);

        Vec3 pos = Vec3.atCenterOf(seedPos);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4F, 0.6F);
    }

    private static boolean isShroudingFog(ItemStack item) {
        PotionContents contents = item.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        try {
            var opt = contents.potion();
            if (opt != null && !opt.isEmpty()) {
                // Holder.is(Identifier) / is(ResourceKey) en 26.3
                if (opt.get().is(SHROUDING_ID)) {
                    return true;
                }
                if (opt.get().is(ModPotions.SHROUDING_FOG.getKey())) {
                    return true;
                }
                Potion potion = opt.get().value();
                Identifier id = BuiltInRegistries.POTION.getKey(potion);
                if (id != null && id.equals(SHROUDING_ID)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        String s = item.toString();
        return s.contains("potion_of_shrouding_fog") || s.contains("shrouding_fog");
    }

    private static BlockPos resolveOpenAirSeed(Level level, HitResult hit, Projectile proj) {
        BlockPos hitPos = BlockPos.containing(hit.getLocation());
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos outside = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (!isBlocking(level, outside)) {
                return outside.immutable();
            }
            if (!isBlocking(level, hitPos)) {
                return hitPos.immutable();
            }
        }
        if (!isBlocking(level, hitPos)) {
            return hitPos.immutable();
        }
        Vec3 motion = proj.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6) {
            BlockPos backPos = BlockPos.containing(hit.getLocation().subtract(motion.normalize().scale(0.6)));
            if (!isBlocking(level, backPos)) {
                return backPos.immutable();
            }
        }
        BlockPos found = findNearestOpenAir(level, hitPos);
        return found != null ? found : hitPos.immutable();
    }

    private static BlockPos resolveOpenAirSeed(Level level, BlockPos start, Projectile proj) {
        if (!isBlocking(level, start)) {
            return start.immutable();
        }
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
        try {
            var f = event.getClass().getDeclaredField("ray");
            f.setAccessible(true);
            Object v = f.get(event);
            if (v instanceof HitResult hr) {
                return hr;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean isThrownPotion(Entity e) {
        String n = e.getClass().getName().toLowerCase();
        return n.contains("thrownpotion")
                || n.contains("abstractthrownpotion")
                || n.contains("throwableitemprojectile")
                || n.contains("thrownitemprojectile");
    }

    private static ItemStack getPotionItem(Projectile proj) {
        for (String method : new String[]{"getItem", "getItemRaw", "getEffectSource"}) {
            try {
                Object o = proj.getClass().getMethod(method).invoke(proj);
                if (o instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // Campos comunes
        for (String field : new String[]{"item", "stack", "potionItem"}) {
            try {
                var f = proj.getClass().getDeclaredField(field);
                f.setAccessible(true);
                Object o = f.get(proj);
                if (o instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        Class<?> c = proj.getClass();
        while (c != null && c != Object.class) {
            for (var f : c.getDeclaredFields()) {
                if (ItemStack.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object o = f.get(proj);
                        if (o instanceof ItemStack stack && !stack.isEmpty()
                                && (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)
                                || stack.has(DataComponents.POTION_CONTENTS))) {
                            return stack;
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
            c = c.getSuperclass();
        }
        return ItemStack.EMPTY;
    }
}
