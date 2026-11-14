package com.drbenja.spd.event.enchantment;

import com.drbenja.spd.entity.classy.EntityClassHelper;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.ProcPower;
import com.drbenja.spd.effect.BurningEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * spd:blazing — SPD Blazing
 * <p>
 * Own proc: (lvl+1)/(lvl+3) × multiplier → L1 33% / L2 50% / L3 60%.
 * On proc: if not Burning → apply Burning 8s and consume 1 powerMulti.
 * Remaining powerMulti: bonus fire damage ~ Normal(1, 3+depth/4) × 0.67 × powerMulti.
 * depth via SpdProgression when available; fallback modest range.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BlazingEnchantmentHandler {

    public static final ResourceKey<Enchantment> BLAZING_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "blazing"));

    private static final ThreadLocal<Boolean> APPLYING_BONUS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private BlazingEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (Boolean.TRUE.equals(APPLYING_BONUS.get())) {
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
        // Immune while Imbued with Fire
        if (target.hasEffect(ModEffects.IMBUED_WITH_FIRE)) {
            return;
        }

        int level = 0;
        if (direct instanceof Projectile projectile && direct != attacker) {
            level = getBlazingLevelFromProjectile(server, projectile);
        } else if (direct == attacker) {
            level = getBlazingLevel(server, attacker.getMainHandItem());
        }
        if (level <= 0) {
            return;
        }

        if (EntityClassHelper.immuneToBlazingEnchant(target)) {
            return;
        }

        int spdLvl = Math.max(0, level - 1);
        float baseChance = (spdLvl + 1f) / (spdLvl + 3f);
        float procChance = ProcPower.chance(attacker, baseChance);
        if (attacker.getRandom().nextFloat() >= procChance) {
            return;
        }
        float powerMulti = Math.max(1f, procChance);

        boolean alreadyBurning = target.hasEffect(ModEffects.BURNING);

        // Apply / refresh Burning 8s; first application "costs" 1 powerMulti (SPD)
        if (!alreadyBurning) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.BURNING,
                    BurningEffect.DEFAULT_DURATION_TICKS,
                    0,
                    false,
                    true,
                    true));
            powerMulti -= 1f;
        } else {
            // Refresh duration even when already burning
            target.addEffect(new MobEffectInstance(
                    ModEffects.BURNING,
                    BurningEffect.DEFAULT_DURATION_TICKS,
                    0,
                    false,
                    true,
                    true));
        }

        // Remaining powerMulti → bonus burn damage on the SAME hit (avoids invuln frames).
        // SPD: NormalIntRange(1, 3+depth/4) * 0.67 * powerMulti. First apply costs 1 powerMulti.
        if (powerMulti > 0f) {
            int depth = com.drbenja.spd.util.SpdProgression.depth(attacker);
            int maxRoll = 3 + Math.max(0, depth) / 4;
            int burnDamage = 1 + attacker.getRandom().nextInt(Math.max(1, maxRoll));
            float scaled = Math.max(1f, Math.round(burnDamage * 0.67f * powerMulti));
            // Fold into the current attack amount so i-frames from this hit do not block it
            event.setAmount(event.getAmount() + scaled);
            server.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10 + level * 2, 0.3, 0.35, 0.3, 0.04);
            server.sendParticles(ParticleTypes.LAVA,
                    target.getX(), target.getY() + 0.2, target.getZ(),
                    4, 0.2, 0.15, 0.2, 0.0);
        }

        server.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                level + 2, 0.25, 0.3, 0.25, 0.02);
    }

    private static int getBlazingLevel(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(BLAZING_KEY);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getBlazingLevelFromProjectile(ServerLevel level, Projectile projectile) {
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(BLAZING_KEY);

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
                int main = getBlazingLevel(level, living.getMainHandItem());
                if (main > 0) {
                    return main;
                }
                return getBlazingLevel(level, living.getOffhandItem());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
