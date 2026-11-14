package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.DeferredDamageEffect;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:repulsion — Melee-only (not projectiles). Proc (lvl+1)/(lvl+5).
 * Knockback distance scales with powerMulti (SPD: throwChar distance 2 * powerMulti).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class RepulsionEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "repulsion"));

    private RepulsionEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;

        Entity direct = event.getSource().getDirectEntity();
        Entity attackerEnt = event.getSource().getEntity();
        // Projectiles (arrows, etc.) never trigger repulsion
        if (direct instanceof Projectile) return;
        if (!(attackerEnt instanceof LivingEntity attacker) || attacker == defender) return;
        // Require melee-range contact
        if (attacker.distanceTo(defender) > 3.5) return;

        int rep = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (rep <= 0) return;

        int s = EnchantLevels.spd(rep);
        float procChance = ProcPower.chance(defender, (s + 1f) / (s + 5f));
        if (defender.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(defender, procChance);

        double strength = 1.1 * powerMulti;
        Vec3 away = attacker.position().subtract(defender.position());
        if (away.lengthSqr() < 1.0e-6) {
            away = new Vec3(defender.getRandom().nextGaussian(), 0, defender.getRandom().nextGaussian());
        }
        away = away.normalize().scale(strength);
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(away.x, 0.35 * powerMulti, away.z));
    }
}


