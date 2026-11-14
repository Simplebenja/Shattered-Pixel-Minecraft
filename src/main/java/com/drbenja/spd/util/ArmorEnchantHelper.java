package com.drbenja.spd.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ArmorEnchantHelper {
    private ArmorEnchantHelper() {}

    public static int getLevel(ServerLevel level, LivingEntity entity, EquipmentSlot slot, ResourceKey<Enchantment> key) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return 0;
        try {
            Holder<Enchantment> h = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
            return EnchantmentHelper.getItemEnchantmentLevel(h, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getAnyArmorLevel(ServerLevel level, LivingEntity entity, ResourceKey<Enchantment> key) {
        int max = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            max = Math.max(max, getLevel(level, entity, slot, key));
        }
        return max;
    }
}
