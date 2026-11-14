package com.drbenja.spd.item;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.drbenja.spd.block.GoldenBeehiveBlock;
import com.drbenja.spd.block.entity.GoldenBeehiveBlockEntity;

/**
 * MC 26.3:
 * - Tooltip: Consumer&lt;Component&gt; + TooltipDisplay
 * - BLOCK_ENTITY_DATA: TypedEntityData; leer NBT con getUnsafe()/copyTagWithoutId() (tag() es private)
 */
public class GoldenBeehiveItem extends BlockItem {
    public GoldenBeehiveItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, lines, flag);

        int honey = 0;
        BlockItemStateProperties stateProps = stack.get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            BlockState applied = stateProps.apply(this.getBlock().defaultBlockState());
            if (applied.hasProperty(GoldenBeehiveBlock.HONEY_LEVEL)) {
                honey = applied.getValue(GoldenBeehiveBlock.HONEY_LEVEL);
            }
        }

        int bees = 0;
        TypedEntityData<BlockEntityType<?>> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData != null) {
            try {
                // BE guarda en NestData.Occupants (saveAdditional); tag() es private
                CompoundTag tag = beData.copyTagWithoutId();
                CompoundTag nestData = tag.getCompound("NestData").orElse(tag);
                if (nestData.contains("Occupants")) {
                    bees = nestData.getListOrEmpty("Occupants").size();
                } else if (tag.contains("Occupants")) {
                    bees = tag.getListOrEmpty("Occupants").size();
                } else if (tag.contains("Bees")) {
                    bees = tag.getListOrEmpty("Bees").size();
                }
            } catch (Exception ignored) {
            }
        }

        lines.accept(Component.translatable("block.spd.golden_beehive.honey", honey, GoldenBeehiveBlock.MAX_HONEY)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.accept(Component.translatable("block.spd.golden_beehive.bees", bees, GoldenBeehiveBlockEntity.MAX_OCCUPANTS)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
