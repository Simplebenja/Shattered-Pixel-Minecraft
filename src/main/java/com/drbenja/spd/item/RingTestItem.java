package com.drbenja.spd.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Debug ring: when held in the offhand, forces all SPD weapon enchant procs to 100%.
 * Creative-only test item — not craftable.
 */
public class RingTestItem extends Item {

    public RingTestItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.literal("§e[TEST] Offhand → 100% enchant procs"));
        tooltip.accept(Component.literal("§7Does not craft. Creative only."));
    }
}
