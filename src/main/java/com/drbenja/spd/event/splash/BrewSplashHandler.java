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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.cloud.BlizzardCloudEntity;
import com.drbenja.spd.entity.cloud.InfernalCloudEntity;
import com.drbenja.spd.entity.cloud.ShockingFieldEntity;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.init.ModPotions;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BrewSplashHandler {

    private static final int AIR_SEARCH = 64;

    private BrewSplashHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide()) return;
        if (!isThrownPotion(proj)) return;
        ItemStack item = getItem(proj);
        if (item.isEmpty()) return;
        String id = potionId(item);
        if (id == null) return;

        HitResult hit = resolveHit(event);
        ServerLevel level = (ServerLevel) proj.level();
        BlockPos seed = hit != null ? openAir(level, hit, proj) : openAir(level, BlockPos.containing(proj.position()));

        switch (id) {
            case "infernal_brew" -> {
                InfernalCloudEntity c = new InfernalCloudEntity(ModEntities.INFERNAL_CLOUD.get(), level);
                c.setPos(seed.getX() + 0.5, seed.getY() + 0.5, seed.getZ() + 0.5);
                c.seed(seed, InfernalCloudEntity.SEED_AMOUNT);
                level.addFreshEntity(c);
                sound(level, seed, true);
            }
            case "blizzard_brew" -> {
                BlizzardCloudEntity c = new BlizzardCloudEntity(ModEntities.BLIZZARD_CLOUD.get(), level);
                c.setPos(seed.getX() + 0.5, seed.getY() + 0.5, seed.getZ() + 0.5);
                c.seed(seed, BlizzardCloudEntity.SEED_AMOUNT);
                level.addFreshEntity(c);
                sound(level, seed, true);
            }
            case "shocking_brew" -> {
                ShockingFieldEntity f = new ShockingFieldEntity(ModEntities.SHOCKING_FIELD.get(), level);
                f.setOrigin(seed);
                level.addFreshEntity(f);
                level.playSound(null, seed, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                level.playSound(null, seed, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.4F, 1.6F);
            }
            case "caustic_brew" -> {
                applyCaustic(level, seed);
                level.playSound(null, seed, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
            }
            default -> {}
        }
    }

    private static void applyCaustic(ServerLevel level, BlockPos origin) {
        int r = 3;
        AABB box = new AABB(origin.getX() - r, origin.getY(), origin.getZ() - r,
                origin.getX() + r + 1, origin.getY() + 3, origin.getZ() + r + 1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            // Potion of Purity: immune to Caustic Brew application (and its DoT)
            if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
                continue;
            }
            if (com.drbenja.spd.entity.classy.EntityClassHelper.immuneToCausticOoze(e)) {
                continue;
            }
            e.addEffect(new MobEffectInstance(ModEffects.CAUSTIC_OOZE, 20 * 20, 0, false, true, true));
        }
        // Splash visual en el área 7×7×3
        level.sendParticles(ParticleTypes.ITEM_SLIME,
                origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                60, 3.0, 1.2, 3.0, 0.08);
        level.sendParticles(ParticleTypes.SQUID_INK,
                origin.getX() + 0.5, origin.getY() + 0.8, origin.getZ() + 0.5,
                40, 2.8, 1.0, 2.8, 0.05);
        level.sendParticles(ParticleTypes.SMOKE,
                origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                25, 2.5, 0.8, 2.5, 0.02);
    }

    private static void sound(ServerLevel level, BlockPos seed, boolean gas) {
        level.playSound(null, seed, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (gas) level.playSound(null, seed, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.5F, 0.5F);
    }

    private static String potionId(ItemStack item) {
        PotionContents c = item.get(DataComponents.POTION_CONTENTS);
        if (c == null) return null;
        try {
            var opt = c.potion();
            if (opt == null || opt.isEmpty()) return null;
            if (opt.get().is(ModPotions.INFERNAL_BREW.getKey())) return "infernal_brew";
            if (opt.get().is(ModPotions.BLIZZARD_BREW.getKey())) return "blizzard_brew";
            if (opt.get().is(ModPotions.SHOCKING_BREW.getKey())) return "shocking_brew";
            if (opt.get().is(ModPotions.CAUSTIC_BREW.getKey())) return "caustic_brew";
            Identifier id = BuiltInRegistries.POTION.getKey(opt.get().value());
            if (id != null && "spd".equals(id.getNamespace())) {
                String p = id.getPath();
                if (p.contains("infernal")) return "infernal_brew";
                if (p.contains("blizzard")) return "blizzard_brew";
                if (p.contains("shocking")) return "shocking_brew";
                if (p.contains("caustic_brew")) return "caustic_brew";
            }
        } catch (Throwable ignored) {}
        String s = item.toString();
        if (s.contains("infernal_brew")) return "infernal_brew";
        if (s.contains("blizzard_brew")) return "blizzard_brew";
        if (s.contains("shocking_brew")) return "shocking_brew";
        if (s.contains("caustic_brew")) return "caustic_brew";
        return null;
    }

    private static BlockPos openAir(Level level, HitResult hit, Projectile proj) {
        BlockPos hitPos = BlockPos.containing(hit.getLocation());
        if (hit instanceof BlockHitResult bh) {
            BlockPos out = bh.getBlockPos().relative(bh.getDirection());
            if (!blocking(level, out)) return out.immutable();
            if (!blocking(level, hitPos)) return hitPos.immutable();
        }
        if (!blocking(level, hitPos)) return hitPos.immutable();
        return openAir(level, hitPos);
    }

    private static BlockPos openAir(Level level, BlockPos start) {
        if (!blocking(level, start)) return start.immutable();
        Queue<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        q.add(start.immutable()); seen.add(start.immutable());
        int v = 0;
        while (!q.isEmpty() && v++ < AIR_SEARCH) {
            BlockPos cur = q.poll();
            if (!blocking(level, cur)) return cur;
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (seen.add(n.immutable())) q.add(n.immutable());
            }
        }
        return start.immutable();
    }

    private static boolean blocking(Level level, BlockPos pos) {
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
        return n.contains("thrownpotion") || n.contains("abstractthrownpotion") || n.contains("throwableitemprojectile");
    }

    private static ItemStack getItem(Projectile proj) {
        try {
            Object o = proj.getClass().getMethod("getItem").invoke(proj);
            if (o instanceof ItemStack s) return s;
        } catch (ReflectiveOperationException ignored) {}
        return ItemStack.EMPTY;
    }
}
