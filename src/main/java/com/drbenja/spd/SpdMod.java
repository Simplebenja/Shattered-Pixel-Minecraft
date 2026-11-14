package com.drbenja.spd;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.config.effect.CleansedConfig;
import com.drbenja.spd.config.entity.EntityClassesConfig;
import com.drbenja.spd.config.goldenbee.GoldenBeeConfig;
import com.drbenja.spd.config.effect.ImbuedWithFrostConfig;
import com.drbenja.spd.config.effect.PurityConfig;
import com.drbenja.spd.entity.mob.GoldenBeeEntity;
import com.drbenja.spd.entity.mob.MirrorImageEntity;
import com.drbenja.spd.init.ModBlockEntities;
import com.drbenja.spd.init.ModBlocks;
import com.drbenja.spd.init.ModCreativeTabs;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.init.ModItems;
import com.drbenja.spd.init.ModPotions;

@Mod(SpdMod.MOD_ID)
public class SpdMod {
    public static final String MOD_ID = "spd";

    public SpdMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModPotions.POTIONS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                ImbuedWithFrostConfig.SPEC, "spd/imbued_with_frost.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                ChillImmuneEntitiesConfig.SPEC, "spd/chill_immune_entities.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                PurityConfig.SPEC, "spd/purity.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                CleansedConfig.SPEC, "spd/cleansed.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                EntityClassesConfig.SPEC, "spd/entity_classes.toml");
        modBus.addListener(ImbuedWithFrostConfig::onConfigEvent);
        modBus.addListener(ChillImmuneEntitiesConfig::onConfigEvent);
        modBus.addListener(PurityConfig::onConfigEvent);
        modBus.addListener(CleansedConfig::onConfigEvent);
        modBus.addListener(EntityClassesConfig::onConfigEvent);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onSpawnPlacements);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(GoldenBeeConfig::ensureLoaded);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GOLDEN_BEE.get(), GoldenBeeEntity.createAttributes().build());
        event.put(ModEntities.MIRROR_IMAGE.get(), MirrorImageEntity.createAttributes().build());
    }

    private void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        GoldenBeeEntity.init(event);
    }
}


