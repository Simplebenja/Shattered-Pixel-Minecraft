package com.drbenja.spd.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.entity.cloud.ToxicGasCloudEntity;
import com.drbenja.spd.init.ModEntities;

/**
 * SPD ToxicImbue: inmune a poison y gas tóxico; emite gas tóxico alrededor cada segundo.
 * Duración por defecto 50 s.
 */
public class ImbuedWithToxicityEffect extends MobEffect {

    public static final int DEFAULT_DURATION_TICKS = 50 * 20;

    public ImbuedWithToxicityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x55AA22);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        entity.removeEffect(MobEffects.POISON);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.removeEffect(MobEffects.POISON);
        if (level.isClientSide()) {
            return true;
        }
        // Nube pequeña alrededor del portador (SPD: ~6 por vecino)
        ToxicGasCloudEntity cloud = new ToxicGasCloudEntity(ModEntities.TOXIC_GAS.get(), level);
        BlockPos pos = entity.blockPosition();
        cloud.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        cloud.seed(pos, 100);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            cloud.seed(pos.relative(d), 40);
        }
        level.addFreshEntity(cloud);
        return true;
    }
}
