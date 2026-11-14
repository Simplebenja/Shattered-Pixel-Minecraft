package com.drbenja.spd.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.UUID;

/**
 * Scoreboard team helpers for SPD allies (Corrupted, Enthralled, future pets/minions).
 * Puts the ally on the same team as the leader (or the leader's owner if tameable)
 * so vanilla/mod pets that respect teams stop attacking them.
 */
public final class AllyTeamHelper {

    private static final String TEAM_PREFIX = "spd_ally_";

    private AllyTeamHelper() {}

    /** Prefer the owner of a tameable attacker; otherwise the attacker. */
    public static LivingEntity resolveLeader(LivingEntity attacker) {
        if (attacker instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof LivingEntity living) {
                return living;
            }
        }
        return attacker;
    }

    /**
     * Ensure {@code leader} has a team and move {@code ally} onto it.
     * Safe to call repeatedly.
     */
    public static void assignAllyToLeaderTeam(ServerLevel level, LivingEntity leader, LivingEntity ally) {
        if (level == null || leader == null || ally == null) return;
        leader = resolveLeader(leader);
        Scoreboard board = level.getScoreboard();

        PlayerTeam team = board.getPlayersTeam(leader.getScoreboardName());
        if (team == null) {
            team = findOrCreateTeam(board, leader);
            board.addPlayerToTeam(leader.getScoreboardName(), team);
        }

        PlayerTeam current = board.getPlayersTeam(ally.getScoreboardName());
        if (current != team) {
            if (current != null) {
                board.removePlayerFromTeam(ally.getScoreboardName());
            }
            board.addPlayerToTeam(ally.getScoreboardName(), team);
        }

        team.setAllowFriendlyFire(false);
        team.setSeeFriendlyInvisibles(true);
    }

    /** @deprecated use {@link #assignAllyToLeaderTeam} */
    @Deprecated
    public static void assignCorruptedToLeaderTeam(ServerLevel level, LivingEntity leader, LivingEntity corrupted) {
        assignAllyToLeaderTeam(level, leader, corrupted);
    }

    private static PlayerTeam findOrCreateTeam(Scoreboard board, LivingEntity leader) {
        String preferred = TEAM_PREFIX + shortId(leader.getUUID());
        PlayerTeam existing = board.getPlayerTeam(preferred);
        if (existing != null) {
            return existing;
        }
        PlayerTeam created = board.addPlayerTeam(preferred);
        created.setDisplayName(Component.literal("SPD Ally"));
        created.setAllowFriendlyFire(false);
        created.setSeeFriendlyInvisibles(true);
        created.setCollisionRule(Team.CollisionRule.PUSH_OWN_TEAM);
        return created;
    }

    private static String shortId(UUID uuid) {
        String s = uuid.toString().replace("-", "");
        return s.substring(0, Math.min(12, s.length()));
    }

    public static boolean sameTeam(LivingEntity a, LivingEntity b) {
        if (a == null || b == null) return false;
        Team ta = a.getTeam();
        Team tb = b.getTeam();
        return ta != null && ta.isAlliedTo(tb);
    }

    public static boolean isFriendlyForPets(LivingEntity attacker, LivingEntity target) {
        if (sameTeam(attacker, target)) return true;
        if (attacker instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                if (sameTeam(livingOwner, target)) return true;
                if (target.getTeam() != null && livingOwner.getTeam() != null
                        && target.getTeam().isAlliedTo(livingOwner.getTeam())) {
                    return true;
                }
            }
        }
        return false;
    }
}
