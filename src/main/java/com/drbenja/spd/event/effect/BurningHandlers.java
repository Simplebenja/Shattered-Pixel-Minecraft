package com.drbenja.spd.event.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.BurningEffect;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BurningHandlers {

    private BurningHandlers() {}

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst != null && inst.getEffect().is(ModEffects.BURNING)) {
            BurningEffect.clearFireOnEnd(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst != null && inst.getEffect().is(ModEffects.BURNING)) {
            BurningEffect.clearFireOnEnd(event.getEntity());
        }
    }
}
