package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(BowItem.class)
public abstract class UpgradeBowMixin {
    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
    private float bfa$draw(int time, Operation<Float> original, @Local(argsOnly = true) ItemStack stack) {
        return original.call((int)Math.floor(time * dev.betterforbiddenarcanus.PrismEffects.bowDraw(stack)));
    }
}
