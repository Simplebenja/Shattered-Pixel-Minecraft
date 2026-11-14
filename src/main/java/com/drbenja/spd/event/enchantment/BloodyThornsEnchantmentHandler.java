package com.drbenja.spd.event.enchantment;

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
import com.drbenja.spd.effect.BleedingEffect;
import com.drbenja.spd.effect.DeferredDamageEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:bloody_thorns (SPD Thorns) —
 * Proc (lvl+2)/(lvl+12); Bleeding level = (4 + level) * powerMulti.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BloodyThornsEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "bloody_thorns"));

    private BloodyThornsEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;

        Entity attackerEnt = event.getSource().getEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || attacker == defender) return;
        if (EntityClassHelper.immuneToBleeding(attacker)) return;

        int thorns = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (thorns <= 0) return;

        int s = EnchantLevels.spd(thorns);
        float procChance = ProcPower.chance(defender, (s + 2f) / (s + 12f));
        if (defender.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(defender, procChance);

        float bleed = (4f + s) * powerMulti;
        BleedingEffect.applyOrMax(attacker, bleed);
    }
}
