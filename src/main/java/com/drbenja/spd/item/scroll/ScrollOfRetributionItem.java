package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ScrollHelpers;

public class ScrollOfRetributionItem extends SpdScrollItem {
    public ScrollOfRetributionItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        ScrollHelpers.retributionBlast(level, player, false);
        player.addEffect(new MobEffectInstance(ModEffects.BLINDED, 20 * 20, 0, false, true, true));
        player.addEffect(new MobEffectInstance(ModEffects.WEAKNESS_SPD, 20 * 20, 0, false, true, true));
    }
}
