package com.drbenja.spd.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, SpdMod.MOD_ID);

    public static final DeferredHolder<Potion, Potion> REGENERATIVE_HEALING_0 = POTIONS.register(
            "potion_of_regenerative_healing_0",
            () -> new Potion("potion_of_regenerative_healing_0",
                    new MobEffectInstance(ModEffects.REGENERATIVE_HEALING, 3600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> REGENERATIVE_HEALING_EXT = POTIONS.register(
            "potion_of_regenerative_healing_ext",
            () -> new Potion("potion_of_regenerative_healing_ext",
                    new MobEffectInstance(ModEffects.REGENERATIVE_HEALING, 9600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> REGENERATIVE_HEALING_1 = POTIONS.register(
            "potion_of_regenerative_healing_1",
            () -> new Potion("potion_of_regenerative_healing_1",
                    new MobEffectInstance(ModEffects.REGENERATIVE_HEALING, 1800, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> REGENERATIVE_HEALING_2 = POTIONS.register(
            "potion_of_regenerative_healing_2",
            () -> new Potion("potion_of_regenerative_healing_2",
                    new MobEffectInstance(ModEffects.REGENERATIVE_HEALING, 1200, 2, false, true)));

    public static final DeferredHolder<Potion, Potion> REGENERATIVE_HEALING_1_EXT = POTIONS.register(
            "potion_of_regenerative_healing_1_ext",
            () -> new Potion("potion_of_regenerative_healing_1_ext",
                    new MobEffectInstance(ModEffects.REGENERATIVE_HEALING, 3600, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> HONEYED_HEALING_0 = POTIONS.register(
            "elixir_of_honeyed_healing_0",
            () -> new Potion("elixir_of_honeyed_healing_0",
                    new MobEffectInstance(ModEffects.HONEYED_HEALING, 3600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> HONEYED_HEALING_EXT = POTIONS.register(
            "elixir_of_honeyed_healing_ext",
            () -> new Potion("elixir_of_honeyed_healing_ext",
                    new MobEffectInstance(ModEffects.HONEYED_HEALING, 9600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> HONEYED_HEALING_1 = POTIONS.register(
            "elixir_of_honeyed_healing_1",
            () -> new Potion("elixir_of_honeyed_healing_1",
                    new MobEffectInstance(ModEffects.HONEYED_HEALING, 1800, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> HONEYED_HEALING_2 = POTIONS.register(
            "elixir_of_honeyed_healing_2",
            () -> new Potion("elixir_of_honeyed_healing_2",
                    new MobEffectInstance(ModEffects.HONEYED_HEALING, 1200, 2, false, true)));

    public static final DeferredHolder<Potion, Potion> HONEYED_HEALING_1_EXT = POTIONS.register(
            "elixir_of_honeyed_healing_1_ext",
            () -> new Potion("elixir_of_honeyed_healing_1_ext",
                    new MobEffectInstance(ModEffects.HONEYED_HEALING, 3600, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> AQUATIC_HEALING_0 = POTIONS.register(
            "elixir_of_aquatic_rejuvenation_0",
            () -> new Potion("elixir_of_aquatic_rejuvenation_0",
                    new MobEffectInstance(ModEffects.AQUATIC_HEALING, 3600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> AQUATIC_HEALING_EXT = POTIONS.register(
            "elixir_of_aquatic_rejuvenation_ext",
            () -> new Potion("elixir_of_aquatic_rejuvenation_ext",
                    new MobEffectInstance(ModEffects.AQUATIC_HEALING, 9600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> AQUATIC_HEALING_1 = POTIONS.register(
            "elixir_of_aquatic_rejuvenation_1",
            () -> new Potion("elixir_of_aquatic_rejuvenation_1",
                    new MobEffectInstance(ModEffects.AQUATIC_HEALING, 1800, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> AQUATIC_HEALING_2 = POTIONS.register(
            "elixir_of_aquatic_rejuvenation_2",
            () -> new Potion("elixir_of_aquatic_rejuvenation_2",
                    new MobEffectInstance(ModEffects.AQUATIC_HEALING, 1200, 2, false, true)));

    public static final DeferredHolder<Potion, Potion> AQUATIC_HEALING_1_EXT = POTIONS.register(
            "elixir_of_aquatic_rejuvenation_1_ext",
            () -> new Potion("elixir_of_aquatic_rejuvenation_1_ext",
                    new MobEffectInstance(ModEffects.AQUATIC_HEALING, 3600, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> SHIELDING_0 = POTIONS.register(
            "potion_of_shielding_0",
            () -> new Potion("potion_of_shielding_0",
                    new MobEffectInstance(ModEffects.BARRIER, 3600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> SHIELDING_EXT = POTIONS.register(
            "potion_of_shielding_ext",
            () -> new Potion("potion_of_shielding_ext",
                    new MobEffectInstance(ModEffects.BARRIER, 9600, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> SHIELDING_1 = POTIONS.register(
            "potion_of_shielding_1",
            () -> new Potion("potion_of_shielding_1",
                    new MobEffectInstance(ModEffects.BARRIER, 1800, 1, false, true)));

    public static final DeferredHolder<Potion, Potion> SHIELDING_2 = POTIONS.register(
            "potion_of_shielding_2",
            () -> new Potion("potion_of_shielding_2",
                    new MobEffectInstance(ModEffects.BARRIER, 1200, 2, false, true)));

    public static final DeferredHolder<Potion, Potion> SHIELDING_1_EXT = POTIONS.register(
            "potion_of_shielding_1_ext",
            () -> new Potion("potion_of_shielding_1_ext",
                    new MobEffectInstance(ModEffects.BARRIER, 3600, 1, false, true)));

    /** Instant — duration 1. amp 0 = +1 level bar; amp 1 = +2 level bars (SPD-style). */
    public static final DeferredHolder<Potion, Potion> EXPERIENCE_0 = POTIONS.register(
            "potion_of_experience_0",
            () -> new Potion("potion_of_experience_0",
                    new MobEffectInstance(ModEffects.EXPERIENCE, 1, 0, false, true)));

    public static final DeferredHolder<Potion, Potion> EXPERIENCE_1 = POTIONS.register(
            "potion_of_experience_1",
            () -> new Potion("potion_of_experience_1",
                    new MobEffectInstance(ModEffects.EXPERIENCE, 1, 1, false, true)));

    // Haste: 20s / 53s / 10s amp1 — ticks 400 / 1060 / 200
    public static final DeferredHolder<Potion, Potion> HASTE_0 = POTIONS.register(
            "potion_of_haste_0",
            () -> new Potion("potion_of_haste_0",
                    new MobEffectInstance(ModEffects.HASTE, 400, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> HASTE_EXT = POTIONS.register(
            "potion_of_haste_ext",
            () -> new Potion("potion_of_haste_ext",
                    new MobEffectInstance(ModEffects.HASTE, 1060, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> HASTE_1 = POTIONS.register(
            "potion_of_haste_1",
            () -> new Potion("potion_of_haste_1",
                    new MobEffectInstance(ModEffects.HASTE, 200, 1, false, true)));

    // Stamina: 100s / 266s / 50s amp1 — ticks 2000 / 5320 / 1000
    public static final DeferredHolder<Potion, Potion> STAMINA_0 = POTIONS.register(
            "potion_of_stamina_0",
            () -> new Potion("potion_of_stamina_0",
                    new MobEffectInstance(ModEffects.STAMINA, 2000, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> STAMINA_EXT = POTIONS.register(
            "potion_of_stamina_ext",
            () -> new Potion("potion_of_stamina_ext",
                    new MobEffectInstance(ModEffects.STAMINA, 5320, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> STAMINA_1 = POTIONS.register(
            "potion_of_stamina_1",
            () -> new Potion("potion_of_stamina_1",
                    new MobEffectInstance(ModEffects.STAMINA, 1000, 1, false, true)));

    // Purity: 20s / 53s — no powered
    public static final DeferredHolder<Potion, Potion> PURITY_0 = POTIONS.register(
            "potion_of_purity_0",
            () -> new Potion("potion_of_purity_0",
                    new MobEffectInstance(ModEffects.PURIFICATION_BARRIER, 400, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> PURITY_EXT = POTIONS.register(
            "potion_of_purity_ext",
            () -> new Potion("potion_of_purity_ext",
                    new MobEffectInstance(ModEffects.PURIFICATION_BARRIER, 1060, 0, false, true)));

    // Cleansing: 5s / 10s — no powered
    public static final DeferredHolder<Potion, Potion> CLEANSING_0 = POTIONS.register(
            "potion_of_cleansing_0",
            () -> new Potion("potion_of_cleansing_0",
                    new MobEffectInstance(ModEffects.CLEANSED, 100, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> CLEANSING_EXT = POTIONS.register(
            "potion_of_cleansing_ext",
            () -> new Potion("potion_of_cleansing_ext",
                    new MobEffectInstance(ModEffects.CLEANSED, 200, 0, false, true)));

    // Fixed hover levitation: 20s / 53s — no powered
    public static final DeferredHolder<Potion, Potion> LEVITATION_0 = POTIONS.register(
            "potion_of_levitation_0",
            () -> new Potion("potion_of_levitation_0",
                    new MobEffectInstance(ModEffects.FIXED_HOVER, 400, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> LEVITATION_EXT = POTIONS.register(
            "potion_of_levitation_ext",
            () -> new Potion("potion_of_levitation_ext",
                    new MobEffectInstance(ModEffects.FIXED_HOVER, 1060, 0, false, true)));


    // Elixir of Might: strength durations + Max Health Boost (amp 1 = 2x HP boost)
    // strength 3:00 / long 8:00 / strong 1:30
    public static final DeferredHolder<Potion, Potion> MIGHT_0 = POTIONS.register(
            "elixir_of_might_0",
            () -> new Potion("elixir_of_might_0",
                    new MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 3600, 0, false, true),
                    new MobEffectInstance(ModEffects.MAX_HEALTH_BOOST, 3600, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> MIGHT_EXT = POTIONS.register(
            "elixir_of_might_ext",
            () -> new Potion("elixir_of_might_ext",
                    new MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 9600, 0, false, true),
                    new MobEffectInstance(ModEffects.MAX_HEALTH_BOOST, 9600, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> MIGHT_1 = POTIONS.register(
            "elixir_of_might_1",
            () -> new Potion("elixir_of_might_1",
                    new MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 1800, 1, false, true),
                    new MobEffectInstance(ModEffects.MAX_HEALTH_BOOST, 1800, 1, false, true)));


    // True invisibility: 20s / 53s — no powered. Crafted from vanilla invis + glowstone.
    public static final DeferredHolder<Potion, Potion> INVISIBILITY_0 = POTIONS.register(
            "potion_of_invisibility_0",
            () -> new Potion("potion_of_invisibility_0",
                    new MobEffectInstance(ModEffects.TRUE_INVISIBILITY, 400, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> INVISIBILITY_EXT = POTIONS.register(
            "potion_of_invisibility_ext",
            () -> new Potion("potion_of_invisibility_ext",
                    new MobEffectInstance(ModEffects.TRUE_INVISIBILITY, 1060, 0, false, true)));


    // Mind Vision: 20s / 53s / powered 10s (6 chunks). amp0=3 chunks, amp1=6 chunks
    public static final DeferredHolder<Potion, Potion> MIND_VISION_0 = POTIONS.register(
            "potion_of_mind_vision_0",
            () -> new Potion("potion_of_mind_vision_0",
                    new MobEffectInstance(ModEffects.MIND_VISION, 400, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> MIND_VISION_EXT = POTIONS.register(
            "potion_of_mind_vision_ext",
            () -> new Potion("potion_of_mind_vision_ext",
                    new MobEffectInstance(ModEffects.MIND_VISION, 1060, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> MIND_VISION_1 = POTIONS.register(
            "potion_of_mind_vision_1",
            () -> new Potion("potion_of_mind_vision_1",
                    new MobEffectInstance(ModEffects.MIND_VISION, 200, 1, false, true)));

    /** Blob splash: sin efecto al beber; el handler crea la nube de escarcha. */
    public static final DeferredHolder<Potion, Potion> FROST = POTIONS.register(
            "potion_of_frost",
            () -> new Potion("potion_of_frost"));

    /**
     * Snap Freeze: marker effect only for bottle tooltip ("Frozen 00:10").
     * Real Frozen is applied only in 3×3×3 by SnapFreezeSplashHandler; marker is stripped on entities.
     */
    public static final DeferredHolder<Potion, Potion> SNAP_FREEZE = POTIONS.register(
            "potion_of_snap_freeze",
            () -> new Potion("potion_of_snap_freeze",
                    new MobEffectInstance(ModEffects.SNAP_FREEZE_MARKER, 200, 0, false, true, true)));

    // Elixir of Icy Touch: 50s / 100s / 25s powered (amp1 = 6s chill on hit)
    public static final DeferredHolder<Potion, Potion> ICY_TOUCH_0 = POTIONS.register(
            "elixir_of_icy_touch_0",
            () -> new Potion("elixir_of_icy_touch_0",
                    new MobEffectInstance(ModEffects.IMBUED_WITH_FROST, 1000, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> ICY_TOUCH_EXT = POTIONS.register(
            "elixir_of_icy_touch_ext",
            () -> new Potion("elixir_of_icy_touch_ext",
                    new MobEffectInstance(ModEffects.IMBUED_WITH_FROST, 2000, 0, false, true)));
    public static final DeferredHolder<Potion, Potion> ICY_TOUCH_1 = POTIONS.register(
            "elixir_of_icy_touch_1",
            () -> new Potion("elixir_of_icy_touch_1",
                    new MobEffectInstance(ModEffects.IMBUED_WITH_FROST, 500, 1, false, true)));

    /** Splash only: incendio 3×3×3 + Burning. */
    public static final DeferredHolder<Potion, Potion> LIQUID_FLAME = POTIONS.register(
            "potion_of_liquid_flame",
            () -> new Potion("potion_of_liquid_flame"));

    /** Bebible: cono de fuego al beber (efecto marcador 1 tick). */
    public static final DeferredHolder<Potion, Potion> DRAGON_BREATH = POTIONS.register(
            "potion_of_dragons_breath",
            () -> new Potion("potion_of_dragons_breath",
                    new MobEffectInstance(ModEffects.DRAGON_BREATH, 1, 0, false, false, true)));

    public static final DeferredHolder<Potion, Potion> DRAGONS_BLOOD = POTIONS.register(
            "elixir_of_dragons_blood",
            () -> new Potion("elixir_of_dragons_blood",
                    new MobEffectInstance(ModEffects.IMBUED_WITH_FIRE, 1000, 0, false, true)));

    /** Splash: blob de gas paralítico que se difunde. */
    public static final DeferredHolder<Potion, Potion> PARALYTIC_GAS = POTIONS.register(
            "potion_of_paralytic_gas",
            () -> new Potion("potion_of_paralytic_gas"));

    /**
     * Potion of Earthen Armor: aplica Barkskin según max HP.
     * Escalones de 50 s; potencia = 2 + ((maxHP-15)/5)/3 (división entera).
     */
    public static final DeferredHolder<Potion, Potion> EARTHEN_ARMOR = POTIONS.register(
            "potion_of_earthen_armor",
            () -> new Potion("potion_of_earthen_armor",
                    new MobEffectInstance(ModEffects.EARTHEN_ARMOR, 1000, 0, false, false, false)));

    /** Elixir of Arcane Armor: 80 s por escalón; bloqueo mágico aleatorio 0..nivel. */
    public static final DeferredHolder<Potion, Potion> ARCANE_ARMOR = POTIONS.register(
            "elixir_of_arcane_armor",
            () -> new Potion("elixir_of_arcane_armor",
                    new MobEffectInstance(ModEffects.ARCANE_ARMOR_APPLY, 1600, 0, false, false, false)));

    /** Splash: nube de gas tóxico (1 dmg/s magic, no undead). */
    public static final DeferredHolder<Potion, Potion> TOXIC_GAS = POTIONS.register(
            "potion_of_toxic_gas",
            () -> new Potion("potion_of_toxic_gas"));

    /** Splash: nube corrosiva más densa (dmg escalante, tipo spd:corrosive). */
    public static final DeferredHolder<Potion, Potion> CORROSIVE_GAS = POTIONS.register(
            "potion_of_corrosive_gas",
            () -> new Potion("potion_of_corrosive_gas"));

    /** Elixir: Imbued with Toxicity 50 s. */
    public static final DeferredHolder<Potion, Potion> TOXIC_ESSENCE = POTIONS.register(
            "elixir_of_toxic_essence",
            () -> new Potion("elixir_of_toxic_essence",
                    new MobEffectInstance(ModEffects.IMBUED_WITH_TOXICITY, 1000, 0, false, true)));

    /** Splash: Shrouding Fog → Blinded. Craft: true invis + lapis. */
    public static final DeferredHolder<Potion, Potion> SHROUDING_FOG = POTIONS.register(
            "potion_of_shrouding_fog",
            () -> new Potion("potion_of_shrouding_fog"));

    /** Splash: Storm Clouds. Craft: levitation + lapis. */
    public static final DeferredHolder<Potion, Potion> STORM_CLOUDS = POTIONS.register(
            "potion_of_storm_clouds",
            () -> new Potion("potion_of_storm_clouds"));

    public static final DeferredHolder<Potion, Potion> INFERNAL_BREW = POTIONS.register(
            "infernal_brew", () -> new Potion("infernal_brew"));
    public static final DeferredHolder<Potion, Potion> BLIZZARD_BREW = POTIONS.register(
            "blizzard_brew", () -> new Potion("blizzard_brew"));
    public static final DeferredHolder<Potion, Potion> SHOCKING_BREW = POTIONS.register(
            "shocking_brew", () -> new Potion("shocking_brew"));
    public static final DeferredHolder<Potion, Potion> CAUSTIC_BREW = POTIONS.register(
            "caustic_brew", () -> new Potion("caustic_brew"));

    private ModPotions() {}
}
