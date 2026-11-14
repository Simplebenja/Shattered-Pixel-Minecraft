package com.drbenja.spd.item.stone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.init.ModEffects;

public class StoneOfFearItem extends SpdStoneItem {
    public StoneOfFearItem(Properties props) {
        super(props);
    }

    @Override
    public void onImpact(ServerLevel level, Player thrower, HitResult hit, Vec3 impactPos) {
        LivingEntity target = resolveTarget(level, hit, impactPos, thrower);
        if (target == null) return;
        target.addEffect(new MobEffectInstance(ModEffects.TERROR, 20 * 20, 0, false, true, true));
        if (thrower != null) TerrorEffect.setSource(target, thrower);
    }
}
