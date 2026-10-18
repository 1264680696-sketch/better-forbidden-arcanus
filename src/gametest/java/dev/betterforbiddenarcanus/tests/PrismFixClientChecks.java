package dev.betterforbiddenarcanus.tests;
import com.mojang.authlib.GameProfile;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
public final class PrismFixClientChecks {
 private static void require(boolean b,String text){if(!b)throw new IllegalStateException(text);}
 private static void elapsed(RemotePlayer p,int elapsed){try{var flag=LivingEntity.class.getDeclaredMethod("setLivingEntityFlag",int.class,boolean.class);flag.setAccessible(true);flag.invoke(p,1,true);var f=LivingEntity.class.getDeclaredField("useItemRemaining");f.setAccessible(true);f.setInt(p,72000-elapsed);}catch(Exception e){throw new IllegalStateException(e);}}
 public static void bow(Minecraft mc){
  var user=new RemotePlayer(mc.level,new GameProfile(java.util.UUID.randomUUID(),"bow-visual-check"));var ordinary=new ItemStack(Items.BOW);user.setItemInHand(InteractionHand.MAIN_HAND,ordinary);user.startUsingItem(InteractionHand.MAIN_HAND);elapsed(user,20);
  var fullNormal=mc.getItemRenderer().getModel(ordinary,mc.level,user,0);float normalFov=user.getFieldOfViewModifier();
  var slow=new ItemStack(Items.BOW);var recipe=(com.stal111.forbidden_arcanus.common.item.crafting.ApplyModifierRecipe)mc.level.getRecipeManager().byKey(ResourceLocation.parse("forbidden_arcanus:smithing/apply_demolishing_modifier")).orElseThrow().value();
  com.stal111.forbidden_arcanus.common.item.modifier.ModifierHelper.setModifier(slow,recipe.modifier());user=new RemotePlayer(mc.level,new GameProfile(java.util.UUID.randomUUID(),"slow-bow-check"));user.setItemInHand(InteractionHand.MAIN_HAND,slow);user.startUsingItem(InteractionHand.MAIN_HAND);elapsed(user,20);
  var pull=ItemProperties.getProperty(slow,ResourceLocation.withDefaultNamespace("pull"));float value=pull.call(slow,mc.level,user,0);require(value>.69&&value<.73,"Slow bow texture still fully pulled at twenty ticks: "+value);
  require(mc.getItemRenderer().getModel(slow,mc.level,user,0)!=fullNormal,"Pull texture is still ordinary fully drawn model");require(user.getFieldOfViewModifier()>normalFov,"FOV zoom did not slow down");require(BowVisuals.elapsed(slow,20)==14,"First-person elapsed draw matches release calculation");
  elapsed(user,28);require(Math.abs(pull.call(slow,mc.level,user,0)-1)<.001,"Texture fully drawn at twenty-eight ticks");require(mc.getItemRenderer().getModel(slow,mc.level,user,0)==fullNormal,"Final draw texture wrong");
  slow.set(ForgeUpgrades.TIER,5);elapsed(user,20);require(Math.abs(ItemProperties.getProperty(slow,ResourceLocation.withDefaultNamespace("pull")).call(slow,mc.level,user,0)-1)<.001,"Forge speed applies to draw visuals once");user.stopUsingItem();
 }
 public static void renderer(Minecraft mc){
  net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,mc.level.registryAccess());
  var entries=SoulItems.TAB.get().getDisplayItems();
  require(entries.stream().anyMatch(s->PrismEffects.axe(s)&&PrismEffects.bits(s)==15&&s.has(net.minecraft.core.component.DataComponents.UNBREAKABLE)),"Four-prism axe absent from addon creative tab");
  require(entries.stream().anyMatch(s->PrismEffects.axe(s)&&PrismEffects.bits(s)==0),"Plain axe disappeared from creative tab");

  var shot=new RunicAxeProjectile(RunicAxeProjectile.TYPE.get(),mc.level);
  require(mc.getEntityRenderDispatcher().getRenderer(shot) instanceof RunicAxeRenderer,"Generic camera-facing renderer still registered");
  require(Math.abs(RunicAxeRenderer.heading(1,0,0))<.001,"Along-X throw must rotate in world XY vertical plane");
  require(Math.abs(RunicAxeRenderer.spin(5)-RunicAxeRenderer.spin(0))==180,"Axe must rotate end over end with age");
 }
}
