package com.drbenja.spd.event.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.HasteEffect;
import com.drbenja.spd.effect.StaminaEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * SPD-style hunger: moving faster from <b>our</b> sources should cover more ground
 * per unit of food. Only counts:
 * <ul>
 *   <li>{@link ModEffects#HASTE}</li>
 *   <li>{@link ModEffects#STAMINA}</li>
 *   <li>Swiftness / Flow armor enchant modifiers ({@code spd:swiftness_move}, {@code spd:flow_move})</li>
 * </ul>
 * Vanilla Speed (and other mods) are ignored — no refund when only those are active.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class SpeedHungerHandlers {

    private static final String TAG_LAST = "SpdHungerLastPos";

    /** Same IDs used in ArmorEnchantmentHandlers for leg-slot speed enchants. */
    private static final Identifier SWIFT_MOD = Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "swiftness_move");
    private static final Identifier FLOW_MOD = Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "flow_move");

    private SpeedHungerHandlers() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        // Early out: no SPD speed sources → never touch hunger
        if (!hasSpdSpeedSource(player)) {
            storePos(player);
            return;
        }

        float mult = spdOnlyMultiplier(player);
        Vec3 pos = player.position();
        var data = player.getPersistentData();

        double lastX = data.getDoubleOr(TAG_LAST + "X", pos.x);
        double lastY = data.getDoubleOr(TAG_LAST + "Y", pos.y);
        double lastZ = data.getDoubleOr(TAG_LAST + "Z", pos.z);
        data.putDouble(TAG_LAST + "X", pos.x);
        data.putDouble(TAG_LAST + "Y", pos.y);
        data.putDouble(TAG_LAST + "Z", pos.z);

        if (mult <= 1.02f) return;

        double dist = pos.distanceTo(new Vec3(lastX, lastY, lastZ));
        if (dist < 1.0E-4 || dist > 8.0) return; // ignore teleports / loading

        // Approximate vanilla movement exhaustion charged this tick by distance
        float charged = 0f;
        if (player.isSprinting()) {
            charged += (float) (dist * 0.1f);
        } else if (player.isSwimming() || player.isInWater()) {
            charged += (float) (dist * 0.01f);
        } else if (player.onGround() && dist > 0) {
            charged += (float) (dist * 0.01f);
        }
        if (charged <= 0f) return;

        // Keep only 1/mult of the cost attributable to SPD speed (extra distance is "free")
        float refund = charged * (1.0f - (1.0f / mult));
        if (refund > 0f) {
            player.causeFoodExhaustion(-refund);
        }
    }

    private static void storePos(ServerPlayer player) {
        Vec3 pos = player.position();
        var data = player.getPersistentData();
        data.putDouble(TAG_LAST + "X", pos.x);
        data.putDouble(TAG_LAST + "Y", pos.y);
        data.putDouble(TAG_LAST + "Z", pos.z);
    }

    /** True only if Haste, Stamina, Swiftness or Flow is actively contributing. */
    private static boolean hasSpdSpeedSource(Player player) {
        if (player.hasEffect(ModEffects.HASTE) || player.hasEffect(ModEffects.STAMINA)) {
            return true;
        }
        AttributeInstance inst = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return false;
        return inst.getModifier(SWIFT_MOD) != null || inst.getModifier(FLOW_MOD) != null;
    }

    /**
     * Product of SPD-only speed multipliers. Does <b>not</b> read total attribute
     * value (that would include vanilla Speed / other mods).
     * <p>
     * ADD_MULTIPLIED_TOTAL modifiers stack multiplicatively: (1+a)*(1+b)*…
     */
    private static float spdOnlyMultiplier(Player player) {
        float mult = 1.0f;

        // Prefer live modifier amounts (always match what is actually applied)
        AttributeInstance inst = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) {
            mult *= factorFrom(inst.getModifier(HasteEffect.MODIFIER_ID));
            mult *= factorFrom(inst.getModifier(StaminaEffect.MODIFIER_ID));
            mult *= factorFrom(inst.getModifier(SWIFT_MOD));
            mult *= factorFrom(inst.getModifier(FLOW_MOD));
        } else {
            // Fallback from effect amplifiers if attribute instance missing
            var haste = player.getEffect(ModEffects.HASTE);
            if (haste != null) {
                mult *= haste.getAmplifier() >= 1 ? 6.0f : 3.0f;
            }
            var stamina = player.getEffect(ModEffects.STAMINA);
            if (stamina != null) {
                mult *= stamina.getAmplifier() >= 1 ? 2.0f : 1.5f;
            }
        }
        return Math.max(1.0f, mult);
    }

    private static float factorFrom(AttributeModifier mod) {
        if (mod == null) return 1.0f;
        // ADD_MULTIPLIED_TOTAL amount a means ×(1+a)
        return (float) (1.0 + mod.amount());
    }
}
