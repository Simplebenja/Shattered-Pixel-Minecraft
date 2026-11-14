package com.drbenja.spd.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import com.drbenja.spd.SpdMod;

/**
 * Mind Vision — client hooks.
 * <p>
 * Enemy Glowing is applied server-side in {@link com.drbenja.spd.effect.MindVisionEffect}.
 * <p>
 * <b>Block transparency through walls is deferred.</b> MC 26.3 / Renderpearl removed the
 * legacy path ({@code RenderType}, {@code Minecraft#renderBuffers},
 * {@code LevelRenderer#renderLineBox}, {@code GameRenderer#getMainCamera}). True
 * per-block see-through needs {@code SubmitNodeCollector} / {@code RenderPass} or a
 * block-render mixin; both are non-trivial on the new pipeline.
 * <p>
 * Revisit when NeoForge documents custom geometry submission for 26.3.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID, value = Dist.CLIENT)
public final class MindVisionClientHandlers {
    private MindVisionClientHandlers() {}
}
