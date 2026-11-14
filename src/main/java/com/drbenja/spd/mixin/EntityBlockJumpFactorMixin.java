package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.init.ModEffects;

/**
 * Honey block and similar reduce jump via getBlockJumpFactor — restore full jump with Purity.
 */
@Mixin(Entity.class)
public abstract class EntityBlockJumpFactorMixin {

    @Inject(method = "getBlockJumpFactor", at = @At("HEAD"), cancellable = true)
    private void spd$purityJumpFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            cir.setReturnValue(1.0F);
        }
    }
}
