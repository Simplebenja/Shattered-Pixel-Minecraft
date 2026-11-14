package com.drbenja.spd.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.cloud.FrostCloudEntity;
import com.drbenja.spd.entity.cloud.ParalyticGasCloudEntity;
import com.drbenja.spd.entity.cloud.ToxicGasCloudEntity;
import com.drbenja.spd.entity.cloud.CorrosiveGasCloudEntity;
import com.drbenja.spd.entity.cloud.ShroudingFogCloudEntity;
import com.drbenja.spd.entity.cloud.StormCloudEntity;
import com.drbenja.spd.entity.cloud.InfernalCloudEntity;
import com.drbenja.spd.entity.cloud.BlizzardCloudEntity;
import com.drbenja.spd.entity.cloud.ShockingFieldEntity;
import com.drbenja.spd.entity.projectile.ThrownAquaBrewEntity;
import com.drbenja.spd.entity.projectile.ThrownStoneEntity;
import com.drbenja.spd.entity.cloud.ChallengeZoneEntity;

import com.drbenja.spd.entity.mob.GoldenBeeEntity;
import com.drbenja.spd.entity.mob.MirrorImageEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SpdMod.MOD_ID);

    public static final ResourceKey<EntityType<?>> GOLDEN_BEE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "golden_bee"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoldenBeeEntity>> GOLDEN_BEE =
            ENTITIES.register("golden_bee",
                    () -> EntityType.Builder.of(GoldenBeeEntity::new, MobCategory.CREATURE)
                            .sized(0.7F, 0.6F)
                            .clientTrackingRange(8)
                            .build(GOLDEN_BEE_KEY));

    public static final ResourceKey<EntityType<?>> MIRROR_IMAGE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "mirror_image"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorImageEntity>> MIRROR_IMAGE =
            ENTITIES.register("mirror_image",
                    () -> EntityType.Builder.of(MirrorImageEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(MIRROR_IMAGE_KEY));


    public static final ResourceKey<EntityType<?>> FROST_CLOUD_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "frost_cloud"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostCloudEntity>> FROST_CLOUD =
            ENTITIES.register("frost_cloud",
                    () -> EntityType.Builder.<FrostCloudEntity>of(FrostCloudEntity::new, MobCategory.MISC)
                            .sized(3.0F, 3.0F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(FROST_CLOUD_KEY));

    public static final ResourceKey<EntityType<?>> PARALYTIC_GAS_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "paralytic_gas"));

    public static final DeferredHolder<EntityType<?>, EntityType<ParalyticGasCloudEntity>> PARALYTIC_GAS =
            ENTITIES.register("paralytic_gas",
                    () -> EntityType.Builder.<ParalyticGasCloudEntity>of(ParalyticGasCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build(PARALYTIC_GAS_KEY));

    public static final ResourceKey<EntityType<?>> TOXIC_GAS_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "toxic_gas"));

    public static final DeferredHolder<EntityType<?>, EntityType<ToxicGasCloudEntity>> TOXIC_GAS =
            ENTITIES.register("toxic_gas",
                    () -> EntityType.Builder.<ToxicGasCloudEntity>of(ToxicGasCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build(TOXIC_GAS_KEY));

    public static final ResourceKey<EntityType<?>> CORROSIVE_GAS_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "corrosive_gas"));

    public static final DeferredHolder<EntityType<?>, EntityType<CorrosiveGasCloudEntity>> CORROSIVE_GAS =
            ENTITIES.register("corrosive_gas",
                    () -> EntityType.Builder.<CorrosiveGasCloudEntity>of(CorrosiveGasCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build(CORROSIVE_GAS_KEY));

    public static final ResourceKey<EntityType<?>> SHROUDING_FOG_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "shrouding_fog"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShroudingFogCloudEntity>> SHROUDING_FOG =
            ENTITIES.register("shrouding_fog",
                    () -> EntityType.Builder.<ShroudingFogCloudEntity>of(ShroudingFogCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build(SHROUDING_FOG_KEY));

    public static final ResourceKey<EntityType<?>> STORM_CLOUD_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "storm_cloud"));

    public static final DeferredHolder<EntityType<?>, EntityType<StormCloudEntity>> STORM_CLOUD =
            ENTITIES.register("storm_cloud",
                    () -> EntityType.Builder.<StormCloudEntity>of(StormCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build(STORM_CLOUD_KEY));


    public static final ResourceKey<EntityType<?>> INFERNAL_CLOUD_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "infernal_cloud"));
    public static final DeferredHolder<EntityType<?>, EntityType<InfernalCloudEntity>> INFERNAL_CLOUD =
            ENTITIES.register("infernal_cloud",
                    () -> EntityType.Builder.<InfernalCloudEntity>of(InfernalCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).build(INFERNAL_CLOUD_KEY));

    public static final ResourceKey<EntityType<?>> BLIZZARD_CLOUD_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "blizzard_cloud"));
    public static final DeferredHolder<EntityType<?>, EntityType<BlizzardCloudEntity>> BLIZZARD_CLOUD =
            ENTITIES.register("blizzard_cloud",
                    () -> EntityType.Builder.<BlizzardCloudEntity>of(BlizzardCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).build(BLIZZARD_CLOUD_KEY));

    public static final ResourceKey<EntityType<?>> SHOCKING_FIELD_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "shocking_field"));
    public static final DeferredHolder<EntityType<?>, EntityType<ShockingFieldEntity>> SHOCKING_FIELD =
            ENTITIES.register("shocking_field",
                    () -> EntityType.Builder.<ShockingFieldEntity>of(ShockingFieldEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).build(SHOCKING_FIELD_KEY));

    public static final ResourceKey<EntityType<?>> THROWN_AQUA_BREW_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "thrown_aqua_brew"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownAquaBrewEntity>> THROWN_AQUA_BREW =
            ENTITIES.register("thrown_aqua_brew",
                    () -> EntityType.Builder.<ThrownAquaBrewEntity>of(ThrownAquaBrewEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build(THROWN_AQUA_BREW_KEY));


    public static final ResourceKey<EntityType<?>> THROWN_STONE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "thrown_stone"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownStoneEntity>> THROWN_STONE =
            ENTITIES.register("thrown_stone",
                    () -> EntityType.Builder.<ThrownStoneEntity>of(ThrownStoneEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build(THROWN_STONE_KEY));


    public static final ResourceKey<EntityType<?>> CHALLENGE_ZONE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "challenge_zone"));
    public static final DeferredHolder<EntityType<?>, EntityType<ChallengeZoneEntity>> CHALLENGE_ZONE =
            ENTITIES.register("challenge_zone",
                    () -> EntityType.Builder.<ChallengeZoneEntity>of(ChallengeZoneEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).build(CHALLENGE_ZONE_KEY));

    private ModEntities() {}
}

