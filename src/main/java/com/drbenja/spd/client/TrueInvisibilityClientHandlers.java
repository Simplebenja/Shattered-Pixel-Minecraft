package com.drbenja.spd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/**
 * True Invisibility visuals: hide the full entity (body + armor + held items)
 * for all viewers, and hide first-person hands/items for the local player.
 * <p>
 * NeoForge 26.3: {@link RenderLivingEvent} is {@code <T, S, M>} and has no
 * {@code getEntity()}. Match render-state pose against entity bounding boxes
 * with generous vertical slack so interpolated Y (jump/fall/fly) does not flicker.
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID, value = Dist.CLIENT)
public final class TrueInvisibilityClientHandlers {

    /** Extra slack for pose interpolation (especially vertical movement). */
    private static final double MATCH_INFLATE = 1.25;

    private TrueInvisibilityClientHandlers() {}

    private static boolean trulyInvisible(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.TRUE_INVISIBILITY);
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        var state = event.getRenderState();
        double x = state.x;
        double y = state.y;
        double z = state.z;

        LocalPlayer player = mc.player;
        if (trulyInvisible(player) && boxContainsPose(player, x, y, z)) {
            event.setCanceled(true);
            return;
        }

        AABB probe = new AABB(x, y, z, x, y, z).inflate(MATCH_INFLATE);
        for (LivingEntity e : mc.level.getEntitiesOfClass(
                LivingEntity.class, probe, TrueInvisibilityClientHandlers::trulyInvisible)) {
            if (boxContainsPose(e, x, y, z)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * True if the render-state pose lies inside the entity's bounding box
     * expanded for partial-tick lag (inflate more on Y for jump/fall).
     */
    private static boolean boxContainsPose(LivingEntity e, double x, double y, double z) {
        AABB bb = e.getBoundingBox().inflate(0.75, 1.5, 0.75);
        return bb.contains(x, y, z) || bb.contains(x, y + e.getBbHeight() * 0.5, z);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (trulyInvisible(player)) {
            event.setCanceled(true);
        }
    }
}
