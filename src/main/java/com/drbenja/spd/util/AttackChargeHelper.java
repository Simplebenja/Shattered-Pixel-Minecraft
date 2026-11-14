package com.drbenja.spd.util;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Weapon proc enchants (except on-kill like Corrupting) only fire when the attack
 * is at least 90% charged — matches avoiding spam with uncharged spam-clicks.
 * <p>
 * Bows and crossbows never proc on melee; only their projectiles do.
 * Detection: {@link BowItem}/{@link CrossbowItem} plus
 * {@code #minecraft:enchantable/bow} and {@code #minecraft:enchantable/crossbow}
 * (covers most mod bows that accept Power/Quick Charge, etc.).
 */
public final class AttackChargeHelper {

    public static final float MIN_CHARGE = 0.90f;

    private AttackChargeHelper() {}

    /**
     * @param attacker entity dealing damage
     * @param direct   direct damage source entity (weapon entity or projectile)
     * @return true if charge is high enough for procs
     */
    public static boolean isChargedEnough(LivingEntity attacker, Entity direct) {
        if (attacker == null) return false;
        // Projectiles: treat as full charge (bows already require draw)
        if (direct instanceof Projectile && direct != attacker) {
            return true;
        }
        if (attacker instanceof Player player) {
            return player.getAttackStrengthScale(0.5f) >= MIN_CHARGE;
        }
        return true;
    }

    /**
     * Whether this hit may run SPD weapon-enchant procs.
     * Melee swings with a bow/crossbow in hand are rejected.
     */
    public static boolean allowsWeaponProc(LivingEntity attacker, Entity direct) {
        if (attacker == null || direct == null) return false;
        // Projectile path always OK for ranged weapons
        if (direct instanceof Projectile && direct != attacker) {
            return true;
        }
        // Melee (direct == attacker or non-projectile): block bows/crossbows
        if (isRangedWeapon(attacker.getMainHandItem())) {
            return false;
        }
        return true;
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return true;
        }
        // 26.3: no ItemTags.BOWS / CROSSBOWS — use enchantable tags instead
        return stack.is(ItemTags.BOW_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE);
    }
}

