package dev.betterforbiddenarcanus.tests;
import com.stal111.forbidden_arcanus.common.event.*;
import com.stal111.forbidden_arcanus.common.item.modifier.*;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import com.stal111.forbidden_arcanus.core.init.other.ModAttachmentTypes;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import dev.betterforbiddenarcanus.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("bfa_prism_fix_tests") @PrefixGameTestTemplate(false)
public final class PrismFixGameTests {
 private static void close(GameTestHelper h,double a,double b,String why){h.assertTrue(Math.abs(a-b)<.02,why+": "+a+" != "+b);}
 private static ItemStack modifier(GameTestHelper h,ItemStack stack,String name){ModifierHelper.setModifier(stack,h.getLevel().registryAccess().registryOrThrow(FARegistries.ITEM_MODIFIER).getHolderOrThrow(ResourceKey.create(FARegistries.ITEM_MODIFIER,ResourceLocation.parse("forbidden_arcanus:"+name))));return stack;}
 @GameTest(template="guide_area") public static void bowReachesFullPowerAtTwentyEightTicks(GameTestHelper h) {
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4))));
  var bow=modifier(h,new ItemStack(Items.BOW),"demolishing");p.setItemInHand(InteractionHand.MAIN_HAND,bow);p.getInventory().add(new ItemStack(Items.ARROW,8));p.startUsingItem(InteractionHand.MAIN_HAND);
  Items.BOW.releaseUsing(bow,h.getLevel(),p,72000-20);var first=h.getEntities(EntityType.ARROW).getFirst();h.assertTrue(!first.isCritArrow(),"Twenty ticks is not fully charged");h.assertTrue(first.getDeltaMovement().length()<2.3,"Partial shot is physically slower");first.discard();
  p.startUsingItem(InteractionHand.MAIN_HAND);Items.BOW.releaseUsing(bow,h.getLevel(),p,72000-28);var second=h.getEntities(EntityType.ARROW).stream().filter(e->!e.isRemoved()).findFirst().orElseThrow();h.assertTrue(second.isCritArrow(),"Full charge at twenty-eight ticks");h.assertTrue(Math.abs(second.getDeltaMovement().length()-3)<.15,"Full velocity including native launch spread");second.discard();
  bow.set(ForgeUpgrades.TIER,5);p.startUsingItem(InteractionHand.MAIN_HAND);Items.BOW.releaseUsing(bow,h.getLevel(),p,72000-20);h.assertTrue(h.getEntities(EntityType.ARROW).stream().filter(e->!e.isRemoved()).findFirst().orElseThrow().isCritArrow(),"Forty percent forge speed compensates the slowdown exactly");h.succeed();
 }
 @GameTest(template="guide_area") public static void eachRunicPrismAddsThreeBeforeForgeScaling(GameTestHelper h) {
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setNoGravity(true);var axe=new ItemStack(dev.betterforbiddenarcanus.legacy.LegacyContent.AXE.get());
  for(int i=0;i<4;i++){
   axe=PrismEffects.applyAxe(axe,1<<i);p.setItemInHand(InteractionHand.MAIN_HAND,axe);p.tick();p.tick();close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),5+3*(i+1),"Each unique prism adds three");
  }
  axe.set(ForgeUpgrades.TIER,5);p.tick();p.tick();close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),23.8,"Full seventeen base damage gains forty-percent forge upgrade once");
  p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(dev.betterforbiddenarcanus.legacy.LegacyContent.AXE.get()));p.tick();p.tick();close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),5,"Plain axe unchanged");h.succeed();
 }
 @GameTest(template="guide_area") public static void doubleJumpResetsFallOriginButStillCountsLowerLandings(GameTestHelper h) {
  var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());var origin=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,8,4)));p.setPos(origin);p.fallDistance=50;
  PrismJump.resetFallOrigin(p,origin);close(h,p.fallDistance,0,"Existing fall distance reset");h.assertTrue(p.isIgnoringFallDamageFromCurrentImpulse()&&p.currentImpulseImpactPos.equals(origin),"Native impulse origin registered");
  p.causeFallDamage(50,1,h.getLevel().damageSources().fall());close(h,p.getHealth(),20,"Landing at the second-jump height ignores both old fall and extra ascent");
  p.setPos(origin);PrismJump.resetFallOrigin(p,origin);p.setPos(origin.add(0,-8,0));p.invulnerableTime=0;p.causeFallDamage(50,1,h.getLevel().damageSources().fall());close(h,p.getHealth(),15,"Eight lower blocks cause five damage, not fifty-block damage");h.succeed();
 }
 private static AreaEffectCloud cloud(GameTestHelper h,Vec3 pos,boolean marked){
  var c=new AreaEffectCloud(h.getLevel(),pos.x,pos.y,pos.z);c.setRadius(2.5F);c.setWaitTime(0);c.setDuration(400);c.setPotionContents(PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.HARM,1,1)));c.getPersistentData().putBoolean("bfa_draco_breath",marked);c.tickCount=5;h.getLevel().addFreshEntity(c);return c;
 }
 @GameTest(template="guide_area") public static void onlyDracoBreathExcludesAllPlayers(GameTestHelper h){
  var pos=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4)));var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());p.setPos(pos);h.getLevel().addFreshEntity(p);
  var other=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(other.getAbilities());other.setPos(pos.add(.5,0,.5));h.getLevel().addFreshEntity(other);
  var cow=EntityType.COW.create(h.getLevel());cow.setPos(pos);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);h.getLevel().addFreshEntity(cow);
  var marked=cloud(h,pos,true);marked.setOwner(p);marked.tick();close(h,p.getHealth(),20,"Shooter immune");close(h,other.getHealth(),20,"Other player immune");h.assertTrue(cow.getHealth()<100&&cow.hasEffect(MobEffects.WEAKNESS),"Mob still takes breath damage and debuffs");marked.discard();
  var normal=cloud(h,pos,false);normal.tick();h.assertTrue(p.getHealth()<20&&other.getHealth()<20,"Unmarked vanilla cloud still harms players");h.succeed();
 }
}
