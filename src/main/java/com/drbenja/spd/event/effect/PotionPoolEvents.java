package com.drbenja.spd.event.effect;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.AquaticHealingEffect;
import com.drbenja.spd.effect.BarrierEffect;
import com.drbenja.spd.effect.HoneyedHealingEffect;
import com.drbenja.spd.effect.RegenerativeHealingEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Guarda duration/amp entrantes y limpia pools al remover (excepto conversión a infinito).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class PotionPoolEvents {

    private PotionPoolEvents() {}

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CompoundTag data = entity.getPersistentData();

        if (inst.is(ModEffects.REGENERATIVE_HEALING)) {
            if (data.getBooleanOr(RegenerativeHealingEffect.KEY_JUST_CONVERTED, false)) return;
            data.putInt(RegenerativeHealingEffect.KEY_INCOMING_DURATION, inst.getDuration());
            data.putInt(RegenerativeHealingEffect.KEY_INCOMING_AMP, inst.getAmplifier());
        } else if (inst.is(ModEffects.HONEYED_HEALING)) {
            if (data.getBooleanOr(HoneyedHealingEffect.KEY_JUST_CONVERTED, false)) return;
            data.putInt(HoneyedHealingEffect.KEY_INCOMING_DURATION, inst.getDuration());
            data.putInt(HoneyedHealingEffect.KEY_INCOMING_AMP, inst.getAmplifier());
        } else if (inst.is(ModEffects.AQUATIC_HEALING)) {
            if (data.getBooleanOr(AquaticHealingEffect.KEY_JUST_CONVERTED, false)) return;
            data.putInt(AquaticHealingEffect.KEY_INCOMING_DURATION, inst.getDuration());
            data.putInt(AquaticHealingEffect.KEY_INCOMING_AMP, inst.getAmplifier());
        } else if (inst.is(ModEffects.BARRIER)) {
            if (data.getBooleanOr(BarrierEffect.KEY_JUST_CONVERTED, false)) return;
            data.putInt(BarrierEffect.KEY_INCOMING_DURATION, inst.getDuration());
            data.putInt(BarrierEffect.KEY_INCOMING_AMP, inst.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();

        if (inst.is(ModEffects.REGENERATIVE_HEALING)) {
            if (data.getBooleanOr(RegenerativeHealingEffect.KEY_JUST_CONVERTED, false)) return;
            data.remove(RegenerativeHealingEffect.KEY_LEFT);
            data.remove(RegenerativeHealingEffect.KEY_INCOMING_DURATION);
            data.remove(RegenerativeHealingEffect.KEY_INCOMING_AMP);
            data.remove(RegenerativeHealingEffect.KEY_RATE_MULT);
        } else if (inst.is(ModEffects.HONEYED_HEALING)) {
            if (data.getBooleanOr(HoneyedHealingEffect.KEY_JUST_CONVERTED, false)) return;
            data.remove(HoneyedHealingEffect.KEY_LEFT);
            data.remove(HoneyedHealingEffect.KEY_INCOMING_DURATION);
            data.remove(HoneyedHealingEffect.KEY_INCOMING_AMP);
            data.remove(HoneyedHealingEffect.KEY_RATE_MULT);
        } else if (inst.is(ModEffects.AQUATIC_HEALING)) {
            if (data.getBooleanOr(AquaticHealingEffect.KEY_JUST_CONVERTED, false)) return;
            data.remove(AquaticHealingEffect.KEY_LEFT);
            data.remove(AquaticHealingEffect.KEY_INCOMING_DURATION);
            data.remove(AquaticHealingEffect.KEY_INCOMING_AMP);
            data.remove(AquaticHealingEffect.KEY_RATE_MULT);
        } else if (inst.is(ModEffects.BARRIER)) {
            if (data.getBooleanOr(BarrierEffect.KEY_JUST_CONVERTED, false)) return;
            data.remove(BarrierEffect.KEY_LEFT);
            data.remove(BarrierEffect.KEY_INCOMING_DURATION);
            data.remove(BarrierEffect.KEY_INCOMING_AMP);
            data.remove(BarrierEffect.KEY_RATE_MULT);
            data.remove(BarrierEffect.KEY_PARTIAL);
            data.remove(BarrierEffect.KEY_INIT_DUR);
            data.remove(BarrierEffect.KEY_LAST_ABS);
            data.remove(BarrierEffect.KEY_OUR_MAX);
        }
    }
}
