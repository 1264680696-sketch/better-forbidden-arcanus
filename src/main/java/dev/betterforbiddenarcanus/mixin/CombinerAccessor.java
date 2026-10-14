package dev.betterforbiddenarcanus.mixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemCombinerMenu.class)
public interface CombinerAccessor {
    @Accessor("player") Player bfa$player();
}
