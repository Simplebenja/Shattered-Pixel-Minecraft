package com.drbenja.spd.init;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.block.GoldenBeehiveBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SpdMod.MOD_ID);

    /**
     * NeoForge 26.3: registerBlock(name, factory, Supplier&lt;Properties&gt;)
     * para que el id se inyecte en Properties (evita "Block id not set").
     */
    public static final DeferredBlock<GoldenBeehiveBlock> GOLDEN_BEEHIVE = BLOCKS.registerBlock(
            "golden_beehive",
            GoldenBeehiveBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(0.3F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    private ModBlocks() {}
}
