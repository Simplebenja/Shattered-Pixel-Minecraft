package com.drbenja.spd.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.init.ModEntities;
import com.drbenja.spd.item.stone.SpdStoneItem;

/**
 * Piedra arrojadiza: solo se activa al impactar entidad o bloque.
 * Timeout largo solo descarta (no activa el efecto).
 */
public class ThrownStoneEntity extends ItemEntity {

    private boolean activated;
    private java.util.UUID ownerId;

    public ThrownStoneEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        setPickUpDelay(Integer.MAX_VALUE);
    }

    public static ThrownStoneEntity create(Level level, Player player, ItemStack stack) {
        ThrownStoneEntity e = new ThrownStoneEntity(ModEntities.THROWN_STONE.get(), level);
        e.setItem(stack);
        e.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        e.shootFromRotation(player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        e.ownerId = player.getUUID();
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
        if (activated) {
            if (!level().isClientSide()) discard();
            return;
        }
        if (level().isClientSide()) {
            return;
        }

        // Primeros ticks: no impactar al lanzador (spawn en su hitbox). Después sí puede (y a cualquier jugador).
        final int grace = 4;
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, e -> {
            if (tickCount < grace && ownerId != null && e.getUUID().equals(ownerId)) {
                return false;
            }
            return true;
        });
        if (hit.getType() != HitResult.Type.MISS) {
            activate(hit);
            return;
        }

        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
        setDeltaMovement(getDeltaMovement().add(0, -0.03, 0).scale(0.99));

        // Solo activar si realmente tocó el suelo (bloque sólido), no por timeout
        if (onGround()) {
            activate(new BlockHitResult(position(), getDirection(), blockPosition(), false));
            return;
        }

        // Timeout: descartar SIN activar (evita blink/blast en el aire)
        if (tickCount > 200) {
            discard();
        }
    }

    private void activate(HitResult hit) {
        if (activated || !(level() instanceof ServerLevel server)) return;
        activated = true;
        ItemStack stack = getItem();
        if (stack.getItem() instanceof SpdStoneItem stone) {
            Player thrower = null;
            if (ownerId != null) {
                var pl = server.getServer().getPlayerList().getPlayer(ownerId);
                if (pl != null) thrower = pl;
            }
            stone.onImpact(server, thrower, hit, hit.getLocation());
        }
        discard();
    }
}
