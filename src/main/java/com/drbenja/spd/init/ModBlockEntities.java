package com.drbenja.spd.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.block.entity.GoldenBeehiveBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SpdMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GoldenBeehiveBlockEntity>> GOLDEN_BEEHIVE =
            BLOCK_ENTITIES.register("golden_beehive",
                    () -> new BlockEntityType<>(GoldenBeehiveBlockEntity::new, ModBlocks.GOLDEN_BEEHIVE.get()));

    private ModBlockEntities() {}
}
