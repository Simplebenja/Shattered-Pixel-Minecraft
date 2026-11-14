package com.drbenja.spd.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Potion of Experience — estilo Shattered Pixel Dungeon.
 *
 * SPD: {@code hero.earnExp(hero.maxExp())} → tamaño completo de la barra del nivel actual.
 * amp 0: +1 barra; amp 1: barra L + barra L+1.
 * Splash/lingering: {@code scale} escala la XP (como Instant Health).
 *
 * MC 26.3: clase {@link InstantaneousMobEffect} (grafía correcta con "a");
 * método {@code applyInstantaneousEffect(ServerLevel, ...)}.
 */
public class ExperienceEffect extends InstantaneousMobEffect {

    public ExperienceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFF55);
    }

    /** Bebible / tick (scale = 1.0). */
    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        grant(entity, amplifier, 1.0);
        return true;
    }

    /**
     * Splash / lingering: {@code scale} = efectividad por distancia.
     * Firma 26.3: (ServerLevel, source, owner, mob, amp, scale).
     */
    @Override
    public void applyInstantaneousEffect(ServerLevel level, @Nullable Entity source, @Nullable Entity owner,
                                         LivingEntity mob, int amplification, double scale) {
        grant(mob, amplification, scale);
    }

    private static void grant(LivingEntity living, int amplifier, double scale) {
        if (living.level().isClientSide() || !(living instanceof Player player)) {
            return;
        }
        int base = baseXpFor(player, amplifier);
        int amount = Math.max(0, (int) Math.round(base * scale));
        if (amount > 0) {
            player.giveExperiencePoints(amount);
        }
    }

    public static int baseXpFor(Player player, int amplifier) {
        int level = Math.max(0, player.experienceLevel);
        int xp = xpBarForLevel(level);
        if (amplifier >= 1) {
            xp += xpBarForLevel(level + 1);
        }
        return Math.max(1, xp);
    }

    public static int xpBarForLevel(int level) {
        if (level >= 30) {
            return 9 * level - 158;
        }
        if (level >= 15) {
            return 5 * level - 38;
        }
        return 2 * level + 7;
    }
}
