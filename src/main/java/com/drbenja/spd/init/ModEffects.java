package com.drbenja.spd.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.effect.AquaticHealingEffect;
import com.drbenja.spd.effect.AquaticHealingGuiEffect;
import com.drbenja.spd.effect.BarrierEffect;
import com.drbenja.spd.effect.BleedingEffect;
import com.drbenja.spd.effect.PoisonedEffect;
import com.drbenja.spd.effect.CausticOozeEffect;
import com.drbenja.spd.effect.DungeonModeEffect;
import com.drbenja.spd.effect.ExperienceEffect;
import com.drbenja.spd.effect.HasteEffect;
import com.drbenja.spd.effect.StaminaEffect;
import com.drbenja.spd.effect.PurificationBarrierEffect;
import com.drbenja.spd.effect.CleansedEffect;
import com.drbenja.spd.effect.FixedHoverEffect;
import com.drbenja.spd.effect.MaxHealthBoostEffect;
import com.drbenja.spd.effect.TrueInvisibilityEffect;
import com.drbenja.spd.effect.MindVisionEffect;
import com.drbenja.spd.effect.ChilledEffect;
import com.drbenja.spd.effect.FrozenEffect;
import com.drbenja.spd.effect.SnapFreezeMarkerEffect;
import com.drbenja.spd.effect.BurningEffect;
import com.drbenja.spd.effect.CrippledEffect;
import com.drbenja.spd.effect.DragonBreathEffect;
import com.drbenja.spd.effect.ImbuedWithFireEffect;
import com.drbenja.spd.effect.ParalyzedEffect;
import com.drbenja.spd.effect.BarkskinEffect;
import com.drbenja.spd.effect.EarthenArmorEffect;
import com.drbenja.spd.effect.ArcaneArmorEffect;
import com.drbenja.spd.effect.ArcaneArmorApplyEffect;
import com.drbenja.spd.effect.ImbuedWithToxicityEffect;
import com.drbenja.spd.effect.BlindedEffect;
import com.drbenja.spd.effect.BlockingEffect;
import com.drbenja.spd.effect.CorruptedEffect;
import com.drbenja.spd.effect.DoomedEffect;
import com.drbenja.spd.effect.DeferredDamageEffect;
import com.drbenja.spd.effect.HerbalArmorEffect;
import com.drbenja.spd.effect.HerbalArmorGuiEffect;
import com.drbenja.spd.effect.CharmedEffect;
import com.drbenja.spd.effect.TerrorEffect;
import com.drbenja.spd.effect.ChallengeZoneEffect;
import com.drbenja.spd.effect.AntiMagicEffect;
import com.drbenja.spd.effect.WeaknessSpdEffect;
import com.drbenja.spd.effect.EnthralledEffect;
import com.drbenja.spd.effect.AggressionEffect;
import com.drbenja.spd.effect.AmokEffect;
import com.drbenja.spd.effect.MagicalSleepEffect;
import com.drbenja.spd.effect.DrowsyEffect;
import com.drbenja.spd.effect.DreadEffect;

