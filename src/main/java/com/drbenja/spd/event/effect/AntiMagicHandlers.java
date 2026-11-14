package com.drbenja.spd.event.effect;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/** Anti-Magic: cancela daño mágico (witch-resistant / IS_MAGIC). */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class AntiMagicHandlers {

    private AntiMagicHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.hasEffect(ModEffects.ANTI_MAGIC)) return;

        if (event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            event.setCanceled(true);
            return;
        }
        try {
            if (event.getSource().is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC)) {
                event.setCanceled(true);
            }
        } catch (Throwable ignored) {
        }
    }
}
