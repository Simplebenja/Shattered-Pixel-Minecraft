package com.drbenja.spd.event.effect;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.mob.GoldenBeeEntity;
import com.drbenja.spd.init.ModEffects;

/**
 * Domesticar Golden Bee cuando recibe Honeyed Healing (splash/lingering/efecto),
 * no solo por click con botella.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class HoneyedBeeTameHandler {

    private HoneyedBeeTameHandler() {}

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof GoldenBeeEntity bee)) {
            return;
        }
        if (bee.isTame()) {
            return;
        }
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null || !inst.is(ModEffects.HONEYED_HEALING)) {
            return;
        }
        // Fuente del efecto (jugador que lanzó splash / aplicó efecto)
        Entity source = event.getEffectSource();
        if (source instanceof Player player) {
            bee.tameBy(player);
            bee.level().broadcastEntityEvent(bee, (byte) 7);
        }
    }
}
