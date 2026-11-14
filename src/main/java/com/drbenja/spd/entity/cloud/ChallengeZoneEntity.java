package com.drbenja.spd.entity.cloud;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.util.SpdProgression;

/**
 * Zona Challenge: prisma cuadrado.
 * Normal: 7×7 en planta (half = 3.5). Boss floor / boss cerca: 3×3 (half = 1.5).
 * Altura ~4 bloques. Partículas rojas en el perímetro del cuadrado.
 */
public class ChallengeZoneEntity extends Entity {

    private int life = 100 * 20;
    /** Half-extent horizontal (3.5 → 7 bloques de lado; 1.5 → 3 de lado). */
    private double half = 3.5;

    private static final int CHALLENGE_COLOR = 0xFF3030;
    private static final int PERIMETER_COLOR = 0xFF1010;

    public ChallengeZoneEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void spawn(ServerLevel level, BlockPos center, int ticks) {
        ChallengeZoneEntity e = new ChallengeZoneEntity(ModEntities.CHALLENGE_ZONE.get(), level);
        e.setPos(center.getX() + 0.5, center.getY() + 0.1, center.getZ() + 0.5);
        e.life = ticks;
        e.half = resolveHalf(level, center);
        level.addFreshEntity(e);
    }

    /**
     * 3×3 si hay boss cercano o piso de boss (cuando exista);
     * 7×7 en caso contrario.
     */
    public static double resolveHalf(ServerLevel level, BlockPos center) {
        // Piso de boss (placeholder SpdProgression; siempre false hasta implementar mazmorra)
        if (SpdProgression.isBossFloor(level, center)) {
            return 1.5;
        }
        AABB scan = new AABB(center).inflate(48.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, scan, LivingEntity::isAlive)) {
            if (EntityClassHelper.isBoss(e)) {
                return 1.5;
            }
        }
        return 3.5;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    private AABB effectBox() {
        return new AABB(getX() - half, getY() - 1, getZ() - half,
                getX() + half, getY() + 4, getZ() + half);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnSquarePerimeterClient();
            return;
        }
        if (--life <= 0) {
            discard();
            return;
        }
        if (level() instanceof ServerLevel server) {
            if (tickCount % 3 == 0) {
                spawnSquarePerimeterServer(server);
            }
            if (tickCount % 10 == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (random.nextDouble() * 2 - 1) * half * 0.7;
                    double oz = (random.nextDouble() * 2 - 1) * half * 0.7;
                    double oy = random.nextDouble() * 2.0;
                    server.sendParticles(new DustParticleOptions(CHALLENGE_COLOR, 0.9F),
                            getX() + ox, getY() + oy, getZ() + oz, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
        if (tickCount % 10 != 0) return;
        for (Player p : level().getEntitiesOfClass(Player.class, effectBox(), Player::isAlive)) {
            p.addEffect(new MobEffectInstance(ModEffects.CHALLENGE_ZONE, 20, 0, true, true, true));
        }
    }

    private void spawnSquarePerimeterServer(ServerLevel server) {
        int edge = Math.max(8, (int) (half * 4));
        // Cuatro lados del cuadrado en y=base y y=top
        for (int i = 0; i <= edge; i++) {
            double t = (double) i / edge;
            double x0 = getX() - half + t * (half * 2);
            double z0 = getZ() - half;
            double z1 = getZ() + half;
            double zSide = getZ() - half + t * (half * 2);
            double xL = getX() - half;
            double xR = getX() + half;

            // base ring
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.3F), x0, getY() + 0.12, z0, 1, 0, 0, 0, 0);
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.3F), x0, getY() + 0.12, z1, 1, 0, 0, 0, 0);
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.3F), xL, getY() + 0.12, zSide, 1, 0, 0, 0, 0);
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.3F), xR, getY() + 0.12, zSide, 1, 0, 0, 0, 0);

            // top ring
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.0F), x0, getY() + 2.4, z0, 1, 0, 0, 0, 0);
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.0F), x0, getY() + 2.4, z1, 1, 0, 0, 0, 0);
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.0F), xL, getY() + 2.4, zSide, 1, 0, 0, 0, 0);
            server.sendParticles(new DustParticleOptions(PERIMETER_COLOR, 1.0F), xR, getY() + 2.4, zSide, 1, 0, 0, 0, 0);
        }
        // Vertical corners
        double[][] corners = {
                {-half, -half}, {half, -half}, {-half, half}, {half, half}
        };
        for (double[] c : corners) {
            for (double y = 0.3; y <= 2.3; y += 0.35) {
                server.sendParticles(new DustParticleOptions(CHALLENGE_COLOR, 1.1F),
                        getX() + c[0], getY() + y, getZ() + c[1], 1, 0, 0, 0, 0);
            }
        }
    }

    private void spawnSquarePerimeterClient() {
        int edge = Math.max(6, (int) (half * 3));
        for (int i = 0; i <= edge; i++) {
            double t = (double) i / edge;
            double x0 = getX() - half + t * (half * 2);
            double zSide = getZ() - half + t * (half * 2);
            level().addParticle(new DustParticleOptions(PERIMETER_COLOR, 1.1F),
                    x0, getY() + 0.15, getZ() - half, 0, 0.01, 0);
            level().addParticle(new DustParticleOptions(PERIMETER_COLOR, 1.1F),
                    x0, getY() + 0.15, getZ() + half, 0, 0.01, 0);
            level().addParticle(new DustParticleOptions(PERIMETER_COLOR, 1.1F),
                    getX() - half, getY() + 0.15, zSide, 0, 0.01, 0);
            level().addParticle(new DustParticleOptions(PERIMETER_COLOR, 1.1F),
                    getX() + half, getY() + 0.15, zSide, 0, 0.01, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", 100 * 20);
        half = input.getDoubleOr("Half", 3.5);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putDouble("Half", half);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
