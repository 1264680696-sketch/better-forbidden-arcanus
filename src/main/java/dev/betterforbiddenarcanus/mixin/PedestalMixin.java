package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity;
import com.stal111.forbidden_arcanus.common.block.pedestal.effect.PedestalEffectTrigger;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PedestalBlockEntity.class, remap = false)
public abstract class PedestalMixin {
    @Inject(method = "clearStack", at = @At("HEAD"), cancellable = true)
    private void bfa$retainRitualCatalyst(Player player, PedestalEffectTrigger trigger, CallbackInfo ci) {
        var pedestal = (PedestalBlockEntity) (Object) this;
        if (trigger == PedestalEffectTrigger.RITUAL_FINISHED && SoulItems.isEternal(pedestal.getStack())) {
            pedestal.setItemHeightTarget(PedestalBlockEntity.DEFAULT_ITEM_HEIGHT);
            ci.cancel();
        }
    }
}
