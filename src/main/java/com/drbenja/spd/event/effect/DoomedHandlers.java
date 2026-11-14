package com.drbenja.spd.event.effect;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.DoomedEffect;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class DoomedHandlers {

    private DoomedHandlers() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().hasEffect(ModEffects.DOOMED)) return;
        float amount = event.getAmount();
        if (amount <= 0f) return;
        event.setAmount(amount * DoomedEffect.DAMAGE_MULTIPLIER);
    }
}
