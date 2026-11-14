package com.drbenja.spd.event.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.HasteEffect;
import com.drbenja.spd.effect.MaxHealthBoostEffect;
import com.drbenja.spd.effect.StaminaEffect;

/** Limpia modifiers de Haste/Stamina/MaxHealthBoost al expirar o quitar el efecto. */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class SpeedEffectHandlers {
    private SpeedEffectHandlers() {}

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        clear(event.getEntity(), inst.getEffect());
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        clear(event.getEntity(), inst.getEffect());
    }

    private static void clear(LivingEntity entity, Holder<MobEffect> effect) {
        if (effect.value() instanceof HasteEffect) {
            HasteEffect.clearModifier(entity);
        } else if (effect.value() instanceof StaminaEffect) {
            StaminaEffect.clearModifier(entity);
        } else if (effect.value() instanceof MaxHealthBoostEffect) {
            MaxHealthBoostEffect.clearModifier(entity);
        }
    }
}
