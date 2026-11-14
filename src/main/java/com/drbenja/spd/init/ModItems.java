package com.drbenja.spd.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.item.AquaBrewItem;
import com.drbenja.spd.item.GoldenBeehiveItem;
import com.drbenja.spd.item.RingTestItem;
import com.drbenja.spd.item.scroll.ScrollOfAntiMagicItem;
import com.drbenja.spd.item.scroll.ScrollOfChallengeItem;
import com.drbenja.spd.item.scroll.ScrollOfDreadItem;
import com.drbenja.spd.item.scroll.ScrollOfLullabyItem;
import com.drbenja.spd.item.scroll.ScrollOfPassageItem;
import com.drbenja.spd.item.scroll.ScrollOfPsionicBlastItem;
import com.drbenja.spd.item.scroll.ScrollOfRageItem;
import com.drbenja.spd.item.scroll.ScrollOfRemoveCurseItem;
import com.drbenja.spd.item.scroll.ScrollOfRetributionItem;
import com.drbenja.spd.item.scroll.ScrollOfSirensSongItem;
import com.drbenja.spd.item.scroll.ScrollOfTeleportationItem;
import com.drbenja.spd.item.scroll.ScrollOfTerrorItem;
import com.drbenja.spd.item.scroll.ScrollOfMirrorImageItem;
import com.drbenja.spd.item.scroll.ScrollOfPrismaticImageItem;
import com.drbenja.spd.item.stone.StoneOfAggressionItem;
import com.drbenja.spd.item.stone.StoneOfBlastItem;
import com.drbenja.spd.item.stone.StoneOfBlinkItem;
import com.drbenja.spd.item.stone.StoneOfDeepSleepItem;
import com.drbenja.spd.item.stone.StoneOfFearItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpdMod.MOD_ID);

    public static final DeferredItem<Item> SPD_TAB_ICON =
            ITEMS.registerItem("spd_tab_icon", Item::new);

    public static final DeferredItem<GoldenBeehiveItem> GOLDEN_BEEHIVE =
            ITEMS.registerItem("golden_beehive",
                    props -> new GoldenBeehiveItem(ModBlocks.GOLDEN_BEEHIVE.get(), props));

    public static final DeferredItem<SpawnEggItem> GOLDEN_BEE_SPAWN_EGG =
            ITEMS.registerItem("golden_bee_spawn_egg",
                    props -> new SpawnEggItem(props.component(
                            DataComponents.ENTITY_DATA,
                            TypedEntityData.of(ModEntities.GOLDEN_BEE.get(), new CompoundTag()))));

    public static final DeferredItem<AquaBrewItem> AQUA_BREW =
            ITEMS.registerItem("aqua_brew", props -> new AquaBrewItem(props.stacksTo(16)));

    public static final DeferredItem<ScrollOfTerrorItem> SCROLL_OF_TERROR =
            ITEMS.registerItem("scroll_of_terror", ScrollOfTerrorItem::new);
    public static final DeferredItem<ScrollOfMirrorImageItem> SCROLL_OF_MIRROR_IMAGE =
            ITEMS.registerItem("scroll_of_mirror_image", ScrollOfMirrorImageItem::new);
    public static final DeferredItem<ScrollOfPrismaticImageItem> SCROLL_OF_PRISMATIC_IMAGE =
            ITEMS.registerItem("scroll_of_prismatic_image", ScrollOfPrismaticImageItem::new);
    public static final DeferredItem<ScrollOfDreadItem> SCROLL_OF_DREAD =
            ITEMS.registerItem("scroll_of_dread", ScrollOfDreadItem::new);
    public static final DeferredItem<ScrollOfTeleportationItem> SCROLL_OF_TELEPORTATION =
            ITEMS.registerItem("scroll_of_teleportation", ScrollOfTeleportationItem::new);
    public static final DeferredItem<ScrollOfPassageItem> SCROLL_OF_PASSAGE =
            ITEMS.registerItem("scroll_of_passage", ScrollOfPassageItem::new);
    public static final DeferredItem<ScrollOfRemoveCurseItem> SCROLL_OF_REMOVE_CURSE =
            ITEMS.registerItem("scroll_of_remove_curse", ScrollOfRemoveCurseItem::new);
    public static final DeferredItem<ScrollOfAntiMagicItem> SCROLL_OF_ANTI_MAGIC =
            ITEMS.registerItem("scroll_of_anti_magic", ScrollOfAntiMagicItem::new);
    public static final DeferredItem<ScrollOfLullabyItem> SCROLL_OF_LULLABY =
            ITEMS.registerItem("scroll_of_lullaby", ScrollOfLullabyItem::new);
    public static final DeferredItem<ScrollOfSirensSongItem> SCROLL_OF_SIRENS_SONG =
            ITEMS.registerItem("scroll_of_sirens_song", ScrollOfSirensSongItem::new);
    public static final DeferredItem<ScrollOfRageItem> SCROLL_OF_RAGE =
            ITEMS.registerItem("scroll_of_rage", ScrollOfRageItem::new);
    public static final DeferredItem<ScrollOfChallengeItem> SCROLL_OF_CHALLENGE =
            ITEMS.registerItem("scroll_of_challenge", ScrollOfChallengeItem::new);
    public static final DeferredItem<ScrollOfRetributionItem> SCROLL_OF_RETRIBUTION =
            ITEMS.registerItem("scroll_of_retribution", ScrollOfRetributionItem::new);
    public static final DeferredItem<ScrollOfPsionicBlastItem> SCROLL_OF_PSIONIC_BLAST =
            ITEMS.registerItem("scroll_of_psionic_blast", ScrollOfPsionicBlastItem::new);

    public static final DeferredItem<StoneOfFearItem> STONE_OF_FEAR =
            ITEMS.registerItem("stone_of_fear", StoneOfFearItem::new);
    public static final DeferredItem<StoneOfBlinkItem> STONE_OF_BLINK =
            ITEMS.registerItem("stone_of_blink", StoneOfBlinkItem::new);
    public static final DeferredItem<StoneOfDeepSleepItem> STONE_OF_DEEP_SLEEP =
            ITEMS.registerItem("stone_of_deep_sleep", StoneOfDeepSleepItem::new);
    public static final DeferredItem<StoneOfAggressionItem> STONE_OF_AGGRESSION =
            ITEMS.registerItem("stone_of_aggression", StoneOfAggressionItem::new);
    public static final DeferredItem<StoneOfBlastItem> STONE_OF_BLAST =
            ITEMS.registerItem("stone_of_blast", StoneOfBlastItem::new);

    /** Debug: offhand → 100% SPD weapon enchant procs. */
    public static final DeferredItem<RingTestItem> RING_TEST =
            ITEMS.registerItem("ring_test", RingTestItem::new);

    private ModItems() {}
}

