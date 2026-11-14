package com.drbenja.spd.event.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.KineticChargeEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AttackChargeHelper;
import com.drbenja.spd.util.EnchantResolveHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:kinetic — ConservedDamage (SPD):
 *   preservedDamage -= max(preserved * 0.025, 0.1) per second
 * Visual: kinetic_charge duration ticks = ceil(damage) * 20  → seconds shown = damage.
 * Amplifier stays 0 (seconds convey the charge, not potency).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class KineticEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "kinetic"));

    public static final String NBT_CONSERVED = "SpdKineticConserved";
    public static final String NBT_PRE_HP = "SpdKineticPreHp";
    public static final String NBT_HIT_AMOUNT = "SpdKineticHitAmount";
    public static final String NBT_DECAY_ACC = "SpdKineticDecayAcc";

    private KineticEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity target = event.getEntity();
        Entity attackerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(attackerEnt instanceof LivingEntity attacker) || direct == null) return;
        if (!AttackChargeHelper.allowsWeaponProc(attacker, direct)) return;

        int level = resolveLevel(server, attacker, direct);
        if (level <= 0) return;

        target.getPersistentData().putFloat(NBT_PRE_HP, target.getHealth());
        target.getPersistentData().putFloat(NBT_HIT_AMOUNT, event.getAmount());

        float conserved = attacker.getPersistentData().getFloat(NBT_CONSERVED).orElse(0f);
        if (conserved >= 1f) {
            int bonus = (int) Math.ceil(conserved);
            event.setAmount(event.getAmount() + bonus);
            attacker.getPersistentData().putFloat(NBT_CONSERVED, 0f);
            attacker.getPersistentData().remove(NBT_DECAY_ACC);
            syncVisual(attacker, 0f);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity dead = event.getEntity();
        Entity killerEnt = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(killerEnt instanceof LivingEntity killer) || direct == null) return;
        if (!AttackChargeHelper.allowsWeaponProc(killer, direct)) return;

        int level = resolveLevel(server, killer, direct);
        if (level <= 0) return;

        CompoundTag data = dead.getPersistentData();
        float preHp = data.getFloat(NBT_PRE_HP).orElse(0f);
        if (preHp <= 0f) return;

        float amount = data.getFloat(NBT_HIT_AMOUNT).orElse(0f);
        float overkill = Math.max(0f, amount - preHp);
        if (overkill < 1f) return;
        // Empowered (future rings): store more conserved damage
        overkill *= ProcPower.multiplier(killer);

        float existing = killer.getPersistentData().getFloat(NBT_CONSERVED).orElse(0f);
        float total = existing + overkill;
        killer.getPersistentData().putFloat(NBT_CONSERVED, total);
        syncVisual(killer, total);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        CompoundTag data = living.getPersistentData();
        float conserved = data.getFloat(NBT_CONSERVED).orElse(0f);

        // No charge: force-remove any leftover visual icons immediately
        if (conserved <= 0.05f) {
            if (data.contains(NBT_CONSERVED)) {
                data.remove(NBT_CONSERVED);
            }
            if (data.contains(NBT_DECAY_ACC)) {
                data.remove(NBT_DECAY_ACC);
            }
            clearAllKineticVisuals(living);
            return;
        }

        // Decay once per second (SPD ConservedDamage.act):
        // preservedDamage -= max(preserved * 0.025, 0.1)
        int acc = data.getInt(NBT_DECAY_ACC).orElse(0) + 1;
        if (acc >= 20) {
            data.putInt(NBT_DECAY_ACC, 0);
            conserved = conserved - Math.max(conserved * 0.025f, 0.1f);
            if (conserved <= 0.05f) {
                data.remove(NBT_CONSERVED);
                data.remove(NBT_DECAY_ACC);
                clearAllKineticVisuals(living);
                return;
            }
            data.putFloat(NBT_CONSERVED, conserved);
        } else {
            data.putInt(NBT_DECAY_ACC, acc);
        }

        // Every tick: visual must match current conserved (tier + duration)
        syncVisual(living, conserved);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        var player = event.getEntity();
        if (player == null) return;
        float conserved = player.getPersistentData().getFloat(NBT_CONSERVED).orElse(0f);
        if (conserved < 1f) return;
        try {
            Holder<Enchantment> h = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
            if (EnchantmentHelper.getItemEnchantmentLevel(h, stack) <= 0) return;
            int shown = (int) Math.ceil(conserved);
            event.getToolTip().add(Component.translatable("tooltip.spd.kinetic_charge", shown)
                    .withColor(0xFFAA33));
        } catch (Exception ignored) {}
    }

    /**
     * Duration seconds == ceil(conserved damage). Amplifier always 0 so the
     * effect timer is the readable charge indicator (like SPD icon text).
     * Only one tier effect is ever present: other tiers are removed when
     * crossing cutoffs (up or down).
     */
    private static void syncVisual(LivingEntity entity, float conserved) {
        if (conserved < 0.05f) {
            clearAllKineticVisuals(entity);
            return;
        }
        int seconds = Math.max(1, (int) Math.ceil(conserved));
        int ticks = seconds * 20;
        var wanted = switch (KineticChargeEffect.tierFor(conserved)) {
            case HIGH -> ModEffects.KINETIC_CHARGE_HIGH;
            case MID -> ModEffects.KINETIC_CHARGE_MID;
            case LOW -> ModEffects.KINETIC_CHARGE;
        };

        // Always strip the other tiers first (previous icon must not linger)
        if (wanted != ModEffects.KINETIC_CHARGE) {
            entity.removeEffect(ModEffects.KINETIC_CHARGE);
        }
        if (wanted != ModEffects.KINETIC_CHARGE_MID) {
            entity.removeEffect(ModEffects.KINETIC_CHARGE_MID);
        }
        if (wanted != ModEffects.KINETIC_CHARGE_HIGH) {
            entity.removeEffect(ModEffects.KINETIC_CHARGE_HIGH);
        }

        MobEffectInstance cur = entity.getEffect(wanted);
        // Re-add when missing, wrong duration, or wrong amp so HUD stays in sync
        if (cur == null || Math.abs(cur.getDuration() - ticks) > 10) {
            entity.addEffect(new MobEffectInstance(wanted, ticks, 0, true, true, true));
        }
    }

    private static void clearAllKineticVisuals(LivingEntity entity) {
        // Use ALLOW_REMOVE in case sticky is re-enabled later for these effects
        Boolean prev = com.drbenja.spd.event.effect.StickyEffectHandlers.ALLOW_REMOVE.get();
        try {
            com.drbenja.spd.event.effect.StickyEffectHandlers.ALLOW_REMOVE.set(Boolean.TRUE);
            entity.removeEffect(ModEffects.KINETIC_CHARGE);
            entity.removeEffect(ModEffects.KINETIC_CHARGE_MID);
            entity.removeEffect(ModEffects.KINETIC_CHARGE_HIGH);
        } finally {
            com.drbenja.spd.event.effect.StickyEffectHandlers.ALLOW_REMOVE.set(prev);
        }
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
