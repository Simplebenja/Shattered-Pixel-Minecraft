package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import com.drbenja.spd.init.ModEffects;

/**
 * Bajo Amok, huecos/aire (OPEN) se planifican como WALKABLE.
 * El mob sigue la ruta más corta hacia el jugador aunque cruce vacío;
 * la caída la aplica la física, no una evaluación de “peligro”.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorAmokMixin {

    @Inject(method = "getPathType", at = @At("RETURN"), cancellable = true)
    private void spd$amokGapsAsWalkable(PathfindingContext context, int x, int y, int z,
                                        CallbackInfoReturnable<PathType> cir) {
        PathType type = cir.getReturnValue();
        if (type == null || !isGapType(type)) return;

        Mob mob = ((NodeEvaluatorAccessor) this).spd$getMob();
        if (mob == null || !mob.hasEffect(ModEffects.AMOK)) return;

        cir.setReturnValue(PathType.WALKABLE);
    }

    private static boolean isGapType(PathType type) {
        if (type == PathType.OPEN) return true;
        String name = type.name();
        return name.equals("OPEN") || name.contains("OPEN_AIR") || name.equals("AIR");
    }
}
