package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.input.EssenceDataInput;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EssenceDataInput.class, remap = false)
public abstract class EssenceInputMixin {
    @Inject(method = "finishInput", at = @At("HEAD"), cancellable = true)
    private void bfa$retainSoul(ItemStack stack, int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (SoulItems.isEternal(stack)) cir.setReturnValue(stack);
    }
}
