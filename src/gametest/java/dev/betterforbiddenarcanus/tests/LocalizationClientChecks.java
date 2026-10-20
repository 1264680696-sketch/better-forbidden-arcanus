package dev.betterforbiddenarcanus.tests;

import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.*;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.book.*;

@EventBusSubscriber(modid="bfa_tests",value=Dist.CLIENT)
public final class LocalizationClientChecks {
 private record Spread(BookEntry entry,int offset){}
 private static final String[] LANGUAGES={"zh_cn","en_us","zh_cn"};
 private static long started;private static boolean done,prepared;private static int phase,frame,ticks;
 private static CompletableFuture<Void> reload;private static Book book;private static final List<Spread> spreads=new ArrayList<>();private static Set<net.minecraft.resources.ResourceLocation> entryIds;
 private static void require(boolean yes,String message){if(!yes)throw new IllegalStateException(message);}
 private static Object field(Class<?> type,String key,Object obj){try{var f=type.getDeclaredField(key);f.setAccessible(true);return f.get(obj);}catch(Exception e){throw new IllegalStateException(e);}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
  if(!Boolean.getBoolean("bfa.localizationTest")||done)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
  if(started==0&&mc.screen instanceof TitleScreen){started=System.currentTimeMillis();var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
   var settings=new LevelSettings("Localization",GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT);
   mc.createWorldOpenFlows().createFreshLevel("bfa-localization-"+started,settings,new WorldOptions(1,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);return;
  }
  if(started==0)return;require(System.currentTimeMillis()-started<480000,"Localization client check timed out");if(mc.level==null||mc.player==null)return;
  String locale=LANGUAGES[phase];boolean english=locale.equals("en_us");
  if(!prepared){
   if(reload==null){if(mc.screen!=null)return;mc.getLanguageManager().setSelected(locale);mc.options.languageCode=locale;reload=mc.reloadResourcePacks();return;}
   if(!reload.isDone())return;reload.join();if(mc.screen!=null)return;
   require(ClientBookRegistry.INSTANCE.currentLang.equals(locale),"Patchouli did not follow the selected language");
   book=BookRegistry.INSTANCE.books.get(SoulItems.id("forbidden_arcanus"));require(book!=null,"Missing handbook");book.reloadContents(mc.level,false);require(!book.getContents().isErrored(),"Book content error: "+book.getContents().getException());
   require(book.getBookItem().getHoverName().getString().equals(english?"Hephaestus' Journal":"火神手记"),"Localized item name failed");
   require(I18n.get(book.landingText).startsWith(english?"Thank you":"感谢"),"Localized landing text failed");
   var axe=PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),15);require(axe.getHoverName().getString().equals(english?"Runic Battleaxe":"符文战斧"),"Rainbow name language failed: "+axe.getHoverName().getString());
   var fire=PrismEffects.applyAxe(new ItemStack(LegacyContent.AXE.get()),PrismEffects.FIRE);require(fire.getHoverName().getString().equals(english?"Fiery Runic Battleaxe":"熔炉 符文战斧"),"Prism prefix language failed");
   require(book.getContents().categories.size()==10,"Missing translated categories");var ids=Set.copyOf(book.getContents().entries.keySet());if(entryIds==null)entryIds=ids;else require(entryIds.equals(ids),"Entry IDs differ after switching languages");
   require(book.getContents().entries.get(SoulItems.id("practical/crescent_moon")).getName().getString().equals(english?"Crescent Moon":"待盈新月"),"Entry title language failed");
   spreads.clear();for(var entry:book.getContents().entries.values().stream().sorted(Comparator.comparing(e->e.getId().toString())).toList())for(int i=0;i<entry.getPages().size();i+=2)spreads.add(new Spread(entry,i));
   prepared=true;frame=0;ticks=0;show();return;
  }
  if(++ticks<3)return;ticks=0;require(mc.screen instanceof GuiBookEntry,"Book page failed to open");var spread=spreads.get(frame);
  if(english)for(int i=spread.offset;i<Math.min(spread.offset+2,spread.entry.getPages().size());i++)if(spread.entry.getPages().get(i) instanceof PageWithText page){var renderer=(BookTextRenderer)field(PageWithText.class,"textRender",page);if(renderer!=null)require((float)field(BookTextRenderer.class,"scale",renderer)>=.999f,"English text overflow: "+spread.entry.getId()+" page "+i);}
  if(spread.entry.getId().getPath().equals("practical/divine_pact")&&spread.offset==0)Screenshot.grab(mc.gameDirectory,"localization-"+locale+".png",mc.getMainRenderTarget(),c->{});
  if(++frame<spreads.size()){show();return;}
  com.mojang.logging.LogUtils.getLogger().info("BFA_LOCALIZATION_PHASE_PASS: {} / {} entries / {} spreads",locale,entryIds.size(),spreads.size());mc.setScreen(null);prepared=false;reload=null;
  if(++phase==LANGUAGES.length){done=true;com.mojang.logging.LogUtils.getLogger().info("BFA_LOCALIZATION_PASS: zh_cn/en_us/zh_cn live resource reload, complete handbook, translated item names and prism colors");mc.stop();}
 }
 private static void show(){var s=spreads.get(frame);PatchouliAPI.get().openBookEntry(SoulItems.id("forbidden_arcanus"),s.entry.getId(),s.offset);}
}
