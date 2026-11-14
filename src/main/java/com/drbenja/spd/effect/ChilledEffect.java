package com.drbenja.spd.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD Chill: −10% por segundo restante (tope −50%) a:
 * - MOVEMENT_SPEED
 * - ATTACK_SPEED (cuerpo a cuerpo; en MC también es el ritmo de swing que limita re-ataques)
 *
 * Equivale a Char.spend() en SPD, que ralentiza todas las acciones del turno.
 * Se disipa al contacto con fuego / lava.
 */
public class ChilledEffect extends MobEffect {

    public static final Identifier MOVE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("spd", "chilled_movement");
    public static final Identifier ATTACK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("spd", "chilled_attack");

    /** @deprecated use MOVE_MODIFIER_ID; kept so old callers that referenced MODIFIER_ID still compile if any */
    @Deprecated
    public static final Identifier MODIFIER_ID = MOVE_MODIFIER_ID;

    public static final int FREEZE_THRESHOLD_TICKS = 200;

    public ChilledEffect() {
        super(MobEffectCategory.HARMFUL, 0x87CEEB);
    }

    /**
     * Factor de velocidad SPD: max(0.5, 1 - segundosRestantes * 0.1).
     * 0 ticks → 1.0; 100 ticks (5s) → 0.5; ≥100 ticks sigue en 0.5.
     */
    public static double speedFactor(int durationTicks) {
        double seconds = Math.max(0, durationTicks) / 20.0D;
        return Math.max(0.5D, 1.0D - seconds * 0.1D);
    }

    public static void applyModifierForDuration(LivingEntity entity, int durationTicks) {
        double amount = speedFactor(durationTicks) - 1.0D;
        applyOne(entity, Attributes.MOVEMENT_SPEED, MOVE_MODIFIER_ID, amount);
        applyOne(entity, Attributes.ATTACK_SPEED, ATTACK_MODIFIER_ID, amount);
    }

    private static void applyOne(
            LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier id,
            double amount) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (amount != 0.0D) {
            inst.addTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void clearModifier(LivingEntity entity) {
        clearOne(entity, Attributes.MOVEMENT_SPEED, MOVE_MODIFIER_ID);
        clearOne(entity, Attributes.ATTACK_SPEED, ATTACK_MODIFIER_ID);
    }

    private static void clearOne(
            LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier id) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(id);
        }
    }

    /** Fuego, lava, campfire lit, o ticks de fuego. */
    public static boolean isMelting(LivingEntity entity) {
        if (entity.isInLava() || entity.isOnFire() || entity.getRemainingFireTicks() > 0) {
            return true;
        }
        BlockPos pos = entity.blockPosition();
        for (BlockPos p : new BlockPos[]{pos, pos.above(), pos.below()}) {
            BlockState st = entity.level().getBlockState(p);
            if (st.getBlock() instanceof BaseFireBlock
                    || st.is(Blocks.LAVA)
                    || st.is(Blocks.FIRE)
                    || st.is(Blocks.SOUL_FIRE)
                    || (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT)
                    && Boolean.TRUE.equals(st.getValue(CampfireBlock.LIT)))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.hasEffect(ModEffects.BURNING)) {
            entity.clearFire();
            entity.removeEffect(ModEffects.BURNING);
        }

        entity.clearFire();
        entity.setRemainingFireTicks(0);
        MobEffectInstance self = entity.getEffect(ModEffects.CHILLED);
        int dur = self != null ? self.getDuration() : 0;
        if (dur >= FREEZE_THRESHOLD_TICKS) {
            convertToFrozen(entity, dur);
            return;
        }
        applyModifierForDuration(entity, dur);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (isMelting(entity)) {
            clearModifier(entity);
            entity.removeEffect(ModEffects.CHILLED);
            return false;
        }
        MobEffectInstance self = entity.getEffect(ModEffects.CHILLED);
        if (self == null) {
            clearModifier(entity);
            return false;
        }
        int dur = self.getDuration();
        if (dur >= FREEZE_THRESHOLD_TICKS) {
            convertToFrozen(entity, dur);
            return false;
        }
        applyModifierForDuration(entity, dur);
        return true;
    }

    private static void convertToFrozen(LivingEntity entity, int chillDurationTicks) {
        clearModifier(entity);
        entity.removeEffect(ModEffects.CHILLED);
        int frozenTicks = Math.min(chillDurationTicks, FREEZE_THRESHOLD_TICKS);
        FrozenEffect.applyOrExtend(entity, frozenTicks);
    }
}
