package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.DeferredDamageEffect;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:viscosity — Always-on percent defer:
 * percent = (lvl+1)/(lvl+6) * multiplier.
 * If percent &gt; 1: dmg = round(dmg/percent), all deferred (true reduction).
 * Else: defer ceil(dmg * percent), rest immediate.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ViscosityEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "viscosity"));

    private ViscosityEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        DamageSource src = event.getSource();
        if (DeferredDamageEffect.isDeferredSource(src)) return;

        int vis = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (vis <= 0) return;

        float amount = event.getAmount();
        if (amount <= 0f) return;

        int s = EnchantLevels.spd(vis);
        float percent = (s + 1f) / (s + 6f) * ProcPower.multiplier(defender);

        int deferred;
        if (percent > 1f) {
            // Empowered past 100%: reduce then defer all remaining
            float reduced = amount / percent;
            event.setAmount(0f);
            deferred = Math.max(1, Math.round(reduced));
            DeferredDamageEffect.addDeferred(defender, deferred);
        } else {
            deferred = (int) Math.ceil(amount * percent);
            deferred = Math.min(deferred, (int) Math.ceil(amount));
            if (deferred > 0) {
                event.setAmount(Math.max(0f, amount - deferred));
                DeferredDamageEffect.addDeferred(defender, deferred);
            }
        }
    }
}
