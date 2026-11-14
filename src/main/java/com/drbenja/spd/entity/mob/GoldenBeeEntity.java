package com.drbenja.spd.entity.mob;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.config.goldenbee.GoldenBeeConfig;
import com.drbenja.spd.block.GoldenBeehiveBlock;
import com.drbenja.spd.block.entity.GoldenBeehiveBlockEntity;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.init.ModPotions;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Golden Bee — extiende Bee para panales/polinización vanilla.
 * Domesticable con Honeyed Healing (click o splash/lingering).
 *
 * Comandos domesticada: FOLLOW → STAY → WANDER → WORK → FOLLOW
 * WORK: se ancla al panal más cercano y actúa como abeja ordinaria.
 */
public class GoldenBeeEntity extends Bee {

    public enum BeeCommand {
        FOLLOW, STAY, WANDER, WORK
    }

    private static final EntityDataAccessor<Boolean> DATA_TAME =
            SynchedEntityData.defineId(GoldenBeeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SITTING =
            SynchedEntityData.defineId(GoldenBeeEntity.class, EntityDataSerializers.BOOLEAN);

    private BeeCommand command = BeeCommand.FOLLOW;
    @Nullable
    private BeeCommand commandBeforeDefend;
    @Nullable
    private BlockPos wanderHome;
    @Nullable
    private BlockPos stayAnchor;
    @Nullable
    private UUID ownerUUID;
    private boolean defending;
    private boolean flyingToStay;
    private boolean carryingNectar;
    @Nullable
    private BlockPos rememberedFlower;
    @Nullable
    private BlockPos rememberedHive;
    /** Origen del área de trabajo (modo WORK domesticada). */
    @Nullable
    private BlockPos workOrigin;
    /** Radio de búsqueda progresivo actual (flores/panales). */
    private int searchRadius = 8;
    private static final int SEARCH_RADIUS_MIN = 8;
    private static final int SEARCH_RADIUS_MAX = 32;
    private static final int SEARCH_RADIUS_STEP = 1;
    private static final int WORK_AREA_RADIUS = 24;
    private int searchFailCooldown;
    private int defendCooldown;
    private int ambientCooldown;
    private int interactCooldown;
    private int debugCooldown;
    private int hiveGuardCheckCooldown;
    /** Ticks tras salir del panal antes de poder volver a entrar. */
    private int hiveReenterCooldown;

    public GoldenBeeEntity(EntityType<? extends GoldenBeeEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TAME, false);
        builder.define(DATA_SITTING, false);
    }

    // -------------------------------------------------------------------------
    // Taming (sin TamableAnimal; Bee no lo permite)
    // -------------------------------------------------------------------------

    public boolean isTame() {
        return this.entityData.get(DATA_TAME);
    }

    public void setTame(boolean tame) {
        this.entityData.set(DATA_TAME, tame);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }

    @Nullable
    public LivingEntity getOwner() {
        UUID id = getOwnerUUID();
        if (id == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel server) {
            Entity e = server.getEntity(id);
            if (e instanceof LivingEntity living) {
                return living;
            }
            Player p = server.getServer().getPlayerList().getPlayer(id);
            return p;
        }
        return this.level().getPlayerByUUID(id);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity != null && entity.getUUID().equals(getOwnerUUID());
    }

    public boolean isOrderedToSit() {
        return this.entityData.get(DATA_SITTING);
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(DATA_SITTING, sit);
    }

