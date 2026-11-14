package com.drbenja.spd.entity.mob;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Allied copy of the player (SPD Mirror Image).
 * Combat: Skeleton bow, custom crossbow state machine, Drowned/player trident + Loyalty.
 * Backup of previous revision: {@code MirrorImageEntity.java.bak}
 */
public class MirrorImageEntity extends PathfinderMob implements RangedAttackMob, CrossbowAttackMob {

    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(MirrorImageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_CROSSBOW =
            SynchedEntityData.defineId(MirrorImageEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUUID;
    private int lifeTicks = 20 * 180;
    private int tridentThrowCooldown;

    public MirrorImageEntity(EntityType<? extends MirrorImageEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0F);
        this.setDropChance(EquipmentSlot.MAINHAND, 0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0f);
        this.setDropChance(EquipmentSlot.HEAD, 0f);
        this.setDropChance(EquipmentSlot.CHEST, 0f);
        this.setDropChance(EquipmentSlot.LEGS, 0f);
        this.setDropChance(EquipmentSlot.FEET, 0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, "");
        builder.define(DATA_CHARGING_CROSSBOW, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreeperAvoidGoal(this));
        // Crossbow state machine (charge → release → aim → shoot → reload)
        this.goalSelector.addGoal(2, new MirrorCrossbowGoal(this));
        // Trident throw only with Loyalty
        this.goalSelector.addGoal(2, new MirrorTridentGoal(this, 1.0D, 40, 20.0F));
        // Bow: interval ~20 draw + 5 aim
        this.goalSelector.addGoal(3, new RangedBowAttackGoal<>(this, 1.0D, 25, 15.0F));
        this.goalSelector.addGoal(4, new MirrorMeleeGoal(this, 1.2D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                (target, level) -> isValidHuntTarget(target)));
    }

    private boolean isValidHuntTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (target == this) return false;
        LivingEntity owner = getOwner();
        if (target == owner) return false;
        if (owner != null && target.isAlliedTo(owner)) return false;
        if (target instanceof MirrorImageEntity other && sameOwner(other)) return false;

        boolean provoked = false;
        if (target instanceof Mob mob) {
            LivingEntity mobTarget = mob.getTarget();
            provoked = mobTarget == this || (owner != null && mobTarget == owner)
                    || mob.getLastHurtByMob() == this
                    || (owner != null && mob.getLastHurtByMob() == owner);
        }

        // Creepers: only hunt if ranged weapon, or point-blank melee
        if (target instanceof Creeper) {
            double d = this.distanceToSqr(target);
            if (holdingBow() || holdingCrossbow()) {
                return true; // kite / shoot
            }
            // Melee / trident-no-loyalty / fists: only at point-blank (≤ ~1.5 blocks)
            return d <= 2.25;
        }

        if (target instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin) {
            return provoked; // gold rules → TODO
        }
        if (target instanceof net.minecraft.world.entity.NeutralMob) {
            return provoked;
        }
        if (target instanceof Enemy) return true;
        return provoked;
    }

    private boolean sameOwner(MirrorImageEntity other) {
        return ownerUUID != null && ownerUUID.equals(other.ownerUUID);
    }

