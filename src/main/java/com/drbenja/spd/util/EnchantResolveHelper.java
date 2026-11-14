package com.drbenja.spd.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Shared resolution of enchantment level from melee weapon or projectile.
 * Used by all SPD weapon enchant handlers.
 */
public final class EnchantResolveHelper {

    private EnchantResolveHelper() {}

    /**
     * Resolve level for a damage event:
     * - Projectile (arrow/trident/etc.): read from weapon/pickup stack or owner hands.
     * - Melee (direct == attacker): main hand.
     */
    public static int resolveLevel(ServerLevel level, LivingEntity attacker, Entity direct,
                                   ResourceKey<Enchantment> key) {
        if (direct instanceof Projectile proj && direct != attacker) {
            return getLevelFromProjectile(level, proj, key);
        }
        if (direct == attacker) {
            return getLevel(level, attacker.getMainHandItem(), key);
        }
        return 0;
    }

    public static int getLevel(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(key);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getLevelFromProjectile(ServerLevel level, Projectile projectile,
                                             ResourceKey<Enchantment> key) {
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(key);

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
                int main = getLevel(level, living.getMainHandItem(), key);
                if (main > 0) {
                    return main;
                }
                return getLevel(level, living.getOffhandItem(), key);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
