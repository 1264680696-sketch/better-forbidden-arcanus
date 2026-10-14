package dev.betterforbiddenarcanus.client;

import dev.betterforbiddenarcanus.TyrArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

/** Visual cooldown only: never use ItemCooldowns, which would also block armor use. */
public final class TyrCooldownClient {
    private static long deadline;
    private static int pending;
    private TyrCooldownClient() {}
    public static void receive(int ticks) {
        var level = Minecraft.getInstance().level;
        pending = Math.clamp(ticks, 0, 2400);
        deadline = level == null ? 0 : level.getGameTime() + pending;
    }
    public static int remaining() {
        var level = Minecraft.getInstance().level;
        if (level == null) return 0;
        if (deadline == 0 && pending > 0) deadline = level.getGameTime() + pending;
        return (int)Math.clamp(deadline - level.getGameTime(), 0, 2400);
    }
    public static float armorOverlay(ItemStack stack, int x, int y) {
        var mc = Minecraft.getInstance(); int remaining = remaining();
        if (remaining == 0 || mc.player == null || !TyrArmor.isTyr(stack) || !(mc.screen instanceof AbstractContainerScreen<?> screen)) return 0;
        boolean armorSlot = screen.getMenu().slots.stream().anyMatch(slot -> slot.container == mc.player.getInventory()
                && slot.getContainerSlot() >= 36 && slot.getContainerSlot() < 40
                && x == slot.x && y == slot.y && ItemStack.isSameItemSameComponents(stack, slot.getItem()));
        return armorSlot ? remaining / 2400F : 0;
    }
}
