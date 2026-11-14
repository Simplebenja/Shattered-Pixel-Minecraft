package com.drbenja.spd.event.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.CleansedEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Cleansed effect: blocks application of effects that Cleansed should strip/prevent.
 * Split from former PurityCleansingHandlers.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class CleansingHandlers {

    private CleansingHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null) {
            return;
        }
        if (!entity.hasEffect(ModEffects.CLEANSED)) {
            return;
        }
        if (incoming.is(ModEffects.CLEANSED)) {
            return;
        }
        if (CleansedEffect.shouldAffect(incoming.getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
