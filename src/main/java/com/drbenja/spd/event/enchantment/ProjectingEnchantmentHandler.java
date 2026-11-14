package com.drbenja.spd.event.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:projecting
 * - Melee reach only while holding the weapon (transient attribute).
 * - Ranged: one-time launch boost (more range / flatter effective arc).
 *   Continuous anti-gravity was removed — it caused arrows to "bounce" after
 *   hitting solid blocks (Y push every tick while stuck in ground).
 *
 * Gravity is what shapes the arc; increasing initial velocity extends range
 * without fighting the physics once the projectile has landed.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ProjectingEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "projecting"));

    private static final Identifier REACH_MOD_ID =
            Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "projecting_reach");

    private ProjectingEnchantmentHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel server)) return;

        int level = getLevel(server, player.getMainHandItem());
        AttributeInstance reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach == null) return;

        reach.removeModifier(REACH_MOD_ID);
        if (level > 0) {
            // Empowered: slightly longer reach (multiplier normally 1)
            double amount = 0.5 * level * ProcPower.multiplier(player);
            reach.addTransientModifier(new AttributeModifier(
                    REACH_MOD_ID,
                    amount,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        if (!(event.getLevel() instanceof ServerLevel server)) return;
        int level = levelFromFiredWeapon(server, projectile);
        if (level <= 0) return;

        // One-shot range boost: +12% horizontal per level, scaled by multiplier when empowered
        float empower = 1f;
        if (projectile.getOwner() instanceof net.minecraft.world.entity.LivingEntity living) {
            empower = ProcPower.multiplier(living);
        }
        double mult = 1.0 + 0.12 * level * empower;
        Vec3 v = projectile.getDeltaMovement();
        projectile.setDeltaMovement(v.x * mult, v.y * (1.0 + 0.04 * level * empower), v.z * mult);
        projectile.getPersistentData().putInt("SpdProjecting", level);
        projectile.getPersistentData().putInt("SpdProjectingAge", 0);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.level().isClientSide()) return;
        if (!(e instanceof Projectile projectile)) return;
        int level = projectile.getPersistentData().getInt("SpdProjecting").orElse(0);
        if (level <= 0) return;

        // Stop any residual logic once nearly stopped or on ground (prevents bounce)
        if (projectile.onGround() || projectile.getDeltaMovement().lengthSqr() < 0.0025) {
            projectile.getPersistentData().remove("SpdProjecting");
            return;
        }

        int age = projectile.getPersistentData().getInt("SpdProjectingAge").orElse(0) + 1;
        projectile.getPersistentData().putInt("SpdProjectingAge", age);
        // Safety: only touch velocity during early flight (≤3s)
        if (age > 60) {
            projectile.getPersistentData().remove("SpdProjecting");
        }
    }

    private static int levelFromFiredWeapon(ServerLevel level, Projectile projectile) {
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
        } catch (Exception ignored) {}
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
}
