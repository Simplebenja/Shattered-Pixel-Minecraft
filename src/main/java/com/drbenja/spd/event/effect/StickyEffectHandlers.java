package com.drbenja.spd.event.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/**
 * Corrupted, Doomed and Deferred Damage cannot be removed by milk, Cleansed,
 * or other curative means. Natural expiry still works ({@code MobEffectEvent.Expired}).
 * <p>
 * {@code /effect clear} is allowed (detected via stack frames from the effect command).
 * Internal code can set {@link #ALLOW_REMOVE} to strip intentionally.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID)
public final class StickyEffectHandlers {

    /** Set true only when our own code intentionally strips a sticky effect. */
    public static final ThreadLocal<Boolean> ALLOW_REMOVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private StickyEffectHandlers() {}

    public static boolean isSticky(Holder<MobEffect> holder) {
        // Kinetic charge visuals must be removable when conserved damage changes
        // tier or is spent; they are re-applied from NBT each tick if needed.
        return holder.is(ModEffects.CORRUPTED)
                || holder.is(ModEffects.DOOMED)
                || holder.is(ModEffects.DEFERRED_DAMAGE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRemove(MobEffectEvent.Remove event) {
        if (Boolean.TRUE.equals(ALLOW_REMOVE.get())) {
            return;
        }
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null || !isSticky(inst.getEffect())) {
            return;
        }
        // Allow /effect clear and /effect clear <player> <effect>
        if (isEffectCommandClear()) {
            return;
        }
        tryCancel(event);
    }

    /**
     * True when the remove is driven by the vanilla/NeoForge effect command
     * (so ops can still clear sticky effects for debugging).
     */
    private static boolean isEffectCommandClear() {
        try {
            return StackWalker.getInstance().walk(stream -> stream
                    .limit(24)
                    .anyMatch(f -> {
                        String cn = f.getClassName();
                        String mn = f.getMethodName();
                        if (cn.contains("EffectCommands") || cn.contains("EffectCommand")) {
                            return true;
                        }
                        // Brigadier command dispatch for effect
                        if (cn.contains("commands.Effect") || cn.endsWith(".Effect")) {
                            return true;
                        }
                        return "clearEffect".equals(mn)
                                || "clearAllEffects".equals(mn)
                                || "removeAllEffects".equals(mn) && cn.contains("Command");
                    }));
        } catch (Throwable t) {
            return false;
        }
    }

    private static void tryCancel(MobEffectEvent.Remove event) {
        try {
            var mCancelable = event.getClass().getMethod("isCancelable");
            Object can = mCancelable.invoke(event);
            if (can instanceof Boolean b && b) {
                event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
                return;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            event.setCanceled(true);
        } catch (Throwable ignored) {
        }
    }
}
