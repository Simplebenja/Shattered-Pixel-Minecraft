package com.drbenja.spd.client.renderer.mob;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;

import com.drbenja.spd.entity.mob.MirrorImageEntity;

/**
 * Humanoid body for mirror images.
 * Arm poses follow Drowned (trident) / player bow & crossbow conventions:
 * {@link HumanoidMobRenderer#getArmPose} only returns SPEAR/EMPTY by default.
 */
public class MirrorImageRenderer
        extends HumanoidMobRenderer<MirrorImageEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    public MirrorImageRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        try {
            this.addLayer(new HumanoidArmorLayer<>(
                    this,
                    ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                    context.getEquipmentRenderer()));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(MirrorImageEntity entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
    }

    /**
     * Critical for bow / crossbow / trident arm lift — base HumanoidMobRenderer does not set these.
     */
    @Override
    protected ArmPose getArmPose(MirrorImageEntity mob, HumanoidArm arm) {
        ItemStack held = mob.getItemHeldByArm(arm);
        boolean main = mob.getMainArm() == arm;

        // While using item (drawing bow / charging crossbow / winding trident)
        if (mob.isUsingItem() && main) {
            ItemStack use = mob.getUseItem();
            if (use.getItem() instanceof BowItem) {
                return ArmPose.BOW_AND_ARROW;
            }
            if (use.getItem() instanceof CrossbowItem) {
                return ArmPose.CROSSBOW_CHARGE;
            }
            if (use.getItem() instanceof TridentItem || use.is(Items.TRIDENT)) {
                return ArmPose.THROW_TRIDENT;
            }
        }

        // Crossbow charged & held (Pillager CROSSBOW_HOLD equivalent)
        if (main && held.getItem() instanceof CrossbowItem) {
            if (mob.isChargingCrossbow()) {
                return ArmPose.CROSSBOW_CHARGE;
            }
            try {
                if (CrossbowItem.isCharged(held)) {
                    return ArmPose.CROSSBOW_HOLD;
                }
            } catch (Throwable ignored) {
                // isCharged API may differ — still try hold pose when aggressive
                if (mob.isAggressive()) {
                    return ArmPose.CROSSBOW_HOLD;
                }
            }
        }

        // Trident ready / aggressive (DrownedRenderer)
        if (main && mob.isAggressive()
                && (held.getItem() instanceof TridentItem || held.is(Items.TRIDENT))) {
            return ArmPose.THROW_TRIDENT;
        }

        return super.getArmPose(mob, arm);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return DefaultPlayerSkin.getDefaultTexture();
    }
}

