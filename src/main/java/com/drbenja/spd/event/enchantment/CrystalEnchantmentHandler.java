package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:crystal (SPD Crystal)
 * Always: +ceil(hit * 0.33 * multiplier) on the same attack (magic-style bonus in SPD).
 * When empowered (multiplier &gt; 1 from future rings/talents), the bonus scales up.
 * Also 33% chance of one extra durability wear on the weapon.
 * (SPD durability shatter/repair system is not fully ported.)
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class CrystalEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "crystal"));

    private CrystalEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity target = event.getEntity();
        Entity attackerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || direct == null) return;
        if (attacker == target) return;
        if (!AttackChargeHelper.isChargedEnough(attacker, direct)) return;
        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) return;

        int level = EnchantResolveHelper.resolveLevel(server, attacker, direct, KEY);
        if (level <= 0) return;

        float amount = event.getAmount();
        // SPD: magicDmg = ceil(damage * 0.33 * genericProcChanceMultiplier)
        float extra = (float) Math.ceil(amount * 0.33f * ProcPower.multiplier(attacker));
        if (extra > 0f) {
            event.setAmount(amount + extra);
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.isEmpty() && weapon.isDamageableItem() && attacker.getRandom().nextFloat() < 0.33f) {
            int d = weapon.getDamageValue();
            int max = weapon.getMaxDamage();
            if (d < max - 1) {
                weapon.setDamageValue(d + 1);
            }
        }
    }
}
