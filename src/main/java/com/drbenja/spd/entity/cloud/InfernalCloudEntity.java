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
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.drbenja.spd.effect.BurningEffect;
import com.drbenja.spd.init.ModEffects;

/** Inferno gas: quema, prende flamables, aplica Burning; se anula con Blizzard. */
public class InfernalCloudEntity extends Entity {

    public static final int SEED_AMOUNT = 1350;
    public static final int MAX_CELLS = 40;
    public static final int MAX_LIFE_TICKS = 20 * 120;
    public static final int EVOLVE_INTERVAL = 20;

    private final Map<BlockPos, Integer> gas = new HashMap<>();
    private int life = MAX_LIFE_TICKS;
    private BlockPos origin = BlockPos.ZERO;

    public InfernalCloudEntity(EntityType<? extends InfernalCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void seed(BlockPos origin, int amount) {
        this.origin = origin.immutable();
        gas.merge(this.origin, amount, Integer::sum);
    }

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
        // Anular celdas que solapan blizzard
        cancelAgainstBlizzard((ServerLevel) level);
        if (life % EVOLVE_INTERVAL == 0) {
            evolve((ServerLevel) level);
            burnWorld((ServerLevel) level);
        }
        if (life % 20 == 0) applyBurning((ServerLevel) level);
        if (life % 3 == 0) spawnServer((ServerLevel) level);
        if (gas.isEmpty()) discard();
    }

    private void cancelAgainstBlizzard(ServerLevel level) {
        for (BlizzardCloudEntity b : level.getEntitiesOfClass(BlizzardCloudEntity.class, getBoundingBox().inflate(16))) {
            gas.keySet().removeIf(p -> b.covers(p));
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
        trim(next);
        gas.clear(); gas.putAll(next);
        expandVerticalLayers(level);
        recenter(planeY);
    }

    private void burnWorld(ServerLevel level) {
        for (BlockPos p : gas.keySet()) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) continue;
            if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT) && !st.getValue(CampfireBlock.LIT)) {
                level.setBlock(p, st.setValue(CampfireBlock.LIT, true), 3);
            }
            if (st.getBlock() instanceof CandleBlock && st.hasProperty(CandleBlock.LIT) && !st.getValue(CandleBlock.LIT)) {
                level.setBlock(p, st.setValue(CandleBlock.LIT, true), 3);
            }
            if (st.getBlock() instanceof TntBlock) {
                level.removeBlock(p, false);
                level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
            }
            if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW)) {
                level.removeBlock(p, false);
            }
            // Prender aire adyacente si el bloque actual es flamable
            if (st.ignitedByLava() || st.is(Blocks.OAK_LEAVES) || st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.SHORT_GRASS)
                    || st.is(Blocks.TALL_GRASS) || st.is(Blocks.VINE)) {
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    if (level.getBlockState(n).isAir() && BaseFireBlock.canBePlacedAt(level, n, Direction.UP)) {
                        level.setBlock(n, BaseFireBlock.getState(level, n), 3);
                    }
                }
            }
            if (level.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(level, p, Direction.UP)) {
                // fuego en celda del gas si hay flamable debajo
                BlockState below = level.getBlockState(p.below());
                if (below.ignitedByLava() || !below.getCollisionShape(level, p.below()).isEmpty()) {
                    level.setBlock(p, BaseFireBlock.getState(level, p), 3);
                }
            }
        }
    }

    private void applyBurning(ServerLevel level) {
        if (gas.isEmpty()) return;
        int y = origin.getY();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box(y), Entity::isAlive)) {
            if (!coversEntity(e, y)) continue;
            if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) continue; // Potion of Purity
            e.addEffect(new MobEffectInstance(ModEffects.BURNING, BurningEffect.DEFAULT_DURATION_TICKS, 0, false, true, true));
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 40));
        }
    }

    private boolean coversEntity(LivingEntity e, int y) {
        BlockPos feet = e.blockPosition();
        return gas.containsKey(new BlockPos(feet.getX(), y, feet.getZ())) || gas.containsKey(feet) || gas.containsKey(feet.above());
    }

    private Map<BlockPos, Integer> filterPlane(int y) {
        Map<BlockPos, Integer> m = new HashMap<>();
        for (var e : gas.entrySet()) if (e.getKey().getY() == y) m.put(e.getKey(), e.getValue());
        return m;
    }

    private void trim(Map<BlockPos, Integer> next) {
        if (next.size() <= MAX_CELLS) return;
        BlockPos o = origin;
        next.entrySet().stream()
                .sorted((a, b) -> {
                    int c = Integer.compare(b.getValue(), a.getValue());
                    return c != 0 ? c : Integer.compare(a.getKey().distManhattan(o), b.getKey().distManhattan(o));
                })
                .skip(MAX_CELLS).map(Map.Entry::getKey).toList().forEach(next::remove);
    }

    private void recenter(int y) {
        if (gas.isEmpty()) return;
        long sx = 0, sz = 0;
        for (BlockPos p : gas.keySet()) { sx += p.getX(); sz += p.getZ(); }
        int n = gas.size();
        setPos(sx / (double) n + 0.5, y + 0.5, sz / (double) n + 0.5);
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
            level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.15, 0.15, 0.15, 0.01);
            level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void spawnClient() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
        }
    }

    public boolean covers(BlockPos p) { return gas.containsKey(p); }
}
