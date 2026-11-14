package com.drbenja.spd.entity.cloud;

import com.drbenja.spd.entity.classy.EntityClassHelper;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.SpdProgression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Storm Cloud: difusión horizontal (seed SPD ~1080 +25% = 1350).
 * - Waterloggea bloques waterlogeables
 * - Pone agua corriente (no source) sobre sólidos
 * - Apaga fuego / campfires
 * - 1 dmg/s a entidades fireImmune (fiery SPD)
 */
public class StormCloudEntity extends Entity {

    public static final int SEED_AMOUNT = 1350;
    public static final int MAX_CELLS = 40;
    public static final int MAX_LIFE_TICKS = 20 * 120;
    public static final int EVOLVE_INTERVAL = 20;
    public static final int GAS_COLOR = 0x4488CC;

    private final Map<BlockPos, Integer> gas = new HashMap<>();
    private int life = MAX_LIFE_TICKS;
    private BlockPos origin = BlockPos.ZERO;

    public StormCloudEntity(EntityType<? extends StormCloudEntity> type, Level level) {
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
            applyWorldEffects((ServerLevel) level);
        }
        if (life % 20 == 0) {
            applyFieryDamage((ServerLevel) level);
        }
        if (life % 3 == 0) {
            spawnServerParticles((ServerLevel) level);
        }
        if (gas.isEmpty()) {
            discard();
        }
    }

    private void evolve(ServerLevel level) {
        if (gas.isEmpty()) return;
        int planeY = origin.getY();
        Map<BlockPos, Integer> plane = new HashMap<>();
        for (var e : gas.entrySet()) {
            if (e.getKey().getY() == planeY) plane.put(e.getKey(), e.getValue());
        }
        if (plane.isEmpty()) {
            BlockPos any = gas.keySet().iterator().next();
            planeY = any.getY();
            origin = new BlockPos(origin.getX(), planeY, origin.getZ());
            for (var e : gas.entrySet()) {
                if (e.getKey().getY() == planeY) plane.put(e.getKey(), e.getValue());
            }
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
            if (value > 0) next.put(cell.immutable(), value);
        }
        if (next.size() > MAX_CELLS) {
            BlockPos originRef = this.origin;
            next.entrySet().stream()
                    .sorted((a, b) -> {
                        int c = Integer.compare(b.getValue(), a.getValue());
                        if (c != 0) return c;
                        return Integer.compare(a.getKey().distManhattan(originRef), b.getKey().distManhattan(originRef));
                    })
                    .skip(MAX_CELLS).map(Map.Entry::getKey).toList()
                    .forEach(next::remove);
        }
        gas.clear();
        gas.putAll(next);
        expandVerticalLayers(level);
        if (!gas.isEmpty()) {
            long sx = 0, sz = 0;
            for (BlockPos p : gas.keySet()) { sx += p.getX(); sz += p.getZ(); }
            int n = gas.size();
            setPos(sx / (double) n + 0.5, planeY + 0.5, sz / (double) n + 0.5);
        }
    }

    private void applyWorldEffects(ServerLevel level) {
        for (BlockPos p : gas.keySet()) {
            // Waterlog this cell and the one below (gas often sits in air above solids)
            tryWaterlog(level, p);
            tryWaterlog(level, p.below());

            BlockState st = level.getBlockState(p);
            // Extinguish fire
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) {
                level.removeBlock(p, false);
                st = level.getBlockState(p);
            }
            if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT)
                    && st.getValue(CampfireBlock.LIT)) {
                level.setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
                st = level.getBlockState(p);
            }

            // Flowing water (not source) in the gas cell if air above solid
            BlockPos below = p.below();
            BlockState belowSt = level.getBlockState(below);

            boolean solidBelow = !belowSt.getCollisionShape(level, below).isEmpty();
            if (solidBelow && (st.isAir() || st.canBeReplaced() || st.is(Blocks.WATER))) {
                level.setBlock(p, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), 3);
            }
            // Solid gas cell → flowing water on top
            if (!st.getCollisionShape(level, p).isEmpty()) {
                BlockPos up = p.above();
                BlockState upSt = level.getBlockState(up);
                if (upSt.isAir() || upSt.canBeReplaced()) {
                    level.setBlock(up, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), 3);
                }
                tryWaterlog(level, p);
            }
        }
    }

    private static void tryWaterlog(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (st.hasProperty(BlockStateProperties.WATERLOGGED)
                && !Boolean.TRUE.equals(st.getValue(BlockStateProperties.WATERLOGGED))) {
            level.setBlock(p, st.setValue(BlockStateProperties.WATERLOGGED, true), 3);
        }
    }

    private void applyFieryDamage(ServerLevel level) {
        if (gas.isEmpty()) return;
        int y = origin.getY();
        AABB box = boundingBoxOfGas(y);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            BlockPos feet = e.blockPosition();
            BlockPos atPlane = new BlockPos(feet.getX(), y, feet.getZ());
            if (!(gas.containsKey(atPlane) || gas.containsKey(feet) || gas.containsKey(feet.above()))) {
                continue;
            }
            if (e.hasEffect(ModEffects.PURIFICATION_BARRIER)) {
                continue; // Potion of Purity: immune to gases/clouds
            }
            // SPD Property.FIERY — blaze / magma_cube (entity class "fiery" later)
            if (EntityClassHelper.isFiery(e)) {
                float dmg = SpdProgression.stormCloudFieryDamage(e);
                e.hurtServer(level, e.damageSources().magic(), dmg);
            }
        }
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

    private void spawnServerParticles(ServerLevel level) {
        int shown = 0;
        for (BlockPos p : gas.keySet()) {
            if (shown >= 32) break;
            if (level.getRandom().nextInt(2) == 0) continue;
            shown++;
            level.sendParticles(ParticleTypes.CLOUD,
                    p.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.7,
                    p.getY() + 0.5 + level.getRandom().nextDouble() * 0.5,
                    p.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.7,
                    1, 0.05, 0.05, 0.05, 0.01);
            if (level.getRandom().nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.FALLING_WATER,
                        p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5,
                        1, 0.1, 0.0, 0.1, 0.0);
            }
        }
    }

    private void spawnClientParticles() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.CLOUD,
                    getX() + (random.nextDouble() - 0.5) * 2.0,
                    getY() + random.nextDouble(),
                    getZ() + (random.nextDouble() - 0.5) * 2.0,
                    0, 0.02, 0);
        }
    }

}
