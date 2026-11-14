package com.drbenja.spd.event.enchantment;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.PoisonedEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:venomous — SPD Venomous:
 * Flat 1/3 proc. Extends Poisoned by powerMulti * (spdLvl/2 + 3) turns.
 * First application delays damage by 3 seconds; further procs extend immediately.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class VenomousEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "venomous"));

    private VenomousEnchantmentHandler() {}

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

        if (EntityClassHelper.isInorganic(target) || EntityClassHelper.immuneToToxicGas(target)) {
            return;
        }

        int level = EnchantResolveHelper.resolveLevel(server, attacker, direct, KEY);
        if (level <= 0) return;

        float procChance = ProcPower.chance(attacker, 1f / 3f);
        if (attacker.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(attacker, procChance);

        int s = EnchantLevels.spd(level);
        float addLeft = powerMulti * (s / 2f + 3f);
        boolean hadNone = PoisonedEffect.getLeft(target) <= 0.01f;
        PoisonedEffect.apply(target, addLeft, hadNone ? 3 : 0);

        server.sendParticles(ParticleTypes.ITEM_SLIME,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                8, 0.25, 0.3, 0.25, 0.02);
        server.sendParticles(ParticleTypes.WITCH,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                6, 0.2, 0.25, 0.2, 0.01);
    }
}
