package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * +50% movimiento (amp 0) / +100% (amp 1). Stackea con Haste y Speed vanilla.
 */
public class StaminaEffect extends MobEffect {
    public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("spd", "stamina_speed");

    public StaminaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x33CC66);
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
        double amount = amplifier >= 1 ? 1.0D : 0.5D;
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
