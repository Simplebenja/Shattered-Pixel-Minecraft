package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.drbenja.spd.init.ModEffects;

/**
 * Purification Barrier: ignore block speed factors (soul sand, honey, slime, modded blocks, etc.).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockSpeedMixin {

    @Inject(method = "getBlockSpeedFactor", at = @At("HEAD"), cancellable = true)
    private void spd$purityIgnoreBlockSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            cir.setReturnValue(1.0F);
        }
    }
}
