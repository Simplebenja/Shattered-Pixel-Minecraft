package com.drbenja.spd.event.effect;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.BleedingEffect;
import com.drbenja.spd.effect.CausticOozeEffect;
import com.drbenja.spd.effect.PoisonedEffect;

/**
 * Cancela retroceso cuando el ultimo daño fue bleeding / caustic_ooze
 * (refuerzo ademas del tag #minecraft:no_knockback).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class NoKnockbackDamageEvents {

    private NoKnockbackDamageEvents() {}

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource last = entity.getLastDamageSource();
        if (last == null) {
            return;
        }
        if (last.is(DamageTypeTags.NO_KNOCKBACK)
                || last.is(BleedingEffect.DAMAGE_TYPE)
                || last.is(CausticOozeEffect.DAMAGE_TYPE)
                || last.is(PoisonedEffect.DAMAGE_TYPE)) {
            event.setCanceled(true);
        }
    }
}

