package com.drbenja.spd.entity.cloud;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.init.ModEffects;

/**
 * Nube de escarcha estilo blob SPD Freezing:
 * cubo ~3×3×3, dura 10 s, cada segundo acumula Chilled (no se reduce al afectar entidades).
 */
public class FrostCloudEntity extends Entity {

    public static final int LIFETIME_TICKS = 200;
    public static final int APPLY_INTERVAL = 20;
    /** +4 s tierra / +6 s agua (incluye 1 s extra para compensar el intervalo). */
    public static final int CHILL_ADD_DRY = 78;
    public static final int CHILL_ADD_WET = 118;
    public static final double HALF_EXTENT_XZ = 1.5D;
    /** ±2 vertical from impact (gas-style height). */
    public static final double HALF_EXTENT_Y = 2.0D;

    private int life = LIFETIME_TICKS;

    public FrostCloudEntity(EntityType<? extends FrostCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.life = input.getIntOr("Life", LIFETIME_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", this.life);
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();

        if (level.isClientSide()) {
            spawnClientParticles();
            return;
        }

        if (life-- <= 0) {
            discard();
            return;
        }

        // Entrada sin efecto: aplicar al instante; con efecto: sumar cada segundo
        applyChillToEntities((ServerLevel) level, life % APPLY_INTERVAL == 0);

        if (life % 5 == 0) {
            extinguishFireInVolume(level);
        }
    }

    /**
     * @param periodic true en el tick de intervalo (sumar a quien ya tiene efecto)
     */
    private void applyChillToEntities(ServerLevel level, boolean periodic) {
        AABB box = getCloudBox();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (!box.intersects(entity.getBoundingBox())) {
                continue;
            }
            if (entity.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
                continue; // Potion of Purity: immune to gases/clouds
            }
            if (entity.hasEffect(ModEffects.FROZEN)) {
                // Already frozen: leave duration alone. Re-applying every second re-fired SFX.
                // Only top up if about to expire (< 2s).
                if (periodic) {
                    MobEffectInstance fr = entity.getEffect(ModEffects.FROZEN);
                    int dur = fr != null ? fr.getDuration() : 0;
                    if (dur > 0 && dur < 40) {
                        com.drbenja.spd.effect.FrozenEffect.extendSilent(entity, 40);
                    }
                }
                continue;
            }

            if (ChillImmuneEntitiesConfig.isChillImmune(entity)) {
                continue;
            }
            boolean hasChill = entity.hasEffect(ModEffects.CHILLED);
            // Sin chilled: aplicar ya al entrar. Con chilled: solo en el intervalo.
            if (!hasChill || periodic) {
                int add = entity.isInWater() || entity.isUnderWater() ? CHILL_ADD_WET : CHILL_ADD_DRY;
                MobEffectInstance cur = entity.getEffect(ModEffects.CHILLED);
                int base = cur != null ? cur.getDuration() : 0;
                entity.addEffect(new MobEffectInstance(
                        ModEffects.CHILLED, base + add, 0, false, true, true));
            }
        }
    }

    private void extinguishFireInVolume(Level level) {
        BlockPos center = blockPosition();
        int rxz = 1;
        int ry = 2;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.getX() - rxz, center.getY() - ry, center.getZ() - rxz,
                center.getX() + rxz, center.getY() + ry, center.getZ() + rxz)) {
            BlockState st = level.getBlockState(pos);
            if (st.getBlock() instanceof BaseFireBlock
                    || st.is(Blocks.FIRE)
                    || st.is(Blocks.SOUL_FIRE)) {
                level.removeBlock(pos, false);
            } else if (st.getBlock() instanceof CampfireBlock
                    && st.hasProperty(CampfireBlock.LIT)
                    && Boolean.TRUE.equals(st.getValue(CampfireBlock.LIT))) {
                level.setBlock(pos, st.setValue(CampfireBlock.LIT, false), 3);
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, getCloudBox(), Entity::isAlive)) {
            if (entity.isOnFire() || entity.getRemainingFireTicks() > 0) {
                entity.clearFire();
                entity.setRemainingFireTicks(0);
            }
        }
    }

    public AABB getCloudBox() {
        return new AABB(
                getX() - HALF_EXTENT_XZ, getY() - HALF_EXTENT_Y, getZ() - HALF_EXTENT_XZ,
                getX() + HALF_EXTENT_XZ, getY() + HALF_EXTENT_Y, getZ() + HALF_EXTENT_XZ);
    }

    private void spawnClientParticles() {
        Level level = level();
        for (int i = 0; i < 6; i++) {  // +50% partículas (antes 4)
            double x = getX() + (random.nextDouble() - 0.5D) * 3.0D;
            double y = getY() + (random.nextDouble() - 0.5D) * 4.0D;
            double z = getZ() + (random.nextDouble() - 0.5D) * 3.0D;
            level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
