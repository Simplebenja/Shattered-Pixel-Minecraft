package com.drbenja.spd.util;

/**
 * Minecraft enchantment levels are 1-based (1, 2, 3…).
 * Shattered Pixel Dungeon glyph/enchant math is usually 0-based (0, 1, 2…).
 * Use {@link #spd(int)} when porting SPD formulas that start at 0.
 */
public final class EnchantLevels {
    private EnchantLevels() {}

    /** Convert MC level (1+) to SPD-style level (0+). */
    public static int spd(int mcLevel) {
        return Math.max(0, mcLevel - 1);
    }
}
