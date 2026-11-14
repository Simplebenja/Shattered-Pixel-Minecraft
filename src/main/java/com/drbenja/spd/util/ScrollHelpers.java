package com.drbenja.spd.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ScrollHelpers {
    private ScrollHelpers() {}

    public static LivingEntity lookTarget(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestDist = range * range;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == player) continue;
            double d = e.distanceToSqr(player);
            if (d >= bestDist) continue;
            Vec3 to = e.getEyePosition().subtract(eye).normalize();
            if (to.dot(look) > 0.85) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    public static void safeRandomTeleport(ServerLevel level, Player player, int radius) {
        BlockPos origin = player.blockPosition();
        for (int i = 0; i < 48; i++) {
            int dx = level.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = level.getRandom().nextInt(radius * 2 + 1) - radius;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!isSafe(level, feet)) continue;
            player.teleportTo(x + 0.5, y, z + 0.5);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        var below = level.getBlockState(feet.below());
        var at = level.getBlockState(feet);
        var head = level.getBlockState(feet.above());
        if (below.getCollisionShape(level, feet.below()).isEmpty()) return false;
        if (!at.getCollisionShape(level, feet).isEmpty()) return false;
        if (!head.getCollisionShape(level, feet.above()).isEmpty()) return false;
        if (below.liquid() || at.liquid()) return false;
        if (below.is(net.minecraft.world.level.block.Blocks.LAVA)
                || at.is(net.minecraft.world.level.block.Blocks.LAVA)
                || below.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) return false;
        return true;
    }

    public static void teleportToNearestVillage(ServerLevel level, Player player) {
        try {
            BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, player.blockPosition(), 128, false);
            if (found != null) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX(), found.getZ());
                BlockPos feet = new BlockPos(found.getX(), y, found.getZ());
                for (int r = 0; r < 8; r++) {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            BlockPos p = new BlockPos(feet.getX() + dx,
                                    level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX() + dx, feet.getZ() + dz),
                                    feet.getZ() + dz);
                            if (isSafe(level, p)) {
                                player.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                                level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        safeRandomTeleport(level, player, 48);
    }

    /**
     * Retribution / Psionic Blast: daño a TODAS las entidades vivas en FoV
     * (animales, aldeanos, jugadores, mascotas, monstruos). Glowing siempre.
     * Psionic también daña al lector.
     */
    /**
     * SPD-faithful damage:
     * <ul>
     *   <li>Retribution: power = min(4, 4.45 * missingHp%); dmg = HT/10 + HP * power * 0.225</li>
     *   <li>Psionic: dmg = HT/2 + HP/2 (kills non-resistant at full HP); self = HT * 0.5 * 0.9^targets</li>
     * </ul>
     * Hits living entities in vision (+ glowing anywhere). Applies Blinded to survivors (SPD).
     */
    public static void retributionBlast(ServerLevel level, Player player, boolean psionic) {
        float missing = (player.getMaxHealth() - player.getHealth()) / Math.max(1f, player.getMaxHealth());
        float power = Math.min(4f, 4.45f * missing);

        double r = SpdVision.visionRadius(player);
        AABB box = player.getBoundingBox().inflate(r);

        java.util.ArrayList<LivingEntity> targets = new java.util.ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == player) continue;
            if (e.distanceToSqr(player) <= r * r) {
                targets.add(e);
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SpdVision.ALERT_RADIUS),
                ent -> ent.isAlive() && ent != player
                        && ent.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING))) {
            if (e.distanceToSqr(player) > r * r) {
                targets.add(e);
            }
        }

        for (LivingEntity e : targets) {
            float ht = e.getMaxHealth();
            float hp = e.getHealth();
            float dmg;
            if (psionic) {
                // SPD: HT/2 + HP/2 — full kill on non-resistant at full HP
                dmg = Math.round(ht / 2f + hp / 2f);
            } else {
                // SPD: HT/10 + HP * power * 0.225
                dmg = Math.round(ht / 10f + (hp * power * 0.225f));
            }
            if (dmg < 1f) dmg = 1f;
            e.hurtServer(level, player.damageSources().magic(), dmg);
            if (e.isAlive()) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.drbenja.spd.init.ModEffects.BLINDED, 20 * 10, 0, false, true, true));
            }
        }

        if (psionic) {
            // SPD: max(0, round(HT * (0.5 * 0.9^targets)))
            int n = targets.size();
            float self = Math.round(player.getMaxHealth() * (0.5f * (float) Math.pow(0.9, n)));
            if (self > 0f) {
                player.hurtServer(level, player.damageSources().magic(), self);
            }
        }
    }

    /** Alert monsters in wide range: set target to player. */
    public static void alertMonsters(ServerLevel level, Player player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (mob.getType().getCategory() == MobCategory.MONSTER || mob.getTarget() == player) {
                mob.setTarget(player);
            }
        }
    }
}
