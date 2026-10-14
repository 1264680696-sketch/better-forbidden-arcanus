package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.betterforbiddenarcanus.TyrCombat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class TyrInventoryMixin {
    @Shadow @Final public Player player;
    @WrapMethod(method = "setItem")
    private void bfa$armorSlotChange(int slot, ItemStack stack, Operation<Void> original) {
        original.call(slot, stack);
        if (slot >= 36 && slot < 40) TyrCombat.update(player);
    }
    @Inject(method = "setChanged", at = @At("TAIL"))
    private void bfa$containerChange(CallbackInfo ci) { TyrCombat.update(player); }
}
