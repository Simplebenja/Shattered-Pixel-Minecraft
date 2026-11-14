package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * ×3 movimiento (amp 0) / ×6 (amp 1). UUID propio → stackea con Speed vanilla y Stamina.
 */
public class HasteEffect extends MobEffect {
    public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("spd", "haste_speed");

    public HasteEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x57ADFF);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        applyModifier(entity, amplifier);
    }

    public static void applyModifier(LivingEntity entity, int amplifier) {
        AttributeInstance inst = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) {
            return;
        }
        inst.removeModifier(MODIFIER_ID);
        // ADD_MULTIPLIED_TOTAL: ×3 → +2.0; ×6 → +5.0
        double amount = amplifier >= 1 ? 5.0D : 2.0D;
        inst.addTransientModifier(new AttributeModifier(
                MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void clearModifier(LivingEntity entity) {
        AttributeInstance inst = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) {
            inst.removeModifier(MODIFIER_ID);
        }
    }
}
