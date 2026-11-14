package com.drbenja.spd.event.enchantment;

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

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:vampiric (max level 1).
 * Heal chance scales with attacker missing HP: 5% at full → 30% at 0 HP.
 * On proc: restore 50% of damage dealt.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class VampiricEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "vampiric"));

    private VampiricEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;

        LivingEntity target = event.getEntity();
        Entity attackerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || direct == null) return;
        if (attacker == target) return;

        if (!attacker.isAlive()) return;
        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) return;

        int level = resolveLevel(server, attacker, direct);
        if (level <= 0) return;

        float maxHp = Math.max(1f, attacker.getMaxHealth());
        float missing = (maxHp - attacker.getHealth()) / maxHp;
        float chance = ProcPower.chance(attacker, 0.05f + 0.25f * missing);
        if (attacker.getRandom().nextFloat() >= chance) return;
        float powerMulti = ProcPower.powerMulti(attacker, chance);

        float damage = event.getAmount();
        if (damage <= 0f) return;
        float heal = damage * 0.5f * powerMulti;
        float room = maxHp - attacker.getHealth();
        if (room <= 0f) return;
        heal = Math.min(heal, room);
        attacker.heal(heal);
        server.sendParticles(ParticleTypes.HEART,
                attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.8, attacker.getZ(),
                3, 0.2, 0.15, 0.2, 0.0);
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
