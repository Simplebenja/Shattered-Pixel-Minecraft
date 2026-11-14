package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Mob;

import com.drbenja.spd.init.ModEffects;

/**
 * RESPALDO (desactivado): ampliar getMaxFallDistance bajo Amok.
 * El comportamiento principal de precipicios pasa por
 * {@link WalkNodeEvaluatorAmokMixin} (huecos = WALKABLE).
 * Reactivar {@link #ENABLE} si hace falta complementar.
 */
@Mixin(Mob.class)
public abstract class MobAmokFallDistanceMixin {

    /** false = desactivado; el path de huecos lo lleva WalkNodeEvaluator. */
    private static final boolean ENABLE = false;

    /** Solo se usa si ENABLE = true. */
    private static final int AMOK_MAX_FALL = 60;

    @Inject(method = "getMaxFallDistance", at = @At("RETURN"), cancellable = true)
    private void spd$amokExtraFall(CallbackInfoReturnable<Integer> cir) {
        if (!ENABLE) return;
        Mob self = (Mob) (Object) this;
        if (!self.hasEffect(ModEffects.AMOK)) return;
        int vanilla = cir.getReturnValue();
        cir.setReturnValue(Math.max(vanilla, AMOK_MAX_FALL));
    }
}
