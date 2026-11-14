package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Eldritch terror: flee from the source entity. Damage shortens duration.
 */
public class TerrorEffect extends MobEffect {

    private static final String KEY_SOURCE = "SpdTerrorSource";

    public TerrorEffect() {
        super(MobEffectCategory.HARMFUL, 0x222222);
    }

    public static void setSource(LivingEntity target, LivingEntity source) {
        target.getPersistentData().putString(KEY_SOURCE, source.getUUID().toString());
    }

    public static UUID getSource(LivingEntity target) {
        String s = target.getPersistentData().getString(KEY_SOURCE).orElse("");
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }
}
