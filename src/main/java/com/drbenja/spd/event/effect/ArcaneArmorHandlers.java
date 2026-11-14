package com.drbenja.spd.event.effect;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.ArcaneArmorEffect;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ArcaneArmorHandlers {

    private ArcaneArmorHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (ArcaneArmorEffect.currentLevel(entity) <= 0 || event.getAmount() <= 0) {
            return;
        }
        // NeoForge: daño mágico
        if (!event.getSource().is(Tags.DamageTypes.IS_MAGIC)) {
            return;
        }
        int roll = ArcaneArmorEffect.rollReduction(entity);
        if (roll <= 0) {
            return;
        }
        event.setAmount(Math.max(0.0F, event.getAmount() - roll));
    }
}
