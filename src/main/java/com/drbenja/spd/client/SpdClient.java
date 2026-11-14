package com.drbenja.spd.client;

import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.client.model.Modelgoldenbee4;
import com.drbenja.spd.client.renderer.cloud.FrostCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.ParalyticGasCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.ToxicGasCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.CorrosiveGasCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.ShroudingFogCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.StormCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.InfernalCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.BlizzardCloudRenderer;
import com.drbenja.spd.client.renderer.cloud.ShockingFieldRenderer;
import com.drbenja.spd.client.renderer.projectile.ThrownAquaBrewRenderer;
import com.drbenja.spd.client.renderer.projectile.ThrownStoneRenderer;
import com.drbenja.spd.client.renderer.cloud.ChallengeZoneRenderer;
import com.drbenja.spd.client.renderer.mob.GoldenBeeRenderer;
import com.drbenja.spd.client.renderer.mob.MirrorImageRenderer;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModEntities;

@EventBusSubscriber(modid = SpdMod.MOD_ID, value = Dist.CLIENT)
public final class SpdClient {
    private SpdClient() {}

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(Modelgoldenbee4.LAYER_LOCATION, Modelgoldenbee4::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLDEN_BEE.get(), GoldenBeeRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_IMAGE.get(), MirrorImageRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_CLOUD.get(), FrostCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.PARALYTIC_GAS.get(), ParalyticGasCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.TOXIC_GAS.get(), ToxicGasCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.CORROSIVE_GAS.get(), CorrosiveGasCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.SHROUDING_FOG.get(), ShroudingFogCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM_CLOUD.get(), StormCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.INFERNAL_CLOUD.get(), InfernalCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.BLIZZARD_CLOUD.get(), BlizzardCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOCKING_FIELD.get(), ShockingFieldRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_AQUA_BREW.get(), ThrownAquaBrewRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_STONE.get(), ThrownStoneRenderer::new);
        event.registerEntityRenderer(ModEntities.CHALLENGE_ZONE.get(), ChallengeZoneRenderer::new);
    }

    /**
     * Efectos "madre" (lógica/pool): ocultos en HUD e inventario.
     * Solo los *_GUI deben verse (y emitir partículas).
     */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientMobEffectExtensions hidden = new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                return false;
            }

            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) {
                return false;
            }
        };
        event.registerMobEffect(hidden, ModEffects.REGENERATIVE_HEALING.get());
        event.registerMobEffect(hidden, ModEffects.AQUATIC_HEALING.get());
        event.registerMobEffect(hidden, ModEffects.HONEYED_HEALING.get());
        event.registerMobEffect(hidden, ModEffects.HERBAL_ARMOR.get());
    }
}

