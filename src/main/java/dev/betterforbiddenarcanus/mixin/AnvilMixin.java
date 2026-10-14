package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.betterforbiddenarcanus.ArtisanEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(AnvilMenu.class)
public abstract class AnvilMixin {
    // This is the computed operation cost before vanilla's rename cap and 40-level check.
    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(JJJ)J")))
    private void bfa$discount(DataSlot slot, int value, Operation<Void> original) {
        Player player = ((CombinerAccessor)this).bfa$player();
        original.call(slot, ArtisanEffects.equipped(player) ? ArtisanEffects.discountedCost(value) : value);
    }
}
