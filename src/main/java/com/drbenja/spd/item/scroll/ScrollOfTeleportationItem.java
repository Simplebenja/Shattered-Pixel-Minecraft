package com.drbenja.spd.item.scroll;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ScrollHelpers;

public class ScrollOfTeleportationItem extends SpdScrollItem {
    public ScrollOfTeleportationItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        ScrollHelpers.safeRandomTeleport(level, player, 32);

    }
}
