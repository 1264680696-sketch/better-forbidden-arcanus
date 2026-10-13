package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.item.DarkSoulItem;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DarkSoulItem.class, remap = false)
public abstract class DarkSoulMixin {
    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private void bfa$consumeOrdinaryOnly(ItemStack stack, int amount, LivingEntity user) {
        if (!SoulItems.isEternal(stack)) stack.consume(amount, user);
    }
}
