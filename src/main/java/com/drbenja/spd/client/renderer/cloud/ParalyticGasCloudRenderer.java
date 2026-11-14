package com.drbenja.spd.client.renderer.cloud;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.drbenja.spd.entity.cloud.ParalyticGasCloudEntity;

/**
 * Sin modelo: la nube solo emite partículas desde la entidad.
 * Equivalente a EmptyEntityRenderer / AreaEffectCloud sin mesh.
 */
public class ParalyticGasCloudRenderer extends EntityRenderer<ParalyticGasCloudEntity, EntityRenderState> {

    public ParalyticGasCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
