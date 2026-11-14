package com.drbenja.spd.event.enchantment;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:eldritch (SPD Eldritch)
 * Standard curve (L1 25% / L2 40% / L3 50%).
 * On proc: Terror on nearby hostiles that can "see" the fight (approx. 10-block box
 * around target, excluding attacker/allies). Duration = 5s * powerMulti.
 * When empowered (procChance &gt; 1), powerMulti stretches Terror duration.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class EldritchEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "eldritch"));

    private EldritchEnchantmentHandler() {}

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

        float procChance = ProcPower.standardWeaponProc(attacker, level);
        if (attacker.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(attacker, procChance);

        int terrorTicks = Math.round(5 * 20 * powerMulti);
        AABB box = target.getBoundingBox().inflate(10);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != attacker && x != target && x.isAlive()
                        && !attacker.isAlliedTo(x) && !x.isAlliedTo(attacker))) {
            e.addEffect(new MobEffectInstance(ModEffects.TERROR, terrorTicks, 0, false, true, true));
            TerrorEffect.setSource(e, attacker);
            if (e instanceof Mob mob && mob.getTarget() == attacker) {
                mob.setTarget(null);
            }
        }
        server.sendParticles(ParticleTypes.SMOKE,
                attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                16, 0.45, 0.45, 0.45, 0.02);
        server.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 0.5, target.getZ(),
                12, 0.35, 0.4, 0.35, 0.05);
    }
}
