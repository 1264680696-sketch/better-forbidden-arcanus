package dev.betterforbiddenarcanus;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class RunicAxeItem extends AxeItem {
 public RunicAxeItem(){super(Tiers.DIAMOND,new Item.Properties().attributes(AxeItem.createAttributes(Tiers.DIAMOND,1,-2.8F)));}
 @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){return PrismEffects.has(context.getItemInHand(),PrismEffects.WIND)?InteractionResult.PASS:super.useOn(context);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);if(!PrismEffects.has(stack,PrismEffects.WIND))return super.use(level,player,hand);
  player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
 }
 @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
 @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
 @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
  if(level.isClientSide()||!(user instanceof Player p)||!PrismEffects.has(stack,PrismEffects.WIND))return;
  float power=BowItem.getPowerForTime(getUseDuration(stack,user)-remaining);if(power<.1F)return;
  var axe=new RunicAxeProjectile(level,p,stack,power);
  axe.shootFromRotation(p,p.getXRot(),p.getYRot(),0,2.5F*power,1);
  if(level.addFreshEntity(axe))stack.shrink(1);
 }
}
