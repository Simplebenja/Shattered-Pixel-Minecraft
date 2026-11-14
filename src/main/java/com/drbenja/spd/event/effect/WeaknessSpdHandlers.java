package com.drbenja.spd.event.effect;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/** Weakness SPD: -33% daño infligido. */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class WeaknessSpdHandlers {

    private WeaknessSpdHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity atk
                && atk.hasEffect(ModEffects.WEAKNESS_SPD)) {
            event.setAmount(event.getAmount() * 0.67f);
        }
    }
}