    public void tameBy(Player player) {
        if (this.isTame()) {
            return;
        }
        this.setTame(true);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    8, 0.4, 0.4, 0.4, 0.05);
        }
        this.setOwnerUUID(player.getUUID());
        this.command = BeeCommand.FOLLOW;
        this.defending = false;
        this.flyingToStay = false;
        this.setOrderedToSit(false);
        this.setTarget(null);
        this.setTarget(null);
        try { this.forgetCurrentTargetAndRefreshUniversalAnger(); } catch (Exception ignored) {}
        this.level().broadcastEntityEvent(this, (byte) 7);
    }

    public BeeCommand getCommand() {
        return this.command;
    }

    // -------------------------------------------------------------------------
    // Atributos / navegación
    // -------------------------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        // Velocidad igual a abeja vanilla (FLYING 0.6 / MOVE 0.3)
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.STEP_HEIGHT, 0.6D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    // -------------------------------------------------------------------------
    // Goals
    // -------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        // IMPORTANTE: Bee inicializa beePollinateGoal aquí.
        // Si no se llama, BeeLookControl hace NPE al tick.
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);

        // Objetivos estilo Bee + domesticación
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GoldenSitGoal(this));
        this.goalSelector.addGoal(1, new SafeLandWhenSittingGoal(this));
        this.goalSelector.addGoal(2, new GoldenBreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
        // Atraída por flores / Honeyed Healing
        this.goalSelector.addGoal(3, new HoneyOrFlowerTemptGoal(this));
        this.goalSelector.addGoal(4, new FlyingMeleeAttackGoal(this));
        this.goalSelector.addGoal(5, new ConditionalFollowOwnerGoal(this));
        // Polinizar / entrar panal: solo salvaje o WORK
        this.goalSelector.addGoal(6, new ConditionalBeeEnterHiveGoal(this));
        this.goalSelector.addGoal(7, new ConditionalPollinateGoal(this));
        this.goalSelector.addGoal(8, new GoldenBeeWanderGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetCustom(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetCustom(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (GoldenBeeEntity.this.isTame() && GoldenBeeEntity.this.getOwner() != null) {
                    LivingEntity last = GoldenBeeEntity.this.getLastHurtByMob();
                    if (last != null && GoldenBeeEntity.this.isOwnedBy(last)) {
                        return false;
                    }
                }
                return super.canUse();
            }

            @Override
            protected void alertOthers() {
                // Alerta a otras Bee (incluye Golden) en radio
                double range = GoldenBeeEntity.this.getAttributeValue(Attributes.FOLLOW_RANGE);
                AABB box = AABB.unitCubeFromLowerCorner(GoldenBeeEntity.this.position()).inflate(range, 10.0D, range);
                List<Bee> bees = GoldenBeeEntity.this.level().getEntitiesOfClass(Bee.class, box);
                for (Bee other : bees) {
                    if (other != GoldenBeeEntity.this && !other.isBaby()) {
                        other.setLastHurtByMob(GoldenBeeEntity.this.getLastHurtByMob());
                    }
                }
            }
        }.setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new HiveGuardTargetGoal(this));
        this.targetSelector.addGoal(6, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** ¿Puede usar IA de abeja (panal/polen)? */
    public boolean canUseBeeWorkAi() {
        if (!this.isTame()) {
            return true;
        }
        return this.command == BeeCommand.WORK && !this.defending && !this.isOrderedToSit();
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        this.tickWildHiveGuardAndReturn();
        if (this.level().isClientSide()) {
            return;
        }

        if (this.defendCooldown > 0) {
            this.defendCooldown--;
        }

        if (this.interactCooldown > 0) {
            this.interactCooldown--;
        }
        if (this.hiveReenterCooldown > 0) {
            this.hiveReenterCooldown--;
        }
        this.tickOwnerDefense();
        if (this.searchFailCooldown > 0) {
            this.searchFailCooldown--;
        }
        this.tickAmbient();
        this.tickDebugGoals();
        this.tickKeepFlying();
        this.tickExpandSearch();
        // Salvaje: ancla de panal + re-check cada 60s
        if (!this.isTame()) {
            this.tickWildHiveAnchor();
        }

        // FOLLOW: teletransporte al dueño si se aleja / cambia dimensión
        if (this.isTame() && this.command == BeeCommand.FOLLOW && !this.defending && !this.isOrderedToSit()) {
            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer player && player.level() instanceof ServerLevel dest) {
                if (player.level() != this.level()) {
                    this.changeDimensionToOwner(dest, player);
                } else if (this.distanceToSqr(player) > 32.0D * 32.0D) {
                    this.teleportToOwner(player);
                }
            }
        }
    }


    /** Bloque donde PUEDE ENTRAR (nunca vanilla). */
    private boolean isEnterableHiveBlock(@Nullable BlockPos pos) {
        if (pos == null || this.level() == null) return false;
        BlockState st = this.level().getBlockState(pos);
        Identifier id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
        return GoldenBeeConfig.isHiveBlockAllowed(id);
    }

    /** Panal vanilla u otro: solo para patrulla / defensa, NO entrada. */
    private boolean isProtectableHiveBlock(@Nullable BlockPos pos) {
        if (pos == null || this.level() == null) return false;
        BlockState st = this.level().getBlockState(pos);
        if (st.is(Blocks.BEEHIVE) || st.is(Blocks.BEE_NEST)) return true;
        return isEnterableHiveBlock(pos);
    }

    private boolean isHiveUsable(@Nullable BlockPos pos) {
        if (!isEnterableHiveBlock(pos)) return false;
        if (this.level().getBlockEntity(pos) instanceof GoldenBeehiveBlockEntity nest) {
            return !nest.isFull();
        }
        return true;
    }

    private boolean isHivePreferred(@Nullable BlockPos pos) {
        return isHiveUsable(pos) && nestHoneyLevel(pos) < GoldenBeehiveBlock.MAX_HONEY;
    }

    private int nestHoneyLevel(@Nullable BlockPos pos) {
        if (pos == null || this.level() == null) return 0;
        BlockState st = this.level().getBlockState(pos);
        if (st.hasProperty(GoldenBeehiveBlock.HONEY_LEVEL)) {
            return st.getValue(GoldenBeehiveBlock.HONEY_LEVEL);
        }
        return 0;
    }

    private int hiveHoneyLevel(@Nullable BlockPos pos) {
        return nestHoneyLevel(pos);
    }

    private boolean isHiveBlock(@Nullable BlockPos pos) {
        return isEnterableHiveBlock(pos);
    }


    BlockPos findHiveProgressive() {
        if (this.searchFailCooldown > 0) {
            return null;
        }
        BlockPos origin = this.blockPosition();
        if (this.isTame() && this.command == BeeCommand.WORK && this.workOrigin != null) {
            // No iniciar búsqueda fuera del área de trabajo
            if (!origin.closerThan(this.workOrigin, WORK_AREA_RADIUS + 2)) {
                return null;
            }
        }

        int r = this.searchRadius;
        this.debugSearch("panal", r);
        BlockPos found = scanHiveInRadius(origin, r);
        if (found != null) {
            this.searchRadius = SEARCH_RADIUS_MIN;
            this.debugSearch("panal OK " + found.toShortString(), r);
            return found;
        }

        if (this.searchRadius < SEARCH_RADIUS_MAX) {
            this.searchRadius = Math.min(SEARCH_RADIUS_MAX, this.searchRadius + SEARCH_RADIUS_STEP);
            return null;
        }

        // Máximo alcanzado: solo WORK domesticada hace pausa (10 s); salvaje sigue
        this.searchRadius = SEARCH_RADIUS_MIN;
        if (this.isTame() && this.command == BeeCommand.WORK) {
            this.searchFailCooldown = 20 * 10; // 10 s
        }
        return null;
    }

    @Nullable
    private BlockPos scanHiveInRadius(BlockPos origin, int r) {
        BlockPos preferred = null;
        BlockPos fallback = null;
        double bestPref = Double.MAX_VALUE;
        double bestFall = Double.MAX_VALUE;
        int step = r <= 8 ? 1 : 2;
        for (int dx = -r; dx <= r; dx += step) {
            for (int dz = -r; dz <= r; dz += step) {
                for (int dy = -3; dy <= 4; dy++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (this.isTame() && this.command == BeeCommand.WORK && this.workOrigin != null) {
                        if (!p.closerThan(this.workOrigin, WORK_AREA_RADIUS)) {
                            continue;
                        }
                    }
                    if (!isHiveUsable(p)) {
                        continue;
                    }
                    double d = p.distSqr(origin);
                    if (hiveHoneyLevel(p) < 5) {
                        if (d < bestPref) {
                            bestPref = d;
                            preferred = p.immutable();
                        }
                    } else if (d < bestFall) {
                        bestFall = d;
                        fallback = p.immutable();
                    }
                }
            }
        }
        return preferred != null ? preferred : fallback;
    }

    @Nullable
    BlockPos findFlowerProgressive() {
        if (this.searchFailCooldown > 0 && this.rememberedFlower == null) {
            return null;
        }
        BlockPos origin = this.blockPosition();
        if (this.isTame() && this.command == BeeCommand.WORK && this.workOrigin != null) {
            if (!origin.closerThan(this.workOrigin, WORK_AREA_RADIUS + 2)) {
                return null;
            }
        }

        int r = this.searchRadius;
        this.debugSearch("flor", r);
        BlockPos found = scanFlowerInRadius(origin, r);
        if (found != null) {
            this.searchRadius = SEARCH_RADIUS_MIN;
            this.debugSearch("flor OK " + found.toShortString(), r);
            return found;
        }

        if (this.searchRadius < SEARCH_RADIUS_MAX) {
            this.searchRadius = Math.min(SEARCH_RADIUS_MAX, this.searchRadius + SEARCH_RADIUS_STEP);
            return null;
        }

        this.searchRadius = SEARCH_RADIUS_MIN;
        if (this.isTame() && this.command == BeeCommand.WORK) {
            this.searchFailCooldown = 20 * 10;
        }
        return null;
    }

    @Nullable
    private BlockPos scanFlowerInRadius(BlockPos origin, int r) {
        BlockPos found = null;
        double best = Double.MAX_VALUE;
        int step = r <= 6 ? 1 : 2;
        for (int dx = -r; dx <= r; dx += step) {
            for (int dz = -r; dz <= r; dz += step) {
                for (int dy = -2; dy <= 3; dy++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!this.level().getBlockState(p).is(BlockTags.FLOWERS)) {
                        continue;
                    }
                    if (this.isTame() && this.command == BeeCommand.WORK && this.workOrigin != null) {
                        // Nueva búsqueda solo dentro del área de trabajo
                        if (!p.closerThan(this.workOrigin, WORK_AREA_RADIUS)) {
                            continue;
                        }
                    }
                    double d = p.distSqr(origin);
                    if (d < best) {
                        best = d;
                        found = p.immutable();
                    }
                }
            }
        }
        return found;
    }

    /** Intenta marcar néctar vanilla (package-private) para que el panal suba miel. */

    /**
     * Salvaje: (1) agro solo por radio al panal; (2) si lejos del panal, volver.
     * Recheck de existencia del panal sigue en HIVE_RECHECK_INTERVAL (60 s).
     */
    private void tickWildHiveGuardAndReturn() {
        if (this.isTame() || this.level().isClientSide()) {
            return;
        }
        BlockPos hive = this.rememberedHive != null ? this.rememberedHive : this.getHivePos();
        if (hive == null) {
            return;
        }

        // Agro: si el target actual está a >10 del panal, soltar (no usar distancia a la abeja)
        LivingEntity target = this.getTarget();
        if (target != null && !target.blockPosition().closerThan(hive, 10.0D)) {
            this.setTarget(null);
            try { this.stopBeingAngry(); } catch (Exception ignored) {}
            try { this.forgetCurrentTargetAndRefreshUniversalAnger(); } catch (Exception ignored) {}
            this.getNavigation().moveTo(hive.getX() + 0.5D, hive.getY() + 1.0D, hive.getZ() + 0.5D, 1.0D);
        }

        // Si no hay combate y está lejos del panal (>16), regresar a patrullar
        if (this.getTarget() == null && !this.blockPosition().closerThan(hive, 16.0D)) {
            if (this.tickCount % 20 == 0) {
                this.getNavigation().moveTo(hive.getX() + 0.5D, hive.getY() + 1.0D, hive.getZ() + 0.5D, 0.9D);
            }
        }
    }

    public void markNectarForHive() {
        this.carryingNectar = true;
        try {
            var m = Bee.class.getDeclaredMethod("setHasNectar", boolean.class);
            m.setAccessible(true);
            m.invoke(this, true);
        } catch (Exception ignored) {
        }
    }

    public void applyHiveExitState(int commandOrdinal) {
        applyHiveExitState(commandOrdinal, null, null);
    }

    /**
     * Restaura estado tras salir del panal.
     * Conserva el comando previo (p.ej. WORK) y el ancla de trabajo.
     */
    public void applyHiveExitState(int commandOrdinal, BlockPos workOrigin, BlockPos hive) {
        this.hiveReenterCooldown = 400;
        this.clearNectarFlag();
        this.defending = false;
        this.flyingToStay = false;
        this.setOrderedToSit(false);

        if (!this.isTame()) {
            return;
        }

        BeeCommand[] vals = BeeCommand.values();
        BeeCommand restored = vals[Mth.clamp(commandOrdinal, 0, vals.length - 1)];
        // Preferir WORK si venía de trabajar / ordinal WORK
        if (restored == BeeCommand.WORK || commandOrdinal == BeeCommand.WORK.ordinal()) {
            this.command = BeeCommand.WORK;
            if (workOrigin != null) {
                this.workOrigin = workOrigin.immutable();
            } else if (this.workOrigin == null) {
                this.workOrigin = (hive != null ? hive : this.blockPosition()).immutable();
            }
            if (hive != null) {
                this.rememberedHive = hive.immutable();
                this.setHivePos(hive);
            }
            this.searchRadius = SEARCH_RADIUS_MIN;
            this.searchFailCooldown = 0;
        } else {
            this.command = restored;
            if (restored == BeeCommand.STAY) {
                this.setOrderedToSit(true);
                this.flyingToStay = true;
            } else if (restored == BeeCommand.WANDER) {
                this.wanderHome = this.blockPosition();
            }
        }
    }

    public void setHiveReenterCooldown(int ticks) {
        this.hiveReenterCooldown = ticks;
    }

    
    /** Carga parcial desde NBT del nido si entity.load falla. */
    public void loadFromNestTag(net.minecraft.nbt.CompoundTag data) {
        if (data.contains("SpdCommand")) {
            int cmd = data.getIntOr("SpdCommand", 0);
            BeeCommand[] vals = BeeCommand.values();
            if (cmd >= 0 && cmd < vals.length) {
                this.command = vals[cmd];
            }
        }
        if (data.contains("SpdWorkX")) {
            this.workOrigin = new BlockPos(
                    data.getIntOr("SpdWorkX", 0),
                    data.getIntOr("SpdWorkY", 0),
                    data.getIntOr("SpdWorkZ", 0));
        } else if (data.contains("WorkOX")) {
            this.workOrigin = new BlockPos(
                    data.getIntOr("WorkOX", 0),
                    data.getIntOr("WorkOY", 0),
                    data.getIntOr("WorkOZ", 0));
        }
        this.carryingNectar = data.getBooleanOr("HasNectar", false)
                || data.getBooleanOr("CarryingNectar", false);
    }

    public BlockPos getWorkOriginPublic() {
        return this.workOrigin;
    }

    /** Alias usado por GoldenBeehiveBlockEntity. */
    public BlockPos getWorkOrigin() {
        return this.workOrigin;
    }

    public boolean isCarryingNectarPublic() {
        return this.carryingNectar || this.hasNectar();
    }

    public void clearNectarFlag() {
        this.carryingNectar = false;
        try {
            var m = Bee.class.getDeclaredMethod("setHasNectar", boolean.class);
            m.setAccessible(true);
            m.invoke(this, false);
        } catch (Exception ignored) {
        }
    }

    private void debugSearch(String what, int r) {
        // DEBUG desactivado (dejar para pruebas futuras)
        /*
        if (this.tickCount % 40 != 0) {
            return;
        }
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        String mode = this.isTame() ? ("tame/" + this.command) : "wild";
        String msg = "§e[GB-search]§r " + what + " r=" + r + " mode=" + mode
                + " workAi=" + this.canUseBeeWorkAi();
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16))) {
            if (p instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(msg));
            }
        }
        */
    }

    private void tickExpandSearch() {
        if (!this.canUseBeeWorkAi()) {
            return;
        }
        if (this.searchFailCooldown > 0) {
            return;
        }
        if (this.tickCount % 40 != 0) {
            return;
        }
        boolean needHive = this.carryingNectar || this.hasNectar()
                || this.level().isRaining() || this.level().getSkyDarken() >= 4;
        boolean needFlower = this.isTame() && this.command == BeeCommand.WORK
                && !this.carryingNectar && !this.hasNectar();
        if (needHive && this.rememberedHive == null) {
            BlockPos h = this.findHiveProgressive();
            if (h != null) {
                this.rememberedHive = h;
                this.setHivePos(h);
            }
        } else if (needFlower && this.rememberedFlower == null) {
            BlockPos f = this.findFlowerProgressive();
            if (f != null) {
                this.rememberedFlower = f;
            }
        }
    }

    private void tickKeepFlying() {
        if (this.isOrderedToSit() && this.command == BeeCommand.STAY) {
            return;
        }
        // Empuje ligero hacia arriba si está en el suelo y no debe aterrizar
        if (this.onGround() && this.getTarget() == null) {
            boolean shouldHover = !this.isTame()
                    || this.command == BeeCommand.FOLLOW
                    || this.command == BeeCommand.WORK
                    || this.command == BeeCommand.WANDER
                    || this.defending;
            if (shouldHover) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.12D, 0.0D));
            }
        }
    }

    private void tickAmbient() {
        if (this.ambientCooldown > 0) {
            this.ambientCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 0.6F, 1.0F);
            this.ambientCooldown = 40 + this.getRandom().nextInt(30);
        } else if (this.getRandom().nextInt(200) == 0) {
            // Mucho menos frecuente que vanilla
            this.playSound(SoundEvents.BEE_LOOP, 0.25F, 1.0F);
            this.ambientCooldown = 160 + this.getRandom().nextInt(120);
        }
    }

    /** Mensajes de depuración de IA (desactivados; descomentar para pruebas). */
    private void tickDebugGoals() {
        // DEBUG desactivado: no spamear chat. Mensajes de comando/muerte siguen activos.
        if (this.debugCooldown > 0) {
            this.debugCooldown--;
            return;
        }
        this.debugCooldown = 200;
        /*
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        boolean work = this.canUseBeeWorkAi();
        String mode = this.isTame() ? ("tame/" + this.command) : "wild";
        String defMode = !this.defending ? "off"
                : (this.commandBeforeDefend == BeeCommand.FOLLOW ? "active" : "passive");
        String hive = this.rememberedHive != null ? this.rememberedHive.toShortString()
                : (this.getHivePos() != null ? this.getHivePos().toShortString() : "none");
        String flower = this.rememberedFlower != null ? this.rememberedFlower.toShortString() : "none";
        String workO = this.workOrigin != null ? this.workOrigin.toShortString() : "none";
        String msg = "§e[GB]§r " + mode
                + " workAi=" + work
                + " hive=" + hive
                + " flower=" + flower
                + " nectar=" + this.carryingNectar
                + " rSearch=" + this.searchRadius
                + " workOrigin=" + workO
                + " sit=" + this.isOrderedToSit()
                + " def=" + defMode;
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16))) {
            if (p instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(msg));
            }
        }
        */
    }

    private void tickOwnerDefense() {
        if (!this.isTame()) {
            return;
        }
        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }

        // 1) Dueño ES atacado → salir a defender (cualquier estado)
        LivingEntity threat = owner.getLastHurtByMob();
        if (threat != null && threat.isAlive() && threat != this
                && !(threat instanceof Bee)
                && !(threat instanceof Creeper)
                && this.canAttack(threat)
                && this.distanceToSqr(threat) < 16.0D * 16.0D) {

            if (!this.defending) {
                this.commandBeforeDefend = this.command;
                this.defending = true;
                this.setOrderedToSit(false);
                this.flyingToStay = false;
            }
            this.setTarget(threat);
            return;
        }

        // 2) Dueño ATACA → solo FOLLOW ayuda (defend activo)
        LivingEntity ownerTarget = owner.getLastHurtMob();
        if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != this
                && !(ownerTarget instanceof Bee)
                && !(ownerTarget instanceof Creeper)
                && this.canAttack(ownerTarget)
                && this.distanceToSqr(ownerTarget) < 16.0D * 16.0D) {
            if (this.command == BeeCommand.FOLLOW || this.defending) {
                if (this.command == BeeCommand.FOLLOW && !this.defending) {
                    this.commandBeforeDefend = this.command;
                    this.defending = true;
                    this.setOrderedToSit(false);
                    this.flyingToStay = false;
                }
                this.setTarget(ownerTarget);
            }
            return;
        }

        if (this.defending) {
            LivingEntity t = this.getTarget();
            if (t == null || !t.isAlive() || this.distanceToSqr(t) > 20.0D * 20.0D) {
                this.defending = false;
                this.setTarget(null);
                BeeCommand restore = this.commandBeforeDefend != null
                        ? this.commandBeforeDefend
                        : BeeCommand.FOLLOW;
                this.commandBeforeDefend = null;
                this.command = restore;
                if (restore == BeeCommand.STAY) {
                    this.setOrderedToSit(true);
                    this.flyingToStay = true;
                } else {
                    this.setOrderedToSit(false);
                }
            }
        }
    }

    private void teleportToOwner(LivingEntity owner) {
        for (int i = 0; i < 10; i++) {
            double ox = owner.getX() + (this.getRandom().nextDouble() - 0.5D) * 4.0D;
            double oy = owner.getY() + this.getRandom().nextInt(3);
            double oz = owner.getZ() + (this.getRandom().nextDouble() - 0.5D) * 4.0D;
            BlockPos pos = BlockPos.containing(ox, oy, oz);
            if (this.level().noCollision(this, this.getBoundingBox().move(
                    ox - this.getX(), oy - this.getY(), oz - this.getZ()))) {
                this.snapTo(ox, oy, oz, this.getYRot(), this.getXRot());
                this.navigation.stop();
                return;
            }
        }
    }

    private void changeDimensionToOwner(ServerLevel dest, LivingEntity owner) {
        this.command = BeeCommand.FOLLOW;
        this.setOrderedToSit(false);
        this.defending = false;
        TeleportTransition transition = new TeleportTransition(
                dest,
                owner.position(),
                Vec3.ZERO,
                owner.getYRot(),
                owner.getXRot(),
                TeleportTransition.DO_NOTHING
        );
        this.teleport(transition);
    }

    // -------------------------------------------------------------------------
    
    private static final int HIVE_RECHECK_INTERVAL = 1200;
    private int hiveRecheckCooldown;

    private void tickWildHiveAnchor() {
        if (this.hiveRecheckCooldown > 0) this.hiveRecheckCooldown--;
        if (this.rememberedHive != null) {
            if (this.hiveRecheckCooldown <= 0) {
                this.hiveRecheckCooldown = HIVE_RECHECK_INTERVAL;
                if (!isProtectableHiveBlock(this.rememberedHive)) {
                    this.rememberedHive = null;
                }
            }
        }
        if (this.rememberedHive == null) {
            // Buscar panal protegible (vanilla o golden) para patrullar — no entrar
            BlockPos origin = this.blockPosition();
            int r = this.searchRadius;
            BlockPos found = null;
            double best = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -6, -r), origin.offset(r, 6, r))) {
                if (!isProtectableHiveBlock(p)) continue;
                double d = p.distSqr(origin);
                if (d < best) { best = d; found = p.immutable(); }
            }
            if (found != null) {
                this.rememberedHive = found;
                this.searchRadius = 8;
                this.hiveRecheckCooldown = HIVE_RECHECK_INTERVAL;
            } else if (this.searchRadius < 32) {
                this.searchRadius++;
            } else {
                this.searchRadius = 8;
            }
        }
    }


    // Combate / targets
    // -------------------------------------------------------------------------

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Bee) {
            return false;
        }
        if (target instanceof Creeper) {
            return false;
        }
        if (this.isTame() && this.isOwnedBy(target)) {
            return false;
        }
        // No atacar mascotas del dueño ni miembros del mismo equipo
        if (this.isTame()) {
            LivingEntity owner = this.getOwner();
            if (owner != null) {
                if (target instanceof TamableAnimal tamable && tamable.isTame()) {
                    LivingEntity otherOwner = tamable.getOwner();
                    if (otherOwner != null && otherOwner.getUUID().equals(owner.getUUID())) {
                        return false;
                    }
                }
                // Otras Golden Bee del mismo dueño
                if (target instanceof GoldenBeeEntity otherBee && otherBee.isTame()) {
                    UUID otherOwnerId = otherBee.getOwnerUUID();
                    if (otherOwnerId != null && otherOwnerId.equals(owner.getUUID())) {
                        return false;
                    }
                }
                if (target.isAlliedTo(owner) || owner.isAlliedTo(target)) {
                    return false;
                }
            }
        }
        return super.canAttack(target);
    }

    @Override
    public boolean isAngryAt(LivingEntity target, ServerLevel level) {
        if (target instanceof Bee || target instanceof Creeper) {
            return false;
        }
        if (this.isTame() && this.isOwnedBy(target)) {
            return false;
        }
        if (!this.canAttack(target)) {
            return false;
        }
        // Equivalente al default de NeutralMob sin NeutralMob.super
        if (target instanceof Player && this.isAngryAtAllPlayers(level)) {
            return true;
        }
        return this.isAngry() && this.getTarget() == target;
    }

    /**
     * Ataque sin veneno y sin suicidio post-aguijón de la abeja vanilla.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean ok = target.hurtServer(level, this.damageSources().mobAttack(this), dmg);
        if (ok) {
            this.setLastHurtMob(target instanceof LivingEntity le ? le : null);
        }
        return ok;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() == this) {
            return false;
        }
        String mid = source.getMsgId();
        if (mid != null && mid.contains("sting")) {
            return false;
        }
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && !(living instanceof Bee)) {
            // Alerta abejas cercanas (vanilla + golden)
            AABB box = this.getBoundingBox().inflate(8.0D, 6.0D, 8.0D);
            List<Bee> bees = level.getEntitiesOfClass(Bee.class, box);
            for (Bee other : bees) {
                if (other != this) {
                    other.setLastHurtByMob(living);
                    other.setTarget(living);
                }
            }
        }
        if (this.isOrderedToSit()) {
            this.setOrderedToSit(false);
            this.flyingToStay = false;
        }
        return super.hurtServer(level, source, amount);
    }

    // -------------------------------------------------------------------------
    // Interacción
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide()) {
            if (isHoneyedHealingPotion(stack) || this.isFood(stack)) {
                return InteractionResult.SUCCESS;
            }
            if (this.isTame() && this.isOwnedBy(player)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // --- Honeyed Healing: domesticar si salvaje + aplicar efecto ---
        if (isHoneyedHealingPotion(stack)) {
            if (!this.isTame()) {
                tameBy(player);
            }
            int amp = 0;
            int dur = 3600;
            PotionContents pc = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            for (MobEffectInstance inst : pc.getAllEffects()) {
                if (inst.is(ModEffects.HONEYED_HEALING)) {
                    amp = inst.getAmplifier();
                    dur = Math.max(20, inst.getDuration());
                    break;
                }
            }
            this.getPersistentData().putInt(
                    com.drbenja.spd.effect.HoneyedHealingEffect.KEY_INCOMING_DURATION, dur);
            this.getPersistentData().putInt(
                    com.drbenja.spd.effect.HoneyedHealingEffect.KEY_INCOMING_AMP, amp);
            this.addEffect(new MobEffectInstance(
                    ModEffects.HONEYED_HEALING, dur, amp, false, true, true));
            if (this.getTarget() == player) {
                this.setTarget(null);
            }
            consumePotion(player, stack);
            this.level().broadcastEntityEvent(this, (byte) 7);
            return InteractionResult.SUCCESS;
        }

        // --- Flor ---
        if (this.isFood(stack)) {
            // Bebé: acelerar crecimiento
            if (this.isBaby() && this.getAge() < 0) {
                this.usePlayerItem(player, hand, stack);
                this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
                return InteractionResult.SUCCESS;
            }

            if (!this.isTame()) {
                // Salvaje: solo amor si no está en amor ni en cooldown de cría
                if (this.isInLove() || this.getAge() > 0) {
                    return InteractionResult.FAIL; // no gasta flor
                }
                if (this.getAge() == 0 && this.canFallInLove()) {
                    this.usePlayerItem(player, hand, stack);
                    this.setInLove(player);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            }

            // Domesticada: curar > amor > cambiar estado
            if (this.getHealth() < this.getMaxHealth() - 0.01F) {
                this.heal(1.0F);
                this.usePlayerItem(player, hand, stack);
                this.level().broadcastEntityEvent(this, (byte) 7);
                return InteractionResult.SUCCESS;
            }
            if (this.isInLove() || this.getAge() > 0) {
                // No puede criar → cambiar estado (no gasta flor)
                if (this.isOwnedBy(player) && this.interactCooldown <= 0) {
                    this.interactCooldown = 10;
                    cycleCommand(player);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            }
            if (this.getAge() == 0 && this.canFallInLove()) {
                this.usePlayerItem(player, hand, stack);
                this.setInLove(player);
                return InteractionResult.SUCCESS;
            }
            // Vida full y no puede criar → ciclo de estado
            if (this.isOwnedBy(player) && this.interactCooldown <= 0) {
                this.interactCooldown = 10;
                cycleCommand(player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        // --- Cualquier otro item o mano vacía ---
        if (this.isTame() && this.isOwnedBy(player) && this.interactCooldown <= 0) {
            this.interactCooldown = 10;
            cycleCommand(player);
            return InteractionResult.SUCCESS;
        }

        // Salvaje + otro item: nada
        return InteractionResult.PASS;
    }


    @Override
    public void die(DamageSource source) {
        // Solo dueño (como lobo): mensaje con causa via CombatTracker
        if (!this.level().isClientSide() && this.isTame()) {
            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer sp) {
                sp.sendSystemMessage(this.getCombatTracker().getDeathMessage());
            }
        }
        super.die(source);
    }

    private void cycleCommand(Player player) {
        this.defending = false;
        this.commandBeforeDefend = null;
        this.flyingToStay = false;
        this.setTarget(null);

        switch (this.command) {
            case FOLLOW -> {
                this.command = BeeCommand.STAY;
                this.setOrderedToSit(true);
                this.flyingToStay = true;
                this.stayAnchor = null;
                speakTo(player, BeeCommand.STAY);
            }
            case STAY -> {
                this.command = BeeCommand.WANDER;
                this.setOrderedToSit(false);
                this.wanderHome = this.blockPosition();
                speakTo(player, BeeCommand.WANDER);
            }
            case WANDER -> {
                this.command = BeeCommand.WORK;
                this.setOrderedToSit(false);
                this.wanderHome = null;
                this.workOrigin = this.blockPosition().immutable();
                this.searchRadius = SEARCH_RADIUS_MIN;
                this.searchFailCooldown = 0;
                // Mantener memoria si aún es válida; si no, buscar ya
                if (this.rememberedFlower != null
                        && !this.level().getBlockState(this.rememberedFlower).is(BlockTags.FLOWERS)) {
                    this.rememberedFlower = null;
                }
                if (this.rememberedHive != null && !this.isHiveUsable(this.rememberedHive)) {
                    this.rememberedHive = null;
                }
                if (this.rememberedHive == null) {
                    BlockPos h = this.findHiveProgressive();
                    if (h != null) {
                        this.rememberedHive = h;
                        this.setHivePos(h);
                    }
                }
                if (this.rememberedFlower == null) {
                    BlockPos f = this.findFlowerProgressive();
                    if (f != null) {
                        this.rememberedFlower = f;
                    }
                }
                speakTo(player, BeeCommand.WORK);
            }
            case WORK -> {
                this.command = BeeCommand.FOLLOW;
                this.setOrderedToSit(false);
                this.setHivePos(null);
                this.workOrigin = null;
                this.rememberedFlower = null;
                this.rememberedHive = null;
                speakTo(player, BeeCommand.FOLLOW);
            }
        }
    }

    private void speakTo(Player player, BeeCommand cmd) {
        int max;
        String prefix;
        switch (cmd) {
            case FOLLOW -> { prefix = "spd.golden_bee.cmd.follow."; max = 5; }
            case STAY -> { prefix = "spd.golden_bee.cmd.stay."; max = 5; }
            case WANDER -> { prefix = "spd.golden_bee.cmd.wander."; max = 5; }
            case WORK -> { prefix = "spd.golden_bee.cmd.work."; max = 10; }
            default -> { prefix = "spd.golden_bee.cmd.follow."; max = 5; }
        }
        // Work 1-5 comunes; 6-10 raros (~15%)
        int n;
        if (cmd == BeeCommand.WORK) {
            if (this.getRandom().nextFloat() < 0.15F) {
                n = 6 + this.getRandom().nextInt(5); // 6-10
            } else {
                n = 1 + this.getRandom().nextInt(5); // 1-5
            }
        } else {
            n = 1 + this.getRandom().nextInt(max);
        }
        if (player instanceof ServerPlayer sp) {
            net.minecraft.network.chat.MutableComponent msg = Component.translatable(
                    "spd.golden_bee.cmd.label." + cmd.name().toLowerCase())
                    .withStyle(net.minecraft.ChatFormatting.GOLD);
            msg.append(Component.literal(": ").withStyle(net.minecraft.ChatFormatting.GRAY));
            msg.append(Component.translatable(prefix + n).withStyle(net.minecraft.ChatFormatting.YELLOW));
            sp.sendSystemMessage(msg);
        }
    }

    private void msg(Player player, String text) {
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(text));
        }
    }

    public boolean canAcceptFlower() {
        if (this.isBaby()) {
            return true;
        }
        if (this.isTame()) {
            if (this.getHealth() < this.getMaxHealth() - 0.01F) {
                return true;
            }
            return this.getAge() == 0 && this.canFallInLove();
        }
        return this.getAge() == 0 && this.canFallInLove();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.BEE_FOOD);
    }

    public static boolean isHoneyedHealingPotion(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance inst : contents.getAllEffects()) {
            if (inst.is(ModEffects.HONEYED_HEALING)) {
                return true;
            }
        }
        return contents.is(ModPotions.HONEYED_HEALING_0)
                || contents.is(ModPotions.HONEYED_HEALING_EXT)
                || contents.is(ModPotions.HONEYED_HEALING_1)
                || contents.is(ModPotions.HONEYED_HEALING_2)
                || contents.is(ModPotions.HONEYED_HEALING_1_EXT);
    }

    private static void consumePotion(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }
        boolean drinkable = stack.is(Items.POTION);
        stack.shrink(1);
        if (drinkable) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), bottle);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cría → abeja vanilla
    // -------------------------------------------------------------------------

    @Override
    public Bee getBreedOffspring(ServerLevel level, AgeableMob other) {
        boolean partnerGolden = other instanceof GoldenBeeEntity;
        boolean makeGolden;
        if (partnerGolden) {
            // Golden x Golden → siempre Golden
            makeGolden = true;
        } else if (other instanceof Bee) {
            // Golden x abeja vanilla → 50%
            makeGolden = this.getRandom().nextBoolean();
        } else {
            makeGolden = false;
        }

        Bee baby;
        if (makeGolden) {
            baby = ModEntities.GOLDEN_BEE.get().create(level, EntitySpawnReason.BREEDING);
        } else {
            baby = EntityTypes.BEE.create(level, EntitySpawnReason.BREEDING);
        }
        if (baby != null) {
            baby.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        }
        return baby;
    }

    @Override
    public boolean canMate(Animal other) {
        if (other == this) {
            return false;
        }
        // Cualquier Bee (vanilla o Golden)
        if (!(other instanceof Bee)) {
            return false;
        }
        return this.isInLove() && other.isInLove();
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        AgeableMob baby = this.getBreedOffspring(level, partner);
        if (baby != null) {
            baby.setBaby(true);
            baby.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            level.addFreshEntityWithPassengers(baby);
            level.addFreshEntity(new ExperienceOrb(level, this.getX(), this.getY(), this.getZ(),
                    1 + this.getRandom().nextInt(7)));
        }
        this.setAge(6000);
        partner.setAge(6000);
        this.resetLove();
        partner.resetLove();
    }

    // -------------------------------------------------------------------------
    // Sonidos / inmunidad
    // -------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getRandom().nextBoolean() ? SoundEvents.BEE_LOOP : SoundEvents.BEE_POLLINATE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 80 + this.getRandom().nextInt(60);
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.getMsgId().equals("sting")) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.is(net.minecraft.world.effect.MobEffects.POISON)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Tame", this.isTame());
        UUID owner = getOwnerUUID();
        if (owner != null) {
            output.putString("OwnerUUID", owner.toString());
        }
        output.putBoolean("Sitting", this.isOrderedToSit());
        output.putInt("BeeCommand", this.command.ordinal());
        output.putBoolean("CarryingNectar", this.carryingNectar);
        if (this.rememberedFlower != null) {
            output.putInt("MemFlowerX", this.rememberedFlower.getX());
            output.putInt("MemFlowerY", this.rememberedFlower.getY());
            output.putInt("MemFlowerZ", this.rememberedFlower.getZ());
        }
        if (this.rememberedHive != null) {
            output.putInt("MemHiveX", this.rememberedHive.getX());
            output.putInt("MemHiveY", this.rememberedHive.getY());
            output.putInt("MemHiveZ", this.rememberedHive.getZ());
        }
        if (this.workOrigin != null) {
            output.putInt("WorkOX", this.workOrigin.getX());
            output.putInt("WorkOY", this.workOrigin.getY());
            output.putInt("WorkOZ", this.workOrigin.getZ());
        }
        output.putInt("SearchRadius", this.searchRadius);
        if (this.wanderHome != null) {
            output.putInt("WanderX", this.wanderHome.getX());
            output.putInt("WanderY", this.wanderHome.getY());
            output.putInt("WanderZ", this.wanderHome.getZ());
        }
        if (this.stayAnchor != null) {
            output.putInt("StayX", this.stayAnchor.getX());
            output.putInt("StayY", this.stayAnchor.getY());
            output.putInt("StayZ", this.stayAnchor.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setTame(input.getBooleanOr("Tame", false));
        input.getString("OwnerUUID").ifPresent(s -> {
            try {
                this.setOwnerUUID(UUID.fromString(s));
            } catch (Exception ignored) {
            }
        });
        this.setOrderedToSit(input.getBooleanOr("Sitting", false));
        int cmd = input.getIntOr("BeeCommand", 0);
        BeeCommand[] vals = BeeCommand.values();
        this.command = vals[Mth.clamp(cmd, 0, vals.length - 1)];
        this.carryingNectar = input.getBooleanOr("CarryingNectar", false);
        if (input.getInt("MemFlowerX").isPresent()) {
            this.rememberedFlower = new BlockPos(
                    input.getIntOr("MemFlowerX", 0),
                    input.getIntOr("MemFlowerY", 0),
                    input.getIntOr("MemFlowerZ", 0));
        }
        if (input.getInt("MemHiveX").isPresent()) {
            this.rememberedHive = new BlockPos(
                    input.getIntOr("MemHiveX", 0),
                    input.getIntOr("MemHiveY", 0),
                    input.getIntOr("MemHiveZ", 0));
        }
        if (input.getInt("WorkOX").isPresent()) {
            this.workOrigin = new BlockPos(
                    input.getIntOr("WorkOX", 0),
                    input.getIntOr("WorkOY", 0),
                    input.getIntOr("WorkOZ", 0));
        }
        this.searchRadius = input.getIntOr("SearchRadius", SEARCH_RADIUS_MIN);
        if (input.getInt("WanderX").isPresent()) {
            this.wanderHome = new BlockPos(
                    input.getIntOr("WanderX", 0),
                    input.getIntOr("WanderY", 0),
                    input.getIntOr("WanderZ", 0));
        }
        if (input.getInt("StayX").isPresent()) {
            this.stayAnchor = new BlockPos(
                    input.getIntOr("StayX", 0),
                    input.getIntOr("StayY", 0),
                    input.getIntOr("StayZ", 0));
        }
    }

    public static void init(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.GOLDEN_BEE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    // =========================================================================
    // Goals internos
    // =========================================================================

    /**
     * Cría con otra Golden Bee o con Bee vanilla (50/50 → Golden / vanilla vía getBreedOffspring).
     */
    static class GoldenBreedGoal extends BreedGoal {
        private final GoldenBeeEntity bee;

        GoldenBreedGoal(GoldenBeeEntity bee, double speed) {
            super(bee, speed);
            this.bee = bee;
        }

        @Override
        public boolean canUse() {
            if (bee.isOrderedToSit() || bee.defending) {
                return false;
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (bee.isOrderedToSit() || bee.defending) {
                return false;
            }
            return super.canContinueToUse();
        }
    }

    static class GoldenSitGoal extends Goal {
        private final GoldenBeeEntity bee;

        GoldenSitGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bee.isOrderedToSit() && !bee.flyingToStay && !bee.defending;
        }

        @Override
        public void start() {
            bee.getNavigation().stop();
            bee.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void tick() {
            bee.getNavigation().stop();
            bee.setDeltaMovement(bee.getDeltaMovement().multiply(0, 0, 0));
        }
    }

    static class SafeLandWhenSittingGoal extends Goal {
        private final GoldenBeeEntity bee;
        @Nullable
        private BlockPos target;

        SafeLandWhenSittingGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!bee.isOrderedToSit() || !bee.flyingToStay || bee.defending) {
                return false;
            }
            this.target = findSafeLand();
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return bee.isOrderedToSit() && bee.flyingToStay && this.target != null
                    && !bee.blockPosition().closerThan(this.target, 1.5D);
        }

        @Override
        public void start() {
            if (this.target != null) {
                bee.getNavigation().moveTo(
                        this.target.getX() + 0.5D,
                        this.target.getY() + 1.0D,
                        this.target.getZ() + 0.5D,
                        1.0D);
            }
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            if (bee.blockPosition().closerThan(this.target, 1.5D)
                    || bee.onGround()) {
                bee.flyingToStay = false;
                bee.getNavigation().stop();
                bee.stayAnchor = this.target.immutable();
            } else if (!bee.getNavigation().isInProgress()) {
                bee.getNavigation().moveTo(
                        this.target.getX() + 0.5D,
                        this.target.getY() + 1.0D,
                        this.target.getZ() + 0.5D,
                        1.0D);
            }
        }

        @Nullable
        private BlockPos findSafeLand() {
            BlockPos origin = bee.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = 3; dy >= -6; dy--) {
                        BlockPos p = origin.offset(dx, dy, dz);
                        if (!isSafeLand(p)) {
                            continue;
                        }
                        double d = p.distSqr(origin);
                        if (d < bestDist) {
                            bestDist = d;
                            best = p.immutable();
                        }
                    }
                }
            }
            return best;
        }

        private boolean isSafeLand(BlockPos p) {
            Level level = bee.level();
            BlockState below = level.getBlockState(p);
            BlockState feet = level.getBlockState(p.above());
            BlockState head = level.getBlockState(p.above(2));
            if (!below.isSolidRender() && !below.is(BlockTags.LEAVES)) {
                return false;
            }
            if (!feet.getCollisionShape(level, p.above()).isEmpty()) {
                return false;
            }
            if (!head.getCollisionShape(level, p.above(2)).isEmpty()) {
                return false;
            }
            if (below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CACTUS)
                    || below.is(Blocks.SWEET_BERRY_BUSH) || below.is(Blocks.WITHER_ROSE)
                    || below.is(Blocks.FIRE) || below.is(Blocks.LAVA)
                    || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) {
                return false;
            }
            if (!level.getFluidState(p.above()).isEmpty()) {
                return false;
            }
            return true;
        }
    }

    static class ConditionalFollowOwnerGoal extends Goal {
        private final GoldenBeeEntity bee;
        private int timeToRecalc;
        private boolean hovering;

        ConditionalFollowOwnerGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!bee.isTame() || bee.command != BeeCommand.FOLLOW || bee.defending
                    || bee.isOrderedToSit()) {
                return false;
            }
            if (bee.getTarget() != null) {
                return false;
            }
            LivingEntity owner = bee.getOwner();
            return owner != null && owner.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.hovering = false;
            this.timeToRecalc = 0;
        }

        @Override
        public void tick() {
            LivingEntity owner = bee.getOwner();
            if (owner == null) {
                return;
            }
            double distSq = bee.distanceToSqr(owner);
            // Teleport si muy lejos
            if (distSq > 256.0D) { // 16 bloques
                bee.teleportToOwner(owner);
                this.hovering = false;
                return;
            }
            bee.getLookControl().setLookAt(owner, 10.0F, bee.getMaxHeadXRot());

            // >= 6 bloques: seguir activo
            if (distSq > 36.0D) {
                this.hovering = false;
                if (--this.timeToRecalc <= 0) {
                    this.timeToRecalc = 8;
                    bee.getNavigation().moveTo(owner.getX(), owner.getY() + 1.2D, owner.getZ(), 1.15D);
                }
            } else if (distSq > 16.0D) {
                // 4-6: acercarse suavemente
                this.hovering = false;
                if (--this.timeToRecalc <= 0) {
                    this.timeToRecalc = 10;
                    bee.getNavigation().moveTo(owner.getX(), owner.getY() + 1.2D, owner.getZ(), 1.0D);
                }
            } else {
                // <= 4 bloques: deambular en radio 5 alrededor del dueño
                this.hovering = true;
                if (--this.timeToRecalc <= 0) {
                    this.timeToRecalc = 25 + bee.getRandom().nextInt(20);
                    double ang = bee.getRandom().nextDouble() * Math.PI * 2;
                    double rad = 1.5D + bee.getRandom().nextDouble() * 3.5D; // hasta ~5
                    double tx = owner.getX() + Math.cos(ang) * rad;
                    double ty = owner.getY() + 1.0D + bee.getRandom().nextDouble() * 0.8D;
                    double tz = owner.getZ() + Math.sin(ang) * rad;
                    bee.getNavigation().moveTo(tx, ty, tz, 0.7D);
                }
            }
            if (bee.onGround()) {
                bee.setDeltaMovement(bee.getDeltaMovement().add(0.0D, 0.12D, 0.0D));
            }
        }
    }

    static class GoldenBeeWanderGoal extends Goal {
        private final GoldenBeeEntity bee;
        private int cooldown;

        GoldenBeeWanderGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (bee.isOrderedToSit() || bee.flyingToStay || bee.defending) {
                return false;
            }
            if (bee.isTame() && bee.command == BeeCommand.FOLLOW) {
                return false;
            }
            if (bee.isTame() && bee.command == BeeCommand.WORK) {
                return false;
            }
            if (bee.isTame() && bee.command != BeeCommand.WANDER && bee.command != BeeCommand.STAY) {
                return false;
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return bee.getRandom().nextInt(20) == 0;
        }

        @Override
        public void start() {
            Vec3 center;
            if (bee.command == BeeCommand.WANDER && bee.wanderHome != null) {
                center = Vec3.atCenterOf(bee.wanderHome);
            } else if (bee.command == BeeCommand.STAY && bee.stayAnchor != null) {
                center = Vec3.atCenterOf(bee.stayAnchor);
            } else {
                center = bee.position();
            }
            Vec3 dir = bee.getViewVector(0.0F);
            Vec3 pos = HoverRandomPos.getPos(bee, 4, 2, dir.x, dir.z, Mth.PI / 2F, 2, 1);
            if (pos == null) {
                pos = AirAndWaterRandomPos.getPos(bee, 4, 2, -1, dir.x, dir.z, Mth.PI / 2F);
            }
            if (pos != null) {
                if (center.distanceToSqr(pos) > 12.0D * 12.0D) {
                    pos = center.add(
                            (bee.getRandom().nextDouble() - 0.5D) * 6.0D,
                            bee.getRandom().nextDouble() * 2.0D,
                            (bee.getRandom().nextDouble() - 0.5D) * 6.0D);
                }
                bee.getNavigation().moveTo(pos.x, pos.y, pos.z, 0.45D);
            }
            this.cooldown = 20 + bee.getRandom().nextInt(20);
        }
    }

    /** Entrar al panal solo si puede trabajar (salvaje / WORK). */
    static class ConditionalBeeEnterHiveGoal extends Goal {
        private final GoldenBeeEntity bee;
        private int stuckTicks;
        private int repathTicks;

        ConditionalBeeEnterHiveGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private static boolean isNight(Level level) {
            // NeoForge 26: getSkyDarken() (no dayTime en Level)
            return level.getSkyDarken() >= 4;
        }

        @Override
        public boolean canUse() {
            if (!bee.canUseBeeWorkAi()) {
                return false;
            }
            if (bee.hiveReenterCooldown > 0) {
                return false;
            }
            Level level = bee.level();
            // Solo entrar con néctar, de noche o con lluvia (no reentrar en vacío de día)
            boolean want = isNight(level) || level.isRaining()
                    || bee.carryingNectar || bee.hasNectar();
            if (!want) {
                return false;
            }

            // Memoria de panal: no olvidar; preferir miel < 5
            if (bee.rememberedHive != null) {
                if (bee.isHivePreferred(bee.rememberedHive)) {
                    bee.setHivePos(bee.rememberedHive);
                    return true;
                }
                if (bee.isHiveUsable(bee.rememberedHive) && bee.hiveHoneyLevel(bee.rememberedHive) >= 5) {
                    BlockPos alt = bee.findHiveProgressive();
                    if (alt != null && bee.isHivePreferred(alt)) {
                        bee.rememberedHive = alt;
                        bee.setHivePos(alt);
                        return true;
                    }
                    // Sin alternativa mejor: usar el memorizado si no está lleno
                    bee.setHivePos(bee.rememberedHive);
                    return true;
                }
                if (!bee.isHiveUsable(bee.rememberedHive)) {
                    bee.rememberedHive = null;
                    bee.setHivePos(null);
                } else {
                    bee.setHivePos(bee.rememberedHive);
                    return true;
                }
            }

            // Búsqueda progresiva (barata, por anillos)
            BlockPos found = bee.findHiveProgressive();
            if (found == null) {
                return false;
            }
            bee.rememberedHive = found;
            bee.setHivePos(found);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (!bee.canUseBeeWorkAi()) {
                return false;
            }
            BlockPos hive = bee.getHivePos();
            if (hive == null) {
                return false;
            }
            // Solo comprobar existencia/lleno mientras vamos al panal
            return bee.isHiveBlock(hive);
        }

        @Override
        public void start() {
            this.stuckTicks = 0;
            this.repathTicks = 0;
            BlockPos hive = bee.getHivePos();
            if (hive != null) {
                bee.getNavigation().moveTo(hive.getX() + 0.5D, hive.getY() + 0.6D, hive.getZ() + 0.5D, 1.15D);
            }
        }

        @Override
        public void tick() {
            BlockPos hive = bee.getHivePos();
            if (hive == null) {
                hive = bee.rememberedHive;
                if (hive != null) {
                    bee.setHivePos(hive);
                } else {
                    return;
                }
            }

            if (!bee.isHiveUsable(hive)) {
                bee.rememberedHive = null;
                bee.setHivePos(null);
                BlockPos found = bee.findHiveProgressive();
                if (found != null) {
                    bee.rememberedHive = found;
                    bee.setHivePos(found);
                }
                return;
            }

            double hx = hive.getX() + 0.5D;
            double hy = hive.getY() + 0.6D;
            double hz = hive.getZ() + 0.5D;
            // Radio de entrada amplio (3 bloques)
            if (bee.distanceToSqr(hx, hy, hz) < 4.0D) {
                if (bee.level().getBlockEntity(hive) instanceof GoldenBeehiveBlockEntity nest) {
                    if (nest.isFull()) {
                        bee.rememberedHive = null;
                        bee.setHivePos(null);
                        return;
                    }
                    bee.rememberedHive = hive.immutable();
                    bee.setHivePos(hive);
                    nest.tryEnter(bee);
                }
            } else {
                this.stuckTicks++;
                // Re-path cada tick si lleva néctar (prioridad máxima de entrega)
                bee.getNavigation().moveTo(hx, hy, hz, 1.15D);
                bee.getLookControl().setLookAt(hx, hy, hz);
                if (bee.onGround()) {
                    bee.setDeltaMovement(bee.getDeltaMovement().add(0.0D, 0.12D, 0.0D));
                }
                if (this.stuckTicks > 400) {
                    // Teleport-assist cerca del panal si pathfinding falla mucho
                    if (bee.carryingNectar || bee.hasNectar()) {
                        bee.getNavigation().moveTo(hx, hy, hz, 1.3D);
                    }
                    this.stuckTicks = 0;
                }
            }
        }
    }

    /** Polinización simplificada: busca flores, recoge néctar, partículas. */
    static class ConditionalPollinateGoal extends Goal {
        private final GoldenBeeEntity bee;
        @Nullable
        private BlockPos flowerPos;
        private int pollinateTicks;
        /** Ticks intentando acercarse sin avanzar el contador de polinización. */
        private int approachTicks;

        ConditionalPollinateGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!bee.isTame() || bee.getCommand() != BeeCommand.WORK) {
                return false;
            }
            if (!bee.canUseBeeWorkAi()) {
                return false;
            }
            if (bee.carryingNectar || bee.hasNectar()) {
                return false;
            }
            if (bee.rememberedFlower != null) {
                if (bee.level().getBlockState(bee.rememberedFlower).is(BlockTags.FLOWERS)) {
                    this.flowerPos = bee.rememberedFlower;
                    return true;
                }
                bee.rememberedFlower = null;
            }
            if (bee.getRandom().nextInt(10) != 0) {
                return false;
            }
            this.flowerPos = bee.findFlowerProgressive();
            if (this.flowerPos != null) {
                bee.rememberedFlower = this.flowerPos;
            }
            return this.flowerPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!bee.canUseBeeWorkAi()) {
                return false;
            }
            if (bee.carryingNectar || bee.hasNectar()) {
                return false;
            }
            if (this.flowerPos == null) {
                return false;
            }
            if (!bee.level().getBlockState(this.flowerPos).is(BlockTags.FLOWERS)) {
                return false;
            }
            // Abandonar si tarda demasiado en llegar (~10 s) o ya polinizó
            if (this.approachTicks > 200) {
                return false;
            }
            return this.pollinateTicks < 400;
        }

        @Override
        public void start() {
            this.pollinateTicks = 0;
            this.approachTicks = 0;
        }

        @Override
        public void stop() {
            if (this.approachTicks > 200 || this.flowerPos == null) {
                bee.rememberedFlower = null;
            }
            this.flowerPos = null;
            this.pollinateTicks = 0;
            this.approachTicks = 0;
        }

        @Override
        public void tick() {
            if (this.flowerPos == null) {
                return;
            }
            // Ir al centro superior de la flor (como Bee vanilla)
            double fx = this.flowerPos.getX() + 0.5D;
            double fy = this.flowerPos.getY() + 0.7D;
            double fz = this.flowerPos.getZ() + 0.5D;
            bee.getNavigation().moveTo(fx, fy, fz, 1.0D);
            bee.getLookControl().setLookAt(fx, fy, fz);
            // Mantener en vuelo
            if (bee.onGround()) {
                bee.setDeltaMovement(bee.getDeltaMovement().add(0.0D, 0.15D, 0.0D));
            }
            bee.setNoGravity(true);

            double distSq = bee.distanceToSqr(fx, fy, fz);
            // ~3 bloques (9.0) basta para contar
            if (distSq < 9.0D) {
                this.pollinateTicks++;
                this.approachTicks = 0;
                if (this.pollinateTicks % 20 == 0) {
                    bee.playSound(SoundEvents.BEE_POLLINATE, 0.3F, 1.0F);
                }
                if (this.pollinateTicks >= 400) {
                    bee.markNectarForHive();
                    bee.playSound(SoundEvents.BEE_POLLINATE, 0.8F, 1.0F);
                    applyBoneMealNearby();
                    bee.rememberedFlower = null;
                    this.flowerPos = null;
                }
            } else {
                this.approachTicks++;
            }
        }

        private void applyBoneMealNearby() {
            if (!(bee.level() instanceof ServerLevel server)) {
                return;
            }
            BlockPos base = bee.blockPosition();
            for (int i = 0; i < 3; i++) {
                BlockPos p = base.offset(
                        bee.getRandom().nextInt(5) - 2,
                        bee.getRandom().nextInt(3) - 1,
                        bee.getRandom().nextInt(5) - 2);
                BlockState st = server.getBlockState(p);
                if (st.is(BlockTags.CROPS) || st.is(BlockTags.SAPLINGS)) {
                    if (st.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock growable
                            && growable.isValidBonemealTarget(server, p, st, net.minecraft.world.level.block.BonemealSource.MOB)) {
                        growable.performBonemeal(server, bee.getRandom(), p, st, net.minecraft.world.level.block.BonemealSource.MOB);
                        server.levelEvent(1505, p, 15);
                    }
                }
            }
        }
    }

    /** Sigue a jugadores con flor o elixir Honeyed Healing en mano. */
    static class HoneyOrFlowerTemptGoal extends Goal {
        private final GoldenBeeEntity bee;
        private Player tempting;
        private int calm;

        HoneyOrFlowerTemptGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (bee.defending) {
                return false;
            }
            if (bee.isTame() && (bee.command == BeeCommand.FOLLOW || bee.command == BeeCommand.STAY)) {
                return false;
            }
            this.tempting = null;
            for (Player p : bee.level().getEntitiesOfClass(Player.class, bee.getBoundingBox().inflate(10.0D))) {
                ItemStack main = p.getMainHandItem();
                ItemStack off = p.getOffhandItem();
                if (bee.isFood(main) || bee.isFood(off)
                        || GoldenBeeEntity.isHoneyedHealingPotion(main)
                        || GoldenBeeEntity.isHoneyedHealingPotion(off)) {
                    this.tempting = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.tempting != null && this.tempting.isAlive() && canUse();
        }

        @Override
        public void tick() {
            if (this.tempting == null) {
                return;
            }
            bee.getLookControl().setLookAt(this.tempting, 10.0F, bee.getMaxHeadXRot());
            if (--this.calm <= 0) {
                this.calm = 10;
                bee.getNavigation().moveTo(
                        this.tempting.getX(),
                        this.tempting.getY() + 1.0D,
                        this.tempting.getZ(),
                        0.9D);
            }
        }
    }

    /** Ataque en vuelo: no aterriza al llegar al objetivo. */
    static class FlyingMeleeAttackGoal extends Goal {
        private final GoldenBeeEntity bee;
        private int ticksUntilNextAttack;
        private int repath;

        FlyingMeleeAttackGoal(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = bee.getTarget();
            return t != null && t.isAlive() && bee.canAttack(t);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.ticksUntilNextAttack = 0;
            this.repath = 0;
            bee.setNoGravity(true);
        }

        @Override
        public void stop() {
            bee.setNoGravity(false);
        }

        @Override
        public void tick() {
            LivingEntity t = bee.getTarget();
            if (t == null) {
                return;
            }
            bee.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double dist = bee.distanceToSqr(t);
            if (--this.repath <= 0) {
                this.repath = 6;
                double ty = t.getY() + Math.max(0.8D, t.getBbHeight() * 0.6D);
                bee.getNavigation().moveTo(t.getX(), ty, t.getZ(), 1.25D);
            }
            // Forzar vuelo: no posarse
            if (bee.onGround() || bee.getY() < t.getY() + 0.5D) {
                bee.setDeltaMovement(bee.getDeltaMovement().x, Math.max(bee.getDeltaMovement().y, 0.2D), bee.getDeltaMovement().z);
            }
            bee.setNoGravity(true);
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            double reach = bee.getBbWidth() * 2.0D * bee.getBbWidth() * 2.0D + t.getBbWidth();
            if (dist <= reach && this.ticksUntilNextAttack <= 0) {
                this.ticksUntilNextAttack = 20;
                if (bee.level() instanceof ServerLevel server) {
                    bee.doHurtTarget(server, t);
                }
            }
        }
    }

    /** Guardia del panal (salvaje): hostil a quien se acerque, excepto excepciones. */
    static class HiveGuardTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private final GoldenBeeEntity bee;
        /** True si el target actual lo eligió este goal (proximidad al panal). */
        private boolean hiveGuardTarget;

        HiveGuardTargetGoal(GoldenBeeEntity bee) {
            super(bee, LivingEntity.class, 20, true, false, (target, level) -> {
                if (target instanceof Bee || target instanceof Creeper) {
                    return false;
                }
                if (bee.isTame()) {
                    return false;
                }
                if (!bee.canAttack(target)) {
                    return false;
                }
                BlockPos hive = bee.rememberedHive != null ? bee.rememberedHive : bee.getHivePos();
                if (hive == null) {
                    return false;
                }
                // Distancia del OBJETIVO al PANAL (no a la abeja)
                return target.blockPosition().closerThan(hive, 6.0D);
            });
            this.bee = bee;
        }

        @Override
        public void start() {
            super.start();
            this.hiveGuardTarget = true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = bee.getTarget();
            if (t == null || !t.isAlive()) {
                return false;
            }
            // SIEMPRE medir distancia del OBJETIVO al PANAL (no a la abeja)
            BlockPos hive = bee.rememberedHive != null ? bee.rememberedHive : bee.getHivePos();
            if (hive == null) {
                clearHiveThreat();
                return false;
            }
            // Fuera de 10 bloques del panal → soltar (independiente de distancia a la abeja)
            if (!t.blockPosition().closerThan(hive, 10.0D)) {
                clearHiveThreat();
                return false;
            }
            // Dentro del radio del panal: seguir aunque la abeja esté lejos del target
            return t.isAlive() && bee.canAttack(t);
        }

        private void clearHiveThreat() {
            bee.setTarget(null);
            try {
                bee.forgetCurrentTargetAndRefreshUniversalAnger();
            } catch (Exception ignored) {
            }
            try {
                bee.stopBeingAngry();
            } catch (Exception ignored) {
            }
            this.hiveGuardTarget = false;
            // Volver a patrullar cerca del panal
            BlockPos hive = bee.rememberedHive != null ? bee.rememberedHive : bee.getHivePos();
            if (hive != null && bee.level() instanceof ServerLevel) {
                bee.getNavigation().moveTo(hive.getX() + 0.5D, hive.getY() + 1.0D, hive.getZ() + 0.5D, 1.0D);
            }
        }

        @Override
        public void stop() {
            if (this.hiveGuardTarget) {
                clearHiveThreat();
            }
            super.stop();
            this.hiveGuardTarget = false;
        }
    }

    static class OwnerHurtByTargetCustom extends Goal {
        private final GoldenBeeEntity bee;
        private LivingEntity attacker;
        private int timestamp;

        OwnerHurtByTargetCustom(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!bee.isTame()) {
                return false;
            }
            LivingEntity owner = bee.getOwner();
            if (owner == null) {
                return false;
            }
            this.attacker = owner.getLastHurtByMob();
            int ts = owner.getLastHurtByMobTimestamp();
            return ts != this.timestamp
                    && this.attacker != null
                    && bee.canAttack(this.attacker)
                    && !(this.attacker instanceof Bee);
        }

        @Override
        public void start() {
            bee.setTarget(this.attacker);
            LivingEntity owner = bee.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtByMobTimestamp();
            }
            // FOLLOW también puede defender cuando el dueño es atacado
            if (!bee.defending) {
                bee.commandBeforeDefend = bee.command;
                bee.defending = true;
                bee.setOrderedToSit(false);
                bee.flyingToStay = false;
            }
            super.start();
        }
    }

    static class OwnerHurtTargetCustom extends Goal {
        private final GoldenBeeEntity bee;
        private LivingEntity target;
        private int timestamp;

        OwnerHurtTargetCustom(GoldenBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!bee.isTame()) {
                return false;
            }
            // Solo en FOLLOW reacciona a que el dueño ataque
            if (bee.command != BeeCommand.FOLLOW && !bee.defending) {
                return false;
            }
            LivingEntity owner = bee.getOwner();
            if (owner == null) {
                return false;
            }
            this.target = owner.getLastHurtMob();
            int ts = owner.getLastHurtMobTimestamp();
            return ts != this.timestamp
                    && this.target != null
                    && bee.canAttack(this.target)
                    && !(this.target instanceof Bee);
        }

        @Override
        public void start() {
            bee.setTarget(this.target);
            LivingEntity owner = bee.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtMobTimestamp();
            }
            // Solo FOLLOW entra en defend activo cuando el dueño ataca
            if (bee.command == BeeCommand.FOLLOW && !bee.defending) {
                bee.commandBeforeDefend = bee.command;
                bee.defending = true;
                bee.setOrderedToSit(false);
                bee.flyingToStay = false;
            }
            super.start();
        }
    }
}
