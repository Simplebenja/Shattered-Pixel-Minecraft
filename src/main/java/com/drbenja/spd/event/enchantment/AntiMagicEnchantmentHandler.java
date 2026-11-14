package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
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
 * spd:anti_magic — SPD drRoll:
 * NormalIntRange(level * mult, (3 + level*1.5) * mult) subtracted from magic damage.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class AntiMagicEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "anti_magic"));

    private AntiMagicEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        DamageSource src = event.getSource();
        if (DeferredDamageEffect.isDeferredSource(src)) return;
        if (!isMagicDamage(src)) return;

        int anti = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (anti <= 0) return;

        int s = EnchantLevels.spd(anti);
        float mult = ProcPower.multiplier(defender);
        int min = Math.round(s * mult);
        int max = Math.round((3 + s * 1.5f) * mult);
        if (max < min) max = min;
        int roll = min + defender.getRandom().nextInt(Math.max(1, max - min + 1));
        event.setAmount(Math.max(0f, event.getAmount() - roll));
    }

    private static boolean isMagicDamage(DamageSource source) {
        return source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypeTags.IS_LIGHTNING);
    }
}
