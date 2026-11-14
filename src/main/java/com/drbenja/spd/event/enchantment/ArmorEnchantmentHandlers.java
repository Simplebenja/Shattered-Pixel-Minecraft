package com.drbenja.spd.event.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import com.drbenja.spd.SpdMod;

/**
 * Legacy KEY constants for armor glyphs. Logic lives in individual handlers.
 */
public final class ArmorEnchantmentHandlers {

    public static final ResourceKey<Enchantment> OBFUSCATION = ObfuscationEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> SWIFTNESS = SwiftnessEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> VISCOSITY = ViscosityEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> POTENTIAL = PotentialEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> BRIMSTONE = BrimstoneEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> ENTANGLEMENT = EntanglementEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> REPULSION = RepulsionEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> CAMOUFLAGE = CamouflageEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> FLOW = FlowEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> AFFECTION = AffectionEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> ANTI_MAGIC = AntiMagicEnchantmentHandler.KEY;
    public static final ResourceKey<Enchantment> BLOODY_THORNS = BloodyThornsEnchantmentHandler.KEY;

    private ArmorEnchantmentHandlers() {}
}

