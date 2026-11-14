package com.drbenja.spd.entity.cloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.init.ModEffects;

/** Blizzard gas: aplica Chilled (doble), apaga fuego; se anula con Inferno. */
public class BlizzardCloudEntity extends Entity {

    public static final int SEED_AMOUNT = 1350;
    public static final int MAX_CELLS = 40;
    public static final int MAX_LIFE_TICKS = 20 * 120;
    public static final int EVOLVE_INTERVAL = 20;
    public static final int CHILL_ADD_TICKS = 40; // freeze twice ≈ +2s each tick of apply

    private final Map<BlockPos, Integer> gas = new HashMap<>();
    private int life = MAX_LIFE_TICKS;
    private BlockPos origin = BlockPos.ZERO;

    public BlizzardCloudEntity(EntityType<? extends BlizzardCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void seed(BlockPos origin, int amount) {
        this.origin = origin.immutable();
        gas.merge(this.origin, amount, Integer::sum);
    }

    public boolean covers(BlockPos p) { return gas.containsKey(p); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        life = input.getIntOr("Life", MAX_LIFE_TICKS);
        origin = new BlockPos(input.getIntOr("ox", 0), input.getIntOr("oy", 0), input.getIntOr("oz", 0));
        gas.clear();
        int n = input.getIntOr("GasCount", 0);
        for (int i = 0; i < n; i++) {
            int v = input.getIntOr("gv" + i, 0);
            if (v > 0) gas.put(new BlockPos(input.getIntOr("gx" + i, 0), input.getIntOr("gy" + i, 0), input.getIntOr("gz" + i, 0)), v);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", life);
        output.putInt("ox", origin.getX()); output.putInt("oy", origin.getY()); output.putInt("oz", origin.getZ());
        output.putInt("GasCount", gas.size());
        int i = 0;
        for (var e : gas.entrySet()) {
            output.putInt("gx" + i, e.getKey().getX());
            output.putInt("gy" + i, e.getKey().getY());
            output.putInt("gz" + i, e.getKey().getZ());
            output.putInt("gv" + i, e.getValue());
            i++;
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide()) { spawnClient(); return; }
        if (life-- <= 0) { discard(); return; }
        cancelAgainstInferno((ServerLevel) level);
        if (life % EVOLVE_INTERVAL == 0) {
            evolve((ServerLevel) level);
            freezeWorld((ServerLevel) level);
        }
        if (life % 20 == 0) applyChill((ServerLevel) level);
        if (life % 3 == 0) spawnServer((ServerLevel) level);
        if (gas.isEmpty()) discard();
    }

    private void cancelAgainstInferno(ServerLevel level) {
        for (InfernalCloudEntity inf : level.getEntitiesOfClass(InfernalCloudEntity.class, getBoundingBox().inflate(16))) {
            gas.keySet().removeIf(inf::covers);
        }
    }

    private void evolve(ServerLevel level) {
        if (gas.isEmpty()) return;
        int planeY = origin.getY();
        Map<BlockPos, Integer> plane = filterPlane(planeY);
        if (plane.isEmpty()) {
            planeY = gas.keySet().iterator().next().getY();
            origin = new BlockPos(origin.getX(), planeY, origin.getZ());
            plane = filterPlane(planeY);
        }
        Map<BlockPos, Integer> work = new HashMap<>(plane);
        for (BlockPos p : plane.keySet()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!isBlocking(level, n)) work.putIfAbsent(n, 0);
            }
        }
        Map<BlockPos, Integer> next = new HashMap<>();
        for (BlockPos cell : work.keySet()) {
            if (isBlocking(level, cell)) continue;
            int sum = plane.getOrDefault(cell, 0), count = 1;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = cell.relative(d);
                if (!isBlocking(level, n)) { sum += plane.getOrDefault(n, 0); count++; }
            }
            int v = sum / count - 1;
            if (v > 0) next.put(cell.immutable(), v);
        }
        if (next.size() > MAX_CELLS) {
            BlockPos o = origin;
            next.entrySet().stream()
                    .sorted((a, b) -> {
                        int c = Integer.compare(b.getValue(), a.getValue());
                        return c != 0 ? c : Integer.compare(a.getKey().distManhattan(o), b.getKey().distManhattan(o));
                    })
                    .skip(MAX_CELLS).map(Map.Entry::getKey).toList().forEach(next::remove);
        }
        gas.clear(); gas.putAll(next);
        expandVerticalLayers(level);
        if (!gas.isEmpty()) {
            long sx = 0, sz = 0;
            for (BlockPos p : gas.keySet()) { sx += p.getX(); sz += p.getZ(); }
            setPos(sx / (double) gas.size() + 0.5, planeY + 0.5, sz / (double) gas.size() + 0.5);
        }
    }

    private void freezeWorld(ServerLevel level) {
        for (BlockPos p : gas.keySet()) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) level.removeBlock(p, false);
            if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT) && st.getValue(CampfireBlock.LIT)) {
                level.setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
            }
        }
    }

    private void applyChill(ServerLevel level) {
        if (gas.isEmpty()) return;
        int y = origin.getY();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box(y), Entity::isAlive)) {
            BlockPos feet = e.blockPosition();
            if (!(gas.containsKey(new BlockPos(feet.getX(), y, feet.getZ())) || gas.containsKey(feet) || gas.containsKey(feet.above()))) continue;
            if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) continue; // Potion of Purity
            if (ChillImmuneEntitiesConfig.isChillImmune(e)) continue;
            // Freezing.freeze twice
            int add = CHILL_ADD_TICKS;
            var existing = e.getEffect(ModEffects.CHILLED);
            int dur = existing == null ? add : existing.getDuration() + add;
            e.addEffect(new MobEffectInstance(ModEffects.CHILLED, dur, 0, false, true, true));
        }
    }

    private Map<BlockPos, Integer> filterPlane(int y) {
        Map<BlockPos, Integer> m = new HashMap<>();
        for (var e : gas.entrySet()) if (e.getKey().getY() == y) m.put(e.getKey(), e.getValue());
        return m;
    }

    private AABB box(int y) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : gas.keySet()) {
            minX = Math.min(minX, p.getX()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxZ = Math.max(maxZ, p.getZ());
        }
        return new AABB(minX, y, minZ, maxX + 1, y + 2, maxZ + 1);
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

    private void spawnServer(ServerLevel level) {
        int n = 0;
        for (BlockPos p : gas.keySet()) {
            if (n++ > 30) break;
            if (level.getRandom().nextInt(2) == 0) continue;
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void spawnClient() {
        level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
    }
}
