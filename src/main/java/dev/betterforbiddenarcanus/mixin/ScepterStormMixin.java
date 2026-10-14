package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.item.DracoArcanusScepterItem;
import dev.betterforbiddenarcanus.ScepterStorm;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value = DracoArcanusScepterItem.class,remap=false)
public abstract class ScepterStormMixin extends Item {
    protected ScepterStormMixin(Properties properties){super(properties);}
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void bfa$use(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        boolean storm=player.isShiftKeyDown();
        if(!ScepterStorm.enough(player,storm?5:1)){cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));return;}
        ScepterStorm.start(player,storm);
    }
    @Inject(method="getUseDuration",at=@At("HEAD"),cancellable=true)
    private void bfa$duration(ItemStack stack,LivingEntity entity,CallbackInfoReturnable<Integer> cir) {
        if(entity instanceof Player p && ScepterStorm.channeling(p))cir.setReturnValue(72000);
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) {
        if(entity instanceof Player p && ScepterStorm.channeling(p)) {
            if(!p.isShiftKeyDown()){p.stopUsingItem();ScepterStorm.start(p,false);return;}
            ScepterStorm.pulse(p);
        }
    }
    @Inject(method="finishUsingItem",at=@At("HEAD"),cancellable=true)
    private void bfa$shot(ItemStack stack,Level level,LivingEntity entity,CallbackInfoReturnable<ItemStack> cir) {
        if(entity instanceof Player p && (ScepterStorm.channeling(p)||!ScepterStorm.pay(p,1)))cir.setReturnValue(stack);
    }
}
