package com.drbenja.spd.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.init.ModItems;
import com.drbenja.spd.item.AquaBrewItem;

/**
 * Throwable Aqua Brew.
 * Extends ItemEntity so vanilla ItemEntityRenderer draws the item model
 * (custom Projectile renderers draw nothing under Renderpearl 26.3).
 */
public class ThrownAquaBrewEntity extends ItemEntity {

    public ThrownAquaBrewEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        setPickUpDelay(Integer.MAX_VALUE);
        if (getItem().isEmpty()) {
            setItem(new ItemStack(ModItems.AQUA_BREW.get()));
        }
    }

    public static ThrownAquaBrewEntity createThrown(Level level, Player player) {
        ThrownAquaBrewEntity e = new ThrownAquaBrewEntity(
                com.drbenja.spd.init.ModEntities.THROWN_AQUA_BREW.get(), level);
        e.setItem(new ItemStack(ModItems.AQUA_BREW.get()));
        e.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        e.shootFromRotation(player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        return e;
    }

    public void shootFromRotation(float xRot, float yRot, float roll, float velocity, float inaccuracy) {
        double x = -Math.sin(yRot * (Math.PI / 180.0)) * Math.cos(xRot * (Math.PI / 180.0));
        double y = -Math.sin((xRot + roll) * (Math.PI / 180.0));
        double z = Math.cos(yRot * (Math.PI / 180.0)) * Math.cos(xRot * (Math.PI / 180.0));
        Vec3 dir = new Vec3(x, y, z).normalize().scale(velocity);
        var r = level().getRandom();
        dir = dir.add(
                (r.nextDouble() - 0.5) * 0.0075 * inaccuracy,
                (r.nextDouble() - 0.5) * 0.0075 * inaccuracy,
                (r.nextDouble() - 0.5) * 0.0075 * inaccuracy);
        setDeltaMovement(dir);
    }

    @Override
    public void tick() {
        baseTick();

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, e -> true);
        if (hit.getType() != HitResult.Type.MISS) {
            onImpact(hit);
            return;
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.add(0, -0.03, 0).scale(0.99));

        if (!level().isClientSide() && tickCount > 80) {
            discard();
        }
    }

    private void onImpact(HitResult result) {
        if (level().isClientSide()) {
            discard();
            return;
        }
        BlockPos pos;
        if (result instanceof BlockHitResult bhr) {
            pos = bhr.getBlockPos().relative(bhr.getDirection());
        } else if (result instanceof EntityHitResult ehr) {
            pos = ehr.getEntity().blockPosition();
        } else {
            pos = BlockPos.containing(result.getLocation());
        }
        AquaBrewItem.activate((ServerLevel) level(), pos, position());
        discard();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
