package com.drbenja.spd.util;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Bajo Amok: solo se anulan peligros de daño (fuego, lava, etc.).
 * No se tocan agua, vallas, trampillas ni tipos genéricos con malus &lt; 0,
 * para no romper el rodear obstáculos / saltar 1 bloque.
 */
public final class AmokPathfinding {

    private AmokPathfinding() {}

    private static final Map<UUID, EnumMap<PathType, Float>> SAVED = new ConcurrentHashMap<>();

    public static void apply(Mob mob) {
        if (mob.level().isClientSide()) return;
        UUID id = mob.getUUID();
        EnumMap<PathType, Float> original = SAVED.get(id);
        if (original == null) {
            original = new EnumMap<>(PathType.class);
            for (PathType type : PathType.values()) {
                if (!isHazardType(type)) continue;
                original.put(type, mob.getPathfindingMalus(type));
            }
            SAVED.put(id, original);
        }
        for (PathType type : original.keySet()) {
            mob.setPathfindingMalus(type, 0.0f);
        }
    }

    public static void restore(Mob mob) {
        if (mob.level().isClientSide()) return;
        EnumMap<PathType, Float> original = SAVED.remove(mob.getUUID());
        if (original == null) return;
        for (var e : original.entrySet()) {
            mob.setPathfindingMalus(e.getKey(), e.getValue());
        }
    }

    public static void clear(UUID id) {
        SAVED.remove(id);
    }

    public static boolean isTracked(Mob mob) {
        return SAVED.containsKey(mob.getUUID());
    }

    /** Solo fuego / lava / daño ambiental — no water, fence, blocked, etc. */
    private static boolean isHazardType(PathType type) {
        String name = type.name();
        return name.contains("FIRE")
                || name.contains("LAVA")
                || name.contains("DAMAGE")
                || name.contains("DANGER")
                || name.contains("POWDER_SNOW");
    }
}
