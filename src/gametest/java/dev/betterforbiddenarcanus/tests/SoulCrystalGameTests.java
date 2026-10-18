package dev.betterforbiddenarcanus.tests;
import com.stal111.forbidden_arcanus.common.event.EntityEvents;
import com.stal111.forbidden_arcanus.common.item.modifier.ModifierHelper;
import com.stal111.forbidden_arcanus.core.init.*;
import com.stal111.forbidden_arcanus.core.init.other.ModAttachmentTypes;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import dev.betterforbiddenarcanus.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;
@GameTestHolder("bfa_soul_crystal_tests") @PrefixGameTestTemplate(false)
public final class SoulCrystalGameTests {
 // Use real death/clone methods with an inert wire; GameTest's vanilla login cannot negotiate Curios payloads.
 private static ServerPlayer player(GameTestHelper h){
  var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"soul-crystal-test"),false);
  var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
  var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(wire);
  p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),wire,p,cookie){
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
  };
  p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(6,2,6))));p.getInventory().clearContent();return p;
 }
 private static int crystals(ServerPlayer p){int total=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ModItems.SOUL_BINDING_CRYSTAL.get()))total+=p.getInventory().getItem(i).getCount();return total;}
 private static void die(ServerPlayer p){p.setHealth(0);p.die(p.level().damageSources().generic());}
 @GameTest(template="guide_area") public static void actualDeathKeepsInventoryCuriosAndExactExperienceOnce(GameTestHelper h){
  var p=player(h);p.getInventory().setItem(7,new ItemStack(ModItems.SOUL_BINDING_CRYSTAL.get()));p.getInventory().setItem(8,new ItemStack(Items.DIAMOND,19));
  var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);armor.setDamageValue(121);armor.set(DataComponents.CUSTOM_NAME,Component.literal("完好的记忆"));armor.set(ForgeUpgrades.TIER,4);armor.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.VANISHING_CURSE),1);p.setItemSlot(EquipmentSlot.CHEST,armor);
  p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("relic",0,new ItemStack(ModItems.ARTISAN_RELIC.get()));
  p.experienceLevel=37;p.totalExperience=1234;p.experienceProgress=.375F;
  var area=p.getBoundingBox().inflate(7);int itemsBefore=h.getLevel().getEntitiesOfClass(ItemEntity.class,area).size();int xpBefore=h.getLevel().getEntitiesOfClass(ExperienceOrb.class,area).size();
  die(p);h.assertTrue(SoulBinding.protectedDeath(p)&&crystals(p)==0,"Actual death consumed one carried crystal");
  h.assertTrue(p.getInventory().getItem(8).getCount()==19&&p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==121&&!p.getOffhandItem().isEmpty(),"All vanilla inventory slots remain");
  h.assertTrue(!CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("relic").orElseThrow().getStacks().getStackInSlot(0).isEmpty(),"Curios remains equipped");
  h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,area).size()==itemsBefore&&h.getLevel().getEntitiesOfClass(ExperienceOrb.class,area).size()==xpBefore,"No item entities or XP orbs created");
  h.assertTrue(!h.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),"World game rule was not changed or leaked");
  var revived=player(h);revived.restoreFrom(p,false);
  h.assertTrue(revived.getInventory().getItem(8).getCount()==19&&revived.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==121&&ForgeUpgrades.tier(revived.getItemBySlot(EquipmentSlot.CHEST))==4,"Inventory and components restored");
  h.assertTrue(revived.getItemBySlot(EquipmentSlot.CHEST).getHoverName().getString().equals("完好的记忆")&&revived.getItemBySlot(EquipmentSlot.CHEST).getEnchantments().size()==1,"Name and vanishing enchantment preserved");
  h.assertTrue(revived.experienceLevel==37&&revived.totalExperience==1234&&revived.experienceProgress==.375F,"Exact experience level/total/progress restored");
  h.assertTrue(!SoulBinding.protectedDeath(revived)&&!SoulBinding.protectedDeath(p),"One-use receipt cleared on both player objects");
  h.assertTrue(!CuriosApi.getCuriosInventory(revived).orElseThrow().getStacksHandler("relic").orElseThrow().getStacks().getStackInSlot(0).isEmpty(),"Curios cloned into respawned player");
  die(revived);h.assertTrue(!SoulBinding.protectedDeath(revived)&&revived.getInventory().getItem(8).isEmpty(),"Next death without another crystal drops inventory normally");
  h.assertTrue(h.getLevel().getEntitiesOfClass(ExperienceOrb.class,area).size()>xpBefore,"Unprotected next death drops experience");h.succeed();
 }
 @GameTest(template="guide_area") public static void stacksOffhandAndRepeatedReceiptConsumeOnlyOne(GameTestHelper h){
  var p=player(h);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModItems.SOUL_BINDING_CRYSTAL.get(),3));p.getInventory().setItem(4,new ItemStack(Items.EMERALD,12));die(p);
  h.assertTrue(crystals(p)==2&&p.getInventory().getItem(4).getCount()==12,"Offhand stack triggers and only one consumed");h.assertTrue(SoulBinding.activate(p)&&crystals(p)==2,"Repeated activation for same death cannot double consume");
  var next=player(h);next.restoreFrom(p,false);die(next);h.assertTrue(crystals(next)==1&&next.getInventory().getItem(4).getCount()==12,"Next protected death uses the next crystal");h.succeed();
 }
 @GameTest(template="guide_area") public static void canceledDeathKeepInventoryAndEndReturnDoNotConsume(GameTestHelper h){
  var p=player(h);p.getInventory().setItem(0,new ItemStack(ModItems.SOUL_BINDING_CRYSTAL.get(),2));
  Consumer<LivingDeathEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(cancel);
  try{p.die(p.level().damageSources().generic());h.assertTrue(crystals(p)==2&&!SoulBinding.protectedDeath(p),"Canceled death keeps crystal unused");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
  var rules=h.getLevel().getGameRules();boolean before=rules.getBoolean(GameRules.RULE_KEEPINVENTORY);
  try{rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true,h.getLevel().getServer());die(p);h.assertTrue(crystals(p)==2&&!SoulBinding.protectedDeath(p),"Vanilla keep inventory needs no crystal");}finally{rules.getRule(GameRules.RULE_KEEPINVENTORY).set(before,h.getLevel().getServer());}
  p.setHealth(20);var returned=player(h);returned.restoreFrom(p,true);h.assertTrue(crystals(returned)==2&&!SoulBinding.protectedDeath(returned),"End return preserves crystals");h.succeed();
 }
 @GameTest(template="guide_area") public static void enderChestDoesNotTriggerAndOldBindingNoLongerProtects(GameTestHelper h){
  var p=player(h);p.getEnderChestInventory().setItem(0,new ItemStack(ModItems.SOUL_BINDING_CRYSTAL.get(),8));
  var sword=new ItemStack(Items.DIAMOND_SWORD);ModifierHelper.setModifier(sword,h.getLevel().registryAccess().registryOrThrow(FARegistries.ITEM_MODIFIER).getHolderOrThrow(ResourceKey.create(FARegistries.ITEM_MODIFIER,ResourceLocation.parse("forbidden_arcanus:soulbound"))));p.getInventory().setItem(1,sword);
  EntityEvents.onLivingDeath(new LivingDeathEvent(p,p.level().damageSources().generic()));h.assertTrue(p.getData(ModAttachmentTypes.SOULBOUND_INVENTORY).isEmpty(),"Native old bound-item extraction disabled");die(p);
  h.assertTrue(!SoulBinding.protectedDeath(p)&&p.getInventory().getItem(1).isEmpty(),"No carried crystal means old bound gear drops");h.assertTrue(p.getEnderChestInventory().getItem(0).getCount()==8,"Ender chest crystals untouched");h.succeed();
 }
 @GameTest(template="guide_area") public static void smithingRecipeRemovedAndCrystalRitualRetained(GameTestHelper h){
  h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("forbidden_arcanus:smithing/apply_soulbound_modifier")).isEmpty(),"Native soulbound smithing resource omitted");
  var p=player(h);var menu=new SmithingMenu(1,p.getInventory());menu.getSlot(0).set(new ItemStack(ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()));menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));menu.getSlot(2).set(new ItemStack(ModItems.SOUL_BINDING_CRYSTAL.get()));menu.createResult();h.assertTrue(menu.getSlot(3).getItem().isEmpty(),"Actual smithing menu has no soulbound output");
  h.assertTrue(h.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL).containsKey(ResourceLocation.parse("forbidden_arcanus:soul_binding_crystal")),"Original forge ritual still exists");h.succeed();
 }
}
