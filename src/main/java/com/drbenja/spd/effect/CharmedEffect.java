package com.drbenja.spd.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Affection charm: mob loses aggro on charmer and cannot target them;
 * player under Charmed deals 0 melee damage to the charmer entity.
 * Duration reduced when the charmed entity takes damage.
 */
public class CharmedEffect extends MobEffect {

    private static final String KEY_CHARMER = "SpdCharmerUUID";

    public CharmedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4488);
    }

    public static void setCharmer(LivingEntity target, LivingEntity charmer) {
        target.getPersistentData().putString(KEY_CHARMER, charmer.getUUID().toString());
    }

    public static UUID getCharmer(LivingEntity target) {
        String s = target.getPersistentData().getString(KEY_CHARMER).orElse("");
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isCharmedBy(LivingEntity target, LivingEntity possibleCharmer) {
        UUID id = getCharmer(target);
        return id != null && possibleCharmer != null && id.equals(possibleCharmer.getUUID());
    }
}
