package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.effect.CharmedEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AllyTeamHelper;
import com.drbenja.spd.util.ScrollHelpers;
import com.drbenja.spd.util.SpdVision;

/**
 * SPD Siren's Song: Charm all non-ally monsters in FoV; Enthrall the looked-at / nearest monster
 * that does <em>not</em> already have Enthralled.
 */
public class ScrollOfSirensSongItem extends SpdScrollItem {
    public ScrollOfSirensSongItem(Properties props) {
        super(props);
    }

    private static boolean isHostileMonster(Mob m) {
        return m.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean alreadyEnthralled(Mob m) {
        return m.hasEffect(ModEffects.ENTHRALLED);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        Mob target = null;
        // Prefer looked-at hostile that is NOT already enthralled
        if (ScrollHelpers.lookTarget(player, SpdVision.visionRadius(player)) instanceof Mob looked
                && isHostileMonster(looked)
                && !alreadyEnthralled(looked)
                && SpdVision.inVision(player, looked)) {
            target = looked;
        }
        if (target == null) {
            double best = Double.MAX_VALUE;
            for (Mob m : level.getEntitiesOfClass(Mob.class, SpdVision.visionBox(player), Mob::isAlive)) {
                if (!isHostileMonster(m)) continue;
                if (alreadyEnthralled(m)) continue;
                if (!SpdVision.inVision(player, m)) continue;
                double d = m.distanceToSqr(player);
                if (d < best) {
                    best = d;
                    target = m;
                }
            }
        }

        // Charm every other hostile monster in vision (SPD)
        for (Mob m : level.getEntitiesOfClass(Mob.class, SpdVision.visionBox(player), Mob::isAlive)) {
            if (!isHostileMonster(m)) continue;
            if (!SpdVision.inVision(player, m)) continue;
            if (m == target) continue;
            m.addEffect(new MobEffectInstance(ModEffects.CHARMED, 20 * 20, 0, false, true, true), player);
            CharmedEffect.setCharmer(m, player);
        }

        if (target == null) return;
        if (EntityClassHelper.immuneToEnthralled(target)) {
            target.addEffect(new MobEffectInstance(ModEffects.CHARMED, 20 * 20, 0, false, true, true), player);
            CharmedEffect.setCharmer(target, player);
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.CHARMED, 20 * 20, 0, false, true, true), player);
        CharmedEffect.setCharmer(target, player);
        // Pass player as effect source so convert loot gets killed_by_player
        target.addEffect(new MobEffectInstance(ModEffects.ENTHRALLED, MobEffectInstance.INFINITE_DURATION, 0, false, true, true), player);
        AllyTeamHelper.assignAllyToLeaderTeam(level, player, target);
    }
}
