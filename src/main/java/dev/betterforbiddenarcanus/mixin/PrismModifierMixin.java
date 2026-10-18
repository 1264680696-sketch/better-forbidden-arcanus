package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.item.modifier.ItemModifier;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import dev.betterforbiddenarcanus.PrismEffects;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Extend the native predicate too, so its recipe-viewer maker sees the new bases. */
@Mixin(value=ItemModifier.class,remap=false)
public abstract class PrismModifierMixin {
 @Inject(method="isValidItem",at=@At("HEAD"),cancellable=true)
 private void valid(ItemStack stack,CallbackInfoReturnable<Boolean> cir){
  var modifier=(ItemModifier)(Object)this;int bit=PrismEffects.bit(modifier);if(bit==0)return;
  boolean incompatible=stack.is(modifier.incompatibleItems())||modifier.incompatibleEnchantments().stream().anyMatch(stack.getTagEnchantments().keySet()::contains);
  if(PrismEffects.axe(stack)){
   var existing=stack.get(ModDataComponents.ITEM_MODIFIER);
   cir.setReturnValue(!incompatible&&stack.getCount()==1&&!PrismEffects.has(stack,bit)&&(existing==null||PrismEffects.bit(existing)!=0));
  }else if(PrismEffects.extraBase(stack,bit))cir.setReturnValue(!incompatible&&!stack.has(ModDataComponents.ITEM_MODIFIER));
 }
}
