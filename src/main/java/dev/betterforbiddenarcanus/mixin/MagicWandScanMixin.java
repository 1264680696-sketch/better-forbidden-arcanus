package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.item.MagicWandItem;
import dev.betterforbiddenarcanus.MeteorScan;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=MagicWandItem.class,remap=false)
public abstract class MagicWandScanMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void bfa$scan(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if(!player.isShiftKeyDown())return;
        if(player instanceof ServerPlayer remote&&!player.isSpectator())MeteorScan.use(remote);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide()));
    }
}
