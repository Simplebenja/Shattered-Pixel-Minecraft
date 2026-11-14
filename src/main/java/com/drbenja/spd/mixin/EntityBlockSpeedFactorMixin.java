package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * Catch block speed factor on Entity (slime, honey, soul sand, modded).
 * LivingEntity also overrides; both mixins keep purity at full speed.
 */
@Mixin(Entity.class)
public abstract class EntityBlockSpeedFactorMixin {

    @Inject(method = "getBlockSpeedFactor", at = @At("HEAD"), cancellable = true)
    private void spd$purityBlockSpeed(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            cir.setReturnValue(1.0F);
        }
    }
}
