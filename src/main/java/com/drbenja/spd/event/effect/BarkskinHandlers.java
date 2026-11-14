package com.drbenja.spd.event.effect;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.BarkskinEffect;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BarkskinHandlers {

    private BarkskinHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        int maxReduction = BarkskinEffect.currentReduction(entity);
        if (maxReduction <= 0 || event.getAmount() <= 0) {
            return;
        }
        // NeoForge: daño físico
        if (!event.getSource().is(Tags.DamageTypes.IS_PHYSICAL)) {
            return;
        }
        // SPD: Random.NormalIntRange(0, level) ≈ uniforme 0..level
        int roll = entity.getRandom().nextInt(maxReduction + 1);
        if (roll <= 0) {
            return;
        }
        event.setAmount(Math.max(0.0F, event.getAmount() - roll));
    }
}
