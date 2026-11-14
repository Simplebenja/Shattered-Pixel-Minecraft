package com.drbenja.spd.event.effect;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.CharmedEffect;
import com.drbenja.spd.effect.CorruptedEffect;
import com.drbenja.spd.init.ModEffects;
import com.drbenja.spd.util.AllyTeamHelper;

/**
 * Shared AI for {@link ModEffects#CORRUPTED} and {@link ModEffects#ENTHRALLED}.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ConvertedAllyAiHandlers {

    private static final double SCAN_RANGE = 16.0;
    private static final long ANGER_TTL_TICKS = 600L;
    private static final String TAG_SLIME_ATK_CD = "SpdSlimeAtkCd";

    /** Prevents LivingChangeTargetEvent → setTarget → LivingChangeTargetEvent recursion. */
    private static final ThreadLocal<Boolean> REENTRANT = ThreadLocal.withInitial(() -> false);

    private ConvertedAllyAiHandlers() {}

    public static boolean isConvertedAlly(LivingEntity e) {
        return e != null
                && (e.hasEffect(ModEffects.CORRUPTED) || e.hasEffect(ModEffects.ENTHRALLED));
    }

    private static void setTargetSafe(Mob mob, LivingEntity target) {
        REENTRANT.set(true);
        try {
            mob.setTarget(target);
        } finally {
            REENTRANT.set(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (Boolean.TRUE.equals(REENTRANT.get())) {
            return;
        }

        LivingEntity entity = event.getEntity();
        LivingEntity neu = event.getNewAboutToBeSetTarget();

        // Charmed (incl. Brain mobs): never target the charmer
        if (entity.hasEffect(ModEffects.CHARMED) && neu != null) {
            if (CharmedEffect.isCharmedBy(entity, neu)) {
                event.setCanceled(true);
                if (entity instanceof Mob mob) {
                    // Clear Brain attack memories so piglins/breeze stop the angry loop
                    clearBrainCombat(mob);
                    setTargetSafe(mob, null);
                    mob.setAggressive(false);
                }
                return;
            }
        }

        // Converted: do not allow Brain/AI to drop a still-valid hunt target
        // (fixes lava/magma-cube target↔detarget angry loops).
        // Cancel only — do NOT call setTarget again (that re-fires this event → StackOverflow).
        if (isConvertedAlly(entity) && neu == null) {
            LivingEntity prev = null;
            try {
                prev = event.getOriginalAboutToBeSetTarget();
            } catch (Throwable ignored) {
            }
            if (prev == null && entity instanceof Mob mob) {
                prev = resolveCurrentTarget(mob);
            }
            if (prev != null && prev.isAlive() && isValidHuntTarget(entity, prev)) {
                event.setCanceled(true);
                if (entity instanceof Mob mob) {
                    ensureBrainAttack(mob, prev);
                    mob.setAggressive(true);
                }
            }
            return;
        }

        if (neu == null) return;

        if (isConvertedAlly(entity)) {
            if (!isValidHuntTarget(entity, neu)) {
                event.setCanceled(true);
                if (entity instanceof Mob mob) {
                    // Soft clear: only brain + aggressive; avoid setTarget recursion
                    REENTRANT.set(true);
                    try {
                        clearCombatFocus(mob);
                    } finally {
                        REENTRANT.set(false);
                    }
                }
            }
            return;
        }

        if (isConvertedAlly(neu)) {
            if (AllyTeamHelper.isFriendlyForPets(entity, neu)) {
                event.setCanceled(true);
                return;
            }
            if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player) {
                if (neu.getTeam() != null || AllyTeamHelper.sameTeam(entity, neu)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;

        // Charmed Brain mobs: keep charmer off ATTACK_TARGET / setTarget
        if (mob.hasEffect(ModEffects.CHARMED) && mob.tickCount % 5 == 0) {
            LivingEntity t = resolveCurrentTarget(mob);
            if (t != null && CharmedEffect.isCharmedBy(mob, t)) {
                clearCombatFocus(mob);
            }
        }

        if (mob.tickCount % 10 == 0) {
            LivingEntity t = mob.getTarget();
            if (t != null && isConvertedAlly(t) && AllyTeamHelper.isFriendlyForPets(mob, t)) {
                clearCombatFocus(mob);
            }
        }

        if (!isConvertedAlly(mob)) return;

        boolean brainHunter = usesBrainCombat(mob);
        boolean hoglinHunter = isHoglin(mob);

        if (isPiglinFamily(mob)) {
            erasePiglinFear(mob);
        }

        // Hoglin / Breeze need frequent re-assert of target (vanilla goals clear same-type / brain resets)
        boolean everyTick = brainHunter || hoglinHunter;

        if (!everyTick && mob.tickCount % 5 != 0) {
            if (mob.getTarget() != null && mob.level() instanceof ServerLevel sl
                    && (isSlimeFamily(mob) || isHoglin(mob))) {
                tryMeleeAssist(mob, mob.getTarget(), sl);
            }
            return;
        }

        LivingEntity current = resolveCurrentTarget(mob);
        if (current != null && !isValidHuntTarget(mob, current)) {
            clearCombatFocus(mob);
            current = null;
        }

        if (current != null && current.isAlive()) {
            // Re-stamp focus so vanilla AI cannot drop it (lava, same-type hoglin, breeze brain)
            setCombatFocus(mob, current);
            if (mob.level() instanceof ServerLevel sl) {
                if (isSlimeFamily(mob) || isHoglin(mob)) {
                    // Hoglin: Sensor/StopAttacking often rejects same-type; force melee like slime assist
                    tryMeleeAssist(mob, current, sl);
                }
            }
            return;
        }

        LivingEntity next = null;
        if (isWitch(mob)) {
            next = findNearestHealAlly(mob);
        }
        if (next == null) {
            next = findNearestHuntTarget(mob);
        }
        if (next != null) {
            setCombatFocus(mob, next);
            if (mob.level() instanceof ServerLevel sl) {
                if (isSlimeFamily(mob) || isHoglin(mob)) {
                    tryMeleeAssist(mob, next, sl);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect().is(ModEffects.CORRUPTED)) {
            CorruptedEffect.clearSessionFlags(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect().is(ModEffects.CORRUPTED)) {
            CorruptedEffect.clearSessionFlags(event.getEntity());
        }
    }

    public static boolean isValidHuntTarget(LivingEntity hunter, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (hunter == target) return false;
        // Converted witch may "target" hurt allies to throw beneficial potions
        if (isWitch(hunter) && isConvertedAlly(hunter) && isHealAllyCandidate(hunter, target)) {
            return true;
        }
        if (isConvertedAlly(target)) return false;
        if (isCreeper(target)) return false;
        if (target instanceof Player) return false;
        if (target instanceof AbstractVillager) return false;
        if (target instanceof Animal) return false;
        if (target instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player) {
            return false;
        }
        if (isIronGolem(target)) {
            return isEngagedAgainstAllies(target, hunter) || wasAttackedBy(hunter, target);
        }
        if (isPiglinFamily(target)) {
            return isEngagedAgainstAllies(target, hunter) || wasAttackedBy(hunter, target);
        }
        if (isHoglin(target)) {
            return true;
        }
        if (target instanceof NeutralMob) {
            return isEngagedAgainstAllies(target, hunter) || wasAttackedBy(hunter, target);
        }
        if (target instanceof Enemy || target instanceof Monster) {
            return true;
        }
        return target.getType().getCategory() == MobCategory.MONSTER;
    }

    @Deprecated
    public static boolean isValidMonsterTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (isConvertedAlly(target)) return false;
        if (isCreeper(target)) return false;
        if (target instanceof Player) return false;
        if (target instanceof AbstractVillager) return false;
        if (target instanceof Animal) return false;
        if (target instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player) {
            return false;
        }
        if (isPiglinFamily(target) || target instanceof NeutralMob || isIronGolem(target)) {
            return false;
        }
        if (target instanceof Enemy || target instanceof Monster) return true;
        return target.getType().getCategory() == MobCategory.MONSTER;
    }

    public static boolean isEngagedAgainstAllies(LivingEntity candidate, LivingEntity convert) {
        if (!(candidate instanceof Mob mob)) return false;
        LivingEntity t = mob.getTarget();
        if (t != null && isAllyOfConvert(t, convert)) return true;
        LivingEntity brainT = getBrainAttackTarget(mob);
        return brainT != null && isAllyOfConvert(brainT, convert);
    }

    private static boolean wasAttackedBy(LivingEntity convert, LivingEntity candidate) {
        return convert.getLastHurtByMob() == candidate;
    }

    private static boolean isAllyOfConvert(LivingEntity entity, LivingEntity convert) {
        if (entity == convert) return true;
        if (AllyTeamHelper.sameTeam(entity, convert)) return true;
        if (entity instanceof Player) {
            if (convert.getTeam() != null && entity.getTeam() != null
                    && convert.getTeam().isAlliedTo(entity.getTeam())) {
                return true;
            }
        }
        if (entity instanceof OwnableEntity ownable) {
            var owner = ownable.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                if (livingOwner == convert || AllyTeamHelper.sameTeam(livingOwner, convert)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static LivingEntity findNearestHuntTarget(Mob hunter) {
        if (!(hunter.level() instanceof ServerLevel level)) return null;
        AABB box = hunter.getBoundingBox().inflate(SCAN_RANGE);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                cand -> isValidHuntTarget(hunter, cand))) {
            if (e == hunter) continue;
            double d = hunter.distanceToSqr(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    public static void clearCombatFocus(Mob mob) {
        setTargetSafe(mob, null);
        mob.setAggressive(false);
        try {
            mob.getNavigation().stop();
        } catch (Throwable ignored) {
        }
        tryStopAnger(mob);
        clearBrainCombat(mob);
    }

    public static void setCombatFocus(Mob mob, LivingEntity target) {
        if (target == null) {
            clearCombatFocus(mob);
            return;
        }
        setTargetSafe(mob, target);
        mob.setAggressive(true);
        ensureBrainAttack(mob, target);
    }

    private static LivingEntity resolveCurrentTarget(Mob mob) {
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive()) return t;
        return getBrainAttackTarget(mob);
    }

    private static boolean usesBrainCombat(Mob mob) {
        // Hoglin also uses brain memories in 1.21+; include so same-type re-assert sticks
        return isPiglinFamily(mob) || isBreeze(mob) || isHoglin(mob);
    }

    private static void clearBrainCombat(Mob mob) {
        try {
            Brain<?> brain = mob.getBrain();
            if (brain == null) return;
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
            brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
        } catch (Throwable ignored) {
        }
    }

    private static void erasePiglinFear(Mob mob) {
        try {
            Brain<?> brain = mob.getBrain();
            if (brain == null) return;
            brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
            tryErase(brain, "NEAREST_VISIBLE_ZOMBIFIED");
            tryErase(brain, "NEAREST_VISIBLE_NEMESIS");
            tryErase(brain, "HUNTED_RECENTLY");
        } catch (Throwable ignored) {
        }
    }

    private static void tryErase(Brain<?> brain, String fieldName) {
        try {
            var f = MemoryModuleType.class.getField(fieldName);
            Object mod = f.get(null);
            if (mod instanceof MemoryModuleType<?> mmt) {
                brain.eraseMemory(mmt);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Brain combat focus tuned from 26.3 decompiled AI:
     * <ul>
     *   <li>HoglinAi.setAttackTarget: ATTACK_TARGET expiry 200; idle only scans players</li>
     *   <li>Breeze FIGHT needs ATTACK_TARGET + no WALK_TARGET; Shoot needs BREEZE_SHOOT Unit</li>
     *   <li>BreezeAi.SlideToTargetSink.stop sets BREEZE_SHOOT for 60 ticks — we set it directly</li>
     * </ul>
     */
    private static void ensureBrainAttack(Mob mob, LivingEntity target) {
        try {
            Brain<?> brain = mob.getBrain();
            if (brain == null) return;

            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            tryErase(brain, "PACIFIED");
            tryErase(brain, "BREED_TARGET");
            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);
            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), ANGER_TTL_TICKS);
            brain.eraseMemory(MemoryModuleType.AVOID_TARGET);

            try {
                brain.setMemory(MemoryModuleType.NEAREST_ATTACKABLE, target);
            } catch (Throwable ignored) {
            }

            if (isBreeze(mob)) {
                // FIGHT gate
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                brain.eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
                try {
                    mob.getNavigation().stop();
                } catch (Throwable ignored) {
                }
                // Shoot behavior requires BREEZE_SHOOT present (normally set after Slide ends)
                try {
                    brain.setMemoryWithExpiry(
                            MemoryModuleType.BREEZE_SHOOT,
                            net.minecraft.util.Unit.INSTANCE,
                            60L);
                } catch (Throwable ignored) {
                }
                // Allow a new shot if stuck on old cooldown for too long
                if (mob.tickCount % 40 == 0) {
                    tryErase(brain, "BREEZE_SHOOT_COOLDOWN");
                    tryErase(brain, "BREEZE_SHOOT_CHARGING");
                    tryErase(brain, "BREEZE_SHOOT_RECOVERING");
                }
                forceFightActivity(brain);
            } else if (isHoglin(mob) || isPiglinFamily(mob)) {
                forceFightActivity(brain);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Prefer FIGHT over IDLE so MeleeAttack / Shoot behaviors can run. */
    private static void forceFightActivity(Brain<?> brain) {
        try {
            brain.setActiveActivityToFirstValid(
                    java.util.List.of(
                            net.minecraft.world.entity.schedule.Activity.FIGHT,
                            net.minecraft.world.entity.schedule.Activity.IDLE));
        } catch (Throwable ignored) {
            try {
                brain.setActiveActivityIfPossible(net.minecraft.world.entity.schedule.Activity.FIGHT);
            } catch (Throwable ignored2) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static LivingEntity getBrainAttackTarget(Mob mob) {
        try {
            Brain<?> brain = mob.getBrain();
            if (brain == null) return null;
            Optional<LivingEntity> atk = (Optional<LivingEntity>) (Optional<?>)
                    brain.getMemory(MemoryModuleType.ATTACK_TARGET);
            if (atk != null && atk.isPresent()) {
                LivingEntity e = atk.get();
                if (e != null && e.isAlive()) return e;
            }
            Optional<UUID> angry = (Optional<UUID>) (Optional<?>)
                    brain.getMemory(MemoryModuleType.ANGRY_AT);
            if (angry != null && angry.isPresent() && mob.level() instanceof ServerLevel level) {
                var entity = level.getEntity(angry.get());
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    return living;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Direct melee when Brain/Sensor refuses the target (slime hitbox, hoglin same-type). */
    private static void tryMeleeAssist(Mob attacker, LivingEntity target, ServerLevel level) {
        if (target == null || !target.isAlive()) return;
        double reach;
        int cooldownTicks;
        if (isSlimeFamily(attacker)) {
            int size = getSlimeSize(attacker);
            reach = Math.max(1.0D, 0.8D * size) + target.getBbWidth() * 0.5D;
            cooldownTicks = Math.max(12, 24 - size * 3);
        } else {
            // Hoglin / generic: slightly generous melee reach
            reach = Math.max(2.5D, attacker.getBbWidth() + target.getBbWidth() + 1.0D);
            cooldownTicks = 20;
        }
        if (attacker.distanceTo(target) > reach) return;

        int cd = attacker.getPersistentData().getIntOr(TAG_SLIME_ATK_CD, 0);
        if (cd > 0) {
            attacker.getPersistentData().putInt(TAG_SLIME_ATK_CD, cd - 1);
            return;
        }
        try {
            boolean hurt = attacker.doHurtTarget(level, target);
            if (hurt) {
                attacker.getPersistentData().putInt(TAG_SLIME_ATK_CD, cooldownTicks);
            }
        } catch (Throwable t) {
            try {
                var m = Mob.class.getMethod("doHurtTarget", LivingEntity.class);
                Object r = m.invoke(attacker, target);
                if (r instanceof Boolean b && b) {
                    attacker.getPersistentData().putInt(TAG_SLIME_ATK_CD, cooldownTicks);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static int getSlimeSize(Mob slime) {
        try {
            var m = slime.getClass().getMethod("getSize");
            Object v = m.invoke(slime);
            if (v instanceof Integer i) return Math.max(1, i);
        } catch (Throwable ignored) {
        }
        return Math.max(1, Math.round(slime.getBbWidth()));
    }

    private static boolean isHealAllyCandidate(LivingEntity witch, LivingEntity target) {
        if (target.getHealth() >= target.getMaxHealth()) return false;
        if (isConvertedAlly(target)) return true;
        if (AllyTeamHelper.sameTeam(witch, target)) return true;
        if (target instanceof Player) {
            if (witch.getTeam() != null && target.getTeam() != null
                    && witch.getTeam().isAlliedTo(target.getTeam())) {
                return true;
            }
        }
        if (target instanceof OwnableEntity ownable) {
            var owner = ownable.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                if (AllyTeamHelper.sameTeam(witch, livingOwner) || livingOwner == witch) {
                    return true;
                }
            }
        }
        return false;
    }

    private static LivingEntity findNearestHealAlly(Mob witch) {
        if (!(witch.level() instanceof ServerLevel level)) return null;
        AABB box = witch.getBoundingBox().inflate(SCAN_RANGE);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                cand -> isHealAllyCandidate(witch, cand))) {
            if (e == witch) continue;
            double d = witch.distanceToSqr(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static boolean isCreeper(LivingEntity e) {
        return pathIs(e, "creeper");
    }

    private static boolean isIronGolem(LivingEntity e) {
        return pathIs(e, "iron_golem");
    }

    public static boolean isPiglinFamily(LivingEntity e) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (id == null) return false;
        String p = id.getPath();
        return "piglin".equals(p) || "piglin_brute".equals(p);
    }

    public static boolean isBreeze(LivingEntity e) {
        return pathIs(e, "breeze");
    }

    public static boolean isHoglin(LivingEntity e) {
        return pathIs(e, "hoglin");
    }

    public static boolean isSlimeFamily(LivingEntity e) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (id == null) return false;
        String p = id.getPath();
        return "slime".equals(p) || "magma_cube".equals(p);
    }

    public static boolean isWitch(LivingEntity e) {
        return pathIs(e, "witch");
    }

    private static boolean pathIs(LivingEntity e, String path) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && path.equals(id.getPath());
    }

    private static void tryStopAnger(LivingEntity e) {
        try {
            if (e instanceof NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
        } catch (Throwable ignored) {
        }
    }
}
