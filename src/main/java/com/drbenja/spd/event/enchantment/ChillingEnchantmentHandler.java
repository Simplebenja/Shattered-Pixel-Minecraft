package com.drbenja.spd.event.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.ProcPower;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;

/**
 * spd:chilling — SPD Chilling
 * <p>
 * Proc (standard): L1 25% / L2 40% / L3 50% × ProcPower.multiplier.
 * On proc: Chill 3s × powerMulti (cap 6s × powerMulti). powerMulti = max(1, procChance).
 * Fiery (#spd:fiery / fireImmune): small bonus damage instead of Chill (tag preferred).
 * Melee + projectile (bow/crossbow/trident) same resolution.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ChillingEnchantmentHandler {

    public static final ResourceKey<Enchantment> CHILLING_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "chilling"));

    /** Base seconds of Chill per proc (SPD). */
    public static final float BASE_CHILL_SECONDS = 3f;
    /** Base max stack seconds (SPD). */
    public static final float BASE_MAX_CHILL_SECONDS = 6f;

    private ChillingEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDamage(LivingIncomingDamageEvent event) {
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
            level = getChillingLevelFromProjectile(server, projectile);
        } else if (direct == attacker) {
            level = getChillingLevel(server, attacker.getMainHandItem());
        }

        if (level <= 0) {
            return;
        }

        // SPD: procChance = (lvl+1)/(lvl+4) * multiplier ; powerMulti = max(1, procChance)
        float procChance = ProcPower.standardWeaponProc(attacker, level);
        if (attacker.getRandom().nextFloat() >= procChance) {
            return;
        }
        float powerMulti = Math.max(1f, procChance);

        // Fiery: bonus damage instead of Chill (tag #spd:fiery preferred over fireImmune)
        if (EntityClassHelper.isFiery(target) || target.fireImmune()) {
            // Modest extra damage scaled by powerMulti and level; keeps parity with prior mod behaviour
            float bonus = (1.0f + level) * powerMulti;
            event.setAmount(event.getAmount() + bonus);
            return;
        }
        if (ChillImmuneEntitiesConfig.isChillImmune(target)) {
            return;
        }
        if (target.hasEffect(ModEffects.FROZEN) || target.hasEffect(ModEffects.IMBUED_WITH_FROST)) {
            return;
        }

        float durationToAddSec = BASE_CHILL_SECONDS * powerMulti;
        MobEffectInstance cur = target.getEffect(ModEffects.CHILLED);
        float currentSec = cur != null ? cur.getDuration() / 20f : 0f;
        float maxSec = BASE_MAX_CHILL_SECONDS * powerMulti;
        float addSec = Math.min(durationToAddSec, maxSec - currentSec);
        if (addSec <= 0f) {
            return;
        }
        int nextTicks = Math.round((currentSec + addSec) * 20f);
        target.addEffect(new MobEffectInstance(
                ModEffects.CHILLED, nextTicks, 0, false, true, true));
    }

    private static int getChillingLevel(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(CHILLING_KEY);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getChillingLevelFromProjectile(ServerLevel level, Projectile projectile) {
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(CHILLING_KEY);

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
                int main = getChillingLevel(level, living.getMainHandItem());
                if (main > 0) {
                    return main;
                }
                return getChillingLevel(level, living.getOffhandItem());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
