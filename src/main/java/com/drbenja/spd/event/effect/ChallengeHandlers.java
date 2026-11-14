package com.drbenja.spd.event.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.ChallengeZoneEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Challenge: −33% daño.
 * Baseline de hambre solo mientras el efecto está activo; se limpia al salir.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ChallengeHandlers {

    private ChallengeHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.hasEffect(ModEffects.CHALLENGE_ZONE)) {
            event.setAmount(event.getAmount() * 0.67f);
        }
    }

    /** Aplicación nueva (sin snapshot previo): capturar food/sat actuales. */
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.CHALLENGE_ZONE.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (ChallengeZoneEffect.hasBaseline(player)) return; // re-apply de zona
        ChallengeZoneEffect.captureBaseline(player);
    }

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.CHALLENGE_ZONE.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasEffect(ModEffects.CHALLENGE_ZONE)) {
            ChallengeZoneEffect.clearBaseline(player);
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModEffects.CHALLENGE_ZONE.get()) return;
        if (event.getEntity() instanceof Player player) {
            ChallengeZoneEffect.clearBaseline(player);
        }
    }
}
