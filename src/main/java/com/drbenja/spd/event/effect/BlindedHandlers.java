package com.drbenja.spd.event.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.BlindedEffect;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BlindedHandlers {

    private BlindedHandlers() {}

    /** Impide adquirir targets más allá de 1 bloque mientras está Blinded. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.BLINDED)) return;
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) return;
        if (entity.distanceTo(newTarget) > BlindedEffect.DETECT_RANGE) {
            event.setCanceled(true);
        }
    }
}
