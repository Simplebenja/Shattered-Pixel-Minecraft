package com.drbenja.spd.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleansed:
 * - Al aplicar: quita efectos negativos (HARMFUL) salvo {@code #spd:cleansing_keep},
 *   y también cualquier efecto en {@code #spd:cleansing_removable}.
 * - Mientras dura: inmunidad a esos mismos efectos.
 * - Llena hambre/saciedad.
 */
public class CleansedEffect extends MobEffect {

    public static final TagKey<MobEffect> REMOVABLE = TagKey.create(
            Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath("spd", "cleansing_removable"));
    public static final TagKey<MobEffect> KEEP = TagKey.create(
            Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath("spd", "cleansing_keep"));

    public CleansedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFF8DC);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        stripHarmful(entity);
        if (entity instanceof Player player) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
        }
    }

    public static void stripHarmful(LivingEntity entity) {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance inst : entity.getActiveEffects()) {
            Holder<MobEffect> holder = inst.getEffect();
            if (shouldAffect(holder)) {
                toRemove.add(holder);
            }
        }
        for (Holder<MobEffect> h : toRemove) {
            entity.removeEffect(h);
        }
    }

    /**
     * true = se quita al limpiar / se bloquea al aplicar.
     * Orden: tag keep / config neverRemove → no; tag removable / config alwaysRemove → sí;
     * si no, categoría HARMFUL. Corrupted y Doomed nunca se limpian.
     */
    public static boolean shouldAffect(Holder<MobEffect> holder) {
        if (holder.is(KEEP) || com.drbenja.spd.config.effect.CleansedConfig.isNeverRemove(holder)) {
            return false;
        }
        if (holder.is(REMOVABLE) || com.drbenja.spd.config.effect.CleansedConfig.isAlwaysRemove(holder)) {
            return true;
        }
        return holder.value().getCategory() == MobEffectCategory.HARMFUL;
    }
}
