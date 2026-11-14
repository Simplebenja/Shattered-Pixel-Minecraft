package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.init.ModEffects;

/**
 * Purification Barrier: skip cobweb / powder-snow style stuck multipliers (and modded equivalents).
 */
@Mixin(Entity.class)
public abstract class EntityMakeStuckInBlockMixin {

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void spd$puritySkipStuck(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            ci.cancel();
        }
    }
}
