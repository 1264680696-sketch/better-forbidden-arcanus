package dev.betterforbiddenarcanus.tests;
import com.stal111.forbidden_arcanus.common.item.crafting.ApplyModifierRecipe;
import com.stal111.forbidden_arcanus.core.init.*;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("bfa_prism_tests") @PrefixGameTestTemplate(false)
public final class PrismGameTests{
 private static void close(GameTestHelper h,double actual,double expected,String text){h.assertTrue(Math.abs(actual-expected)<.02,text+": "+actual+" != "+expected);}
 private static ApplyModifierRecipe recipe(GameTestHelper h,String id){return (ApplyModifierRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();}
 private static ItemStack apply(GameTestHelper h,Item item,String modifier){var s=new ItemStack(item);var holder=h.getLevel().registryAccess().registryOrThrow(FARegistries.ITEM_MODIFIER).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(FARegistries.ITEM_MODIFIER,ResourceLocation.parse(modifier)));com.stal111.forbidden_arcanus.common.item.modifier.ModifierHelper.setModifier(s,holder);return s;}
 private static ItemStack assemble(GameTestHelper h,String id,ItemStack stack){var r=recipe(h,id);var in=new SmithingRecipeInput(r.template().getItems()[0],stack,r.addition().getItems()[0]);h.assertTrue(r.matches(in,h.getLevel()),"Must accept "+id);h.assertTrue(r.modifier().value().isValidItem(stack),"Native viewer must discover base "+id);return r.assemble(in,h.getLevel().registryAccess());}
 @GameTest(template="guide_area") public static void fourPrismsStackAndPreserveComponents(GameTestHelper h){
  var stack=new ItemStack(LegacyContent.AXE.get());stack.setDamageValue(123);stack.set(ForgeUpgrades.TIER,5);stack.set(DataComponents.CUSTOM_NAME,Component.literal("起始之斧"));
  String[] ids={"forbidden_arcanus:smithing/apply_fiery_modifier","forbidden_arcanus:smithing/apply_aquatic_modifier","better_forbidden_arcanus:smithing/apply_gale_legs_modifier","forbidden_arcanus:smithing/apply_demolishing_modifier"};
  int expected=0;
  for(int i=0;i<4;i++){
   stack=assemble(h,ids[i],stack);expected|=1<<i;h.assertTrue(PrismEffects.bits(stack)==expected,"Each bit exactly once");
   h.assertTrue(!recipe(h,ids[i]).isBaseIngredient(stack),"Duplicate rejected");
   h.assertTrue(stack.getDamageValue()==123&&ForgeUpgrades.tier(stack)==5&&stack.get(DataComponents.CUSTOM_NAME).getString().equals("起始之斧"),"Components retained");
  }
  h.assertTrue(stack.has(DataComponents.UNBREAKABLE)&&stack.getHoverName().getSiblings().size()==4,"Full set unbreakable and no prefixes");
  var colors=new java.util.HashSet<Integer>();for(var part:stack.getHoverName().getSiblings())colors.add(part.getStyle().getColor().getValue());h.assertTrue(colors.size()==4,"Four independent letter colors");
  var loaded=ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow();h.assertTrue(ItemStack.matches(stack,loaded),"Stack and components survive saving");
  h.succeed();
 }
 @GameTest(template="guide_area") public static void gearSlotsSmithingAndRealStats(GameTestHelper h){
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setNoGravity(true);
  var sword=assemble(h,"forbidden_arcanus:smithing/apply_demolishing_modifier",new ItemStack(Items.DIAMOND_SWORD));
  p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.tick();p.tick();close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),14,"Sword damage doubled");close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),.96,"Sword speed 40% slower");
  sword.set(ForgeUpgrades.TIER,5);p.tick();p.tick();close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),19.6,"Upgrade and prism multiply once");close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),1.344,"Attack-speed upgrade composed");
  var chest=assemble(h,"forbidden_arcanus:smithing/apply_aquatic_modifier",new ItemStack(Items.DIAMOND_CHESTPLATE));
  h.assertTrue(!recipe(h,"forbidden_arcanus:smithing/apply_demolishing_modifier").isBaseIngredient(chest),"Ordinary gear cannot stack");
  p.setItemSlot(EquipmentSlot.CHEST,chest);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,200));h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN),"Fatigue immune");
  var guardian=EntityType.ELDER_GUARDIAN.create(h.getLevel());var container=new net.neoforged.neoforge.common.damagesource.DamageContainer(h.getLevel().damageSources().mobAttack(guardian),10);
  var e=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(p,container);PrismEffects.guard(e);close(h,e.getNewDamage(),7,"Guardian receives exactly 30% reduction");
  var wind=assemble(h,"better_forbidden_arcanus:smithing/apply_gale_legs_modifier",new ItemStack(Items.DIAMOND_LEGGINGS));h.assertTrue(PrismEffects.has(wind,PrismEffects.WIND),"Legs get wind");
  var axe=PrismEffects.applyAxe(PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),PrismEffects.SEA),PrismEffects.WIND);p.setItemInHand(InteractionHand.MAIN_HAND,axe);p.tick();p.tick();close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),1.8,"Wind axe speed");close(h,p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),5,"Sea axe reach");
  h.succeed();
 }
 private static void arrowHit(AbstractArrow arrow,LivingEntity victim){try{var m=AbstractArrow.class.getDeclaredMethod("onHitEntity",EntityHitResult.class);m.setAccessible(true);m.invoke(arrow,new EntityHitResult(victim));}catch(Exception e){throw new IllegalStateException(e);}}
 @GameTest(template="guide_area") public static void bowSlowChargeAndRecordedDoubleDamage(GameTestHelper h){
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,2,3))));
  var bow=assemble(h,"forbidden_arcanus:smithing/apply_demolishing_modifier",new ItemStack(Items.BOW));
  close(h,PrismEffects.bowDraw(bow),1/1.4,"Forty-percent longer charge");
  p.setItemInHand(InteractionHand.MAIN_HAND,bow);p.getInventory().add(new ItemStack(Items.ARROW,32));p.startUsingItem(InteractionHand.MAIN_HAND);Items.BOW.releaseUsing(bow,h.getLevel(),p,72000-28);
  var arrows=h.getEntities(EntityType.ARROW);h.assertTrue(arrows.size()==1,"One arrow after charge");var arrow=arrows.getFirst();h.assertTrue(arrow.getPersistentData().getBoolean("bfa_demolishing_bow"),"Source captured at launch");
  arrow.setCritArrow(false);arrow.setBaseDamage(2);arrow.setDeltaMovement(0,0,3);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  var cow=EntityType.COW.create(h.getLevel());cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);arrowHit(arrow,cow);close(h,100-cow.getHealth(),12,"Base six becomes twelve after changing weapons");h.succeed();
 }
 @GameTest(template="guide_area") public static void skullIsVisibleToNativeRecipeFilterAndStillWorks(GameTestHelper h){
  var ritual=h.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL).get(SoulItems.id("aurealic_obsidian_skull"));
  h.assertTrue(ritual.result() instanceof com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.TransmuteInputResult,"Native recipe-viewer filter accepts transmutation");
  h.assertTrue(ritual.requirements().essences().get(com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType.AUREAL)==10000,"Visible tier-four Aureal cost");
  SkullGameTests.fullAurealConsecrationAtTierFourAndFive(h);
 }
 @GameTest(template="guide_area",timeoutTicks=120) public static void thrownAxeReturnsExactlyOneOriginalStack(GameTestHelper h){
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4))));h.getLevel().addFreshEntity(p);p.setXRot(-90); // Keep the miss flight inside the test's loaded chunk column.
  var stack=PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),PrismEffects.WIND);stack.set(ForgeUpgrades.TIER,3);stack.setDamageValue(51);stack.set(DataComponents.CUSTOM_NAME,Component.literal("来回"));p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.startUsingItem(InteractionHand.MAIN_HAND);
  ((RunicAxeItem)stack.getItem()).releaseUsing(stack,h.getLevel(),p,72000-20);h.assertTrue(p.getMainHandItem().isEmpty(),"Original axe leaves inventory");
  var axes=h.getLevel().getEntitiesOfClass(RunicAxeProjectile.class,p.getBoundingBox().inflate(8));h.assertTrue(axes.size()==1,"One projectile");
  h.runAfterDelay(90,()->{int count=0;ItemStack returned=ItemStack.EMPTY;for(var s:p.getInventory().items)if(s.is(LegacyContent.AXE.get())){count+=s.getCount();returned=s;}
   h.assertTrue(count==1,"Exactly one axe returned: "+count+" projectile="+axes.getFirst().position()+" ticks="+axes.getFirst().tickCount+" removed="+axes.getFirst().isRemoved()+" owner="+axes.getFirst().getOwner()+" ownerPos="+p.position());h.assertTrue(returned.getDamageValue()==51&&ForgeUpgrades.tier(returned)==3&&PrismEffects.has(returned,PrismEffects.WIND)&&returned.get(DataComponents.CUSTOM_NAME).getString().equals("来回"),"Original components returned");h.succeed();});
 }
 @GameTest(template="guide_area") public static void fieryCriticalUsesTargetMaximumHealthOnce(GameTestHelper h){
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),PrismEffects.FIRE));
  var target=EntityType.COW.create(h.getLevel());target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);
  PrismEffects.critical(new net.neoforged.neoforge.event.entity.player.CriticalHitEvent(p,target,1.5F,true));target.hurt(h.getLevel().damageSources().playerAttack(p),10);close(h,200-target.getHealth(),22,"Ten base plus six percent maximum");
  target.invulnerableTime=0;target.setHealth(200);target.hurt(h.getLevel().damageSources().playerAttack(p),10);close(h,200-target.getHealth(),10,"No stale critical extra");h.succeed();
 }
 @GameTest(template="guide_area") public static void earthPiercingAreaAndFullVoidDamage(GameTestHelper h){
  var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))));
  var axe=PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),PrismEffects.EARTH);p.setItemInHand(InteractionHand.MAIN_HAND,axe);
  var target=EntityType.COW.create(h.getLevel());target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getAttribute(Attributes.ARMOR).setBaseValue(20);target.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(8,2,8))));
  var bystander=EntityType.COW.create(h.getLevel());bystander.setPos(target.position().add(.8,0,.8));bystander.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);bystander.setHealth(100);h.getLevel().addFreshEntity(bystander);
  var outside=EntityType.COW.create(h.getLevel());outside.setPos(target.position().add(3,0,0));h.getLevel().addFreshEntity(outside);
  target.hurt(h.getLevel().damageSources().playerAttack(p),10);close(h,100-target.getHealth(),8,"Twenty armor becomes ten");close(h,100-bystander.getHealth(),10,"Nearby splash once");close(h,outside.getHealth(),10,"Outside splash box untouched");
  p.setItemInHand(InteractionHand.MAIN_HAND,PrismEffects.applyAxe(PrismEffects.applyAxe(PrismEffects.applyAxe(axe,PrismEffects.FIRE),PrismEffects.SEA),PrismEffects.WIND));target.invulnerableTime=0;target.setHealth(100);target.hurt(h.getLevel().damageSources().playerAttack(p),10);close(h,100-target.getHealth(),18,"Eight armor-reduced damage plus flat ten void");h.succeed();
 }
}
