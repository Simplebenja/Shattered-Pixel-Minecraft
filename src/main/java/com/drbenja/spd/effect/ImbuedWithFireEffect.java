package com.drbenja.spd.effect;

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
import net.minecraft.world.level.block.state.BlockState;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD FireImbue: 50 s. Inmune a Burning y daño de fuego (no lava).
 * Melee 50% → Burning 8 s (reignite). Solo prende vegetación cercana.
 */
public class ImbuedWithFireEffect extends MobEffect {

    public static final int DEFAULT_DURATION_TICKS = 1000; // 50 s

    public ImbuedWithFireEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF5500);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.hasEffect(ModEffects.BURNING)) {
            entity.clearFire();
            entity.removeEffect(ModEffects.BURNING);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (level.getGameTime() % 10 == 0) {
            igniteVegetation(level, entity.blockPosition());
        }
        return true;
    }

    /** Solo hojas, hierba, plantas — no madera, lana, etc. */
    public static void igniteVegetation(Level level, BlockPos center) {
        if (level.isClientSide()) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState st = level.getBlockState(pos);
            if (!isVegetation(st)) {
                continue;
            }
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()
                    && BaseFireBlock.canBePlacedAt(level, above, Direction.UP)) {
                level.setBlock(above, BaseFireBlock.getState(level, above), 3);
            } else if (st.isAir() && BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
                BlockPos below = pos.below();
                if (isVegetation(level.getBlockState(below))) {
                    level.setBlock(pos, BaseFireBlock.getState(level, pos), 3);
                }
            }
        }
    }

    public static boolean isVegetation(BlockState st) {
        if (st.is(BlockTags.LEAVES)
                || st.is(BlockTags.FLOWERS)
                || st.is(BlockTags.SAPLINGS)
                || st.is(BlockTags.REPLACEABLE)
                || st.is(Blocks.SHORT_GRASS)
                || st.is(Blocks.TALL_GRASS)
                || st.is(Blocks.FERN)
                || st.is(Blocks.LARGE_FERN)
                || st.is(Blocks.DEAD_BUSH)
                || st.is(Blocks.SWEET_BERRY_BUSH)
                || st.is(Blocks.MOSS_BLOCK)
                || st.is(Blocks.MOSS_CARPET)
                || st.is(Blocks.VINE)
                || st.is(Blocks.GLOW_LICHEN)) {
            return true;
        }
        return false;
    }

    /** SPD reignite: sube duración a al menos 8 s, no baja si ya es mayor. */
    public static void reigniteBurning(LivingEntity target) {
        int desired = BurningEffect.DEFAULT_DURATION_TICKS;
        var cur = target.getEffect(ModEffects.BURNING);
        if (cur != null && cur.getDuration() >= desired) {
            return;
        }
        int ticks = cur != null ? Math.max(cur.getDuration(), desired) : desired;
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.BURNING, ticks, 0, false, true, true));
    }
}
