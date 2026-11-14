package com.drbenja.spd.entity.cloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.effect.ParalyzedEffect;
import com.drbenja.spd.init.ModEffects;

/**
 * Blob de gas paralítico estilo SPD (aprox. 2D en planta):
 * - 1 evolve ≈ 1 turno SPD (cada 20 ticks)
 * - Difusión horizontal (4 vecinos), como el grid 2D de SPD
 * - seed 1000; máx. ~40 celdas; decay (promedio) - 1 por evolve
 * - El centro se mantiene más denso y es de los últimos en desaparecer
 */
public class ParalyticGasCloudEntity extends Entity {

    /**
     * SPD seed 1000 → con evolve 1/s en plano horizontal daba ~34 s / ~29 celdas.
     * 1300 acerca a ~38–39 s y ~32–34 celdas efectivas.
     */
    public static final int SEED_AMOUNT = 1250;
    /** Tope con margen sobre el ~34 típico de SPD. */
    public static final int MAX_CELLS = 48;
    public static final int MAX_LIFE_TICKS = 20 * 120;
    /** 20 ticks = 1 s ≈ 1 turno SPD */
    public static final int EVOLVE_INTERVAL = 20;
    public static final int GAS_COLOR = 0xFFDB65;

    private final Map<BlockPos, Integer> gas = new HashMap<>();
    private int life = MAX_LIFE_TICKS;
    private BlockPos origin = BlockPos.ZERO;

    public ParalyticGasCloudEntity(EntityType<? extends ParalyticGasCloudEntity> type, Level level) {
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

        // Un evolve por segundo (turno SPD)
        if (life % EVOLVE_INTERVAL == 0) {
            evolve((ServerLevel) level);
        }
        applyToEntities((ServerLevel) level);
        if (life % 4 == 0) {
            spawnServerParticles((ServerLevel) level);
        }

        if (gas.isEmpty()) {
            discard();
        }
    }

    /**
     * Difusión horizontal tipo SPD Blob.evolve:
     * off[cell] = (sum vecinos_abiertos + self) / count - 1
     * Solo ejes X/Z (plano); Y fija del origen para no diluir en 3D.
     */
    private void evolve(ServerLevel level) {
        if (gas.isEmpty()) {
            return;
        }

        // Trabajar solo en el plano Y del origen (capa principal del gas)
        int planeY = origin.getY();
        Map<BlockPos, Integer> plane = new HashMap<>();
        for (var e : gas.entrySet()) {
            if (e.getKey().getY() == planeY) {
                plane.put(e.getKey(), e.getValue());
            }
        }
        if (plane.isEmpty()) {
            // Si se perdió el plano, usar cualquier celda como nueva Y
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

        // Priorizar celdas más densas y cercanas al origen si hay exceso
        if (next.size() > MAX_CELLS) {
            BlockPos originRef = this.origin;
            next.entrySet().stream()
                    .sorted((a, b) -> {
                        int c = Integer.compare(b.getValue(), a.getValue());
                        if (c != 0) return c;
                        int da = a.getKey().distManhattan(originRef);
                        int db = b.getKey().distManhattan(originRef);
                        return Integer.compare(da, db);
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

    private void applyToEntities(ServerLevel level) {
        if (gas.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int y = origin.getY();
        for (BlockPos p : gas.keySet()) {
            minX = Math.min(minX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxZ = Math.max(maxZ, p.getZ());
            y = p.getY();
        }
        AABB box = new AABB(minX, y, minZ, maxX + 1, y + 2, maxZ + 1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            BlockPos feet = e.blockPosition();
            BlockPos atPlane = new BlockPos(feet.getX(), y, feet.getZ());
            if (gas.containsKey(atPlane) || gas.containsKey(feet) || gas.containsKey(feet.above())) {
                if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
                    continue; // Potion of Purity: immune to gases/clouds
                }
                e.addEffect(new MobEffectInstance(
                        ModEffects.PARALYZED, ParalyzedEffect.DEFAULT_DURATION_TICKS, 0, false, true, true));
            }
        }
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
