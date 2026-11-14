package com.drbenja.spd.item.scroll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import com.drbenja.spd.init.ModEffects;

/**
 * SPD-style scroll: hold ~1s then apply. Blocked while Anti-Magic is active.
 */
public abstract class SpdScrollItem extends Item {

    public static final int USE_TICKS = 20;

    public SpdScrollItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.hasEffect(ModEffects.ANTI_MAGIC) || player.hasEffect(ModEffects.BLINDED)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide() && level instanceof ServerLevel server) {
                if (player.hasEffect(ModEffects.ANTI_MAGIC) || player.hasEffect(ModEffects.BLINDED)) {
                    return stack;
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
                onRead(server, player, stack);
                player.awardStat(Stats.ITEM_USED.get(this));
                stack.consume(1, player);
            }
        }
        return stack;
    }

    protected abstract void onRead(ServerLevel level, Player player, ItemStack stack);
}
