package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.BlockingEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:brimstone — Immune to burning / on_fire / in_fire (campfire half).
 * When empowered (multiplier &gt; 1) and standing in fire / on hot floor:
 * grants a small Blocking shield scaled by (multiplier).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BrimstoneEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "brimstone"));

    private BrimstoneEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBurningApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        int brim = ArmorEnchantHelper.getAnyArmorLevel(server, event.getEntity(), KEY);
        if (brim <= 0) return;
        var inst = event.getEffectInstance();
        if (inst != null && inst.getEffect().is(ModEffects.BURNING)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFireDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        int brim = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (brim <= 0) return;

        DamageSource src = event.getSource();
        if (src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.IN_FIRE)) {
            event.setCanceled(true);
            defender.clearFire();
            defender.removeEffect(ModEffects.BURNING);
            return;
        }
        if (src.is(DamageTypes.CAMPFIRE)) {
            event.setAmount(event.getAmount() * 0.5f);
        }
        if (defender.hasEffect(ModEffects.BURNING)) {
            defender.removeEffect(ModEffects.BURNING);
        }
        if (!defender.isInLava() && defender.getRemainingFireTicks() > 0) {
            defender.clearFire();
        }
    }

    @SubscribeEvent
    public static void onTickEmpoweredShield(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living.level() instanceof ServerLevel server)) return;
        if ((living.tickCount % 20) != 0) return;

        int brim = ArmorEnchantHelper.getAnyArmorLevel(server, living, KEY);
        if (brim <= 0) return;

        float mult = ProcPower.multiplier(living);
        if (mult <= 1.001f) return; // only when empowered

        boolean hot = living.getRemainingFireTicks() > 0
                || living.isOnFire()
                || isHotFloor(living);
        if (!hot) return;

        // Small regenerative shield while bathed in heat when empowered
        int shield = Math.max(1, Math.round(2f * mult));
        BlockingEffect.apply(living, shield);
    }

    private static boolean isHotFloor(LivingEntity e) {
        var state = e.level().getBlockState(e.blockPosition().below());
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        String p = id.getPath();
        return p.contains("magma") || p.equals("campfire") || p.equals("soul_campfire")
                || p.contains("fire") || p.equals("lava");
    }
}
