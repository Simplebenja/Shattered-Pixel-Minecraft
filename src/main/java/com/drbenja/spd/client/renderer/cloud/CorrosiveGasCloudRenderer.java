package com.drbenja.spd.client.renderer.cloud;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.drbenja.spd.entity.cloud.CorrosiveGasCloudEntity;

public class CorrosiveGasCloudRenderer extends EntityRenderer<CorrosiveGasCloudEntity, EntityRenderState> {
    public CorrosiveGasCloudRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
}
