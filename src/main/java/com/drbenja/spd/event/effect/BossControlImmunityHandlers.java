package com.drbenja.spd.event.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;

/**
 * Miniboss, Boss and Static cannot receive Corrupted, Dread or Enthralled
 * from any source (scrolls, stones, enchants, commands except where Applicable is bypassed).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class BossControlImmunityHandlers {

    private BossControlImmunityHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        Holder<MobEffect> holder = inst.getEffect();

        if (holder.is(ModEffects.CORRUPTED) && EntityClassHelper.immuneToCorruption(entity)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (holder.is(ModEffects.DREAD) && EntityClassHelper.immuneToDread(entity)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (holder.is(ModEffects.ENTHRALLED) && EntityClassHelper.immuneToEnthralled(entity)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
