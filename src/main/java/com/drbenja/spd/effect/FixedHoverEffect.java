package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Levitación SPD: fija la altura Y al aplicar el efecto.
 * Movimiento horizontal normal; no sube ni cae mientras dura.
 */
public class FixedHoverEffect extends MobEffect {
    public static final String KEY_Y = "spd_fixed_hover_y";

    public FixedHoverEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCEFFFF);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        entity.getPersistentData().putDouble(KEY_Y, entity.getY());
        entity.fallDistance = 0.0F;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // MC 26.3: getDouble devuelve Optional<Double> → usar getDoubleOr
        double lockedY = entity.getPersistentData().getDoubleOr(KEY_Y, entity.getY());
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x, 0.0D, v.z);
        entity.setPos(entity.getX(), lockedY, entity.getZ());
        entity.fallDistance = 0.0F;
        entity.setNoGravity(true);
        return true;
    }

    public static void clearHover(LivingEntity entity) {
        entity.setNoGravity(false);
        entity.getPersistentData().remove(KEY_Y);
    }
}
