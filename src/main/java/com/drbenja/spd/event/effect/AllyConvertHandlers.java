package com.drbenja.spd.event.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AllyTeamHelper;
import com.drbenja.spd.util.ConvertLootHelper;

/**
 * Corrupted / Enthralled (shared convert pipeline):
 * - convert loot + XP once on apply
 * - suppress death loot / XP after convert
 * - leave raids
 * - clear aggro toward the converted entity (including ongoing fights)
 * - iron golems only re-aggro if the convert attacks them <em>after</em> conversion
 *   (any monster type: illager, zombie, skeleton, …)
 * - vex summoned by a converted evoker inherit the same convert effect
 * <p>
 * AI hunt / pet ignore is in {@link ConvertedAllyAiHandlers}; teams in {@link com.drbenja.spd.util.AllyTeamHelper}.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class AllyConvertHandlers {

    private static final double AGRO_CLEAR_RANGE = 64.0;
    /** NBT on convert: gameTime when conversion was applied. */
    private static final String TAG_CONVERT_TIME = "spd_ally_convert_time";
    /**
     * NBT on iron golem: UUID of convert that damaged this golem after its conversion.
     * Only then may the golem target that convert again.
     */
    private static final String TAG_PROVOKED_BY = "spd_golem_provoked_by";

    private AllyConvertHandlers() {}

    public static boolean isConvertedAlly(LivingEntity e) {
        return ConvertedAllyAiHandlers.isConvertedAlly(e);
    }

    private static boolean isIronGolem(LivingEntity e) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && "iron_golem".equals(id.getPath());
    }

    private static boolean isEvoker(LivingEntity e) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && "evoker".equals(id.getPath());
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide()) return;
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        if (!(inst.getEffect().is(ModEffects.CORRUPTED) || inst.getEffect().is(ModEffects.ENTHRALLED))) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Entity source = null;
        try {
            source = event.getEffectSource();
        } catch (Throwable ignored) {
        }
        if (source == null && entity.getLastHurtByMob() != null) {
            source = entity.getLastHurtByMob();
        }
        ConvertLootHelper.dropConvertLoot(entity, source);
        leaveRaid(entity);
        entity.getPersistentData().putLong(TAG_CONVERT_TIME, entity.level().getGameTime());
        if (entity.level() instanceof ServerLevel level) {
            // Shared team for Corrupted + Enthralled (pets / same-team stop attacking)
            LivingEntity leader = null;
            if (source instanceof LivingEntity le) {
                leader = AllyTeamHelper.resolveLeader(le);
            } else if (entity.getLastHurtByMob() != null) {
                leader = AllyTeamHelper.resolveLeader(entity.getLastHurtByMob());
            }
            if (leader != null) {
                AllyTeamHelper.assignAllyToLeaderTeam(level, leader, entity);
            }
            clearAggroToward(level, entity);
            untrackFromTrialSpawners(level, entity);
        }
    }

    /**
     * Nearby mobs (golems included) drop target / last-hurt links to the convert.
     * Clears ongoing combat so village defenders stop mid-fight.
     */
    private static void clearAggroToward(ServerLevel level, LivingEntity converted) {
        // Break hurt links both ways so golem anger / revenge resets
        LivingEntity lastBy = converted.getLastHurtByMob();
        if (lastBy != null) {
            if (lastBy.getLastHurtByMob() == converted) {
                lastBy.setLastHurtByMob(null);
            }
            if (lastBy instanceof Mob lm && lm.getTarget() == converted) {
                lm.setTarget(null);
                lm.setAggressive(false);
                lm.getNavigation().stop();
            }
            tryStopAnger(lastBy);
        }
        converted.setLastHurtByMob(null);

        AABB box = converted.getBoundingBox().inflate(AGRO_CLEAR_RANGE);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (m == converted) continue;
            if (m.getTarget() == converted) {
                m.setTarget(null);
                m.setAggressive(false);
                m.getNavigation().stop();
            }
            if (m.getLastHurtByMob() == converted) {
                m.setLastHurtByMob(null);
            }
            tryStopAnger(m);
        }

        if (converted instanceof Mob self) {
            // Always clear combat focus on convert:
            // - drops golem / player / neutral fights
            // - for piglins also erases Brain ATTACK_TARGET + ANGRY_AT (setTarget alone is not enough)
            ConvertedAllyAiHandlers.clearCombatFocus(self);
        }
    }

    private static void tryStopAnger(LivingEntity e) {
        try {
            // NeutralMob (iron golem, etc.)
            if (e instanceof net.minecraft.world.entity.NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (!isConvertedAlly(living)) return;
        if (living.tickCount % 40 == 0) {
            leaveRaid(living);
            if (living.level() instanceof ServerLevel sl) {
                untrackFromTrialSpawners(sl, living);
            }
        }
        // Golems: drop target on any converted monster unless provoked after conversion
        if (living.tickCount % 10 == 0 && living.level() instanceof ServerLevel level) {
            AABB box = living.getBoundingBox().inflate(40);
            for (Mob m : level.getEntitiesOfClass(Mob.class, box, mob -> isIronGolem(mob) && mob.isAlive())) {
                if (m.getTarget() != living) continue;
                if (isGolemProvokedBy(m, living)) continue;
                m.setTarget(null);
                m.setAggressive(false);
                m.getNavigation().stop();
                tryStopAnger(m);
            }
        }
    }

    private static boolean isGolemProvokedBy(LivingEntity golem, LivingEntity convert) {
        String stored = golem.getPersistentData().getStringOr(TAG_PROVOKED_BY, "");
        return !stored.isEmpty() && stored.equals(convert.getUUID().toString());
    }

    private static void leaveRaid(LivingEntity entity) {
        if (!(entity instanceof Raider raider)) return;
        try {
            raider.setCanJoinRaid(false);
            if (raider.hasActiveRaid() && entity.level() instanceof ServerLevel level) {
                var raid = raider.getCurrentRaid();
                if (raid != null) {
                    raid.removeFromRaid(level, raider, true);
                }
            }
            raider.setTicksOutsideRaid(0);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Iron golems may only target a convert if that convert damaged them
     * <em>after</em> conversion (any monster type: illager, zombie, etc.).
     * Pre-conversion combat does not count.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity neu = event.getNewAboutToBeSetTarget();
        if (neu == null || !isConvertedAlly(neu)) return;
        if (!isIronGolem(attacker)) return;

        if (isGolemProvokedBy(attacker, neu)) {
            return;
        }

        event.setCanceled(true);
        if (attacker instanceof Mob am && am.getTarget() == neu) {
            am.setTarget(null);
            am.setAggressive(false);
        }
        tryStopAnger(attacker);
    }

    /**
     * When a convert damages an iron golem after conversion, mark the golem as provoked
     * so it may legally re-aggro that convert.
     */
    @SubscribeEvent
    public static void onConvertDamagesGolem(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity victim = event.getEntity();
        if (!isIronGolem(victim)) return;
        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker)) return;
        if (!isConvertedAlly(attacker)) return;
        long convertAt = attacker.getPersistentData().getLongOr(TAG_CONVERT_TIME, 0L);
        if (convertAt <= 0L || victim.level().getGameTime() < convertAt) return;
        victim.getPersistentData().putString(TAG_PROVOKED_BY, attacker.getUUID().toString());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (ConvertLootHelper.shouldSuppressDeathDrops(event.getEntity())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (ConvertLootHelper.shouldSuppressDeathXp(event.getEntity())) {
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    /**
     * Vex spawned by a converted Evoker inherit Corrupted or Enthralled.
     */
    private static boolean isVex(Entity e) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && "vex".equals(id.getPath());
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!isVex(event.getEntity())) return;
        if (!(event.getEntity() instanceof Mob vex)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // Owner is set slightly after spawn — next tick
        level.getServer().execute(() -> applyConvertToOwnedMinion(vex));
    }

    private static void applyConvertToOwnedMinion(Mob minion) {
        if (!minion.isAlive() || minion.level().isClientSide()) return;
        LivingEntity owner = null;
        try {
            if (minion instanceof net.minecraft.world.entity.OwnableEntity ownable) {
                Entity o = ownable.getOwner();
                if (o instanceof LivingEntity le) owner = le;
            }
        } catch (Throwable ignored) {
        }
        // Only the actual summoning owner — never nearby converted evokers
        if (owner == null || !isConvertedAlly(owner)) return;

        // Inherited convert: no convert loot / XP (and no death loot farm)
        ConvertLootHelper.markSkipConvertLoot(minion);

        if (owner.hasEffect(ModEffects.CORRUPTED) && !minion.hasEffect(ModEffects.CORRUPTED)) {
            MobEffectInstance src = owner.getEffect(ModEffects.CORRUPTED);
            int dur = src != null ? src.getDuration() : -1;
            minion.addEffect(new MobEffectInstance(ModEffects.CORRUPTED, dur, 0, false, true, true));
        } else if (owner.hasEffect(ModEffects.ENTHRALLED) && !minion.hasEffect(ModEffects.ENTHRALLED)) {
            MobEffectInstance src = owner.getEffect(ModEffects.ENTHRALLED);
            int dur = src != null ? src.getDuration() : -1;
            minion.addEffect(new MobEffectInstance(ModEffects.ENTHRALLED, dur, 0, false, true, true));
        }
    }

    /**
     * Vanilla illagers / piglins often ignore damage from "same group".
     * Force non-converted mobs to aggro converted attackers (Brain for piglins).
     */
    @SubscribeEvent
    public static void onHurtByConverted(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity victim = event.getEntity();
        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker)) return;
        if (!isConvertedAlly(attacker)) return;
        if (isConvertedAlly(victim)) return;
        if (!(victim instanceof Mob mob)) return;
        // Don't force golems (handled separately as neutral)
        if (isIronGolem(victim)) return;

        mob.setLastHurtByMob(attacker);
        // Piglins / breeze need Brain memories; everyone else setTarget is enough
        if (ConvertedAllyAiHandlers.isPiglinFamily(mob) || ConvertedAllyAiHandlers.isBreeze(mob)) {
            ConvertedAllyAiHandlers.setCombatFocus(mob, attacker);
        } else if (mob.getTarget() != attacker) {
            mob.setTarget(attacker);
            mob.setAggressive(true);
        }
    }

    /**
     * Converted witches still use vanilla Raider-friendly potion logic.
     * Rewrite splash potion contents when thrown by a converted witch:
     * - never beneficial (regen / healing on living hostiles)
     * - undead (inverted heal/harm): Instant Health (damages them)
     * - poison-immune (spiders, etc.): skip poison → harming / slowness
     */
    @SubscribeEvent
    public static void onWitchPotionSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (!(e instanceof Projectile proj)) return;
        if (!isThrownPotionEntity(e)) return;

        Entity owner = proj.getOwner();
        if (!(owner instanceof LivingEntity witch) || !isConvertedAlly(witch)) return;
        if (!ConvertedAllyAiHandlers.isWitch(witch)) return;

        LivingEntity target = null;
        if (witch instanceof Mob mob) {
            target = mob.getTarget();
        }
        ItemStack stack = getProjectileItem(proj);
        if (stack.isEmpty()) return;

        var chosen = chooseHostilePotion(witch, target);
        try {
            PotionContents contents = new PotionContents(
                    java.util.Optional.of(chosen),
                    java.util.Optional.empty(),
                    java.util.List.of(),
                    java.util.Optional.empty()
            );
            stack.set(DataComponents.POTION_CONTENTS, contents);
            setProjectileItem(proj, stack);
        } catch (Throwable ignored) {
        }
    }

    private static net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> chooseHostilePotion(
            LivingEntity witch, LivingEntity target) {
        if (target != null && target.isAlive()) {
            // Ally (owner team / pets / other converts): beneficial like raid support
            if (isWitchAllyTarget(witch, target)) {
                if (target.isInvertedHealAndHarm()) {
                    // Undead allies: Instant Damage heals them
                    return Potions.HARMING;
                }
                if (target.getHealth() <= 4.0F) {
                    return Potions.HEALING;
                }
                return Potions.REGENERATION;
            }
            // Hostiles: Undead take damage from Instant Health
            if (target.isInvertedHealAndHarm()) {
                return Potions.HEALING;
            }
            boolean poisonImmune = !target.canBeAffected(
                    new MobEffectInstance(MobEffects.POISON, 40, 0));
            double dist = witch.distanceTo(target);
            if (dist >= 8.0D && !target.hasEffect(MobEffects.SLOWNESS)) {
                return Potions.SLOWNESS;
            }
            if (!poisonImmune && target.getHealth() >= 8.0F
                    && !target.hasEffect(MobEffects.POISON)) {
                return Potions.POISON;
            }
            if (dist <= 3.0D && !target.hasEffect(MobEffects.WEAKNESS)
                    && witch.getRandom().nextFloat() < 0.25F) {
                return Potions.WEAKNESS;
            }
        }
        return Potions.HARMING;
    }

    /** Owner team, pets of that team, or other converted allies. */
    private static boolean isWitchAllyTarget(LivingEntity witch, LivingEntity target) {
        if (target == witch) return false;
        if (isConvertedAlly(target)) return true;
        if (AllyTeamHelper.sameTeam(witch, target)) return true;
        if (target instanceof Player) {
            if (witch.getTeam() != null && target.getTeam() != null
                    && witch.getTeam().isAlliedTo(target.getTeam())) {
                return true;
            }
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable) {
            var owner = ownable.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                if (AllyTeamHelper.sameTeam(witch, livingOwner) || livingOwner == witch) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isThrownPotionEntity(Entity e) {
        String n = e.getClass().getName();
        return n.contains("ThrownPotion")
                || n.contains("AbstractThrownPotion")
                || n.contains("throwableitemprojectile");
    }

    private static ItemStack getProjectileItem(Projectile proj) {
        try {
            Object o = proj.getClass().getMethod("getItem").invoke(proj);
            if (o instanceof ItemStack stack) return stack;
        } catch (ReflectiveOperationException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static void setProjectileItem(Projectile proj, ItemStack stack) {
        try {
            proj.getClass().getMethod("setItem", ItemStack.class).invoke(proj, stack);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /**
     * Remove converted mob UUID from nearby trial spawners' {@code currentMobs}.
     * <p>
     * 26.3: {@code TrialSpawner.getStateData().currentMobs} (package-private {@code Set<UUID>}).
     * {@code haveAllCurrentMobsDied()} == {@code currentMobs.isEmpty()}.
     * Vanilla only auto-removes when {@code shouldMobBeUntracked} (dead / unloaded / wrong dim).
     */
    private static void untrackFromTrialSpawners(ServerLevel level, LivingEntity entity) {
        java.util.UUID id = entity.getUUID();
        BlockPos origin = entity.blockPosition();
        int chunkR = 4;
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        for (int dx = -chunkR; dx <= chunkR; dx++) {
            for (int dz = -chunkR; dz <= chunkR; dz++) {
                var chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof TrialSpawnerBlockEntity trialBe) {
                        tryRemoveFromTrialSpawner(trialBe, id);
                    }
                }
            }
        }
    }

    private static void tryRemoveFromTrialSpawner(TrialSpawnerBlockEntity trialBe, java.util.UUID mobId) {
        try {
            Object spawner = trialBe.getTrialSpawner();
            if (spawner == null) return;

            // Preferred 26.3 path: TrialSpawner#getStateData() → TrialSpawnerStateData
            Object data = null;
            try {
                data = spawner.getClass().getMethod("getStateData").invoke(spawner);
            } catch (ReflectiveOperationException ignored) {
            }
            if (data == null) {
                for (Class<?> c = spawner.getClass(); c != null; c = c.getSuperclass()) {
                    for (var f : c.getDeclaredFields()) {
                        if (f.getType().getName().contains("TrialSpawnerStateData")) {
                            f.setAccessible(true);
                            data = f.get(spawner);
                            break;
                        }
                    }
                    if (data != null) break;
                }
            }
            if (data == null) return;

            boolean removed = false;
            // Exact field: final Set<UUID> currentMobs
            for (Class<?> c = data.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    var f = c.getDeclaredField("currentMobs");
                    f.setAccessible(true);
                    Object val = f.get(data);
                    if (val instanceof java.util.Set<?> set) {
                        @SuppressWarnings("unchecked")
                        java.util.Set<Object> raw = (java.util.Set<Object>) set;
                        if (raw.remove(mobId)) {
                            removed = true;
                        }
                        raw.removeIf(o -> mobId.toString().equals(String.valueOf(o)));
                    }
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (!removed) {
                removed = removeUuidFromObjectSets(data, mobId);
            }
            if (removed) {
                trialBe.setChanged();
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean removeUuidFromObjectSets(Object root, java.util.UUID mobId) {
        if (root == null) return false;
        boolean removed = false;
        for (Class<?> c = root.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (var f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(root);
                    if (val instanceof java.util.Set<?> set) {
                        @SuppressWarnings("unchecked")
                        java.util.Set<Object> raw = (java.util.Set<Object>) set;
                        if (raw.remove(mobId)) removed = true;
                        raw.removeIf(o -> mobId.toString().equals(String.valueOf(o)));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return removed;
    }

}