    public void duplicateFrom(Player player) {
        this.ownerUUID = player.getUUID();
        String name;
        try {
            name = player.getGameProfile().name();
        } catch (Throwable t) {
            name = player.getName().getString();
        }
        this.entityData.set(DATA_SKIN, name == null ? "" : name);

        double maxHp = Math.max(10.0, player.getMaxHealth() * 0.5);
        var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(maxHp);
        }
        this.setHealth((float) maxHp);

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!piece.isEmpty()) {
                this.setItemSlot(slot, piece.copy());
            }
        }

        ItemStack weapon = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && isWeaponLike(s)) {
                weapon = s.copy();
                break;
            }
        }
        if (!weapon.isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }

        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, off.copy());
        }
    }

    private static boolean isWeaponLike(ItemStack s) {
        if (s.getItem() instanceof ProjectileWeaponItem) return true;
        if (s.getItem() instanceof TridentItem) return true;
        if (s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)) return true;
        if (s.is(ItemTags.WEAPON_ENCHANTABLE) || s.is(ItemTags.MACE_ENCHANTABLE)) return true;
        if (s.is(ItemTags.TRIDENT_ENCHANTABLE)) return true;
        if (s.is(ItemTags.BOW_ENCHANTABLE) || s.is(ItemTags.CROSSBOW_ENCHANTABLE)) return true;
        if (s.is(ItemTags.SPEARS)) return true;
        return false;
    }

    boolean holdingBow() {
        return getMainHandItem().getItem() instanceof BowItem
                || getOffhandItem().getItem() instanceof BowItem;
    }

    boolean holdingCrossbow() {
        return getMainHandItem().getItem() instanceof CrossbowItem
                || getOffhandItem().getItem() instanceof CrossbowItem;
    }

    boolean holdingTrident() {
        return getMainHandItem().getItem() instanceof TridentItem
                || getMainHandItem().is(Items.TRIDENT);
    }

    boolean holdingSpear() {
        return getMainHandItem().is(ItemTags.SPEARS);
    }

    boolean hasRangedWeapon() {
        return holdingBow() || holdingCrossbow();
    }

    int loyaltyLevel(ItemStack stack) {
        if (!(level() instanceof ServerLevel server) || stack.isEmpty()) return 0;
        var reg = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var loyalty = reg.get(Enchantments.LOYALTY).orElse(null);
        if (loyalty == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(loyalty, stack);
    }

    private int flameLevel(ItemStack stack) {
        if (!(level() instanceof ServerLevel server) || stack.isEmpty()) return 0;
        var reg = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var flame = reg.get(Enchantments.FLAME).orElse(null);
        if (flame == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(flame, stack);
    }

    static boolean isCrossbowCharged(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CrossbowItem)) return false;
        try {
            return CrossbowItem.isCharged(stack);
        } catch (Throwable t) {
            var charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            return charged != null;
        }
    }

    static void clearCrossbowCharge(ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.remove(DataComponents.CHARGED_PROJECTILES);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setAggressive(this.getTarget() != null);
        if (tridentThrowCooldown > 0) {
            tridentThrowCooldown--;
        }
        // Loyalty / pickup: restore trident when projectile is nearby and main hand empty
        if (!level().isClientSide() && getMainHandItem().isEmpty()) {
            for (ThrownTrident trident : level().getEntitiesOfClass(
                    ThrownTrident.class, this.getBoundingBox().inflate(2.5))) {
                if (trident.getOwner() != this) continue;
                ItemStack stack = trident.getWeaponItem();
                if (stack == null || stack.isEmpty()) {
                    stack = new ItemStack(Items.TRIDENT);
                } else {
                    stack = stack.copy();
                }
                this.setItemSlot(EquipmentSlot.MAINHAND, stack);
                trident.discard();
                break;
            }
        }
        if (!level().isClientSide() && lifeTicks > 0 && --lifeTicks <= 0) {
            this.discard();
        }
    }

    // --- RangedAttackMob: bow + loyalty trident ---

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        // Crossbow never shoots from this path — only MirrorCrossbowGoal after full charge
        if (holdingCrossbow()) {
            return;
        }
        if (holdingTrident()) {
            if (loyaltyLevel(getMainHandItem()) > 0) {
                throwTridentAt(target);
            }
            return;
        }
        shootBowAt(target, power);
    }

    private void shootBowAt(LivingEntity target, float power) {
        InteractionHand hand = ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem);
        ItemStack bowItem = this.getItemInHand(hand);
        if (!(bowItem.getItem() instanceof BowItem)) {
            return;
        }
        ItemStack projectile = this.getProjectile(bowItem);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, power, bowItem);
        Item item = bowItem.getItem();
        if (item instanceof ProjectileWeaponItem weaponItem) {
            arrow = weaponItem.customArrow(arrow, projectile, bowItem);
        }

        double xd = target.getX() - this.getX();
        double yd = target.getY(0.3333333333333333) - arrow.getY();
        double zd = target.getZ() - this.getZ();
        double distanceToTarget = Math.sqrt(xd * xd + zd * zd);

        if (level() instanceof ServerLevel serverLevel) {
            float uncertainty = this.rangedAttackUncertainty(serverLevel);
            Projectile.spawnProjectileUsingShoot(
                    arrow, serverLevel, projectile,
                    xd, yd + distanceToTarget * 0.2F, zd,
                    1.6F, uncertainty);
        }

        if (flameLevel(bowItem) > 0) {
            arrow.igniteForSeconds(100);
        }

        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.stopUsingItem();
    }

    private void throwTridentAt(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ItemStack mainHandItem = getMainHandItem();
        if (!(mainHandItem.getItem() instanceof TridentItem) && !mainHandItem.is(Items.TRIDENT)) return;
        if (loyaltyLevel(mainHandItem) <= 0) return; // only Loyalty throws
        if (tridentThrowCooldown > 0) return;

        // Player-like: item leaves the hand; recover via Loyalty return or pickup
        ItemStack tridentItemStack = mainHandItem.copy();
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        ThrownTrident trident = new ThrownTrident(serverLevel, this, tridentItemStack);
        try {
            // ALLOWED so it can be picked up if Loyalty fails to return the item
            trident.pickup = AbstractArrow.Pickup.ALLOWED;
        } catch (Throwable ignored) {
        }

        double xd = target.getX() - this.getX();
        double yd = target.getY(0.3333333333333333) - trident.getY();
        double zd = target.getZ() - this.getZ();
        double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
        float uncertainty = this.rangedAttackUncertainty(serverLevel);

        Projectile.spawnProjectileUsingShoot(
                trident, serverLevel, tridentItemStack,
                xd, yd + distanceToTarget * 0.2F, zd,
                1.6F, uncertainty);

        this.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
        this.tridentThrowCooldown = 40;
        this.stopUsingItem();
    }

    public ItemStack getProjectile(ItemStack weapon) {
        return new ItemStack(Items.ARROW);
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        // Crossbow must not fire through generic projectile paths while charging
        if (weapon instanceof CrossbowItem) {
            return isCrossbowCharged(getMainHandItem()) || isCrossbowCharged(getOffhandItem());
        }
        return true;
    }

    // --- CrossbowAttackMob ---

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING_CROSSBOW, charging);
    }

    public boolean isChargingCrossbow() {
        return this.entityData.get(DATA_CHARGING_CROSSBOW);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean killedByPlayer) {
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID id) {
        this.ownerUUID = id;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerUUID == null) return null;
        if (level() instanceof ServerLevel server) {
            Entity e = server.getEntity(ownerUUID);
            if (e instanceof LivingEntity le) return le;
            return server.getServer().getPlayerList().getPlayer(ownerUUID);
        }
        return level().getPlayerByUUID(ownerUUID);
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerUUID != null) {
            output.putString("OwnerUUID", ownerUUID.toString());
        }
        output.putString("SkinName", getSkinName());
        output.putInt("LifeTicks", lifeTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("OwnerUUID").ifPresent(s -> {
            try {
                ownerUUID = UUID.fromString(s);
            } catch (Exception ignored) {
            }
        });
        input.getString("SkinName").ifPresent(s -> this.entityData.set(DATA_SKIN, s));
        lifeTicks = input.getIntOr("LifeTicks", lifeTicks);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // -------------------------------------------------------------------------
    // Goals
    // -------------------------------------------------------------------------

    /**
     * Crossbow: CHARGE (must finish use) → AIM → SHOOT once → back to CHARGE.
     * Never shoots while still using/charging the item.
     */
    static class MirrorCrossbowGoal extends Goal {
        private enum Phase { CHARGE, AIM, SHOOT_DONE }

        private final MirrorImageEntity img;
        private Phase phase = Phase.CHARGE;
        private int aimTicks;
        private static final int AIM_TICKS = 5;

        MirrorCrossbowGoal(MirrorImageEntity img) {
            this.img = img;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private ItemStack stack() {
            if (img.getMainHandItem().getItem() instanceof CrossbowItem) return img.getMainHandItem();
            if (img.getOffhandItem().getItem() instanceof CrossbowItem) return img.getOffhandItem();
            return ItemStack.EMPTY;
        }

        private InteractionHand hand() {
            return img.getMainHandItem().getItem() instanceof CrossbowItem
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }

        @Override
        public boolean canUse() {
            if (stack().isEmpty()) return false;
            LivingEntity t = img.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            phase = isCrossbowCharged(stack()) ? Phase.AIM : Phase.CHARGE;
            aimTicks = 0;
        }

        @Override
        public void stop() {
            img.setChargingCrossbow(false);
            if (img.isUsingItem()) {
                img.stopUsingItem();
            }
            phase = Phase.CHARGE;
            aimTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = img.getTarget();
            if (target == null) return;
            img.getLookControl().setLookAt(target, 30.0F, 30.0F);

            ItemStack bow = stack();
            if (bow.isEmpty()) return;
            InteractionHand h = hand();

            switch (phase) {
                case CHARGE -> {
                    // Stay put while charging; do not approach or shoot
                    img.getNavigation().stop();
                    img.setChargingCrossbow(true);

                    if (isCrossbowCharged(bow)) {
                        // Already charged (e.g. from previous cycle) → aim
                        if (img.isUsingItem()) {
                            img.stopUsingItem();
                        }
                        img.setChargingCrossbow(false);
                        phase = Phase.AIM;
                        aimTicks = 0;
                        return;
                    }

                    if (!img.isUsingItem()) {
                        img.startUsingItem(h);
                    }
                    ItemStack use = img.getUseItem().isEmpty() ? bow : img.getUseItem();
                    int need = CrossbowItem.getChargeDuration(use, img);
                    if (img.getTicksUsingItem() >= need) {
                        // Finish use → marks charged (player-like release)
                        img.releaseUsingItem();
                        img.setChargingCrossbow(false);
                        // Verify charged; if not, force-stop and retry next tick
                        if (isCrossbowCharged(stack())) {
                            phase = Phase.AIM;
                            aimTicks = 0;
                        }
                    }
                }
                case AIM -> {
                    if (!isCrossbowCharged(stack())) {
                        phase = Phase.CHARGE;
                        aimTicks = 0;
                        return;
                    }
                    // Must not be mid-use while aiming/shooting
                    if (img.isUsingItem()) {
                        img.stopUsingItem();
                    }
                    img.setChargingCrossbow(false);

                    double d = img.distanceToSqr(target);
                    if (d > 64.0) {
                        img.getNavigation().moveTo(target, 1.0);
                        aimTicks = 0;
                        return;
                    }
                    img.getNavigation().stop();
                    aimTicks++;
                    if (aimTicks >= AIM_TICKS) {
                        // Single shot only when fully charged and not using
                        img.performCrossbowAttack(img, 1.6F);
                        clearCrossbowCharge(stack());
                        phase = Phase.CHARGE;
                        aimTicks = 0;
                    }
                }
                default -> phase = Phase.CHARGE;
            }
        }
    }

    /** Loyalty-only trident throw at range; otherwise melee. */
    static class MirrorTridentGoal extends RangedAttackGoal {
        private final MirrorImageEntity img;

        MirrorTridentGoal(MirrorImageEntity mob, double speed, int interval, float radius) {
            super(mob, speed, interval, radius);
            this.img = mob;
        }

        @Override
        public boolean canUse() {
            return super.canUse()
                    && img.holdingTrident()
                    && img.loyaltyLevel(img.getMainHandItem()) > 0
                    && img.tridentThrowCooldown <= 0;
        }

        @Override
        public void start() {
            super.start();
            img.setAggressive(true);
            img.startUsingItem(InteractionHand.MAIN_HAND);
        }

        @Override
        public void stop() {
            super.stop();
            img.stopUsingItem();
        }
    }

    static class MirrorMeleeGoal extends MeleeAttackGoal {
        private final MirrorImageEntity img;

        MirrorMeleeGoal(MirrorImageEntity img, double speed, boolean followIfNotSeen) {
            super(img, speed, followIfNotSeen);
            this.img = img;
        }

        @Override
        public boolean canUse() {
            if (img.holdingCrossbow()) {
                // Crossbow only fights via MirrorCrossbowGoal
                return false;
            }
            if (img.holdingBow()) {
                LivingEntity t = img.getTarget();
                if (t != null && img.distanceToSqr(t) > 9.0) return false;
            }
            if (img.holdingTrident() && img.loyaltyLevel(img.getMainHandItem()) > 0) {
                LivingEntity t = img.getTarget();
                if (t != null && img.distanceToSqr(t) > 16.0) return false;
            }
            // Creepers: melee only at point-blank
            LivingEntity t = img.getTarget();
            if (t instanceof Creeper) {
                if (img.hasRangedWeapon()) return false;
                if (img.distanceToSqr(t) > 2.25) return false;
            }
            return super.canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = img.getTarget();
            if (img.holdingSpear() && target != null) {
                double d = img.distanceToSqr(target);
                boolean moving = img.getDeltaMovement().horizontalDistanceSqr() > 0.0025;
                boolean midRange = d > 4.0 && d < 36.0;
                // Estocada pose only when rushing from mid-range; else normal melee
                img.setAggressive(moving && midRange);
            }
            super.tick();
        }
    }

    /**
     * Flee creepers unless point-blank melee or kiting with ranged.
     * Charged creepers: wider flee radius.
     */
    static class CreeperAvoidGoal extends Goal {
        private final MirrorImageEntity img;
        private Creeper creeperToAvoid;
        private final float farCharged = 8.0F;
        private final float farNormal = 4.0F;

        CreeperAvoidGoal(MirrorImageEntity img) {
            this.img = img;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            var list = img.level().getEntitiesOfClass(
                    Creeper.class, img.getBoundingBox().inflate(farCharged, 3.0, farCharged),
                    Creeper::isAlive);
            Creeper closest = null;
            double best = Double.MAX_VALUE;
            for (Creeper c : list) {
                double d = img.distanceToSqr(c);
                float max = c.isPowered() ? farCharged : farNormal;
                if (d > max * max) continue;
                // Point-blank melee exception (no ranged): don't flee, let melee hit
                if (!img.hasRangedWeapon() && d <= 2.25) continue;
                // Ranged: flee only if too close (< 4 blocks)
                if (img.hasRangedWeapon() && d >= 16.0) continue;
                if (d < best) {
                    best = d;
                    closest = c;
                }
            }
            if (closest == null) return false;
            creeperToAvoid = closest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (creeperToAvoid == null || !creeperToAvoid.isAlive()) return false;
            double d = img.distanceToSqr(creeperToAvoid);
            if (!img.hasRangedWeapon() && d <= 2.25) return false;
            float max = creeperToAvoid.isPowered() ? farCharged : farNormal;
            return d < max * max;
        }

        @Override
        public void start() {
            runAway();
        }

        @Override
        public void tick() {
            if (creeperToAvoid == null) return;
            runAway();
        }

        private void runAway() {
            if (creeperToAvoid == null) return;
            double speed = creeperToAvoid.isPowered() ? 1.3D : 1.15D;
            double dx = img.getX() - creeperToAvoid.getX();
            double dz = img.getZ() - creeperToAvoid.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0e-4) {
                dx = img.getRandom().nextGaussian();
                dz = img.getRandom().nextGaussian();
                len = Math.sqrt(dx * dx + dz * dz);
            }
            double tx = img.getX() + (dx / len) * 5.0;
            double tz = img.getZ() + (dz / len) * 5.0;
            img.getNavigation().moveTo(tx, img.getY(), tz, speed);
        }

        @Override
        public void stop() {
            creeperToAvoid = null;
        }
    }
}

