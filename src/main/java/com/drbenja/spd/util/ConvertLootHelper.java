package com.drbenja.spd.util;

import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * On Corrupted / Enthralled: run the real death loot table + XP once,
 * then suppress normal death drops / XP.
 * <p>
 * Builds {@link LootParams} with {@code LAST_DAMAGE_PLAYER} explicitly so
 * conditions like {@code killed_by_player} (vindicator emeralds) always apply
 * when a player is the convert source.
 */
public final class ConvertLootHelper {

    public static final String TAG_LOOT_DROPPED = "spd_convert_loot_dropped";
    public static final String TAG_XP_DROPPED = "spd_convert_xp_dropped";
    /** Set before applying Corrupted/Enthralled so onEffectAdded skips loot/XP (e.g. inherited vex). */
    public static final String TAG_SKIP_CONVERT_LOOT = "spd_skip_convert_loot";

    private ConvertLootHelper() {}

    public static void markSkipConvertLoot(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_SKIP_CONVERT_LOOT, true);
    }

    public static boolean consumeSkipConvertLoot(LivingEntity entity) {
        var tag = entity.getPersistentData();
        if (tag.getBooleanOr(TAG_SKIP_CONVERT_LOOT, false)) {
            tag.remove(TAG_SKIP_CONVERT_LOOT);
            return true;
        }
        return false;
    }

    /** Prefer effect source / attacker; fall back to last hurt-by player. */
    private static Player resolvePlayer(LivingEntity entity, Entity convertSource) {
        if (convertSource instanceof Player p) {
            return p;
        }
        if (convertSource instanceof LivingEntity living) {
            if (living instanceof OwnableEntity own && own.getOwner() instanceof Player p) {
                return p;
            }
        }
        if (entity.getLastHurtByMob() instanceof Player p) {
            return p;
        }
        try {
            Player lhp = entity.getLastHurtByPlayer();
            if (lhp != null) {
                return lhp;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean alreadyDropped(LivingEntity entity) {
        return entity.getPersistentData().getBooleanOr(TAG_LOOT_DROPPED, false);
    }

    public static boolean alreadyDroppedXp(LivingEntity entity) {
        return entity.getPersistentData().getBooleanOr(TAG_XP_DROPPED, false);
    }

    public static void markDropped(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG_LOOT_DROPPED, true);
        entity.getPersistentData().putBoolean(TAG_XP_DROPPED, true);
    }

    public static void dropConvertLoot(LivingEntity entity) {
        dropConvertLoot(entity, null);
    }

    public static void dropConvertLoot(LivingEntity entity, Entity convertSource) {
        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (alreadyDropped(entity)) return;

        // Inherited convert (vex from converted evoker): no loot, no XP
        if (consumeSkipConvertLoot(entity)) {
            markDropped(entity);
            return;
        }

        if (!(entity instanceof Mob mob)) {
            markDropped(entity);
            return;
        }

        markDropped(entity);

        try {
            Player player = resolvePlayer(entity, convertSource);

            DamageSource src;
            if (player != null) {
                src = level.damageSources().playerAttack(player);
                try {
                    mob.setLastHurtByPlayer(player, 100);
                } catch (Throwable t) {
                    mob.setLastHurtByMob(player);
                }
                mob.setLastHurtByMob(player);
            } else {
                src = level.damageSources().genericKill();
            }

            Optional<ResourceKey<LootTable>> tableKeyOpt = mob.getLootTable();
            ResourceKey<LootTable> tableKey = tableKeyOpt.orElse(null);
            if (tableKey == null) {
                try {
                    Object def = mob.getType().getDefaultLootTable();
                    if (def instanceof ResourceKey<?> rk) {
                        //noinspection unchecked
                        tableKey = (ResourceKey<LootTable>) rk;
                    } else if (def instanceof Optional<?> opt && opt.isPresent()) {
                        //noinspection unchecked
                        tableKey = (ResourceKey<LootTable>) opt.get();
                    }
                } catch (Throwable ignored) {
                }
            }

            if (tableKey != null) {
                LootTable table = level.getServer().reloadableRegistries().getLootTable(tableKey);
                if (table != LootTable.EMPTY) {
                    LootParams.Builder builder = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, mob)
                            .withParameter(LootContextParams.ORIGIN, mob.position())
                            .withParameter(LootContextParams.DAMAGE_SOURCE, src)
                            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY,
                                    player != null ? player : src.getEntity())
                            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,
                                    player != null ? player : src.getDirectEntity());

                    if (player != null) {
                        builder = builder
                                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                                .withLuck(player.getLuck());
                    }

                    LootParams params = builder.create(LootContextParamSets.ENTITY);
                    long seed = mob.getLootTableSeed();
                    if (seed != 0L) {
                        table.getRandomItems(params, seed, stack -> mob.spawnAtLocation(level, stack));
                    } else {
                        table.getRandomItems(params, stack -> mob.spawnAtLocation(level, stack));
                    }
                }
            }

            if (mob.shouldDropExperience()) {
                Entity killer = player != null ? player : convertSource;
                int reward = mob.getExperienceReward(level, killer);
                if (reward > 0) {
                    ExperienceOrb.award(level, mob.position(), reward);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean shouldSuppressDeathDrops(LivingEntity entity) {
        return alreadyDropped(entity);
    }

    public static boolean shouldSuppressDeathXp(LivingEntity entity) {
        return alreadyDroppedXp(entity);
    }
}
