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
import com.drbenja.spd.effect.BleedingEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:vorpal — SPD Vorpal:
 * 25% proc. Weapon damage still applies; additionally sets Bleeding level to
 * powerMulti * (2 + hitDamage/2) (max with existing). Inorganic: immune to bleed →
 * only ordinary weapon damage (as if proc did nothing extra).
 *
 * Note: SPD does NOT convert the hit into pure bleed; both happen. Our previous
 * lang line about "instead of normal damage" was inaccurate and is updated.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class VorpalEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "vorpal"));

    private VorpalEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity target = event.getEntity();
        Entity attackerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || direct == null) return;
        if (attacker == target) return;
        if (!AttackChargeHelper.isChargedEnough(attacker, direct)) return;
        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) return;

        // Inorganic / bleed-immune: no bleed conversion of any kind
        if (EntityClassHelper.immuneToBleeding(target)) {
            return;
        }

        int level = EnchantResolveHelper.resolveLevel(server, attacker, direct, KEY);
        if (level <= 0) return;

        float procChance = ProcPower.chance(attacker, 0.25f);
        if (attacker.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(attacker, procChance);

        // SPD Char.damage: bleedAmt = powerMulti * (2 + dmg/2)
        float amount = event.getAmount();
        float bleedLevel = powerMulti * (2f + amount / 2f);
        BleedingEffect.applyOrMax(target, bleedLevel);

        server.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                8, 0.25, 0.3, 0.25, 0.0);
        server.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                6, 0.2, 0.25, 0.2, 0.05);
    }
}
