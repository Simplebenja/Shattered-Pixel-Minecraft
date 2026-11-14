package com.drbenja.spd.item.stone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.drbenja.spd.entity.projectile.ThrownStoneEntity;

/**
 * Throwable runestone. Impact handled by {@link ThrownStoneEntity}.
 */
public abstract class SpdStoneItem extends Item {

    /** Radio de búsqueda si impacta bloque (diámetro 3 → radio 1.5). */
    public static final double NEARBY_ENTITY_RADIUS = 1.5;

    public SpdStoneItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            ThrownStoneEntity thrown = ThrownStoneEntity.create(level, player, stack.copyWithCount(1));
            level.addFreshEntity(thrown);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    public abstract void onImpact(ServerLevel level, Player thrower, HitResult hit, Vec3 impactPos);

    /**
     * Si el impacto fue entidad → esa entidad.
     * Si fue bloque → la LivingEntity más cercana en esfera de diámetro 3 (radio 1.5).
     */
    protected static LivingEntity resolveTarget(ServerLevel level, HitResult hit, Vec3 impactPos, Player thrower) {
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity living) {
            return living;
        }
        AABB box = new AABB(impactPos, impactPos).inflate(NEARBY_ENTITY_RADIUS);
        LivingEntity best = null;
        double bestD = NEARBY_ENTITY_RADIUS * NEARBY_ENTITY_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            // Incluye al lanzador y a otros jugadores (piedras SPD sí pueden afectarlos)
            double d = e.distanceToSqr(impactPos);
            if (d <= bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }
}
