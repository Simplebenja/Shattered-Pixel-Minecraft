package com.drbenja.spd.event.enchantment;

import com.drbenja.spd.entity.classy.EntityClassHelper;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:grim — chance scales with missing HP *after* the normal hit would apply.
 * On proc: deal remaining HP as damage (bosses: 50% of current HP).
 * maxChance = 0.5 + 0.05 * level (MC level).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class GrimEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "grim"));

    /** Optional vanilla tag; Holder.is works even if the tag file is empty/missing. */
    private static final TagKey<EntityType<?>> BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("bosses"));

    private GrimEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
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

        float amount = event.getAmount();
        if (amount <= 0f) return;

        float hp = target.getHealth();
        float maxHp = Math.max(1f, target.getMaxHealth());
        float after = Math.max(0f, hp - amount);
        float missingRatio = 1f - (after / maxHp);
        if (missingRatio <= 0f) return;

        // SPD: maxChance = 0.5 + 0.05*level (MC level), × multiplier; actual roll vs missing HP after hit
        float maxChance = (0.5f + 0.05f * level) * ProcPower.multiplier(attacker);
        float chance = maxChance * missingRatio * missingRatio;
        if (attacker.getRandom().nextFloat() >= chance) return;

        boolean boss = EntityClassHelper.isBoss(target);
        float execute = boss ? after * 0.5f : after;
        if (execute <= 0f) return;

        event.setAmount(amount + execute);
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        // Burst muy visible: humo + almas + crit oscuro
        // No SOUL / SOUL_FIRE — those read as Corrupting
        server.sendParticles(ParticleTypes.SMOKE, x, y, z, 35, 0.45, 0.5, 0.45, 0.02);
        server.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 22, 0.35, 0.4, 0.35, 0.04);
        server.sendParticles(ParticleTypes.CRIT, x, y, z, 18, 0.3, 0.35, 0.3, 0.12);
        server.sendParticles(ParticleTypes.SMOKE, x, y, z, 16, 0.25, 0.3, 0.25, 0.01);
    }

    /**
     * MC 26.3: EntityType has no static constants and no is(TagKey).
     * Use Holder tag check + registry id path fallback.
     */
    private static boolean isBoss(LivingEntity target) {
        return EntityClassHelper.isBoss(target);
    }

    private static int resolveLevel(ServerLevel level, LivingEntity attacker, Entity direct) {
        if (direct instanceof Projectile proj && direct != attacker) {
            return getLevelFromProjectile(level, proj);
        }
        if (direct == attacker) {
            return getLevel(level, attacker.getMainHandItem());
        }
        return 0;
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
