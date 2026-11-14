package com.drbenja.spd.event.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.entity.mob.MirrorImageEntity;

/**
 * Mirror Image support: half outgoing damage; only true hostiles proactively target copies.
 * Neutrals / piglins do not auto-aggro (treat more like a player).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class MirrorImageHandlers {

    private MirrorImageHandlers() {}

    @SubscribeEvent
    public static void onMirrorDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof MirrorImageEntity) {
            event.setAmount(event.getAmount() * 0.5f);
        }
    }

    @SubscribeEvent
    public static void onHostileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;
        if (mob instanceof MirrorImageEntity) return;
        // Neutrals: only fight back if provoked (HurtByTarget), don't hunt images
        if (mob instanceof NeutralMob) return;
        // Piglins: brain targets players + gold rules — do not force-target images
        if (mob instanceof AbstractPiglin) return;

        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, MirrorImageEntity.class, true));
    }
}

