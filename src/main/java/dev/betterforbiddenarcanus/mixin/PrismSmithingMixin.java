package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.item.crafting.ApplyModifierRecipe;
import dev.betterforbiddenarcanus.PrismEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ApplyModifierRecipe.class,remap=false)
public abstract class PrismSmithingMixin {
 @Inject(method="assemble",at=@At("HEAD"),cancellable=true)
 private void result(SmithingRecipeInput input,HolderLookup.Provider provider,CallbackInfoReturnable<ItemStack> cir){
  if(PrismEffects.axe(input.base())&&PrismEffects.bit(((ApplyModifierRecipe)(Object)this).modifier())!=0)
   cir.setReturnValue(PrismEffects.applyAxe(input.base(),PrismEffects.bit(((ApplyModifierRecipe)(Object)this).modifier())));
 }
}
