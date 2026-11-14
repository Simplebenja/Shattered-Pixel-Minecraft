package com.drbenja.spd.entity.cloud;

import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.util.SpdProgression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.effect.PoisonedEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Gas tóxico: misma difusión horizontal que ParalyticGas (1 evolve/s, plano 2D).
 * SPD seed 1000 → +25% = 1250. 1 dmg/s tipo magic; no undead / no ToxicImbue.
 */
public class ToxicGasCloudEntity extends Entity {

    /** SPD ToxicGas seed 1000 + 25%. */
    public static final int SEED_AMOUNT = 1250;
    public static final int MAX_CELLS = 48;
    public static final int MAX_LIFE_TICKS = 20 * 120;
    public static final int EVOLVE_INTERVAL = 20;
    public static final int GAS_COLOR = 0x55AA22;

    private final Map<BlockPos, Integer> gas = new HashMap<>();
    private int life = MAX_LIFE_TICKS;
    private BlockPos origin = BlockPos.ZERO;

    public ToxicGasCloudEntity(EntityType<? extends ToxicGasCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void seed(BlockPos origin, int amount) {
        this.origin = origin.immutable();
        gas.merge(this.origin, amount, Integer::sum);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.life = input.getIntOr("Life", MAX_LIFE_TICKS);
        this.origin = new BlockPos(
                input.getIntOr("ox", 0),
                input.getIntOr("oy", 0),
                input.getIntOr("oz", 0));
        gas.clear();
        int n = input.getIntOr("GasCount", 0);
        for (int i = 0; i < n; i++) {
            int v = input.getIntOr("gv" + i, 0);
            if (v > 0) {
                gas.put(new BlockPos(
                        input.getIntOr("gx" + i, 0),
                        input.getIntOr("gy" + i, 0),
                        input.getIntOr("gz" + i, 0)), v);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", this.life);
        output.putInt("ox", origin.getX());
        output.putInt("oy", origin.getY());
        output.putInt("oz", origin.getZ());
        output.putInt("GasCount", gas.size());
        int i = 0;
        for (var e : gas.entrySet()) {
            BlockPos p = e.getKey();
            output.putInt("gx" + i, p.getX());
            output.putInt("gy" + i, p.getY());
            output.putInt("gz" + i, p.getZ());
            output.putInt("gv" + i, e.getValue());
            i++;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
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
        if (life % EVOLVE_INTERVAL == 0) {
            evolve((ServerLevel) level);
        }
        if (life % 20 == 0) {
            applyDamage((ServerLevel) level);
        }
        if (life % 4 == 0) {
            spawnServerParticles((ServerLevel) level);
        }
        if (gas.isEmpty()) {
            discard();
        }
    }

    private void evolve(ServerLevel level) {
        if (gas.isEmpty()) {
            return;
        }
        int planeY = origin.getY();
        Map<BlockPos, Integer> plane = new HashMap<>();
        for (var e : gas.entrySet()) {
            if (e.getKey().getY() == planeY) {
                plane.put(e.getKey(), e.getValue());
            }
        }
        if (plane.isEmpty()) {
            BlockPos any = gas.keySet().iterator().next();
            planeY = any.getY();
            origin = new BlockPos(origin.getX(), planeY, origin.getZ());
            for (var e : gas.entrySet()) {
                if (e.getKey().getY() == planeY) {
                    plane.put(e.getKey(), e.getValue());
                }
            }
        }

        Map<BlockPos, Integer> work = new HashMap<>(plane);
        for (BlockPos p : plane.keySet()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!isBlocking(level, n)) {
                    work.putIfAbsent(n, 0);
                }
            }
        }

        Map<BlockPos, Integer> next = new HashMap<>();
        for (BlockPos cell : work.keySet()) {
            if (isBlocking(level, cell)) {
                continue;
            }
            int sum = plane.getOrDefault(cell, 0);
            int count = 1;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = cell.relative(d);
                if (!isBlocking(level, n)) {
                    sum += plane.getOrDefault(n, 0);
                    count++;
                }
            }
            int value = sum / count - 1;
            if (value > 0) {
                next.put(cell.immutable(), value);
            }
        }

        if (next.size() > MAX_CELLS) {
            BlockPos originRef = this.origin;
            next.entrySet().stream()
                    .sorted((a, b) -> {
                        int c = Integer.compare(b.getValue(), a.getValue());
                        if (c != 0) return c;
                        return Integer.compare(
                                a.getKey().distManhattan(originRef),
                                b.getKey().distManhattan(originRef));
                    })
                    .skip(MAX_CELLS)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(next::remove);
        }

        gas.clear();
        gas.putAll(next);
        expandVerticalLayers(level);
        if (!gas.isEmpty()) {
            long sx = 0, sz = 0;
            for (BlockPos p : gas.keySet()) {
                sx += p.getX();
                sz += p.getZ();
            }
            int n = gas.size();
            setPos(sx / (double) n + 0.5, planeY + 0.5, sz / (double) n + 0.5);
        }
    }

    
    /** Copia densidad a ±1 y ±2 en Y si no hay bloque sólido (capa ~3 bloques de alto). */
    private void expandVerticalLayers(ServerLevel level) {
        if (gas.isEmpty()) return;
        Map<BlockPos, Integer> withVert = new HashMap<>(gas);
        for (var e : List.copyOf(gas.entrySet())) {
            BlockPos p = e.getKey();
            int v = e.getValue();
            for (int dy = -2; dy <= 2; dy++) {
                if (dy == 0) continue;
                BlockPos q = p.offset(0, dy, 0);
                if (!isBlocking(level, q)) {
                    int nv = Math.max(1, v / (1 + Math.abs(dy)));
                    withVert.merge(q.immutable(), nv, Math::max);
                }
            }
        }
        gas.clear();
        gas.putAll(withVert);
    }

    private static boolean isBlocking(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private void applyDamage(ServerLevel level) {
        if (gas.isEmpty()) {
            return;
        }
        int y = origin.getY();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, boundingBoxOfGas(y), Entity::isAlive)) {
            BlockPos feet = e.blockPosition();
            BlockPos atPlane = new BlockPos(feet.getX(), y, feet.getZ());
            if (!(gas.containsKey(atPlane) || gas.containsKey(feet) || gas.containsKey(feet.above()))) {
                continue;
            }
            if (!canHarm(e)) {
                continue;
            }
            float dmg = SpdProgression.toxicGasDamage(e);
            e.hurtServer(level, PoisonedEffect.toxicSource(level), dmg);
        }
    }

