package dev.betterforbiddenarcanus.mixin;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class UpgradeNameMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void bfa$name(CallbackInfoReturnable<Component> cir) {
        var stack = (ItemStack)(Object)this;
        var prismName = dev.betterforbiddenarcanus.PrismEffects.axeName(stack);
        if (prismName != null) {
            if (dev.betterforbiddenarcanus.PrismEffects.bits(stack) != 15 && ForgeUpgrades.tier(stack) > 0)
                prismName = prismName.copy().withStyle(ForgeUpgrades.color(stack));
            cir.setReturnValue(prismName); return;
        }
        if (ForgeUpgrades.tier(stack) > 0) cir.setReturnValue(cir.getReturnValue().copy().withStyle(ForgeUpgrades.color(stack)));
    }
}
