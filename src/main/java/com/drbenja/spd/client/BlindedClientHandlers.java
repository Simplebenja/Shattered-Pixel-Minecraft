package com.drbenja.spd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/**
 * Visión ~1 bloque (esfera Ø3): niebla muy cercana. No bloquea sprint (a diferencia de Blindness).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID, value = Dist.CLIENT)
public final class BlindedClientHandlers {

    private BlindedClientHandlers() {}

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.BLINDED)) return;
        // near/far muy bajos ≈ solo el cubo inmediato
        event.setNearPlaneDistance(0.05F);
        event.setFarPlaneDistance(2.2F);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.BLINDED)) return;
        event.setRed(0.25F);
        event.setGreen(0.25F);
        event.setBlue(0.28F);
    }
}
