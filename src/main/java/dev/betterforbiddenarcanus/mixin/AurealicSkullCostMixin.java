package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.skull.ObsidianSkullType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ObsidianSkullType.class, remap = false)
public abstract class AurealicSkullCostMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bfa$noOldBurningDrain(ItemStack stack, Player player, CallbackInfo ci) {
        if ((Object) this == ObsidianSkullType.AUREALIC) ci.cancel();
    }
}