    public static boolean canHarm(LivingEntity e) {
        if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
            return false; // Potion of Purity: immune to gases/clouds
        }
        if (e.hasEffect(ModEffects.IMBUED_WITH_TOXICITY)) {
            return false;
        }
        if (EntityClassHelper.immuneToToxicGas(e)) {
            return false;
        }
        return !e.isInvertedHealAndHarm();
    }

    private AABB boundingBoxOfGas(int y) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : gas.keySet()) {
            minX = Math.min(minX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new AABB(minX, y, minZ, maxX + 1, y + 2, maxZ + 1);
    }

    private void spawnServerParticles(ServerLevel level) {
        DustParticleOptions dust = new DustParticleOptions(GAS_COLOR, 1.2F);
        int shown = 0;
        for (BlockPos p : gas.keySet()) {
            if (shown >= 40) break;
            if (level.getRandom().nextInt(3) == 0) continue;
            shown++;
            level.sendParticles(dust,
                    p.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.7,
                    p.getY() + 0.4 + level.getRandom().nextDouble() * 0.5,
                    p.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.7,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnClientParticles() {
        DustParticleOptions dust = new DustParticleOptions(GAS_COLOR, 1.0F);
        for (int i = 0; i < 2; i++) {
            level().addParticle(dust,
                    getX() + (random.nextDouble() - 0.5) * 2.0,
                    getY() + random.nextDouble() * 0.8,
                    getZ() + (random.nextDouble() - 0.5) * 2.0,
                    0, 0.01, 0);
        }
    }
}

