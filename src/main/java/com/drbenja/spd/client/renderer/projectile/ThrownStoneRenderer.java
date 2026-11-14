package com.drbenja.spd.client.renderer.projectile;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;

/** Thrown runestones reuse vanilla ItemEntity rendering. */
public class ThrownStoneRenderer extends ItemEntityRenderer {
    public ThrownStoneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
