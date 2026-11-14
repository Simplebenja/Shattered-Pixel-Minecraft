package com.drbenja.spd.event.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.effect.CorruptedEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AllyTeamHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:corrupting (very rare)
 * Lethal hit proc: non-boss/non-static mobs → infinite Corrupted + team of attacker/owner.
 * Bosses / minibosses / static: never intercepted — they die normally.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class CorruptingEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "corrupting"));

    private static final int INFINITE = -1;

    private CorruptingEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!(target instanceof Mob)) {
            return;
        }
        if (target.hasEffect(ModEffects.CORRUPTED)) {
            return;
        }
        // Boss / miniboss / static: die normally (no revive-as-corrupted)
        if (EntityClassHelper.immuneToCorruption(target)) {
            return;
        }

        Entity direct = event.getSource().getDirectEntity();
        Entity cause = event.getSource().getEntity();
        LivingEntity attacker = cause instanceof LivingEntity le ? le : null;
        if (attacker == null) {
            return;
        }

        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) {
            return;
        }

        int level = resolveLevel(server, attacker, direct);
        if (level <= 0) {
            return;
        }

        float amount = event.getAmount();
        if (target.getHealth() - amount > 0f) {
            return; // not lethal
        }

        // SPD: (lvl+5)/(lvl+20) × multiplier (SPD lvl 0-based)
        int spdLvl = Math.max(0, level - 1);
        float baseChance = (spdLvl + 5f) / (spdLvl + 20f);
        float procChance = ProcPower.chance(attacker, baseChance);
        if (attacker.getRandom().nextFloat() >= procChance) {
            return;
        }
        float powerMulti = Math.max(1f, procChance);

        event.setCanceled(true);
        target.setHealth(1.0f);

        CorruptedEffect.clearSessionFlags(target);
        // Pass attacker as source so convert loot resolves killed_by_player (one-shot KO too)
        target.addEffect(new MobEffectInstance(ModEffects.CORRUPTED, INFINITE, 0, false, true, true), attacker);

        // SPD: powerMulti > 1.1 → Adrenaline ~5*(powerMulti-1) seconds (no-op until effect exists)
        if (powerMulti > 1.1f) {
            int adrenalTicks = Math.round(5f * (powerMulti - 1f) * 20f);
            // Adrenaline stub: effect not registered yet; leave TODO
        }

        LivingEntity leader = attacker;
        AllyTeamHelper.assignAllyToLeaderTeam(server, leader, target);

        server.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.35, 0.4, 0.35, 0.02);
        server.sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + 0.2, target.getZ(),
                12, 0.3, 0.2, 0.3, 0.01);
    }

    private static int resolveLevel(ServerLevel level, LivingEntity attacker, Entity direct) {
        if (direct instanceof Projectile proj && direct != attacker) {
            return getLevelFromProjectile(level, proj);
        }
        int main = getLevel(level, attacker.getMainHandItem());
        if (main > 0) {
            return main;
        }
        return getLevel(level, attacker.getOffhandItem());
    }

    private static int getLevel(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
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
                    if (l > 0) {
                        return l;
                    }
                }
                ItemStack pickup = arrow.getPickupItemStackOrigin();
                if (pickup != null && !pickup.isEmpty()) {
                    int l = EnchantmentHelper.getItemEnchantmentLevel(h, pickup);
                    if (l > 0) {
                        return l;
                    }
                }
            }
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living) {
                int main = getLevel(level, living.getMainHandItem());
                if (main > 0) {
                    return main;
                }
                return getLevel(level, living.getOffhandItem());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
