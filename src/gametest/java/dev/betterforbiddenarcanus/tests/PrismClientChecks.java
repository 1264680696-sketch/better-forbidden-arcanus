package dev.betterforbiddenarcanus.tests;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.*;
import vazkii.patchouli.client.book.gui.*;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.book.*;
@EventBusSubscriber(modid="bfa_tests",value=Dist.CLIENT)
public final class PrismClientChecks{
 private record Spread(BookEntry entry,int offset){}
 private static final List<Spread> spreads=new ArrayList<>();
 private static long started;private static int frame=-1,ticks,jumpTicks;private static Book book;private static boolean prepared,jumping,finished;private static volatile boolean serverPassed;
 private static void require(boolean b,String text){if(!b)throw new IllegalStateException(text);}
 private static Object field(Class<?> type,String name,Object object){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new IllegalStateException(e);}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  boolean soul=Boolean.getBoolean("bfa.soulCrystalGuideTest");boolean fixes=Boolean.getBoolean("bfa.prismFixTest");if((!Boolean.getBoolean("bfa.prismGuideTest")&&!fixes&&!soul)||finished)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
  if(started==0&&mc.screen instanceof TitleScreen){started=System.currentTimeMillis();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
   var settings=new LevelSettings("Prism Guide",GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT);
   mc.createWorldOpenFlows().createFreshLevel("bfa-prisms-"+started,settings,new WorldOptions(1,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);return;
  }
  if(started==0)return;require(System.currentTimeMillis()-started<240000,"Prism client check timed out");if(mc.level==null||mc.player==null)return;
  if(jumping){
   jumpTicks++;
   if(fixes&&jumpTicks==10)Screenshot.grab(mc.gameDirectory,"runic-axe-spin-a.png",mc.getMainRenderTarget(),c->{});
   if(fixes&&jumpTicks==15)Screenshot.grab(mc.gameDirectory,"runic-axe-spin-b.png",mc.getMainRenderTarget(),c->{});
   if(jumpTicks==20)mc.options.keyJump.setDown(true);
   if(jumpTicks==24)mc.options.keyJump.setDown(false);
   if(jumpTicks==35){var uuid=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{
    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);require(p.getPersistentData().getBoolean("bfa_double_jump_used"),"Actual jump key did not send valid double jump");
    if(fixes)require(p.isIgnoringFallDamageFromCurrentImpulse()&&p.currentImpulseImpactPos!=null,"Server did not register new fall origin");
    require(!PrismJump.jump(p),"Third jump rejected");p.getPersistentData().remove("bfa_double_jump_used");p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);require(!PrismJump.jump(p),"Removed legs reject jump");serverPassed=true;
   });}
   if(fixes&&jumpTicks>=41&&serverPassed){require(mc.player.isIgnoringFallDamageFromCurrentImpulse()&&mc.player.currentImpulseImpactPos!=null,"Client did not receive confirmed fall origin");finished=true;com.mojang.logging.LogUtils.getLogger().info("BFA_PRISM_FIX_PASS: {} changed spreads; actual bow pull textures and FOV, world-aligned spinning axe renderer, double-jump client/server fall origin",spreads.size());mc.stop();return;}
   if(jumpTicks==41&&serverPassed){mc.options.keyJump.setDown(false);PrismJeiChecks.show();return;}
   if(jumpTicks>=47&&serverPassed){Screenshot.grab(mc.gameDirectory,"aurealic-skull-jei.png",mc.getMainRenderTarget(),c->{});finished=true;com.mojang.logging.LogUtils.getLogger().info("BFA_PRISM_GUIDE_PASS: {} changed spreads, live smithing template results, skull ritual visual, actual key/payload double jump, third-jump/removal rejection and actual JEI indexing/layout",spreads.size());mc.stop();}
   return;
  }
  if(!prepared){if(mc.screen!=null||(!fixes&&PrismJeiChecks.runtime==null))return;if(soul){require(mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("forbidden_arcanus:smithing/apply_soulbound_modifier")).isEmpty(),"Soulbound smithing still loaded");require(PrismJeiChecks.runtime.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.SMITHING).get().noneMatch(r->r.id().toString().startsWith("forbidden_arcanus:smithing/apply_soulbound_modifier")),"Old soulbound smithing remains in actual JEI index");}else if(fixes){PrismFixClientChecks.bow(mc);PrismFixClientChecks.renderer(mc);}else PrismJeiChecks.check();prepared=true;book=BookRegistry.INSTANCE.books.get(SoulItems.id("forbidden_arcanus"));require(book!=null,"Missing handbook");book.reloadContents(mc.level,false);require(!book.getContents().isErrored(),"Book reload error: "+book.getContents().getException());
   for(var id:(soul?List.of("forge_tier_4/soul_binding_crystal","practical/modifiers"):fixes?List.of("forge_tier_2/terrastomp_prism","forge_tier_3/whirlwind_prism","practical/draco_arrow","practical/runic_prisms","practical/modifiers","forge_tier_4/soul_binding_crystal"):List.of("forge_tier_1/smelter_prism","forge_tier_2/terrastomp_prism","forge_tier_3/sea_prism","forge_tier_3/whirlwind_prism","forge_tier_4/aurealic_skull","practical/modifiers","practical/legacy_weapons","practical/runic_prisms"))){
    var entry=book.getContents().entries.get(SoulItems.id(id));require(entry!=null,"Missing entry "+id);for(int i=0;i<entry.getPages().size();i+=2)spreads.add(new Spread(entry,i));
   }
   frame=0;show();return;
  }
  if(++ticks<5)return;ticks=0;require(mc.screen instanceof GuiBookEntry,"Missing book entry");var spread=spreads.get(frame);
  for(int i=spread.offset;i<Math.min(spread.offset+2,spread.entry.getPages().size());i++)if(spread.entry.getPages().get(i) instanceof PageWithText page){var renderer=(BookTextRenderer)field(PageWithText.class,"textRender",page);if(renderer!=null)require((float)field(BookTextRenderer.class,"scale",renderer)>=.999f,"Page overflows "+spread.entry.getId()+" "+i);}
  if(spread.entry.getId().getPath().equals("forge_tier_4/aurealic_skull")&&spread.offset==0)Screenshot.grab(mc.gameDirectory,"aurealic-skull-recipe.png",mc.getMainRenderTarget(),c->{});
  if(spread.entry.getId().getPath().equals("practical/runic_prisms")&&spread.offset==0)Screenshot.grab(mc.gameDirectory,"runic-prisms-guide.png",mc.getMainRenderTarget(),c->{});
  if(++frame<spreads.size()){show();return;}
  if(soul){Screenshot.grab(mc.gameDirectory,"soul-crystal-guide.png",mc.getMainRenderTarget(),c->{});finished=true;com.mojang.logging.LogUtils.getLogger().info("BFA_SOUL_CRYSTAL_GUIDE_PASS: {} changed spreads, no text overflow, removed smithing recipe in actual client and JEI index",spreads.size());mc.stop();return;}
  mc.setScreen(null);jumping=true;var uuid=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
   var legs=new ItemStack(Items.DIAMOND_LEGGINGS);var r=(com.stal111.forbidden_arcanus.common.item.crafting.ApplyModifierRecipe)p.level().getRecipeManager().byKey(SoulItems.id("smithing/apply_gale_legs_modifier")).orElseThrow().value();
   com.stal111.forbidden_arcanus.common.item.modifier.ModifierHelper.setModifier(legs,r.modifier());p.setItemSlot(EquipmentSlot.LEGS,legs);p.teleportTo(p.getX(),p.getY()+6,p.getZ());p.inventoryMenu.broadcastChanges();
   var axe=PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),PrismEffects.WIND);p.getInventory().setItem(0,axe);p.inventoryMenu.broadcastChanges();var shot=new RunicAxeProjectile(p.level(),p,axe,1);shot.setPos(p.getX(),p.getY()+1.5,p.getZ()+4);shot.setDeltaMovement(.08,0,0);p.serverLevel().addFreshEntity(shot);
  });
 }
 private static void show(){var s=spreads.get(frame);PatchouliAPI.get().openBookEntry(SoulItems.id("forbidden_arcanus"),s.entry.getId(),s.offset);}
}
