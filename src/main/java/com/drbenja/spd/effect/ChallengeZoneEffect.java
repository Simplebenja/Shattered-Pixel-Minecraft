package com.drbenja.spd.effect;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Challenge: solo congela exhaustion.
 * Si exhaustion no sube, food/sat no bajan por actividad.
 * Comer y cualquier aumento de food/sat siguen normales.
 */
public class ChallengeZoneEffect extends MobEffect {

    private static final Field EXHAUSTION_FIELD;
    /** Exhaustion al entrar / último tick; se limpia al salir. */
    private static final Map<UUID, Float> EXH_FLOOR = new ConcurrentHashMap<>();

    static {
        Field f = null;
        try {
            f = FoodData.class.getDeclaredField("exhaustionLevel");
            f.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
        EXHAUSTION_FIELD = f;
    }

    public ChallengeZoneEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xffaa00);
    }

    public static void captureBaseline(Player player) {
        EXH_FLOOR.put(player.getUUID(), getExhaustion(player.getFoodData()));
    }

    public static void clearBaseline(Player player) {
        EXH_FLOOR.remove(player.getUUID());
    }

    public static boolean hasBaseline(Player player) {
        return EXH_FLOOR.containsKey(player.getUUID());
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) return true;
        FoodData food = player.getFoodData();
        UUID id = player.getUUID();

        float cur = getExhaustion(food);
        Float floor = EXH_FLOOR.get(id);
        if (floor == null) {
            EXH_FLOOR.put(id, cur);
            return true;
        }

        if (cur > floor) {
            setExhaustion(food, floor);
        } else {
            // Exhaustion bajó (p. ej. al comer): nuevo piso
            EXH_FLOOR.put(id, cur);
        }
        return true;
    }

    private static float getExhaustion(FoodData food) {
        if (EXHAUSTION_FIELD == null) return 0f;
        try {
            return EXHAUSTION_FIELD.getFloat(food);
        } catch (IllegalAccessException e) {
            return 0f;
        }
    }

    private static void setExhaustion(FoodData food, float value) {
        if (EXHAUSTION_FIELD == null) return;
        try {
            EXHAUSTION_FIELD.setFloat(food, Math.max(0f, value));
        } catch (IllegalAccessException ignored) {
        }
    }
}
