package com.drbenja.spd.event.effect;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.ImbuedWithFireEffect;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ImbuedFireHandlers {

    private ImbuedFireHandlers() {}

    private static boolean hasImbued(LivingEntity e) {
        return e.hasEffect(ModEffects.IMBUED_WITH_FIRE);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        if (!hasImbued(event.getEntity())) {
            return;
        }
        MobEffectInstance inst = event.getEffectInstance();
        if (inst != null && inst.getEffect().is(ModEffects.BURNING)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (hasImbued(target)) {
            // Fuego/calor (incl. magma block): bloqueado. Lava: 50% daño.
            if (isBlockedFireHeat(event)) {
                event.setCanceled(true);
                target.clearFire();
                return;
            }
            if (isLava(event)) {
                event.setAmount(event.getAmount() * 0.5F);
            }
        }
        // Melee 50% Burning
        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker) || !hasImbued(attacker)) {
            return;
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }
        if (attacker.getRandom().nextBoolean()) {
            ImbuedWithFireEffect.reigniteBurning(target);
        }
    }

    /**
     * Fuego/calor bloqueado (incluye magma/hot floor).
     * Lava NO se cancela aquí: se reduce a la mitad aparte.
     */
    private static boolean isBlockedFireHeat(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (source.is(DamageTypes.LAVA)) {
            return false;
        }
        if (source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.CAMPFIRE)
                || source.is(DamageTypes.HOT_FLOOR)) {
            return true;
        }
        var key = source.typeHolder().unwrapKey();
        if (key.isEmpty()) {
            return false;
        }
        String path = key.get().identifier().getPath();
        if (path.contains("lava")) {
            return false;
        }
        return path.contains("fire") || path.contains("burn") || path.contains("hot_floor")
                || path.contains("magma");
    }

    private static boolean isLava(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (source.is(DamageTypes.LAVA)) {
            return true;
        }
        var key = source.typeHolder().unwrapKey();
        return key.isPresent() && key.get().identifier().getPath().contains("lava");
    }
}
