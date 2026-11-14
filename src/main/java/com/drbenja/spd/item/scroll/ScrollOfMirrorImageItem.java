package com.drbenja.spd.item.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.entity.mob.MirrorImageEntity;
import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.util.AllyTeamHelper;

/**
 * SPD Scroll of Mirror Image — spawn 2 weaker allied copies near the reader.
 */
public class ScrollOfMirrorImageItem extends SpdScrollItem {

    private static final int NIMAGES = 2;

    public ScrollOfMirrorImageItem(Properties props) {
        super(props);
    }

    @Override
    protected void onRead(ServerLevel level, Player player, ItemStack stack) {
        for (int i = 0; i < NIMAGES; i++) {
            spawnOne(level, player);
        }
    }

    private static boolean spawnOne(ServerLevel level, Player player) {
        var random = level.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double ox = player.getX() + (random.nextDouble() - 0.5) * 3.5;
            double oz = player.getZ() + (random.nextDouble() - 0.5) * 3.5;
            BlockPos pos = BlockPos.containing(ox, player.getY(), oz);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            double y = player.getY();
            if (Math.abs(ground.getY() - player.getY()) < 4) {
                y = ground.getY();
            }

            MirrorImageEntity img = ModEntities.MIRROR_IMAGE.get().create(level, EntitySpawnReason.NATURAL);
            if (img == null) return false;
            img.snapTo(ox, y, oz, player.getYRot(), 0f);
            if (!level.noCollision(img)) {
                continue;
            }
            img.duplicateFrom(player);
            level.addFreshEntity(img);
            AllyTeamHelper.assignAllyToLeaderTeam(level, player, img);
            return true;
        }

        MirrorImageEntity img = ModEntities.MIRROR_IMAGE.get().create(level, EntitySpawnReason.NATURAL);
        if (img == null) return false;
        Vec3 p = player.position();
        img.snapTo(p.x, p.y, p.z, player.getYRot(), 0f);
        img.duplicateFrom(player);
        level.addFreshEntity(img);
        AllyTeamHelper.assignAllyToLeaderTeam(level, player, img);
        return true;
    }
}

