package com.drbenja.spd.client.model;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

// Made with Blockbench 5.2.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelgoldenbee4 extends EntityModel<LivingEntityRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath("spd", "modelgoldenbee_4"), "main");
	public final ModelPart body;
	public final ModelPart stinger;
	public final ModelPart rightwing_bone;
	public final ModelPart leftwing_bone;
	public final ModelPart leg_front;
	public final ModelPart leg_mid;
	public final ModelPart leg_back;

	public Modelgoldenbee4(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.stinger = this.body.getChild("stinger");
		this.rightwing_bone = this.body.getChild("rightwing_bone");
		this.leftwing_bone = this.body.getChild("leftwing_bone");
		this.leg_front = this.body.getChild("leg_front");
		this.leg_mid = this.body.getChild("leg_mid");
		this.leg_back = this.body.getChild("leg_back");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition body = partdefinition.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -1.0F, -6.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 24).addBox(1.0F, -1.0F, -6.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(2, 0).addBox(1.5F, -4.0F, -8.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(2, 3)
						.addBox(-2.5F, -4.0F, -8.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.5F, 19.0F, 0.0F));
		PartDefinition stinger = body.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(25, 7).addBox(-0.5F, 0.0F, 4.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 1.0F));
		PartDefinition rightwing_bone = body.addOrReplaceChild("rightwing_bone", CubeListBuilder.create().texOffs(0, 18).addBox(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.2618F, -0.2618F, 0.0F));
		PartDefinition leftwing_bone = body.addOrReplaceChild("leftwing_bone", CubeListBuilder.create().texOffs(9, 24).addBox(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.2618F, 0.2618F, 0.0F));
		PartDefinition leg_front = body.addOrReplaceChild("leg_front", CubeListBuilder.create().texOffs(26, 1).addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 3.0F, -2.0F));
		PartDefinition leg_mid = body.addOrReplaceChild("leg_mid", CubeListBuilder.create().texOffs(26, 3).addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 3.0F, 0.0F));
		PartDefinition leg_back = body.addOrReplaceChild("leg_back", CubeListBuilder.create().texOffs(26, 5).addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 3.0F, 2.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	public void setupAnim(LivingEntityRenderState state) {
		float limbSwing = state.walkAnimationPos;
		float limbSwingAmount = state.walkAnimationSpeed;
		float ageInTicks = state.ageInTicks;
		float netHeadYaw = state.yRot;
		float headPitch = state.xRot;

	}

}