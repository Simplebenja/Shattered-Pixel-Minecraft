package com.drbenja.spd.client.renderer.mob;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.drbenja.spd.entity.mob.GoldenBeeEntity;
import com.drbenja.spd.client.model.Modelgoldenbee4;

import com.mojang.blaze3d.vertex.PoseStack;

public class GoldenBeeRenderer extends MobRenderer<GoldenBeeEntity, LivingEntityRenderState, Modelgoldenbee4> {
	private final Identifier entityTexture = Identifier.parse("spd:textures/entities/goldenbeechill.png");

	public GoldenBeeRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelgoldenbee4(context.bakeLayer(Modelgoldenbee4.LAYER_LOCATION)), 0.3f);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public void extractRenderState(GoldenBeeEntity entity, LivingEntityRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return entityTexture;
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
	}
}