import com.drbenja.spd.effect.KineticChargeEffect;
import com.drbenja.spd.effect.ImbuedWithFrostEffect;
import com.drbenja.spd.effect.HoneyedHealingEffect;
import com.drbenja.spd.effect.HoneyedHealingGuiEffect;
import com.drbenja.spd.effect.RegenerativeHealingEffect;
import com.drbenja.spd.effect.RegenerativeHealingGuiEffect;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, SpdMod.MOD_ID);

    public static final DeferredHolder<MobEffect, RegenerativeHealingEffect> REGENERATIVE_HEALING =
            EFFECTS.register("regenerative_healing", RegenerativeHealingEffect::new);
    public static final DeferredHolder<MobEffect, RegenerativeHealingGuiEffect> REGENERATIVE_HEALING_GUI =
            EFFECTS.register("regenerative_healing_gui", RegenerativeHealingGuiEffect::new);

    public static final DeferredHolder<MobEffect, HoneyedHealingEffect> HONEYED_HEALING =
            EFFECTS.register("honeyed_healing", HoneyedHealingEffect::new);
    public static final DeferredHolder<MobEffect, HoneyedHealingGuiEffect> HONEYED_HEALING_GUI =
            EFFECTS.register("honeyed_healing_gui", HoneyedHealingGuiEffect::new);

    public static final DeferredHolder<MobEffect, AquaticHealingEffect> AQUATIC_HEALING =
            EFFECTS.register("aquatic_healing", AquaticHealingEffect::new);
    public static final DeferredHolder<MobEffect, AquaticHealingGuiEffect> AQUATIC_HEALING_GUI =
            EFFECTS.register("aquatic_healing_gui", AquaticHealingGuiEffect::new);

    public static final DeferredHolder<MobEffect, BarrierEffect> BARRIER =
            EFFECTS.register("barrier", BarrierEffect::new);

    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingEffect::new);

    public static final DeferredHolder<MobEffect, PoisonedEffect> POISONED =
            EFFECTS.register("poisoned", PoisonedEffect::new);
    public static final DeferredHolder<MobEffect, CausticOozeEffect> CAUSTIC_OOZE =
            EFFECTS.register("caustic_ooze", CausticOozeEffect::new);
    public static final DeferredHolder<MobEffect, DungeonModeEffect> DUNGEON_MODE =
            EFFECTS.register("dungeon_mode", DungeonModeEffect::new);

    public static final DeferredHolder<MobEffect, ExperienceEffect> EXPERIENCE =
            EFFECTS.register("experience", ExperienceEffect::new);

    public static final DeferredHolder<MobEffect, HasteEffect> HASTE =
            EFFECTS.register("haste", HasteEffect::new);

    public static final DeferredHolder<MobEffect, StaminaEffect> STAMINA =
            EFFECTS.register("stamina", StaminaEffect::new);

    public static final DeferredHolder<MobEffect, PurificationBarrierEffect> PURIFICATION_BARRIER =
            EFFECTS.register("purification_barrier", PurificationBarrierEffect::new);

    public static final DeferredHolder<MobEffect, CleansedEffect> CLEANSED =
            EFFECTS.register("cleansed", CleansedEffect::new);

    public static final DeferredHolder<MobEffect, FixedHoverEffect> FIXED_HOVER =
            EFFECTS.register("fixed_hover", FixedHoverEffect::new);

    public static final DeferredHolder<MobEffect, MaxHealthBoostEffect> MAX_HEALTH_BOOST =
            EFFECTS.register("max_health_boost", MaxHealthBoostEffect::new);

    public static final DeferredHolder<MobEffect, TrueInvisibilityEffect> TRUE_INVISIBILITY =
            EFFECTS.register("invisible", TrueInvisibilityEffect::new);

    public static final DeferredHolder<MobEffect, MindVisionEffect> MIND_VISION =
            EFFECTS.register("mind_vision", MindVisionEffect::new);

    public static final DeferredHolder<MobEffect, ChilledEffect> CHILLED =
            EFFECTS.register("chilled", ChilledEffect::new);

    public static final DeferredHolder<MobEffect, FrozenEffect> FROZEN =
            EFFECTS.register("frozen", FrozenEffect::new);

    /** Tooltip-only marker for Snap Freeze potion (no gameplay). */
    public static final DeferredHolder<MobEffect, SnapFreezeMarkerEffect> SNAP_FREEZE_MARKER =
            EFFECTS.register("snap_freeze_marker", SnapFreezeMarkerEffect::new);

    public static final DeferredHolder<MobEffect, ImbuedWithFrostEffect> IMBUED_WITH_FROST =
            EFFECTS.register("imbued_with_frost", ImbuedWithFrostEffect::new);

    public static final DeferredHolder<MobEffect, BurningEffect> BURNING =
            EFFECTS.register("burning", BurningEffect::new);

    public static final DeferredHolder<MobEffect, CrippledEffect> CRIPPLED =
            EFFECTS.register("crippled", CrippledEffect::new);

    public static final DeferredHolder<MobEffect, DragonBreathEffect> DRAGON_BREATH =
            EFFECTS.register("dragon_breath", DragonBreathEffect::new);

    public static final DeferredHolder<MobEffect, ImbuedWithFireEffect> IMBUED_WITH_FIRE =
            EFFECTS.register("imbued_with_fire", ImbuedWithFireEffect::new);

    public static final DeferredHolder<MobEffect, ParalyzedEffect> PARALYZED =
            EFFECTS.register("paralyzed", ParalyzedEffect::new);

    public static final DeferredHolder<MobEffect, BarkskinEffect> BARKSKIN =
            EFFECTS.register("barkskin", BarkskinEffect::new);

    public static final DeferredHolder<MobEffect, EarthenArmorEffect> EARTHEN_ARMOR =
            EFFECTS.register("earthen_armor", EarthenArmorEffect::new);

    public static final DeferredHolder<MobEffect, ArcaneArmorEffect> ARCANE_ARMOR =
            EFFECTS.register("arcane_armor", ArcaneArmorEffect::new);

    public static final DeferredHolder<MobEffect, ArcaneArmorApplyEffect> ARCANE_ARMOR_APPLY =
            EFFECTS.register("arcane_armor_apply", ArcaneArmorApplyEffect::new);

    public static final DeferredHolder<MobEffect, ImbuedWithToxicityEffect> IMBUED_WITH_TOXICITY =
            EFFECTS.register("imbued_with_toxicity", ImbuedWithToxicityEffect::new);

    public static final DeferredHolder<MobEffect, BlindedEffect> BLINDED =
            EFFECTS.register("blinded", BlindedEffect::new);


    public static final DeferredHolder<MobEffect, BlockingEffect> BLOCKING =
            EFFECTS.register("blocking", BlockingEffect::new);


    public static final DeferredHolder<MobEffect, KineticChargeEffect> KINETIC_CHARGE =
            EFFECTS.register("kinetic_charge", () -> new KineticChargeEffect(KineticChargeEffect.Tier.LOW));

    public static final DeferredHolder<MobEffect, KineticChargeEffect> KINETIC_CHARGE_MID =
            EFFECTS.register("kinetic_charge_mid", () -> new KineticChargeEffect(KineticChargeEffect.Tier.MID));

    public static final DeferredHolder<MobEffect, KineticChargeEffect> KINETIC_CHARGE_HIGH =
            EFFECTS.register("kinetic_charge_high", () -> new KineticChargeEffect(KineticChargeEffect.Tier.HIGH));

    public static final DeferredHolder<MobEffect, CorruptedEffect> CORRUPTED =
            EFFECTS.register("corrupted", CorruptedEffect::new);

    public static final DeferredHolder<MobEffect, DoomedEffect> DOOMED =
            EFFECTS.register("doomed", DoomedEffect::new);

    public static final DeferredHolder<MobEffect, DeferredDamageEffect> DEFERRED_DAMAGE =
            EFFECTS.register("deferred_damage", DeferredDamageEffect::new);

    public static final DeferredHolder<MobEffect, HerbalArmorEffect> HERBAL_ARMOR =
            EFFECTS.register("herbal_armor", HerbalArmorEffect::new);

    public static final DeferredHolder<MobEffect, HerbalArmorGuiEffect> HERBAL_ARMOR_GUI =
            EFFECTS.register("herbal_armor_gui", HerbalArmorGuiEffect::new);

    public static final DeferredHolder<MobEffect, CharmedEffect> CHARMED =
            EFFECTS.register("charmed", CharmedEffect::new);

    public static final DeferredHolder<MobEffect, TerrorEffect> TERROR =
            EFFECTS.register("terror", TerrorEffect::new);



    public static final DeferredHolder<MobEffect, DreadEffect> DREAD =
            EFFECTS.register("dread", DreadEffect::new);
    public static final DeferredHolder<MobEffect, DrowsyEffect> DROWSY =
            EFFECTS.register("drowsy", DrowsyEffect::new);
    public static final DeferredHolder<MobEffect, MagicalSleepEffect> MAGICAL_SLEEP =
            EFFECTS.register("magical_sleep", MagicalSleepEffect::new);
    public static final DeferredHolder<MobEffect, AmokEffect> AMOK =
            EFFECTS.register("amok", AmokEffect::new);
    public static final DeferredHolder<MobEffect, EnthralledEffect> ENTHRALLED =
            EFFECTS.register("enthralled", EnthralledEffect::new);
    public static final DeferredHolder<MobEffect, WeaknessSpdEffect> WEAKNESS_SPD =
            EFFECTS.register("weakness_spd", WeaknessSpdEffect::new);
    public static final DeferredHolder<MobEffect, AntiMagicEffect> ANTI_MAGIC =
            EFFECTS.register("anti_magic", AntiMagicEffect::new);
    public static final DeferredHolder<MobEffect, ChallengeZoneEffect> CHALLENGE_ZONE =
            EFFECTS.register("challenge_zone", ChallengeZoneEffect::new);

    public static final DeferredHolder<MobEffect, AggressionEffect> AGGRESSION =
            EFFECTS.register("aggression", AggressionEffect::new);


    private ModEffects() {}
}

