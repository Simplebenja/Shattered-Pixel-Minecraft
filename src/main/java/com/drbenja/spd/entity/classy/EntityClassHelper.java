package com.drbenja.spd.entity.classy;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import com.drbenja.spd.config.entity.EntityClassesConfig;

/**
 * Runtime checks: datapack tag {@code #spd:<class>} OR config list OR hardcoded defaults.
 */
public final class EntityClassHelper {

    private EntityClassHelper() {}

    public static boolean isFiery(Entity e) { return inClass(e, SpdEntityTypeTags.FIERY, "fiery"); }
    public static boolean isIcy(Entity e) { return inClass(e, SpdEntityTypeTags.ICY, "icy"); }
    public static boolean isAcidic(Entity e) { return inClass(e, SpdEntityTypeTags.ACIDIC, "acidic"); }
    public static boolean isElectric(Entity e) { return inClass(e, SpdEntityTypeTags.ELECTRIC, "electric"); }
    public static boolean isInorganic(Entity e) { return inClass(e, SpdEntityTypeTags.INORGANIC, "inorganic"); }
    public static boolean isDemonic(Entity e) { return inClass(e, SpdEntityTypeTags.DEMONIC, "demonic"); }
    public static boolean isImmovable(Entity e) { return inClass(e, SpdEntityTypeTags.IMMOVABLE, "immovable"); }
    public static boolean isStatic(Entity e) { return inClass(e, SpdEntityTypeTags.STATIC, "static"); }
    public static boolean isMiniboss(Entity e) { return inClass(e, SpdEntityTypeTags.MINIBOSS, "miniboss"); }
    public static boolean isBoss(Entity e) { return inClass(e, SpdEntityTypeTags.BOSS, "boss"); }

    public static boolean isBossOrMini(Entity e) {
        return isBoss(e) || isMiniboss(e);
    }

    private static boolean inClass(Entity e, TagKey<EntityType<?>> tag, String configKey) {
        if (e == null) {
            return false;
        }
        EntityType<?> type = e.getType();
        try {
            Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
            if (holder.is(tag)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }
        String s = id.toString().toLowerCase();
        if (EntityClassesConfig.configIds(configKey).contains(s)) {
            return true;
        }
        return hardcoded(configKey, s);
    }

    private static boolean hardcoded(String key, String id) {
        return switch (key) {
            case "fiery" -> id.equals("minecraft:blaze") || id.equals("minecraft:magma_cube");
            case "icy" -> id.equals("minecraft:snow_golem") || id.equals("minecraft:stray");
            case "boss" -> id.equals("minecraft:warden") || id.equals("minecraft:wither")
                    || id.equals("minecraft:ender_dragon");
            case "miniboss" -> id.equals("minecraft:elder_guardian") || id.equals("minecraft:end_crystal");
            case "static" -> id.equals("minecraft:end_crystal");
            case "immovable" -> id.equals("minecraft:shulker") || id.equals("minecraft:creaking");
            case "demonic" -> id.equals("minecraft:wither_skeleton") || id.equals("minecraft:wither")
                    || id.equals("minecraft:vex");
            case "inorganic" -> id.equals("minecraft:skeleton") || id.equals("minecraft:stray")
                    || id.equals("minecraft:wither_skeleton") || id.equals("minecraft:bogged")
                    || id.equals("minecraft:vex") || id.equals("minecraft:iron_golem")
                    || id.equals("minecraft:snow_golem") || id.equals("minecraft:end_crystal")
                    || id.equals("minecraft:phantom") || id.equals("minecraft:ghast")
                    || id.equals("minecraft:blaze") || id.equals("minecraft:creaking")
                    || id.equals("minecraft:breeze") || id.equals("minecraft:wither")
                    || id.equals("minecraft:armor_stand") || id.equals("minecraft:item_frame")
                    || id.equals("minecraft:glow_item_frame");
            default -> false;
        };
    }

    public static boolean immuneToBurning(LivingEntity e) { return isFiery(e); }
    public static boolean immuneToBlazingEnchant(LivingEntity e) { return isFiery(e); }
    public static boolean immuneToChillFrozen(LivingEntity e) { return isIcy(e) || isStatic(e); }
    public static boolean immuneToBleeding(LivingEntity e) { return isInorganic(e); }
    public static boolean immuneToToxicGas(LivingEntity e) { return isInorganic(e); }
    public static boolean immuneToCausticOoze(LivingEntity e) { return isAcidic(e); }
    /** Boss / miniboss / static: no corruption, dread, or enthrall. */
    public static boolean immuneToCorruption(LivingEntity e) { return isBossOrMini(e) || isStatic(e); }
    public static boolean immuneToDread(LivingEntity e) { return isBossOrMini(e) || isStatic(e); }
    public static boolean immuneToEnthralled(LivingEntity e) { return isBossOrMini(e) || isStatic(e); }
    public static boolean resistsRepulsionKnockback(LivingEntity e) { return isImmovable(e) || isBoss(e); }

    public static float grimDamageFactor(LivingEntity target) {
        return isBoss(target) ? 0.5f : 1.0f;
    }

    public static float electricDamageFactor(LivingEntity target) {
        return isElectric(target) ? 0.5f : 1.0f;
    }

    public static float corrosiveDamageFactor(LivingEntity target) {
        return isAcidic(target) ? 0.5f : 1.0f;
    }
}
