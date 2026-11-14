package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.DeferredDamageEffect;
import com.drbenja.spd.effect.HerbalArmorEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:entanglement — 25% proc; Earthroot armor pool = (5 + 2 * level) * powerMulti.
 * Herbal armor breaks on intentional jump / sprint (existing behavior).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class EntanglementEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "entanglement"));

    private EntanglementEnchantmentHandler() {}


    /** Absorb through active herbal armor pool before other armor glyphs. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAbsorb(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel)) return;
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;
        LivingEntity defender = event.getEntity();
        float amount = event.getAmount();
        if (amount <= 0f) return;
        float blocked = HerbalArmorEffect.absorb(defender, amount);
        if (blocked > 0f) {
            event.setAmount(Math.max(0f, amount - blocked));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;

        int ent = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (ent <= 0) return;

        float procChance = ProcPower.chance(defender, 0.25f);
        if (defender.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(defender, procChance);

        int s = EnchantLevels.spd(ent);
        float pool = Math.round((5f + 2f * s) * powerMulti);
        HerbalArmorEffect.apply(defender, pool);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) return;
        if (e.hasEffect(ModEffects.HERBAL_ARMOR) && !HerbalArmorEffect.inGrace(e)) {
            HerbalArmorEffect.clear(e);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (living.hasEffect(ModEffects.HERBAL_ARMOR)
                && !HerbalArmorEffect.inGrace(living)
                && living.isSprinting()) {
            HerbalArmorEffect.clear(living);
        }
    }
}
