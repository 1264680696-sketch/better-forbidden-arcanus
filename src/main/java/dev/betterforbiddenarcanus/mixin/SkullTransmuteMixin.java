package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.TransmuteInputResult;
import com.stal111.forbidden_arcanus.common.block.skull.ObsidianSkullType;
import com.stal111.forbidden_arcanus.core.init.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=TransmuteInputResult.class,remap=false)
public abstract class SkullTransmuteMixin {
 @Inject(method="getResultItem",at=@At("RETURN"))
 private void type(ItemStack input,CallbackInfoReturnable<ItemStack> cir){
  var result=cir.getReturnValue();
  if(result.is(ModBlocks.AUREALIC_OBSIDIAN_SKULL.getSkull().asItem())){
   result.set(ModDataComponents.OBSIDIAN_SKULL_TYPE,ObsidianSkullType.AUREALIC);result.remove(ModDataComponents.TICKS_TILL_NEXT_STAGE);
  }
 }
}
