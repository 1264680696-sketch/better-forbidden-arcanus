package dev.betterforbiddenarcanus;
import net.minecraft.world.item.ItemStack;
public interface JarRelicStorage {
    ItemStack bfa$relic();
    void bfa$relic(ItemStack stack);
    int bfa$relicTicks();
    void bfa$relicTicks(int ticks);
}
