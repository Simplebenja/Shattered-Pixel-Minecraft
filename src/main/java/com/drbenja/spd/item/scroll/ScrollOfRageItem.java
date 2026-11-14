package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ScrollHelpers;
import com.drbenja.spd.util.SpdVision;

/**
 * 1) Alert all monsters wide → target player.
 * 2) Amok only in FoV (clears agro, retargets nearest).
 */
public class ScrollOfRageItem extends SpdScrollItem {
    public ScrollOfRageItem(Properties props) {
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
    }
}
