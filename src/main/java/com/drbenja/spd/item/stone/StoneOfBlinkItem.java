package com.drbenja.spd.item.stone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.init.ModEffects;


public class StoneOfBlinkItem extends SpdStoneItem {
    public StoneOfBlinkItem(Properties props) {
        super(props);
    }

    @Override
    public void onImpact(ServerLevel level, Player thrower, HitResult hit, Vec3 impactPos) {
        if (thrower != null) {
            BlockPos pos = BlockPos.containing(impactPos);
            // stand on top of solid if needed
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                pos = pos.above();
            }
            thrower.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        }

    }
}
