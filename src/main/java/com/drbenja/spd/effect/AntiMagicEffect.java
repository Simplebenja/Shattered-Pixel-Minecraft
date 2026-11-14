package com.drbenja.spd.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Blocks magic damage; blocks scroll use; mutes enchant procs. */
public class AntiMagicEffect extends MobEffect {
    public AntiMagicEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88aaff);
    }
}
