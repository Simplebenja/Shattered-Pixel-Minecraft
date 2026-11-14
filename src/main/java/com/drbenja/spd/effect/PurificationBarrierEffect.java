package com.drbenja.spd.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Purification Barrier (Purity).
 * <ul>
 *   <li>Daño ambiental: tag {@code #spd:purification_blocked}</li>
 *   <li>Efectos de splash / nubes lingering: bloqueados (handler)</li>
 *   <li>Rosa de wither: no aplica wither al pisarla (handler)</li>
 *   <li>Bloques lentos (soul sand, honey, slime, etc.):
 *       {@code MOVEMENT_EFFICIENCY=1} hace que {@code getBlockSpeedFactor} ignore el factor del bloque</li>
 *   <li>Agua: {@code WATER_MOVEMENT_EFFICIENCY}</li>
 *   <li>Telaraña / powder snow: mixins {@code makeStuckInBlock}</li>
 *   <li>Lava: compensación por tick en handler (fluido, no block factor)</li>
 * </ul>
 */
public class PurificationBarrierEffect extends MobEffect {

    public static final Identifier MOVE_EFF_ID =
            Identifier.fromNamespaceAndPath("spd", "purity_movement_efficiency");
    public static final Identifier WATER_EFF_ID =
            Identifier.fromNamespaceAndPath("spd", "purity_water_movement_efficiency");

    public PurificationBarrierEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8F0FF);
        // LivingEntity.getBlockSpeedFactor: lerp(efficiency, blockFactor, 1.0) → 1.0 when efficiency=1
        this.addAttributeModifier(
                Attributes.MOVEMENT_EFFICIENCY,
                MOVE_EFF_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(
                Attributes.WATER_MOVEMENT_EFFICIENCY,
                WATER_EFF_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
