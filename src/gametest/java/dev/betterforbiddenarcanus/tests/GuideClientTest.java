package dev.betterforbiddenarcanus.tests;

import dev.betterforbiddenarcanus.SoulItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.PageMultiblock;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.book.BookRegistry;

/** Exercises the actual book renderer in a disposable development world. */
@EventBusSubscriber(modid = "bfa_tests", value = Dist.CLIENT)
public final class GuideClientTest {
    private record Frame(BookEntry entry, int offset) {}
    private static final List<Frame> frames = new ArrayList<>();
    private static final List<String> overflows = new ArrayList<>();
    private static boolean started, ready, finished, projecting;
    private static int frame, ticks, projections;
    private static long start;
    private static boolean dragonScene;
    private static boolean upgradeScene;
    private static net.minecraft.core.BlockPos standingEgg, fallingEgg;
    private static volatile int fallingEntity;
    private static net.minecraft.core.BlockPos corruptionBlock, windGround, galeGround;
    private static double launchY, peakY;
    private static volatile int counterTarget;
    private static volatile boolean counterConsumed;

    private static Object field(Class<?> type, String name, Object instance) {
        try { var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(instance); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("bfa.guideTest") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false; // Keep this unattended development check running when another app gains focus.
        if (!started && mc.screen instanceof TitleScreen) {
            started = true; start = System.currentTimeMillis();
            var rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_NATURAL_REGENERATION).set(false,null);
            var settings = new LevelSettings("BFA Guide Check", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("bfa-guide-" + System.currentTimeMillis(), settings, new WorldOptions(1L,false,false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if (!started) return;
        if (System.currentTimeMillis() - start > 300000) throw new IllegalStateException("Guide world/render check timed out");
        if (mc.level == null || mc.player == null) return;
        if (upgradeScene) { checkUpgradeScene(); return; }
        if (dragonScene) { checkDragonScene(); return; }
        var book = BookRegistry.INSTANCE.books.get(SoulItems.id("forbidden_arcanus"));
        if (!ready) {
            if (mc.screen != null) return;
            book.reloadContents(mc.level, false);
            var contents = book.getContents();
            if (contents.isErrored()) throw new IllegalStateException("Guide contents failed to compile", contents.getException());
            if (contents.entries.size() < 63 || contents.categories.size() < 9) throw new IllegalStateException("Incomplete guide contents");
            var eggState = com.stal111.forbidden_arcanus.core.init.ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState();
            if (mc.getBlockRenderer().getBlockModel(eggState) == mc.getModelManager().getMissingModel())
                throw new IllegalStateException("Arcane dragon egg static/falling block model is still missing");
            try {
                var overflow = book.getClass().getDeclaredField("overflowMode"); overflow.setAccessible(true);
                overflow.set(book, vazkii.patchouli.api.PatchouliConfigAccess.TextOverflowMode.RESIZE);
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            contents.entries.values().stream().sorted(java.util.Comparator.comparing(entry -> entry.getId().toString())).forEach(entry -> {
                for (int offset = 0; offset < entry.getPages().size(); offset += 2) frames.add(new Frame(entry, offset));
                if (entry.isLocked()) throw new IllegalStateException("Guide chapter unexpectedly locked: " + entry.getId());
            });
            ready = true; show(); return;
        }
        if (++ticks < 3) return;
        if (projecting) {
            if (mc.screen != null || PatchouliAPI.get().getCurrentMultiblock() == null)
                throw new IllegalStateException("World projection did not remain visible after closing the book: "
                        + frames.get(frame).entry.getId() + ", screen=" + mc.screen
                        + ", projection=" + PatchouliAPI.get().getCurrentMultiblock());
            if (projections == 1) Screenshot.grab(mc.gameDirectory, "guide-projection.png", mc.getMainRenderTarget(), component -> {});
            projecting = false;
            advance(); return;
        }
        var current = frames.get(frame);
        if (current.entry.getId().getPath().equals("practical/gale_mace") && current.offset == 0)
            Screenshot.grab(mc.gameDirectory,"guide-gale-recipe.png",mc.getMainRenderTarget(),component -> {});
        if (!(mc.screen instanceof GuiBookEntry)) throw new IllegalStateException("Book GUI did not open: " + current.entry.getId());
        for (int index = current.offset; index < Math.min(current.offset + 2, current.entry.getPages().size()); index++) {
            var page = current.entry.getPages().get(index);
            if (page instanceof PageWithText) {
                var renderer = (BookTextRenderer)field(PageWithText.class, "textRender", page);
                if (renderer != null && (float)field(BookTextRenderer.class,"scale",renderer) < 0.999f) {
                    String message = current.entry.getId() + " page " + index + " scale=" + field(BookTextRenderer.class,"scale",renderer);
                    overflows.add(message); com.mojang.logging.LogUtils.getLogger().warn("BFA_GUIDE_OVERFLOW: {}", message);
                }
            }
            if (page instanceof PageMultiblock) {
                if (!((java.util.Set<?>)field(PageMultiblock.class,"erroredTiles",page)).isEmpty()) throw new IllegalStateException("Multiblock tile rendering failed");
                var visualize = mc.screen.children().stream().filter(widget -> widget instanceof Button).map(widget -> (Button)widget)
                        .filter(button -> button.getMessage().getContents() instanceof TranslatableContents translation && translation.getKey().contains("visualize"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Missing visualization button"));
                if (projections == 0) Screenshot.grab(mc.gameDirectory, "guide-construction.png", mc.getMainRenderTarget(), component -> {});
                if (current.entry.getId().getPath().equals("practical/obelisk"))
                    Screenshot.grab(mc.gameDirectory, "guide-obelisk.png", mc.getMainRenderTarget(), component -> {});
                if (current.entry.getId().getPath().equals("practical/corrupted_obelisk"))
                    Screenshot.grab(mc.gameDirectory, "guide-corrupted-obelisk.png", mc.getMainRenderTarget(), component -> {});
                visualize.onPress();
                if (PatchouliAPI.get().getCurrentMultiblock() == null) throw new IllegalStateException("Visualization button did not create world projection");
                mc.setScreen(null); // The player closes the book to see the selected projection.
                mc.player.setXRot(35); mc.player.setYRot(0);
                projections++;
                projecting = true; ticks = 0;
            }
        }
        if (!projecting) advance();
    }

    private static void advance() {
        var mc = Minecraft.getInstance();
        var book = BookRegistry.INSTANCE.books.get(SoulItems.id("forbidden_arcanus"));
        if (++frame >= frames.size()) {
            if (!overflows.isEmpty()) throw new IllegalStateException("Guide text overflow: " + String.join("; ",overflows));
            if (projections != 5) throw new IllegalStateException("Expected five functioning multiblock projections, got " + projections);
            PatchouliAPI.get().clearMultiblock();
            mc.setScreen(null);
            standingEgg = mc.player.blockPosition().offset(0, 0, 4);
            fallingEgg = standingEgg.offset(2, 0, 0);
            mc.player.setXRot(0); mc.player.setYRot(-12);
            dragonScene = true; ticks = 0;
            var dimension = mc.level.dimension();
            mc.getSingleplayerServer().execute(() -> {
                var level = mc.getSingleplayerServer().getLevel(dimension);
                var egg = com.stal111.forbidden_arcanus.core.init.ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState();
                level.setBlockAndUpdate(standingEgg.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(fallingEgg.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(standingEgg, egg);
                level.setBlockAndUpdate(fallingEgg.above(3), egg);
                var entity = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, fallingEgg.above(3), egg);
                entity.setNoGravity(true);
                fallingEntity = entity.getId();
            });
            return;
        }
        show();
    }

    private static void checkDragonScene() {
        var mc = Minecraft.getInstance();
        int elapsed = ++ticks;
        if (elapsed == 30) {
            if (!mc.level.getBlockState(standingEgg).is(com.stal111.forbidden_arcanus.core.init.ModBlocks.ARCANE_DRAGON_EGG.get())
                    || !(mc.level.getEntity(fallingEntity) instanceof net.minecraft.world.entity.item.FallingBlockEntity))
                throw new IllegalStateException("Standing/falling dragon egg scene did not synchronize");
            Screenshot.grab(mc.gameDirectory, "guide-dragon-egg.png", mc.getMainRenderTarget(), component -> {});
            mc.getSingleplayerServer().execute(() -> {
                var entity = mc.getSingleplayerServer().getLevel(mc.level.dimension()).getEntity(fallingEntity);
                entity.setNoGravity(false);
            });
        }
        if (elapsed == 100) {
            if (!mc.level.getBlockState(fallingEgg).is(com.stal111.forbidden_arcanus.core.init.ModBlocks.ARCANE_DRAGON_EGG.get()))
                throw new IllegalStateException("Falling dragon egg did not land as its original block");
            Screenshot.grab(mc.gameDirectory, "guide-dragon-egg-landed.png", mc.getMainRenderTarget(), component -> {});
            dragonScene = false; upgradeScene = true; ticks = 0;
            var playerId = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                player.setGameMode(GameType.SURVIVAL);
                for (int tier = 1; tier <= 5; tier++) {
                    var stack = new net.minecraft.world.item.ItemStack(com.stal111.forbidden_arcanus.core.init.ModItems.DRACO_ARCANUS_SWORD.get());
                    stack.set(dev.betterforbiddenarcanus.ForgeUpgrades.TIER, tier);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("强化 " + tier).withStyle(net.minecraft.ChatFormatting.ITALIC));
                    player.getInventory().setItem(tier - 1, stack);
                    player.getInventory().setItem(tier + 8, stack.copy());
                }
                player.inventoryMenu.broadcastChanges();
            });
        }
    }

    private static void checkUpgradeScene() {
        var mc = Minecraft.getInstance(); int elapsed = ++ticks;
        if (elapsed == 20) {
            for (int tier = 1; tier <= 5; tier++) {
                var stack = mc.player.getInventory().getItem(tier - 1);
                if (dev.betterforbiddenarcanus.ForgeUpgrades.tier(stack) != tier
                        || stack.getHoverName().getStyle().getColor().getValue() != dev.betterforbiddenarcanus.ForgeUpgrades.color(stack).getColor())
                    throw new IllegalStateException("Upgrade component or name did not synchronize to client");
            }
            Screenshot.grab(mc.gameDirectory, "guide-upgrade-hotbar.png", mc.getMainRenderTarget(), component -> {});
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player) {
                @Override public void render(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
                    super.render(graphics,this.leftPos+8+18*4+8,this.topPos+84+8,partialTick);
                }
            });
        }
        if (elapsed == 30) Screenshot.grab(mc.gameDirectory, "guide-upgrade-inventory.png", mc.getMainRenderTarget(), component -> {});
        if (elapsed == 32) {
            mc.setScreen(null);
            var id = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var pos = player.blockPosition().offset(0, 0, 2);
                player.serverLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)player.serverLevel().getBlockEntity(pos);
                for (int tier = 1; tier <= 5; tier++) chest.setItem(tier - 1, player.getInventory().getItem(tier - 1).copy());
                player.openMenu(chest);
            });
        }
        if (elapsed == 60) {
            if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen)) throw new IllegalStateException("Real chest inventory did not open");
            Screenshot.grab(mc.gameDirectory, "guide-upgrade-container.png", mc.getMainRenderTarget(), component -> {});
            mc.player.closeContainer();
            corruptionBlock=mc.player.blockPosition().offset(0,1,3);
            mc.player.setYRot(0);mc.player.setXRot(2);
            var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.serverLevel().setBlockAndUpdate(corruptionBlock,com.stal111.forbidden_arcanus.core.init.ModBlocks.ARCANE_CRYSTAL_BLOCK.get().defaultBlockState());
                player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(com.stal111.forbidden_arcanus.core.init.ModItems.CORRUPTI_DUST.get(),3));
                player.getInventory().selected=0;player.inventoryMenu.broadcastChanges();
            });
        }
        if (elapsed == 90) {
            if (!mc.level.getBlockState(corruptionBlock).is(com.stal111.forbidden_arcanus.core.init.ModBlocks.ARCANE_CRYSTAL_BLOCK.get())
                    || !mc.player.getMainHandItem().is(com.stal111.forbidden_arcanus.core.init.ModItems.CORRUPTI_DUST.get()))
                throw new IllegalStateException("Crystal interaction scene did not synchronize");
            mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(corruptionBlock),net.minecraft.core.Direction.NORTH,corruptionBlock,false));
        }
        if (elapsed == 96) {
            if (!mc.level.getBlockState(corruptionBlock).is(com.stal111.forbidden_arcanus.core.init.ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get())
                    || mc.player.getMainHandItem().getCount()!=2) throw new IllegalStateException("Client/server crystal right-click conversion failed");
            Screenshot.grab(mc.gameDirectory,"guide-crystal-corruption.png",mc.getMainRenderTarget(),component -> {});
        }
        if (elapsed == 110) {
            windGround=mc.player.blockPosition().offset(-1,-1,3);
            mc.player.setYRot(0);mc.player.setXRot(20);
            var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                p.serverLevel().setBlockAndUpdate(windGround,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(com.stal111.forbidden_arcanus.core.init.ModItems.WHIRLWIND_PRISM.get()));
                p.inventoryMenu.broadcastChanges();
            });
        }
        if (elapsed == 140) {
            mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(windGround).add(0,.5,0),net.minecraft.core.Direction.UP,windGround,false));
        }
        if (elapsed == 148) {
            var state=mc.level.getBlockState(windGround.above());
            if (!state.is(com.stal111.forbidden_arcanus.core.init.ModBlocks.WHIRLWIND.get())
                    || mc.getBlockRenderer().getBlockModel(state)==mc.getModelManager().getMissingModel())
                throw new IllegalStateException("Real prism placement has no working whirlwind model");
            Screenshot.grab(mc.gameDirectory,"guide-whirlwind.png",mc.getMainRenderTarget(),component -> {});
        }
        if (elapsed == 165) {
            if (mc.level.getBlockState(windGround.above()).is(com.stal111.forbidden_arcanus.core.init.ModBlocks.WHIRLWIND.get()))
                throw new IllegalStateException("Native whirlwind lifetime changed");
            galeGround=mc.player.blockPosition().below();
            var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var recipe=(net.minecraft.world.item.crafting.SmithingRecipe)p.serverLevel().getRecipeManager().byKey(SoulItems.id("smithing/apply_gale_modifier")).orElseThrow().value();
                var result=recipe.assemble(new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new net.minecraft.world.item.ItemStack(com.stal111.forbidden_arcanus.core.init.ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()),
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MACE),
                    new net.minecraft.world.item.ItemStack(com.stal111.forbidden_arcanus.core.init.ModItems.WHIRLWIND_PRISM.get())),p.serverLevel().registryAccess());
                p.getInventory().setItem(0,result);p.inventoryMenu.broadcastChanges();
            });
        }
        if (elapsed == 190) {
            if (!dev.betterforbiddenarcanus.GaleMace.isGale(mc.player.getMainHandItem())
                    || !mc.player.getMainHandItem().getHoverName().getString().contains("疾风"))
                throw new IllegalStateException("Gale modifier and prefix did not synchronize");
            launchY=peakY=mc.player.getY();
            mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(galeGround).add(0,.5,0),net.minecraft.core.Direction.UP,galeGround,false));
        }
        if (elapsed >= 190 && elapsed <= 250) peakY=Math.max(peakY,mc.player.getY());
        if (elapsed == 200) {
            if(mc.player.getY()<launchY+2)throw new IllegalStateException("Server-authoritative gale impulse did not move client player");
            mc.player.setXRot(70);
            Screenshot.grab(mc.gameDirectory,"guide-gale-launch.png",mc.getMainRenderTarget(),component -> {});
        }
        if (elapsed == 250) {
            if (peakY<launchY+5 || peakY>launchY+10 || mc.player.getHealth()!=20)
                throw new IllegalStateException("Gale ascent/landing: height="+(peakY-launchY)+", health="+mc.player.getHealth());
            var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var armor=java.util.List.of(com.stal111.forbidden_arcanus.core.init.ModItems.TYR_HELMET.get(),com.stal111.forbidden_arcanus.core.init.ModItems.TYR_CHESTPLATE.get(),
                        com.stal111.forbidden_arcanus.core.init.ModItems.TYR_LEGGINGS.get(),com.stal111.forbidden_arcanus.core.init.ModItems.TYR_BOOTS.get());
                var slots=java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET);
                for(int i=0;i<4;i++)p.setItemSlot(slots.get(i),new net.minecraft.world.item.ItemStack(armor.get(i)));
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
                p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
                p.invulnerableTime=0;p.hurt(p.damageSources().generic(),200);
                if(p.getHealth()!=1 || !p.getPersistentData().getBoolean(dev.betterforbiddenarcanus.TyrCombat.COUNTER))
                    throw new IllegalStateException("Real server half-heart trigger: health="+p.getHealth()+", pending="+p.getPersistentData().getBoolean(dev.betterforbiddenarcanus.TyrCombat.COUNTER));
                var cow=net.minecraft.world.entity.EntityType.COW.create(p.serverLevel());cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);cow.setHealth(20);
                cow.setPos(p.position().add(0,0,2));p.serverLevel().addFreshEntity(cow);counterTarget=cow.getId();
            });
        }
        if(elapsed==280) {
            if(mc.player.getHealth()!=1 || dev.betterforbiddenarcanus.TyrCombat.shield(mc.player.getMainHandItem()) || dev.betterforbiddenarcanus.TyrCombat.shield(mc.player.getOffhandItem()))
                throw new IllegalStateException("Real player avatar/shield restriction failed: health="+mc.player.getHealth()+", main="+mc.player.getMainHandItem()+", off="+mc.player.getOffhandItem());
            Screenshot.grab(mc.gameDirectory,"guide-tyr-avatar.png",mc.getMainRenderTarget(),component -> {});
            mc.gameMode.attack(mc.player,mc.level.getEntity(counterTarget));
        }
        if(elapsed==300) {
            var id=mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var cow=(net.minecraft.world.entity.LivingEntity)p.serverLevel().getEntity(counterTarget);
                counterConsumed=Math.abs(cow.getHealth()-15)<.01 && !p.getPersistentData().getBoolean(dev.betterforbiddenarcanus.TyrCombat.COUNTER);
            });
        }
        if(elapsed==310) {
            if(!counterConsumed)throw new IllegalStateException("Real client attack did not consume exactly one fivefold counter");
            finished = true;
            com.mojang.logging.LogUtils.getLogger().info("BFA_GUIDE_PASS: {} entries, {} rendered spreads, {} working world projections; dragon egg, upgrade tooltip, crystal/whirlwind placement, gale launch/landing, avatar and counter",
                    BookRegistry.INSTANCE.books.get(SoulItems.id("forbidden_arcanus")).getContents().entries.size(), frames.size(), projections);
            mc.stop();
        }
    }

    private static void show() {
        PatchouliAPI.get().clearMultiblock();
        var current = frames.get(frame);
        PatchouliAPI.get().openBookEntry(SoulItems.id("forbidden_arcanus"), current.entry.getId(), current.offset);
        ticks = 0;
    }
}
