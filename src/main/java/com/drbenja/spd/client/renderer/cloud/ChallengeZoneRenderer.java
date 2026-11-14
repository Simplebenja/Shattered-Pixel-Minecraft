package com.drbenja.spd.client.renderer.cloud;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import com.drbenja.spd.entity.cloud.ChallengeZoneEntity;

/** Invisible marker; particles handled server-side if needed. */
public class ChallengeZoneRenderer extends EntityRenderer<ChallengeZoneEntity, EntityRenderState> {
    public ChallengeZoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
