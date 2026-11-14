package com.drbenja.spd.client.renderer.cloud;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.drbenja.spd.entity.cloud.ToxicGasCloudEntity;

public class ToxicGasCloudRenderer extends EntityRenderer<ToxicGasCloudEntity, EntityRenderState> {
    public ToxicGasCloudRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
}
