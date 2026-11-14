package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
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
 * spd:potential — SPD: proc (lvl+1)/(lvl+6); powerMulti charges wands.
 * MC: restores hunger/saturation scaled by powerMulti (no wand system).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class PotentialEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "potential"));

    private PotentialEnchantmentHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        LivingEntity defender = event.getEntity();
        if (DeferredDamageEffect.isDeferredSource(event.getSource())) return;
        if (!(defender instanceof Player player)) return;

        int pot = ArmorEnchantHelper.getAnyArmorLevel(server, defender, KEY);
        if (pot <= 0) return;

        int s = EnchantLevels.spd(pot);
        float procChance = ProcPower.chance(defender, (s + 1f) / (s + 6f));
        if (player.getRandom().nextFloat() >= procChance) return;
        float powerMulti = ProcPower.powerMulti(defender, procChance);

        int foodGain = Math.max(1, Math.round(powerMulti));
        float satGain = powerMulti;
        FoodData food = player.getFoodData();
        food.setFoodLevel(Math.min(20, food.getFoodLevel() + foodGain));
        food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + satGain));
    }
}
