package com.drbenja.spd.event.enchantment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:camouflage — While fully concealed in tall foliage, grant
 * {@link ModEffects#TRUE_INVISIBILITY} (not vanilla invisibility).
 * Duration scales: (3 + level/2) * multiplier seconds (refreshed while concealed).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class CamouflageEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "camouflage"));

    private CamouflageEnchantmentHandler() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living.level() instanceof ServerLevel server)) return;
        if ((living.tickCount % 10) != 0) return;

        int level = ArmorEnchantHelper.getAnyArmorLevel(server, living, KEY);
        if (level <= 0) return;
        if (!isFullyConcealed(living)) return;

        int s = EnchantLevels.spd(level);
        float seconds = (3f + s / 2f) * ProcPower.multiplier(living);
        int ticks = Math.max(20, Math.round(seconds * 20f));
        living.addEffect(new MobEffectInstance(ModEffects.TRUE_INVISIBILITY, ticks, 0, false, true, true));
    }

    private static boolean isFullyConcealed(LivingEntity e) {
        BlockPos feet = e.blockPosition();
        BlockPos head = BlockPos.containing(e.getX(), e.getY() + e.getBbHeight() * 0.9, e.getZ());
        return isConcealBlock(e.level().getBlockState(feet))
                && isConcealBlock(e.level().getBlockState(head));
    }

    private static boolean isConcealBlock(BlockState st) {
        if (st.is(Blocks.TALL_GRASS) || st.is(Blocks.LARGE_FERN) || st.is(Blocks.DEAD_BUSH)) return true;
        if (st.is(Blocks.SUGAR_CANE) || st.is(Blocks.KELP) || st.is(Blocks.KELP_PLANT)) return true;
        if (st.is(Blocks.TWISTING_VINES) || st.is(Blocks.TWISTING_VINES_PLANT)
                || st.is(Blocks.WEEPING_VINES) || st.is(Blocks.WEEPING_VINES_PLANT)) return true;
        if (st.is(Blocks.CAVE_VINES) || st.is(Blocks.CAVE_VINES_PLANT)) return true;
        if (st.is(Blocks.ROSE_BUSH) || st.is(Blocks.SUNFLOWER) || st.is(Blocks.LILAC) || st.is(Blocks.PEONY)) return true;
        Identifier id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
        if (id != null) {
            String path = id.getPath();
            if (path.equals("pale_hanging_moss") || path.equals("pitcher_plant") || path.equals("pitcher_crop")) {
                return true;
            }
        }
        return false;
    }
}

