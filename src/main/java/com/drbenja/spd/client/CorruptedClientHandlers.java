package com.drbenja.spd.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import com.drbenja.spd.SpdMod;

/**
 * Client-side hook point for Corrupted visuals.
 * <p>
 * MC 26.3 removed/changed the APIs needed for model tint and billboard quads
 * ({@code setShaderColor}, stage camera, MultiBufferSource packages).
 * Particle bursts and ambient souls are spawned <b>server-side</b> from
 * {@link com.drbenja.spd.effect.CorruptedEffect} via {@code ServerLevel#sendParticles}
 * so they sync reliably to all clients.
 * <p>
 * This class is kept as a client entry point for future render-pipeline work
 * (e.g. ContextKey tint when the 26.3 NeoForge API is confirmed).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID, value = Dist.CLIENT)
public final class CorruptedClientHandlers {
    private CorruptedClientHandlers() {}
}
