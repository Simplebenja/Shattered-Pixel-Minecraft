package com.drbenja.spd.event.enchantment;

import com.drbenja.spd.entity.classy.EntityClassHelper;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.ProcPower;
import com.drbenja.spd.util.AttackChargeHelper;

/**
 * spd:shocking — SPD Shocking
 * <p>
 * Flat base proc 1/3 × ProcPower.multiplier (not the Chilling curve).
 * On proc: arcs deal 50% of hit damage × powerMulti (powerMulti = max(1, procChance))
 * to nearby living (excl. primary target, attacker, allies/pets).
 * Melee + projectile (bow/crossbow/trident) same resolution as Chilling.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ShockingEnchantmentHandler {

    public static final ResourceKey<Enchantment> SHOCKING_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "shocking"));

    /** 5 m diameter → 2.5 block radius (approximate of SPD arc distance). */
    public static final double AOE_RADIUS = 2.5D;
    /** SPD: 50% of triggering hit damage per affected. */
    public static final float DAMAGE_FRACTION = 0.50F;

    private static final ThreadLocal<Boolean> APPLYING_AOE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ShockingEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (Boolean.TRUE.equals(APPLYING_AOE.get())) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel server)) {
            return;
        }

        LivingEntity target = event.getEntity();
        Entity attackerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || direct == null) {
            return;
        }
        if (attacker == target) {
            return;
        }

        if (!AttackChargeHelper.isChargedEnough(attacker, direct)) return;
        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) return;

        int level = 0;
        if (direct instanceof Projectile projectile && direct != attacker) {
            level = getShockingLevelFromProjectile(server, projectile);
        } else if (direct == attacker) {
            level = getShockingLevel(server, attacker.getMainHandItem());
        }

        if (level <= 0) {
            return;
        }

        // SPD: flat 1/3 × multiplier; powerMulti scales damage when proc > 100%
        float procChance = ProcPower.chance(attacker, 1f / 3f);
        if (attacker.getRandom().nextFloat() >= procChance) {
            return;
        }
        float powerMulti = Math.max(1f, procChance);

        float baseDamage = event.getAmount();
        if (baseDamage <= 0.0F) {
            return;
        }
        float aoeDamage = baseDamage * DAMAGE_FRACTION * powerMulti;
        if (aoeDamage <= 0.0F) {
            return;
        }

        applyShockAoe(server, attacker, target, aoeDamage);
    }

    private static void applyShockAoe(ServerLevel level, LivingEntity attacker, LivingEntity center, float damage) {
        AABB box = center.getBoundingBox().inflate(AOE_RADIUS);
        // SPD: the primary defender is not hurt by the lightning arc
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive()
                        && e != attacker
                        && e != center
                        && !isAlliedOrPet(attacker, e)
                        && center.distanceToSqr(e) <= AOE_RADIUS * AOE_RADIUS);

        double cx = center.getX();
        double cy = center.getY() + center.getBbHeight() * 0.5;
        double cz = center.getZ();

        // Esfera visual más densa y clara (shell + núcleo)
        int shell = 72;
        for (int i = 0; i < shell; i++) {
            double theta = level.getRandom().nextDouble() * Math.PI * 2.0;
            double phi = level.getRandom().nextDouble() * Math.PI;
            double rr = AOE_RADIUS * (0.85 + level.getRandom().nextDouble() * 0.15);
            double px = cx + rr * Math.sin(phi) * Math.cos(theta);
            double py = cy + rr * Math.cos(phi) * 0.65;
            double pz = cz + rr * Math.sin(phi) * Math.sin(theta);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 2, 0.03, 0.03, 0.03, 0.01);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.01, 0.01, 0.01, 0.0);
            }
        }
        // Núcleo brillante
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 40, 0.55, 0.55, 0.55, 0.12);
        // FLASH requires ColorParticleOption in 26.3
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 1.0f, 1.0f, 1.0f),
                cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CRIT, cx, cy, cz, 24, 0.6, 0.5, 0.6, 0.15);
        level.playSound(null, center.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 0.65F, 1.25F);
        level.playSound(null, center.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.25F, 1.6F);

        if (victims.isEmpty()) {
            return;
        }

        APPLYING_AOE.set(Boolean.TRUE);
        try {
            for (LivingEntity victim : victims) {
                float dmg = damage * EntityClassHelper.electricDamageFactor(victim);
                if (dmg > 0.0F) {
                    victim.hurtServer(level, victim.damageSources().lightningBolt(), dmg);
                }
                double vx = victim.getX();
                double vy = victim.getY() + victim.getBbHeight() * 0.5;
                double vz = victim.getZ();
                // Pequeños "rayos" de partículas del centro al afectado
                for (int s = 1; s <= 6; s++) {
                    double t = s / 6.0;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            cx + (vx - cx) * t,
                            cy + (vy - cy) * t,
                            cz + (vz - cz) * t,
                            1, 0.02, 0.02, 0.02, 0.0);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, vx, vy, vz, 6, 0.2, 0.25, 0.2, 0.02);
            }
        } finally {
            APPLYING_AOE.set(Boolean.FALSE);
        }
    }

    /**
     * Excludes scoreboard teammates, tamed pets owned by the attacker,
     * OwnableEntity with same owner, and anything {@link Entity#isAlliedTo}.
     */
    private static boolean isAlliedOrPet(LivingEntity attacker, LivingEntity other) {
        if (attacker.isAlliedTo(other) || other.isAlliedTo(attacker)) {
            return true;
        }
        if (other instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
            if (owner != null && (owner == attacker || owner.getUUID().equals(attacker.getUUID()))) {
                return true;
            }
            if (owner != null && (attacker.isAlliedTo(owner) || owner.isAlliedTo(attacker))) {
                return true;
            }
        }
        if (other instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null && (owner == attacker || owner.getUUID().equals(attacker.getUUID()))) {
                return true;
            }
            if (owner instanceof LivingEntity livingOwner
                    && (attacker.isAlliedTo(livingOwner) || livingOwner.isAlliedTo(attacker))) {
                return true;
            }
        }
        return false;
    }

    private static int getShockingLevel(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(SHOCKING_KEY);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getShockingLevelFromProjectile(ServerLevel level, Projectile projectile) {
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(SHOCKING_KEY);

            if (projectile instanceof AbstractArrow arrow) {
                ItemStack weapon = arrow.getWeaponItem();
                if (weapon != null && !weapon.isEmpty()) {
                    int fromWeapon = EnchantmentHelper.getItemEnchantmentLevel(holder, weapon);
                    if (fromWeapon > 0) {
                        return fromWeapon;
                    }
                }
                ItemStack pickup = arrow.getPickupItemStackOrigin();
                if (pickup != null && !pickup.isEmpty()) {
                    int fromPickup = EnchantmentHelper.getItemEnchantmentLevel(holder, pickup);
                    if (fromPickup > 0) {
                        return fromPickup;
                    }
                }
            }

            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living) {
                int main = getShockingLevel(level, living.getMainHandItem());
                if (main > 0) {
                    return main;
                }
                return getShockingLevel(level, living.getOffhandItem());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
