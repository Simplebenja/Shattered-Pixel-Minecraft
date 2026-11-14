package com.drbenja.spd.client.renderer.projectile;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;

/**
 * Reuses vanilla item-entity rendering so the thrown Aqua Brew shows its item model.
 */
public class ThrownAquaBrewRenderer extends ItemEntityRenderer {

    public ThrownAquaBrewRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
