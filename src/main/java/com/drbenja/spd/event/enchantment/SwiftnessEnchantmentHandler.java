package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.util.ArmorEnchantHelper;
import com.drbenja.spd.util.EnchantLevels;
import com.drbenja.spd.util.ProcPower;

/**
 * spd:swiftness — SPD speedBoost = (1.2 + 0.04 * level) * multiplier when no enemy within ~3 tiles.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class SwiftnessEnchantmentHandler {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "swiftness"));

    private static final Identifier SWIFT_MOD =
            Identifier.fromNamespaceAndPath(SpdMod.MOD_ID, "swiftness_move");

    private SwiftnessEnchantmentHandler() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living.level() instanceof ServerLevel server)) return;
        if ((living.tickCount & 3) != 0) return;

        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(SWIFT_MOD);

        int level = ArmorEnchantHelper.getAnyArmorLevel(server, living, KEY);
        if (level <= 0) return;
        if (enemyNear(living, 3.5)) return;

        int s = EnchantLevels.spd(level);
        double mult = (1.2 + 0.04 * s) * ProcPower.multiplier(living);
        speed.addTransientModifier(new AttributeModifier(
                SWIFT_MOD, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static boolean enemyNear(LivingEntity self, double range) {
        return !self.level().getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(range),
                m -> m.isAlive() && m instanceof Enemy).isEmpty();
    }
}
