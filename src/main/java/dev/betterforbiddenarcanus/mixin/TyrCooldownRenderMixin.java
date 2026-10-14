package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.betterforbiddenarcanus.client.TyrCooldownClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(GuiGraphics.class)
public abstract class TyrCooldownRenderMixin {
    @WrapOperation(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/Item;F)F"))
    private float bfa$armorCooldown(ItemCooldowns cooldowns, Item item, float partialTick, Operation<Float> original,
            @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y) {
        return Math.max(original.call(cooldowns, item, partialTick), TyrCooldownClient.armorOverlay(stack, x, y));
    }
}
