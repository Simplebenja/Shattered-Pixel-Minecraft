package com.drbenja.spd.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.init.ModEffects;

/**
 * FoV / visión efectiva de SPD para scrolls y efectos de área.
 * Radio base 8 bloques (esfera diámetro 17). Blinded SPD → radio 1.
 * Centralizado para no tocar cada scroll al ajustar.
 */
public final class SpdVision {

    /** Radio normal de visión (SPD hero FOV ~8 tiles). */
    public static final double BASE_RADIUS = 8.0;

    /** Radio cuando Blinded SPD está activo. */
    public static final double BLINDED_RADIUS = 1.0;

    /** Radio amplio de alerta (Rage / Challenge beckon). */
    public static final double ALERT_RADIUS = 48.0;

    private SpdVision() {}

    /** Radio de visión del spectator (jugador u otra entidad). */
    public static double visionRadius(LivingEntity viewer) {
        if (viewer == null) return BASE_RADIUS;
        if (viewer.hasEffect(ModEffects.BLINDED)) {
            return BLINDED_RADIUS;
        }
        return BASE_RADIUS;
    }

    public static AABB visionBox(LivingEntity viewer) {
        double r = visionRadius(viewer);
        return viewer.getBoundingBox().inflate(r);
    }

    public static boolean inVision(LivingEntity viewer, LivingEntity target) {
        if (viewer == null || target == null) return false;
        double r = visionRadius(viewer);
        return target.distanceToSqr(viewer) <= r * r;
    }

    public static AABB alertBox(LivingEntity viewer) {
        return viewer.getBoundingBox().inflate(ALERT_RADIUS);
    }
}
