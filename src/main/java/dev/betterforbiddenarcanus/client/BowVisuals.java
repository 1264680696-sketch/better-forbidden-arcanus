package dev.betterforbiddenarcanus.client;
import dev.betterforbiddenarcanus.PrismEffects;
import net.minecraft.world.item.ItemStack;
public final class BowVisuals {
    private BowVisuals() {}
    public static int elapsed(ItemStack stack,int ticks) { return (int)Math.floor(ticks*PrismEffects.bowDraw(stack)); }
}
