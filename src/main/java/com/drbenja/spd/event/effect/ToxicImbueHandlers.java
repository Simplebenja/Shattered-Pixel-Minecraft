package com.drbenja.spd.event.effect;

import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ToxicImbueHandlers {

    private ToxicImbueHandlers() {}

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance() == null) return;
        if (!event.getEntity().hasEffect(ModEffects.IMBUED_WITH_TOXICITY)) return;
        if (event.getEffectInstance().getEffect().is(MobEffects.POISON)
                || event.getEffectInstance().getEffect().is(ModEffects.POISONED)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}

