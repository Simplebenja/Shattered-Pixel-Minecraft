package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.CharmedEffect;
import com.drbenja.spd.effect.DeferredDamageEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:affection — Proc (lvl+3)/(lvl+20); Charm duration = 10s * powerMulti.
 * Also hosts Charmed combat rules (cannot target / melee the charmer).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class AffectionEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "affection"));

    private AffectionEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;

        Entity attackerEnt = event.getSource().getEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || attacker == defender) return;

        int aff = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (aff <= 0) return;

        int s = EnchantLevels.spd(aff);
        float procChance = ProcPower.chance(defender, (s + 3f) / (s + 20f));
        if (defender.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(defender, procChance);

        int charmTicks = Math.round(10f * 20f * powerMulti);
        attacker.addEffect(new MobEffectInstance(ModEffects.CHARMED, charmTicks, 0, false, true, true));
        CharmedEffect.setCharmer(attacker, defender);
        if (attacker instanceof Mob mob && mob.getTarget() == defender) {
            mob.setTarget(null);
        }
    }

    /** Taking damage shortens Charm (like Terror). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCharmedDamaged(LivingIncomingDamageEvent event) {
        LivingEntity defender = event.getEntity();
        if (!defender.hasEffect(ModEffects.CHARMED)) return;
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;
        var inst = defender.getEffect(ModEffects.CHARMED);
        if (inst == null) return;
        int neu = Math.max(0, inst.getDuration() - 5 * 20);
        defender.removeEffect(ModEffects.CHARMED);
        if (neu > 0) {
            defender.addEffect(new MobEffectInstance(ModEffects.CHARMED, neu, 0, false, true, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCharmedTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity neu = event.getNewAboutToBeSetTarget();
        if (neu == null || !entity.hasEffect(ModEffects.CHARMED)) return;
        if (CharmedEffect.isCharmedBy(entity, neu)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCharmedMelee(LivingIncomingDamageEvent event) {
        Entity atk = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(atk instanceof LivingEntity attacker)) return;
        if (!attacker.hasEffect(ModEffects.CHARMED)) return;
        if (direct != null && direct != attacker) return;
        LivingEntity target = event.getEntity();
        if (CharmedEffect.isCharmedBy(attacker, target)) {
            event.setAmount(0f);
            event.setCanceled(true);
        }
    }
}
