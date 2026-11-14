package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:flow — SPD speedBoost = (2 + 0.5 * level) * multiplier while in water.
 * <p>
 * MC implementation (Depth Strider style, but stronger):
 * <ul>
 *   <li>{@link Attributes#WATER_MOVEMENT_EFFICIENCY} — same attribute Depth Strider uses
 *       (DS is +0.333/level, max 1.0). Flow maps SPD boost so base L1 already feels like
 *       strong Depth Strider and higher levels / empowerment stay at full efficiency.</li>
 *   <li>{@link NeoForgeMod#SWIM_SPEED} — extra swim speed so open-water movement is obvious.</li>
 * </ul>
 * Mild share while only in rain (not submerged).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class FlowEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "flow"));

    private static final Identifier FLOW_WATER_EFF =
            Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "flow_water_movement_efficiency");
    private static final Identifier FLOW_SWIM =
            Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "flow_swim_speed");

    private FlowEnchantmentHandler() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living.level() instanceof ServerLevel server)) return;
        if ((living.tickCount & 1) != 0) return; // every other tick is enough

        AttributeInstance waterEff = living.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        AttributeInstance swim = living.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (waterEff != null) {
            waterEff.removeModifier(FLOW_WATER_EFF);
        }
        if (swim != null) {
            swim.removeModifier(FLOW_SWIM);
        }

        int level = ArmorEnchantHelper.getAnyArmorLevel(server, living, KEY);
        if (level <= 0) return;

        boolean inWater = living.isInWater();
        boolean wet = inWater || living.isInWaterOrRain();
        if (!wet) return;

        int s = EnchantLevels.spd(level);
        // SPD: (2 + 0.5 * level) * multiplier
        double speedBoost = (2.0 + 0.5 * s) * ProcPower.multiplier(living);
        if (!inWater) {
            // Rain only: mild share
            speedBoost = 1.0 + (speedBoost - 1.0) * 0.35;
        }

        // Depth Strider III ≈ 1.0 efficiency. Map SPD boost so L1 already nears full:
        // speedBoost 2.0 → efficiency 1.0; higher boost still clamps at 1.0 for this attribute.
        double efficiency = Math.min(1.0, speedBoost / 2.0);
        if (waterEff != null && efficiency > 0.01) {
            waterEff.addTransientModifier(new AttributeModifier(
                    FLOW_WATER_EFF,
                    efficiency,
                    AttributeModifier.Operation.ADD_VALUE));
        }

        // Swim speed: additive multiplier on NeoForge swim (1.0 base).
        // speedBoost 2.0 → +1.0 (double swim); 3.0 → +2.0, very noticeable in open water.
        double swimAdd = Math.max(0.0, speedBoost - 1.0);
        if (swim != null && swimAdd > 0.01 && inWater) {
            swim.addTransientModifier(new AttributeModifier(
                    FLOW_SWIM,
                    swimAdd,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }
}

