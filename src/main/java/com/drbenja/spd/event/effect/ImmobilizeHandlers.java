package com.drbenja.spd.event.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/**
 * Inmovilización compartida: Frozen, Paralyzed, Magical Sleep.
 * Un solo modificador de MOVEMENT_SPEED (-100%) para los tres.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ImmobilizeHandlers {

    /** Shared lock used by Frozen / Paralyzed / Magical Sleep. */
    public static final Identifier SPEED_MOD_ID =
            Identifier.fromNamespaceAndPath("spd", "immobilize_speed");

    private ImmobilizeHandlers() {}

    public static boolean isImmobilized(LivingEntity e) {
        return e != null && (e.hasEffect(ModEffects.FROZEN)
                || e.hasEffect(ModEffects.PARALYZED)
                || e.hasEffect(ModEffects.MAGICAL_SLEEP));
    }

    public static void applySpeedLock(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (speed.getModifier(SPEED_MOD_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_MOD_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void clearSpeedLock(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MOD_ID);
        }
    }

    /** Compat: FrozenEffect / ParalyzedEffect still call these names. */
    public static void applyModifiers(LivingEntity entity) {
        applySpeedLock(entity);
    }

    public static void clearModifiers(LivingEntity entity) {
        if (!isImmobilized(entity)) {
            clearSpeedLock(entity);
        }
    }

    @SubscribeEvent
    public static void onTickPre(EntityTickEvent.Pre event) {
        suppressMobControl(event.getEntity());
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        suppressMobControl(event.getEntity());
    }

    private static void suppressMobControl(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (!isImmobilized(living)) return;

        applySpeedLock(living);

        if (living instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setAggressive(false);
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
        }
        var dm = living.getDeltaMovement();
        if (Math.abs(dm.x) > 1.0E-4 || Math.abs(dm.z) > 1.0E-4) {
            living.setDeltaMovement(0, dm.y, 0);
        }
        living.xxa = 0;
        living.zza = 0;
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        var holder = event.getEffectInstance().getEffect();
        if (!(holder.is(ModEffects.FROZEN) || holder.is(ModEffects.PARALYZED)
                || holder.is(ModEffects.MAGICAL_SLEEP))) {
            return;
        }
        LivingEntity e = event.getEntity();
        // After remove, if no other immobilize effect remains, clear shared lock
        // (effect may still be listed until event finishes — clear if none of the other two)
        boolean still = false;
        if (holder.is(ModEffects.FROZEN)) {
            still = e.hasEffect(ModEffects.PARALYZED) || e.hasEffect(ModEffects.MAGICAL_SLEEP);
        } else if (holder.is(ModEffects.PARALYZED)) {
            still = e.hasEffect(ModEffects.FROZEN) || e.hasEffect(ModEffects.MAGICAL_SLEEP);
        } else {
            still = e.hasEffect(ModEffects.FROZEN) || e.hasEffect(ModEffects.PARALYZED);
        }
        if (!still) {
            clearSpeedLock(e);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        onEffectRemove(new MobEffectEvent.Remove(event.getEntity(), event.getEffectInstance()) {
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!isImmobilized(event.getEntity())) return;
        if (event.getNewAboutToBeSetTarget() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOutgoingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity atk && isImmobilized(atk)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        if (isImmobilized(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (isImmobilized(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isImmobilized(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isImmobilized(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isImmobilized(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isImmobilized(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (!(e instanceof Projectile proj)) return;
        Entity owner = proj.getOwner();
        if (owner instanceof LivingEntity living && isImmobilized(living)) {
            event.setCanceled(true);
        }
    }
}
