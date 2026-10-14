package dev.betterforbiddenarcanus.mixin;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CrossbowItem.class)
public abstract class UpgradeCrossbowMixin {
    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void bfa$charge(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (ForgeUpgrades.tier(stack) > 0) cir.setReturnValue(Math.max(1, (int)Math.ceil(cir.getReturnValue() / ForgeUpgrades.multiplier(stack))));
    }
}
