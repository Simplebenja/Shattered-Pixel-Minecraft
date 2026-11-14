package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Aumenta vida máxima: 4 + 5% del max HP base actual.
 * Amplificador ≥1 (versión potenciada): ×2 el boost.
 * Al aplicar cura esa misma cantidad de vida.
 */
public class MaxHealthBoostEffect extends MobEffect {
    public static final Identifier MODIFIER_ID =
            Identifier.fromNamespaceAndPath("spd", "max_health_boost");

    public MaxHealthBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE67E22);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        float boost = computeBoost(entity, amplifier);
        applyModifier(entity, boost);
        // Cura la misma cantidad (sin superar el nuevo máximo)
        entity.heal(boost);
    }

    /** 4 + 5% del max HP actual (antes del modificador propio). Potenciada = ×2. */
    public static float computeBoost(LivingEntity entity, int amplifier) {
        AttributeInstance maxHp = entity.getAttribute(Attributes.MAX_HEALTH);
        double base = maxHp != null ? maxHp.getBaseValue() : 20.0D;
        // Si ya hay modificador nuestro, usamos baseValue; si no, getValue puede incluir otros mods
        if (maxHp != null && maxHp.hasModifier(MODIFIER_ID)) {
            base = maxHp.getBaseValue();
        }
        float boost = (float) (4.0D + base * 0.05D);
        if (amplifier >= 1) {
            boost *= 2.0f;
        }
        return Math.max(1.0f, boost);
    }

    public static void applyModifier(LivingEntity entity, float boost) {
        AttributeInstance inst = entity.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.removeModifier(MODIFIER_ID);
        inst.addTransientModifier(new AttributeModifier(
                MODIFIER_ID, boost, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void clearModifier(LivingEntity entity) {
        AttributeInstance inst = entity.getAttribute(Attributes.MAX_HEALTH);
        if (inst != null) {
            inst.removeModifier(MODIFIER_ID);
        }
        // Si la vida actual supera el nuevo máximo, recortar
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }
}
