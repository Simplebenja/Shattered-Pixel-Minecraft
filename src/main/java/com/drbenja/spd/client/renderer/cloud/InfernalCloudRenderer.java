package com.drbenja.spd.client.renderer.cloud;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.drbenja.spd.entity.cloud.InfernalCloudEntity;

public class InfernalCloudRenderer extends EntityRenderer<InfernalCloudEntity, EntityRenderState> {
    public InfernalCloudRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
}
