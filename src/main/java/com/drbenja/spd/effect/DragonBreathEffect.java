package com.drbenja.spd.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.init.ModEffects;

/**
 * Instantáneo al beber: cono 60° / 6 bloques.
 * Excluye solo al bebedor; enemigos pegados sí se ven afectados.
 * Partículas rellenan el volumen del cono.
 */
public class DragonBreathEffect extends MobEffect {

    public static final double RANGE = 6.0;
    public static final double HALF_ANGLE_RAD = Math.toRadians(30.0);
    /** Solo evita auto-golpe; enemigos a 0.3+ bloques sí cuentan. */
    public static final double MIN_DIST = 0.35;

    public DragonBreathEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6600);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        fireCone(entity);
        entity.removeEffect(ModEffects.DRAGON_BREATH);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return false;
    }

    public static void fireCone(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 origin = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        RandomSource random = level.getRandom();

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.9F);

        // Partículas en todo el volumen del cono (no solo el eje)
        for (int i = 0; i < 80; i++) {
            double dist = MIN_DIST + random.nextDouble() * (RANGE - MIN_DIST);
            // radio del cono a esa distancia: dist * tan(30°)
            double radius = dist * Math.tan(HALF_ANGLE_RAD);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = radius * Math.sqrt(random.nextDouble()); // disco uniforme
            // base ortonormal a look
            Vec3 up = Math.abs(look.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 right = look.cross(up).normalize();
            Vec3 actualUp = right.cross(look).normalize();
            Vec3 offset = right.scale(Math.cos(angle) * r).add(actualUp.scale(Math.sin(angle) * r));
            Vec3 p = origin.add(look.scale(dist)).add(offset);
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        AABB search = new AABB(origin, origin).inflate(RANGE + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, Entity::isAlive)) {
            if (target == caster) {
                continue;
            }
            // Punto más cercano del hitbox al origen del cono (mejor para enemigos muy cerca)
            Vec3 body = closestPointOnAABB(target.getBoundingBox(), origin);
            Vec3 to = body.subtract(origin);
            double dist = to.length();
            if (dist < MIN_DIST || dist > RANGE) {
                continue;
            }
            if (look.dot(to.normalize()) < Math.cos(HALF_ANGLE_RAD)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(
                    ModEffects.BURNING, BurningEffect.DEFAULT_DURATION_TICKS, 0, false, true, true));
            target.addEffect(new MobEffectInstance(
                    ModEffects.CRIPPLED, CrippledEffect.DRAGON_BREATH_DURATION, 0, false, true, true));
        }

        BlockPos min = BlockPos.containing(origin.x - RANGE, origin.y - RANGE, origin.z - RANGE);
        BlockPos max = BlockPos.containing(origin.x + RANGE, origin.y + RANGE, origin.z + RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            Vec3 cell = Vec3.atCenterOf(pos);
            Vec3 to = cell.subtract(origin);
            double dist = to.length();
            if (dist < MIN_DIST || dist > RANGE) {
                continue;
            }
            if (look.dot(to.normalize()) < Math.cos(HALF_ANGLE_RAD)) {
                continue;
            }
            igniteBlock(level, pos);
        }
    }

    private static Vec3 closestPointOnAABB(AABB box, Vec3 point) {
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return new Vec3(x, y, z);
    }

    private static void igniteBlock(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof TntBlock) {
            TntBlock.prime(level, pos);
            return;
        }
        if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT)
                && !Boolean.TRUE.equals(st.getValue(CampfireBlock.LIT))) {
            level.setBlock(pos, st.setValue(CampfireBlock.LIT, true), 3);
            return;
        }
        if (st.hasProperty(BlockStateProperties.LIT) && (st.is(BlockTags.CANDLES) || st.is(BlockTags.CANDLE_CAKES))
                && !Boolean.TRUE.equals(st.getValue(BlockStateProperties.LIT))) {
            level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), 3);
            return;
        }
        if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW)) {
            level.removeBlock(pos, false);
            return;
        }
        if (st.isAir()) {
            if (BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
                level.setBlock(pos, BaseFireBlock.getState(level, pos), 3);
            }
        } else if (st.isFlammable(level, pos, Direction.UP)) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()
                    && BaseFireBlock.canBePlacedAt(level, above, Direction.UP)) {
                level.setBlock(above, BaseFireBlock.getState(level, above), 3);
            }
        }
    }
}
