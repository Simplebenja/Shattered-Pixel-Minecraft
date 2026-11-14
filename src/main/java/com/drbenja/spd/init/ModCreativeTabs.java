package com.drbenja.spd.init;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpdMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SPD = TABS.register("spd",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spd"))
                    .icon(() -> new ItemStack(ModItems.SPD_TAB_ICON.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((params, output) -> {
                        output.accept(ModItems.GOLDEN_BEEHIVE.get());
                        output.accept(ModItems.GOLDEN_BEE_SPAWN_EGG.get());

                        for (Holder<Potion> potion : allPotions()) {
                            output.accept(potionStack(Items.POTION, potion));
                            output.accept(potionStack(Items.SPLASH_POTION, potion));
                            output.accept(potionStack(Items.LINGERING_POTION, potion));
                            output.accept(potionStack(Items.TIPPED_ARROW, potion));
                        }
                        // Potion of Frost: solo splash (blob)
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.FROST));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.SNAP_FREEZE));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.LIQUID_FLAME));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.PARALYTIC_GAS));
                        output.accept(potionStack(Items.POTION, ModPotions.EARTHEN_ARMOR));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.EARTHEN_ARMOR));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.EARTHEN_ARMOR));
                        output.accept(potionStack(Items.TIPPED_ARROW, ModPotions.EARTHEN_ARMOR));
                        output.accept(potionStack(Items.POTION, ModPotions.ARCANE_ARMOR));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.ARCANE_ARMOR));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.ARCANE_ARMOR));
                        output.accept(potionStack(Items.TIPPED_ARROW, ModPotions.ARCANE_ARMOR));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.TOXIC_GAS));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.CORROSIVE_GAS));
                        output.accept(potionStack(Items.POTION, ModPotions.TOXIC_ESSENCE));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.TOXIC_ESSENCE));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.TOXIC_ESSENCE));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.SHROUDING_FOG));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.STORM_CLOUDS));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.INFERNAL_BREW));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.BLIZZARD_BREW));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.SHOCKING_BREW));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.CAUSTIC_BREW));
                        output.accept(ModItems.AQUA_BREW.get());
                        output.accept(ModItems.SCROLL_OF_TERROR.get());
                        output.accept(ModItems.SCROLL_OF_MIRROR_IMAGE.get());
                        output.accept(ModItems.SCROLL_OF_PRISMATIC_IMAGE.get());
                        output.accept(ModItems.SCROLL_OF_DREAD.get());
                        output.accept(ModItems.SCROLL_OF_TELEPORTATION.get());
                        output.accept(ModItems.SCROLL_OF_PASSAGE.get());
                        output.accept(ModItems.SCROLL_OF_REMOVE_CURSE.get());
                        output.accept(ModItems.SCROLL_OF_ANTI_MAGIC.get());
                        output.accept(ModItems.SCROLL_OF_LULLABY.get());
                        output.accept(ModItems.SCROLL_OF_SIRENS_SONG.get());
                        output.accept(ModItems.SCROLL_OF_RAGE.get());
                        output.accept(ModItems.SCROLL_OF_CHALLENGE.get());
                        output.accept(ModItems.SCROLL_OF_RETRIBUTION.get());
                        output.accept(ModItems.SCROLL_OF_PSIONIC_BLAST.get());
                        output.accept(ModItems.STONE_OF_FEAR.get());
                        output.accept(ModItems.STONE_OF_BLINK.get());
                        output.accept(ModItems.STONE_OF_DEEP_SLEEP.get());
                        output.accept(ModItems.STONE_OF_AGGRESSION.get());
                        output.accept(ModItems.STONE_OF_BLAST.get());
                        output.accept(ModItems.RING_TEST.get());

                        output.accept(potionStack(Items.POTION, ModPotions.DRAGON_BREATH));
                        output.accept(potionStack(Items.POTION, ModPotions.DRAGONS_BLOOD));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.DRAGONS_BLOOD));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.DRAGONS_BLOOD));
                        output.accept(potionStack(Items.TIPPED_ARROW, ModPotions.DRAGONS_BLOOD));
                        output.accept(potionStack(Items.POTION, ModPotions.ICY_TOUCH_0));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.ICY_TOUCH_0));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.ICY_TOUCH_0));
                        output.accept(potionStack(Items.TIPPED_ARROW, ModPotions.ICY_TOUCH_0));
                        output.accept(potionStack(Items.POTION, ModPotions.ICY_TOUCH_EXT));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.ICY_TOUCH_EXT));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.ICY_TOUCH_EXT));
                        output.accept(potionStack(Items.TIPPED_ARROW, ModPotions.ICY_TOUCH_EXT));
                        output.accept(potionStack(Items.POTION, ModPotions.ICY_TOUCH_1));
                        output.accept(potionStack(Items.SPLASH_POTION, ModPotions.ICY_TOUCH_1));
                        output.accept(potionStack(Items.LINGERING_POTION, ModPotions.ICY_TOUCH_1));
                        output.accept(potionStack(Items.TIPPED_ARROW, ModPotions.ICY_TOUCH_1));
                    })
                    .build());

    @SuppressWarnings("unchecked")
    private static Holder<Potion>[] allPotions() {
        return new Holder[] {
                ModPotions.REGENERATIVE_HEALING_0,
                ModPotions.REGENERATIVE_HEALING_EXT,
                ModPotions.REGENERATIVE_HEALING_1,
                ModPotions.REGENERATIVE_HEALING_2,
                ModPotions.REGENERATIVE_HEALING_1_EXT,
                ModPotions.HONEYED_HEALING_0,
                ModPotions.HONEYED_HEALING_EXT,
                ModPotions.HONEYED_HEALING_1,
                ModPotions.HONEYED_HEALING_2,
                ModPotions.HONEYED_HEALING_1_EXT,
                ModPotions.AQUATIC_HEALING_0,
                ModPotions.AQUATIC_HEALING_EXT,
                ModPotions.AQUATIC_HEALING_1,
                ModPotions.AQUATIC_HEALING_2,
                ModPotions.AQUATIC_HEALING_1_EXT,
                ModPotions.SHIELDING_0,
                ModPotions.SHIELDING_EXT,
                ModPotions.SHIELDING_1,
                ModPotions.SHIELDING_2,
                ModPotions.SHIELDING_1_EXT,
                ModPotions.EXPERIENCE_0,
                ModPotions.EXPERIENCE_1,
                ModPotions.HASTE_0,
                ModPotions.HASTE_EXT,
                ModPotions.HASTE_1,
                ModPotions.STAMINA_0,
                ModPotions.STAMINA_EXT,
                ModPotions.STAMINA_1,
                ModPotions.PURITY_0,
                ModPotions.PURITY_EXT,
                ModPotions.CLEANSING_0,
                ModPotions.CLEANSING_EXT,
                ModPotions.LEVITATION_0,
                ModPotions.LEVITATION_EXT,
                ModPotions.MIGHT_0,
                ModPotions.MIGHT_EXT,
                ModPotions.MIGHT_1,
                ModPotions.INVISIBILITY_0,
                ModPotions.INVISIBILITY_EXT,
                ModPotions.MIND_VISION_0,
                ModPotions.MIND_VISION_EXT,
                ModPotions.MIND_VISION_1,
        };
    }

    private static ItemStack potionStack(Item bottle, Holder<Potion> potion) {
        PotionContents contents = new PotionContents(
                Optional.of(potion),
                Optional.empty(),
                java.util.List.of(),
                Optional.empty()
        );
        ItemStack stack = new ItemStack(bottle);
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    private ModCreativeTabs() {}
}

