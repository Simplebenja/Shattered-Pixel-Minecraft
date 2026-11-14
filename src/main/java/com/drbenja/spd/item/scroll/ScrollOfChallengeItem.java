package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.entity.cloud.ChallengeZoneEntity;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ScrollHelpers;
import com.drbenja.spd.util.SpdVision;

/**
 * Alert wide + Amok in FoV + Challenge zone ~100s.
 */
public class ScrollOfChallengeItem extends SpdScrollItem {
    public ScrollOfChallengeItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        ScrollHelpers.alertMonsters(level, player, SpdVision.ALERT_RADIUS);

        double r = SpdVision.visionRadius(player);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, SpdVision.visionBox(player), Mob::isAlive)) {
            if (mob.distanceToSqr(player) > r * r) continue;
            if (mob.getType().getCategory() != MobCategory.MONSTER) continue;
            mob.addEffect(new MobEffectInstance(ModEffects.AMOK, 20 * 20, 0, false, true, true));
        }
        ChallengeZoneEntity.spawn(level, player.blockPosition(), 100 * 20);
    }
}
