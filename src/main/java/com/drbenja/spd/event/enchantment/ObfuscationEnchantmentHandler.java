package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:obfuscation — SPD stealthBoost = (1 + level/3) * multiplier.
 * Detection is probabilistic: detectChance = 1 / (1 + stealthBoost).
 * Failed detection cancels the mob targeting the player.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ObfuscationEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "obfuscation"));

    private ObfuscationEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel server)) return;
        LivingEntity neu = event.getNewAboutToBeSetTarget();
        if (!(neu instanceof Player)) return;

        int level = ArmorEnchantHelper.getAnyArmorLevel(server, neu, KEY);
        if (level <= 0) return;

        int s = EnchantLevels.spd(level);
        float stealthBoost = (1f + s / 3f) * ProcPower.multiplier(neu);
        // Higher boost → lower chance to be detected
        float detectChance = 1f / (1f + stealthBoost);
        if (mob.getRandom().nextFloat() > detectChance) {
            event.setCanceled(true);
        }
    }
}
