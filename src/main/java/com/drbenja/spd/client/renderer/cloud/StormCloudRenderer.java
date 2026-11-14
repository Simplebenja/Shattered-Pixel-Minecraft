package com.drbenja.spd.client.renderer.cloud;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.drbenja.spd.entity.cloud.StormCloudEntity;

public class StormCloudRenderer extends EntityRenderer<StormCloudEntity, EntityRenderState> {
    public StormCloudRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
}
