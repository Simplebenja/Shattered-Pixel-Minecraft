package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.SpdVision;

/**
 * SPD Lullaby: Drowsy a todos en FoV + al lector.
 * Magical Sleep al expirar Drowsy si tienen vida faltante (handler).
 */
public class ScrollOfLullabyItem extends SpdScrollItem {
    public static final int DROWSY_TICKS = 20 * 5;

    public ScrollOfLullabyItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        double r = SpdVision.visionRadius(player);
        // Lector siempre (SPD aplica Drowsy al hero)
        player.addEffect(new MobEffectInstance(ModEffects.DROWSY, DROWSY_TICKS, 0, false, true, true));

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, SpdVision.visionBox(player), LivingEntity::isAlive)) {
            if (e == player) continue;
            if (e.distanceToSqr(player) > r * r) continue;
            // Monstruos, animales, aldeanos, mascotas/aliados del jugador
            boolean allyOrPet = e.isAlliedTo(player)
                    || (e instanceof OwnableEntity own && own.getOwner() == player);
            boolean hostileOrNeutral = true; // SPD aplica a todos en FOV
            if (hostileOrNeutral || allyOrPet) {
                e.addEffect(new MobEffectInstance(ModEffects.DROWSY, DROWSY_TICKS, 0, false, true, true));
            }
        }
    }
}
