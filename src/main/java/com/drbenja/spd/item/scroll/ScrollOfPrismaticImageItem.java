package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Exotic upgrade of Mirror Image (SPD Prismatic Image).
 * Craftable now; effect deferred until Mirror Image is solid.
 */
public class ScrollOfPrismaticImageItem extends SpdScrollItem {

    public ScrollOfPrismaticImageItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        // TODO: prismatic copies (SPD exotic) — deferred
    }
}
