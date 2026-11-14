package com.drbenja.spd.event.entity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.mob.GoldenBeeEntity;
import com.drbenja.spd.init.ModEntities;

/**
 * ~2% de spawns naturales de abeja vanilla se convierten en Golden Bee.
 * No afecta salidas de panal ni entidades ya Golden.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class GoldenBeeSpawnHandler {

    private static final double CHANCE = 0.02D;

    private GoldenBeeSpawnHandler() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Bee bee) || entity instanceof GoldenBeeEntity) {
            return;
        }
        if (bee.getHivePos() != null) {
            return;
        }
        if (bee.isNoAi() || !bee.isAlive()) {
            return;
        }
        if (bee.getRandom().nextDouble() >= CHANCE) {
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        GoldenBeeEntity golden = ModEntities.GOLDEN_BEE.get().create(server, EntitySpawnReason.NATURAL);
        if (golden == null) {
            return;
        }

        golden.snapTo(bee.getX(), bee.getY(), bee.getZ(), bee.getYRot(), bee.getXRot());
        golden.setBaby(bee.isBaby());
        event.setCanceled(true);
        server.addFreshEntity(golden);
    }
}
