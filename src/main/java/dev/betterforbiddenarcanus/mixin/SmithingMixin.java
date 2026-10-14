package dev.betterforbiddenarcanus.mixin;
import dev.betterforbiddenarcanus.ArtisanEffects;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SmithingMenu.class)
public abstract class SmithingMixin {
    @Inject(method = "shrinkStackInSlot", at = @At("HEAD"), cancellable = true)
    private void bfa$retainTemplate(int slot, CallbackInfo ci) {
        var player = ((CombinerAccessor)this).bfa$player();
        if (slot == 0 && !player.level().isClientSide() && ArtisanEffects.equipped(player) && player.getRandom().nextFloat() < .25F) ci.cancel();
    }
}
