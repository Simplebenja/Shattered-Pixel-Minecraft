package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.SpdVision;

/**
 * SPD Dread: duración 20s (Dread.DURATION). Flee + despawn fuera de visión.
 * No es infinito: si no despawnea a tiempo, el efecto expira.
 */
public class ScrollOfDreadItem extends SpdScrollItem {
    /** SPD Dread.DURATION = 20 turns → 20 segundos. */
    public static final int DREAD_DURATION_TICKS = 20 * 20;

    public ScrollOfDreadItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        double r = SpdVision.visionRadius(player);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, SpdVision.visionBox(player), Mob::isAlive)) {
            if (mob.distanceToSqr(player) > r * r) continue;
            if (EntityClassHelper.immuneToDread(mob)) continue;
            if (mob.getType().getCategory() == MobCategory.MONSTER || mob.getTarget() == player) {
                mob.addEffect(new MobEffectInstance(ModEffects.DREAD, DREAD_DURATION_TICKS, 0, false, true, true));
                TerrorEffect.setSource(mob, player);
            }
        }
    }
}
