package com.drbenja.spd.effect;

import com.drbenja.spd.entity.classy.EntityClassHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD Burning: mantiene al objetivo en llamas mientras dura el efecto.
 * El daño lo maneja vanilla (ticks de fuego). Se apaga con agua/lluvia/powder snow,
 * Chilled/Frozen, o salpicadura de cualquier splash potion.
 */
public class BurningEffect extends MobEffect {

    /** SPD DURATION ≈ 8 s. */
    public static final int DEFAULT_DURATION_TICKS = 160;

    public BurningEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        entity.removeEffect(ModEffects.CHILLED);
        entity.removeEffect(ModEffects.FROZEN);
        ChilledEffect.clearModifier(entity);
        FrozenEffect.clearModifiers(entity);
        // Encender y mantener (vanilla)
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 160));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (EntityClassHelper.immuneToBurning(entity)) {
            entity.removeEffect(ModEffects.BURNING);
            return false;
        }
        if (shouldExtinguishEnvironment(entity)) {
            entity.clearFire();
            entity.removeEffect(ModEffects.BURNING);
            return false;
        }
        // Mantener llamas mientras el efecto exista
        if (entity.getRemainingFireTicks() < 40) {
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 80));
        }
        // Propagar fuego a bloques inflamables bajo/alrededor
        if (level.getGameTime() % 10 == 0) {
            tryIgniteNearby(level, entity.blockPosition());
        }
        return true;
    }

    public static boolean shouldExtinguishEnvironment(LivingEntity entity) {
        if (entity.isInWater() || entity.isUnderWater() || entity.isInWaterOrRain()) {
            return true;
        }
        if (entity.isInPowderSnow) {
            return true;
        }
        if (entity.hasEffect(ModEffects.CHILLED) || entity.hasEffect(ModEffects.FROZEN)) {
            return true;
        }
        return false;
    }

    public static void clearFireOnEnd(LivingEntity entity) {
        entity.clearFire();
        entity.setRemainingFireTicks(0);
    }

    public static void tryIgniteNearby(Level level, BlockPos center) {
        if (level.isClientSide()) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState st = level.getBlockState(pos);
            if (st.isAir()) {
                BlockPos below = pos.below();
                if (BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)
                        && level.getBlockState(below).isFlammable(level, below, Direction.UP)) {
                    level.setBlock(pos, BaseFireBlock.getState(level, pos), 3);
                }
            }
        }
    }

    /** Efectos del splash Liquid Flame sobre el terreno (3×3×3). */
    public static void affectTerrain(ServerLevel level, BlockPos center) {
        int r = 1;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockState st = level.getBlockState(pos);

            // TNT
            if (st.getBlock() instanceof TntBlock) {
                TntBlock.prime(level, pos);
                continue;
            }
            // Campfire apagado → encender
            if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT)
                    && !Boolean.TRUE.equals(st.getValue(CampfireBlock.LIT))) {
                level.setBlock(pos, st.setValue(CampfireBlock.LIT, true), 3);
                continue;
            }
            // Velas
            if (st.hasProperty(BlockStateProperties.LIT) && st.is(BlockTags.CANDLES)
                    && !Boolean.TRUE.equals(st.getValue(BlockStateProperties.LIT))) {
                level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), 3);
                continue;
            }
            if (st.hasProperty(BlockStateProperties.LIT) && st.is(BlockTags.CANDLE_CAKES)
                    && !Boolean.TRUE.equals(st.getValue(BlockStateProperties.LIT))) {
                level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), 3);
                continue;
            }
            // Nieve (no hielo)
            if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW)) {
                level.removeBlock(pos, false);
                continue;
            }
            // Fuego en aire sobre inflamable
            if (st.isAir()) {
                BlockPos below = pos.below();
                if (BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
                    level.setBlock(pos, BaseFireBlock.getState(level, pos), 3);
                }
            } else if (st.isFlammable(level, pos, Direction.UP)) {
                BlockPos above = pos.above();
                if (level.getBlockState(above).isAir()
                        && BaseFireBlock.canBePlacedAt(level, above, Direction.UP)) {
                    level.setBlock(above, BaseFireBlock.getState(level, above), 3);
                }
            }
        }
    }
}
