package com.drbenja.spd.event.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import com.drbenja.spd.SpdMod;
import com.drbenja.spd.config.entity.ChillImmuneEntitiesConfig;
import com.drbenja.spd.effect.ChilledEffect;
import com.drbenja.spd.effect.FrozenEffect;
import com.drbenja.spd.effect.ParalyzedEffect;
import com.drbenja.spd.init.ModEffects;

@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class ChillFrozenHandlers {

    private ChillFrozenHandlers() {}

    private static boolean isFrozen(LivingEntity e) {
        return e.hasEffect(ModEffects.FROZEN);
    }

    private static boolean isImmobilized(LivingEntity e) {
        return ImmobilizeHandlers.isImmobilized(e);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        // Inmunidad global (golem de nieve, config, etc.): ningún medio aplica Chilled/Frozen
        if ((inst.getEffect().is(ModEffects.CHILLED) || inst.getEffect().is(ModEffects.FROZEN))
                && ChillImmuneEntitiesConfig.isChillImmune(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (inst.getEffect().is(ModEffects.CHILLED)
                && event.getEntity().hasEffect(ModEffects.FROZEN)
                && !FrozenEffect.allowsChilled(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        if (inst.getEffect().is(ModEffects.CHILLED)) {
            ChilledEffect.clearModifier(event.getEntity());
        } else if (inst.getEffect().is(ModEffects.FROZEN)) {
            boolean skip = FrozenEffect.consumeSkipWaterChill(event.getEntity());
            FrozenEffect.finish(event.getEntity(), !skip);
        } else if (inst.getEffect().is(ModEffects.PARALYZED)) {
            ParalyzedEffect.clearModifiers(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) {
            return;
        }
        if (inst.getEffect().is(ModEffects.CHILLED)) {
            ChilledEffect.clearModifier(event.getEntity());
        } else if (inst.getEffect().is(ModEffects.FROZEN)) {
            boolean skip = FrozenEffect.consumeSkipWaterChill(event.getEntity());
            FrozenEffect.finish(event.getEntity(), !skip);
            FrozenEffect.playShatterFx(event.getEntity());
        } else if (inst.getEffect().is(ModEffects.PARALYZED)) {
            ParalyzedEffect.clearModifiers(event.getEntity());
        }
    }


    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }

        // Chilled: derretir de inmediato al detectar fuego/lava
        if (living.hasEffect(ModEffects.CHILLED) && ChilledEffect.isMelting(living)) {
            ChilledEffect.clearModifier(living);
            living.removeEffect(ModEffects.CHILLED);
        }

        if (living.hasEffect(ModEffects.FROZEN)) {
            if (FrozenEffect.isMelting(living)) {
                FrozenEffect.markSkipWaterChill(living);
                living.removeEffect(ModEffects.FROZEN);
                return;
            }
            // Inmovilización de IA: ImmobilizeHandlers
        } else if (living.hasEffect(ModEffects.PARALYZED)) {
            ParalyzedEffect.applyModifiers(living);
        } else {
            FrozenEffect.applyPendingWaterChill(living);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        // Outgoing damage while immobilized: ImmobilizeHandlers

        // Chilled se quita al recibir daño estando en fuego/lava
        if (entity.hasEffect(ModEffects.CHILLED)
                && (entity.isOnFire() || entity.isInLava() || entity.getRemainingFireTicks() > 0
                || ChilledEffect.isMelting(entity))) {
            ChilledEffect.clearModifier(entity);
            entity.removeEffect(ModEffects.CHILLED);
        }

        if (isFrozen(entity) && event.getAmount() > 0.0F) {
            // Gracia tras adquirir Frozen (p. ej. conversión desde Chilled por el mismo golpe)
            if (!FrozenEffect.hasDamageGrace(entity)) {
                FrozenEffect.markSkipWaterChill(entity);
                entity.removeEffect(ModEffects.FROZEN);
            }
            return;
        }

        // Paralyzed: daño reduce duración (fuego no lo quita)
        if (entity.hasEffect(ModEffects.PARALYZED) && event.getAmount() > 0.0F) {
            ParalyzedEffect.reduceByDamage(entity, event.getAmount());
        }
    }






    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if (player instanceof LivingEntity living && isImmobilized(living) && player instanceof ServerPlayer sp) {
            sp.closeContainer();
        }
    }


    // --- Visuales / audio (Chilled nieve, Frozen hielo) ---

    @SubscribeEvent
    public static void onFrozenOrChillAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        if (inst.getEffect().is(ModEffects.FROZEN)) {
            if (FrozenEffect.consumeSilentFx(entity)) {
                return; // extend/refresh — no spam
            }
            FrozenEffect.playApplyFx(entity);
        } else if (inst.getEffect().is(ModEffects.CHILLED)) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    8, 0.25, 0.35, 0.25, 0.01);
        }
    }

    @SubscribeEvent
    public static void onFrozenRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        if (!inst.getEffect().is(ModEffects.FROZEN)) return;
        // Shatter when cleared by command / milk / damage break (Expired has its own path)
        FrozenEffect.playShatterFx(entity);
    }

    @SubscribeEvent
    public static void onChillFrozenVisuals(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!(living.level() instanceof ServerLevel level)) return;

        if (living.hasEffect(ModEffects.CHILLED) && !living.hasEffect(ModEffects.FROZEN)
                && living.tickCount % 10 == 0) {
            // Falling snow motes while chilled
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    living.getX(), living.getY() + living.getBbHeight() * 0.85, living.getZ(),
                    2, 0.2, 0.15, 0.2, 0.0);
        }
        if (living.hasEffect(ModEffects.FROZEN) && living.tickCount % 8 == 0) {
            // Ice shimmer around frozen target
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                    4, 0.25, 0.4, 0.25, 0.0);
            if (living.tickCount % 24 == 0) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        living.getX(), living.getY() + living.getBbHeight() * 0.4, living.getZ(),
                        2, 0.2, 0.25, 0.2, 0.02);
            }
        }
    }


    // --- Ranged charge slow (antes ranged charge slow) ---
    @SubscribeEvent
    public static void onChilledRangedUseTick(LivingEntityUseItemEvent.Tick event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance chill = entity.getEffect(ModEffects.CHILLED);
        if (chill == null) return;
        ItemStack stack = event.getItem();
        Item item = stack.getItem();
        if (!(item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem)) return;
        double factor = ChilledEffect.speedFactor(chill.getDuration());
        if (factor >= 0.999D) return;
        if (entity.getRandom().nextDouble() > factor) {
            event.setDuration(event.getDuration() + 1);
        }
    }

}
