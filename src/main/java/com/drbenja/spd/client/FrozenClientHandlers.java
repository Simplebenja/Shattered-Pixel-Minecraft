package com.drbenja.spd.client;

import com.drbenja.spd.event.effect.ImmobilizeHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.drbenja.spd.SpdMod;
import com.drbenja.spd.init.ModEffects;

/**
 * Frozen (cliente): anula teclas de movimiento vía ClientInput / Input.EMPTY.
 * No toca deltaMovement (física libre).
 */
@EventBusSubscriber(modid = SpdMod.MOD_ID, value = Dist.CLIENT)
public final class FrozenClientHandlers {

    private static float lockedYRot;
    private static float lockedXRot;
    private static boolean hasLock;
    private static int lockedHotbar = 0;
    private static ItemStack lockedOffhand = ItemStack.EMPTY;
    private static ItemStack lockedMainHand = ItemStack.EMPTY;

    private FrozenClientHandlers() {}

    /** Cliente: misma regla que ImmobilizeHandlers (Frozen / Paralyzed / Magical Sleep). */
    private static boolean isFrozen(Player p) {
        return ImmobilizeHandlers.isImmobilized(p);
    }

    private static void captureLocks(LocalPlayer player) {
        lockedYRot = player.getYRot();
        lockedXRot = player.getXRot();
        lockedHotbar = player.getInventory().getSelectedSlot();
        lockedOffhand = player.getOffhandItem().copy();
        lockedMainHand = player.getMainHandItem().copy();
        hasLock = true;
    }

    private static void applyUiLocks(LocalPlayer player) {
        zeroMouseAccumulated(Minecraft.getInstance());
        player.setYRot(lockedYRot);
        player.setXRot(lockedXRot);
        player.yRotO = lockedYRot;
        player.xRotO = lockedXRot;

        var inv = player.getInventory();
        if (inv.getSelectedSlot() != lockedHotbar) {
            inv.setSelectedSlot(lockedHotbar);
        }
        ItemStack off = player.getOffhandItem();
        ItemStack main = player.getMainHandItem();
        if (!ItemStack.isSameItemSameComponents(off, lockedOffhand)
                || !ItemStack.isSameItemSameComponents(main, lockedMainHand)) {
            player.setItemInHand(InteractionHand.OFF_HAND, lockedOffhand.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, lockedMainHand.copy());
        }
    }

    /**
     * API 26.3: ClientInput.keyPresses (Input record) + moveVector.
     * tick() ya rellenó el vector desde las teclas; hay que limpiar ambos.
     */
    private static void clearMovementInput(ClientInput input) {
        input.keyPresses = Input.EMPTY;
        try {
            var f = ClientInput.class.getDeclaredField("moveVector");
            f.setAccessible(true);
            f.set(input, Vec2.ZERO);
        } catch (ReflectiveOperationException e) {
            // Fallback: intentar nombre ofuscado no; Vec2.ZERO vía getMoveVector no es setter
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isFrozen(event.getEntity())) {
            return;
        }
        clearMovementInput(event.getInput());
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !isFrozen(player)) {
            hasLock = false;
            return;
        }
        if (!hasLock) {
            captureLocks(player);
        }
        applyUiLocks(player);
        // Por si el input se actualiza fuera del evento
        if (player.input != null) {
            clearMovementInput(player.input);
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        // Client-side freeze HUD (frost hearts / overlay): ticksFrozen is reduced each tick
        // unless kept high. No freeze damage without powder-snow damage source.
        if (player.hasEffect(ModEffects.FROZEN)) {
            try {
                int need = player.getTicksRequiredToFreeze();
                if (player.getTicksFrozen() < need) {
                    player.setTicksFrozen(need);
                }
            } catch (Throwable ignored) {
            }
        }
        if (!isFrozen(player) || !hasLock) {
            return;
        }
        applyUiLocks(player);
        if (player.input != null) {
            clearMovementInput(player.input);
        }
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isFrozen(mc.player) || !hasLock) {
            return;
        }
        event.setYaw(lockedYRot);
        event.setPitch(lockedXRot);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (isFrozen(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (isFrozen(Minecraft.getInstance().player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static void zeroMouseAccumulated(Minecraft mc) {
        try {
            Object mouse = mc.getClass().getField("mouseHandler").get(mc);
            Class<?> c = mouse.getClass();
            for (String name : new String[]{"accumulatedDX", "accumulatedDY", "accumulatedDeltaX", "accumulatedDeltaY"}) {
                try {
                    var f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    if (f.getType() == double.class) {
                        f.setDouble(mouse, 0.0D);
                    } else if (f.getType() == float.class) {
                        f.setFloat(mouse, 0.0F);
                    }
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isFrozen(mc.player)) {
            return;
        }
        if (event.getScreen() instanceof InventoryScreen
                || event.getScreen() instanceof AbstractContainerScreen) {
            event.setCanceled(true);
        }
    }
}
