package com.drbenja.spd.item;

import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.util.SpdProgression;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Aqua Brew: throwable, stack 16. Geyser al impactar.
 */
public class AquaBrewItem extends Item {

    public AquaBrewItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            var thrown = com.drbenja.spd.entity.projectile.ThrownAquaBrewEntity.createThrown(level, player);
            level.addFreshEntity(thrown);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    public static void activate(ServerLevel level, BlockPos center, Vec3 knockFrom) {
        int r = 2;
        // Incluye 1 bloque por debajo del impacto para waterlog / agua sobre suelo bajo el epicentro
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) {
                        level.removeBlock(p, false);
                        st = level.getBlockState(p);
                    }
                    if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT) && st.getValue(CampfireBlock.LIT)) {
                        level.setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
                        st = level.getBlockState(p);
                    }
                    if (st.getBlock() instanceof CandleBlock && st.hasProperty(CandleBlock.LIT) && st.getValue(CandleBlock.LIT)) {
                        level.setBlock(p, st.setValue(CandleBlock.LIT, false), 3);
                        st = level.getBlockState(p);
                    }
                    // Waterlog cualquier bloque waterlogeable en el volumen
                    if (st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                            && !Boolean.TRUE.equals(st.getValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))) {
                        level.setBlock(p, st.setValue(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 3);
                        st = level.getBlockState(p);
                    }
                    // Agua corriente encima de sólidos (y del suelo bajo el área)
                    boolean solidHere = !st.getCollisionShape(level, p).isEmpty();
                    if (solidHere) {
                        BlockPos above = p.above();
                        BlockState aboveSt = level.getBlockState(above);
                        if (aboveSt.isAir() || aboveSt.canBeReplaced() || aboveSt.is(Blocks.WATER)) {
                            level.setBlock(above, Blocks.WATER.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1), 3);
                        }
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.SPLASH, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                50, 1.8, 1.2, 1.8, 0.25);
        level.sendParticles(ParticleTypes.CLOUD, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5,
                20, 1.5, 0.8, 1.5, 0.05);
        level.playSound(null, center, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 0.9F);

        AABB box = new AABB(center).inflate(2.5, 2.0, 2.5);
        Vec3 centerVec = Vec3.atCenterOf(center);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (EntityClassHelper.isFiery(e)) {
                float dmg = SpdProgression.aquaBrewFieryDamage(e, level.getRandom());
                e.hurtServer(level, e.damageSources().magic(), dmg);
            }
            e.clearFire();
            Vec3 dir = e.position().subtract(centerVec);
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(level.getRandom().nextDouble() - 0.5, 0.0, level.getRandom().nextDouble() - 0.5);
            }
            dir = new Vec3(dir.x, 0.0, dir.z).normalize();
            double hx = dir.x * 1.75;
            double hy = 0.55;
            double hz = dir.z * 1.75;
            e.push(hx, hy, hz);
            // Jugadores: forzar sync de velocidad al cliente (mobs ya aplican push en servidor)
            if (e instanceof net.minecraft.server.level.ServerPlayer sp) {
                try {
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                } catch (Throwable ignored) {
                }
            }
        }
    }

}

