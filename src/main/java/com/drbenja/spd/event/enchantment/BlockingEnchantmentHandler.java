package com.drbenja.spd.event.enchantment;

import com.drbenja.spd.util.SpdProgression;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.ProcPower;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.effect.BlockingEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * spd:blocking — proc grants timed absorption (5s). Shield = 2 + enchant level.
 * Proc chance (attacker max HP): ((maxHp-15)/5 + 1) / ((maxHp-15)/5 + 20)
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BlockingEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "blocking"));

    private BlockingEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity target = event.getEntity();
        Entity attackerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || direct == null) return;
        if (attacker == target) return;

        if (!AttackChargeHelper.isChargedEnough(attacker, direct)) return;
        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) return;

        int level = resolveLevel(server, attacker, direct);
        if (level <= 0) return;

        float chance = ProcPower.chance(attacker, procChance(attacker));
        if (attacker.getRandom().nextFloat() >= chance) return;
        float powerMulti = ProcPower.powerMulti(attacker, chance);

        int shield = Math.round((2 + level) * powerMulti);
        BlockingEffect.apply(attacker, shield);
        server.sendParticles(ParticleTypes.END_ROD,
                attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6, attacker.getZ(),
                6, 0.25, 0.3, 0.25, 0.02);
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.BLOCKING)) {
            BlockingEffect.clearAbsorptionContribution(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.BLOCKING)) {
            BlockingEffect.clearAbsorptionContribution(event.getEntity());
        }
    }

    /** User formula based on max HP. */
    public static float procChance(float maxHp) {
        // Prefer entity-based API; this overload kept for callers with only maxHp.
        float raw = (maxHp - 15f) / 5f;
        return (raw + 1f) / (raw + 20f);
    }

    public static float procChance(net.minecraft.world.entity.LivingEntity attacker) {
        return SpdProgression.blockingProcChance(attacker);
    }

    private static int resolveLevel(ServerLevel level, LivingEntity attacker, Entity direct) {
        return EnchantResolveHelper.resolveLevel(level, attacker, direct, KEY);
    }

    private static int getLevel(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        try {
            Holder<Enchantment> h = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
            return EnchantmentHelper.getItemEnchantmentLevel(h, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getLevelFromProjectile(ServerLevel level, Projectile projectile) {
        try {
            Holder<Enchantment> h = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
            if (projectile instanceof AbstractArrow arrow) {
                ItemStack weapon = arrow.getWeaponItem();
                if (weapon != null && !weapon.isEmpty()) {
                    int l = EnchantmentHelper.getItemEnchantmentLevel(h, weapon);
                    if (l > 0) return l;
                }
                ItemStack pickup = arrow.getPickupItemStackOrigin();
                if (pickup != null && !pickup.isEmpty()) {
                    int l = EnchantmentHelper.getItemEnchantmentLevel(h, pickup);
                    if (l > 0) return l;
                }
            }
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living) {
                int main = getLevel(level, living.getMainHandItem());
                if (main > 0) return main;
                return getLevel(level, living.getOffhandItem());
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
