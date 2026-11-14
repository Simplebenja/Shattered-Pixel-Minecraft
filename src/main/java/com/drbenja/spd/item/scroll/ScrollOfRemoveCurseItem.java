package com.drbenja.spd.item.scroll;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Removes Vanishing and Binding curses from the offhand item (no GUI yet).
 * MC 26.3: ResourceKey uses {@code identifier()}, not {@code location()}.
 */
public class ScrollOfRemoveCurseItem extends SpdScrollItem {
    public ScrollOfRemoveCurseItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }
        ItemEnchantments enchants = off.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) {
            return;
        }

        Holder<Enchantment> vanishing = null;
        Holder<Enchantment> binding = null;
        try {
            var reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            vanishing = reg.get(Enchantments.VANISHING_CURSE).orElse(null);
            binding = reg.get(Enchantments.BINDING_CURSE).orElse(null);
        } catch (Throwable ignored) {
        }

        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            int lvl = enchants.getLevel(holder);
            if (lvl <= 0) {
                continue;
            }
            if (vanishing != null && holder.equals(vanishing)) {
                continue;
            }
            if (binding != null && holder.equals(binding)) {
                continue;
            }
            String path = holder.unwrapKey()
                    .map(k -> k.identifier().getPath())
                    .orElse("");
            if (path.contains("vanishing") || path.contains("binding")) {
                continue;
            }
            mut.set(holder, lvl);
        }
        off.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
    }
}
