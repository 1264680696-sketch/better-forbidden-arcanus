package dev.betterforbiddenarcanus.tests;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.client.LegacyBookScreen;
import dev.betterforbiddenarcanus.legacy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="bfa_tests",value=Dist.CLIENT)
public final class LegacyClientChecks {
    private static boolean active, initialized;
    private static int ticks, category, index;
    public static void begin() { active=true; }
    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!active)return;
        var mc=Minecraft.getInstance();
        if(!initialized) {
            var oldHand = mc.player.getMainHandItem();
            mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(LegacyContent.BOOK.get()));
            var click = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(click);
            require(click.isCanceled() && mc.screen instanceof LegacyBookScreen, "Actual client right-click does not open restored book");
            mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, oldHand);
            var open = net.minecraft.client.renderer.item.ItemProperties.getProperty(new ItemStack(LegacyContent.BOOK.get()), net.minecraft.resources.ResourceLocation.withDefaultNamespace("open"));
            require(open != null && open.call(new ItemStack(LegacyContent.BOOK.get()), mc.level, mc.player, 0) == 1, "Book open model predicate missing");
            require(((LegacyBookScreen)mc.screen).entries().size()==35,"Old book entries missing");
            for(var holder:SoulItems.ITEMS.getEntries()) {
                var stack=new ItemStack(holder.get());var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                require(model!=mc.getModelManager().getMissingModel(),"Missing legacy item model: "+holder.getId());
                for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(1)))
                    require(!quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()),"Missing item texture: "+holder.getId());
            }
            for(var holder:LegacyContent.BLOCKS.getEntries())for(var state:holder.get().getStateDefinition().getPossibleStates()) {
                var model=mc.getBlockRenderer().getBlockModel(state);
                require(model!=mc.getModelManager().getMissingModel(),"Missing restored block model "+holder.getId());
                var directions=new java.util.ArrayList<net.minecraft.core.Direction>(java.util.List.of(net.minecraft.core.Direction.values()));directions.add(null);
                for(var direction:directions)for(var quad:model.getQuads(state,direction,net.minecraft.util.RandomSource.create(1)))
                    require(!quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()),"Missing restored block texture "+holder.getId());
            }
            for(int i=1;i<=5;i++)try(var stream=mc.getResourceManager().open(SoulItems.id("textures/gui/upgrade_border_"+i+".png"));var image=com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                require(image.getWidth()==18&&image.getHeight()==18,"Wrong border dimensions");
                require((image.getPixelRGBA(8,8)>>>24)==0&&(image.getPixelRGBA(0,8)>>>24)==255,"Border must surround transparent slot interior");
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
            for(var item:RelicCurio.items()) {
                var lines=new ItemStack(item).getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);
                require(lines.getLast().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("item.forbidden_arcanus.enhancer"),"Client relic tooltip extends below label");
            }
            initialized=true;return;
        }
        if(++ticks%4!=0)return;
        require(mc.screen instanceof LegacyBookScreen,"Legacy book is not an independent GUI");var screen=(LegacyBookScreen)mc.screen;
        var entries=screen.entries().stream().filter(e->e.category()==category).toList();
        if(index<entries.size()) {
            require(screen.currentEntry().equals(entries.get(index)),"Wrong book category or page");
            require(screen.textPages()>0,"Missing wrapped Chinese text");
            String item=screen.currentEntry().item();
            if(item.equals("better_forbidden_arcanus:runic_battleaxe")) {
                require(screen.recipeCount()>0,"Legacy recipe not shown");
                Screenshot.grab(mc.gameDirectory,"legacy-book-battleaxe.png",mc.getMainRenderTarget(),component->{});
            }
            if(item.equals("better_forbidden_arcanus:runic_tenebris_core"))Screenshot.grab(mc.gameDirectory,"legacy-book-core.png",mc.getMainRenderTarget(),component->{});
            index++;if(index<entries.size())screen.selectEntry(index);return;
        }
        category++;index=0;
        if(category<3){screen.selectCategory(category);return;}
        screen.resize(mc,212,145);require(screen.currentEntry()!=null,"Small GUI lost content");
        screen.resize(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        // Exercise the same hit regions as a mouse click on the first category tab.
        float scale=Math.min(1.35F,Math.min(screen.width/340F,screen.height/236F));
        int left=(int)((screen.width-340*scale)/2),top=(int)((screen.height-236*scale)/2);
        screen.mouseClicked(left+17*scale,top+52*scale,0);require(screen.currentEntry().category()==0,"Scaled category buttons do not respond");
        com.mojang.logging.LogUtils.getLogger().info("BFA_LEGACY_CLIENT_PASS: 35 independent-book entries, live recipes, Chinese paging, scaling, thirteen item models, all block states, five transparent borders and trimmed relic tooltips");
        com.mojang.logging.LogUtils.getLogger().info("BFA_GUIDE_PASS: guide and legacy client checks completed");
        active=false;mc.stop();
    }
}
