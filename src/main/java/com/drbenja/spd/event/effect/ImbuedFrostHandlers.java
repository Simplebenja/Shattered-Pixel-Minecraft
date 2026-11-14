package com.drbenja.spd.event.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.config.effect.ImbuedWithFrostConfig;
import com.drbenja.spd.effect.ImbuedWithFrostEffect;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ImbuedFrostHandlers {

    private ImbuedFrostHandlers() {}

    private static boolean hasImbued(LivingEntity e) {
        return e.hasEffect(ModEffects.IMBUED_WITH_FROST);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (!hasImbued(entity)) {
            return;
        }
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        if (inst.getEffect().is(ModEffects.CHILLED) || inst.getEffect().is(ModEffects.FROZEN)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
        if (id != null && ImbuedWithFrostConfig.isExtraBlockedEffect(id)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * Sin corazones azules / overlay de congelación: forzar ticksFrozen = 0
     * (equivalente a no acumular freeze de powder snow).
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!hasImbued(living)) {
            return;
        }
        if (living.getTicksFrozen() > 0) {
            living.setTicksFrozen(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (hasImbued(target) && ImbuedWithFrostConfig.isFreezeOrBlockedDamage(event.getSource())) {
            event.setCanceled(true);
            target.setTicksFrozen(0);
            return;
        }

        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker) || !hasImbued(attacker)) {
            return;
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }
        if (ChillImmuneEntitiesConfig.isChillImmune(target)) {
            return;
        }

        MobEffectInstance imb = attacker.getEffect(ModEffects.IMBUED_WITH_FROST);
        int amp = imb != null ? imb.getAmplifier() : 0;
        int add = ImbuedWithFrostEffect.chillTicksForAmplifier(amp);
        // Acumular duración (como la nube de Frost), no resetear
        MobEffectInstance cur = target.getEffect(ModEffects.CHILLED);
        int base = cur != null ? cur.getDuration() : 0;
        target.addEffect(new MobEffectInstance(
                ModEffects.CHILLED, base + add, 0, false, true, true));
    }
}
