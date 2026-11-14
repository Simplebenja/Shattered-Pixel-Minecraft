package com.drbenja.spd.entity.classy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import com.drbenja.spd.SpdMod;

/** Datapack tags: {@code data/spd/tags/entity_type/<name>.json}. */
public final class SpdEntityTypeTags {
    public static final TagKey<EntityType<?>> FIERY = tag("fiery");
    public static final TagKey<EntityType<?>> ICY = tag("icy");
    public static final TagKey<EntityType<?>> ACIDIC = tag("acidic");
    public static final TagKey<EntityType<?>> ELECTRIC = tag("electric");
    public static final TagKey<EntityType<?>> INORGANIC = tag("inorganic");
    public static final TagKey<EntityType<?>> DEMONIC = tag("demonic");
    public static final TagKey<EntityType<?>> IMMOVABLE = tag("immovable");
    public static final TagKey<EntityType<?>> STATIC = tag("static");
    public static final TagKey<EntityType<?>> MINIBOSS = tag("miniboss");
    public static final TagKey<EntityType<?>> BOSS = tag("boss");

    private SpdEntityTypeTags() {}

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, path));
    }
}
