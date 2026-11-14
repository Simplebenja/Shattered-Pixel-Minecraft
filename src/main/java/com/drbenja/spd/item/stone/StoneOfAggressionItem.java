package com.drbenja.spd.item.stone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.effect.AggressionEffect;
import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;

/**
 * SPD Stone of Aggression: marca a la entidad (o la más cercana al impacto).
 * 5 s si es hostil, 20 s si es aliado / el lanzador. No aplica a bosses/minibosses.
 */
public class StoneOfAggressionItem extends SpdStoneItem {
    public StoneOfAggressionItem(Properties props) {
        super(props);
    }

    @Override
    public void onImpact(ServerLevel level, Player thrower, HitResult hit, Vec3 impactPos) {
        LivingEntity target = resolveTarget(level, hit, impactPos, thrower);
        if (target == null) return;
        if (EntityClassHelper.isBoss(target) || EntityClassHelper.isMiniboss(target)) {
            return;
        }

        boolean allyOrSelf = target == thrower
                || (thrower != null && (target.isAlliedTo(thrower) || thrower.isAlliedTo(target)));
        int duration = allyOrSelf
                ? AggressionEffect.DURATION_ALLY_TICKS
                : AggressionEffect.DURATION_HOSTILE_TICKS;

        target.addEffect(new MobEffectInstance(ModEffects.AGGRESSION, duration, 0, false, true, true));
    }
}
