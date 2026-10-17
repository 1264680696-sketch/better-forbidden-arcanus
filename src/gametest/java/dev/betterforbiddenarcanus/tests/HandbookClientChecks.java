package dev.betterforbiddenarcanus.tests;

import dev.betterforbiddenarcanus.SoulItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.*;
import vazkii.patchouli.client.book.page.PageMultiblock;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

/** Only the revised handbook, removed items and their dependent recipe are exercised. */
@EventBusSubscriber(modid = "bfa_tests", value = Dist.CLIENT)
public final class HandbookClientChecks {
    private record Spread(BookEntry entry, int offset) {}
    private static final List<Spread> spreads = new ArrayList<>();
    private static boolean started, ready, requested, finished;
    private static long start;
    private static int ticks, frame = -2, projections;
    private static Book book;
    private static boolean lavaScene;
    private static int lavaTicks;

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    private static Object field(Class<?> type, String name, Object instance) {
        try { var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(instance); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        boolean jarOnly = Boolean.getBoolean("bfa.jarGuideTest");
        if ((!Boolean.getBoolean("bfa.handbookTest") && !jarOnly) || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (!started && mc.screen instanceof TitleScreen) {
            started = true; start = System.currentTimeMillis();
            var rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            var settings = new LevelSettings("Handbook Check", GameType.CREATIVE, false,
                    net.minecraft.world.Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("bfa-handbook-" + start, settings,
                    new WorldOptions(1, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if (!started) return;
        require(System.currentTimeMillis() - start < 240000, "Handbook check timed out");
        if (mc.level == null || mc.player == null) return;
        if (lavaScene) { checkLavaScene(); return; }
        if (!requested) {
            if (mc.screen != null) return;
            requested = true;
            book = BookRegistry.INSTANCE.books.get(SoulItems.id("forbidden_arcanus"));
            require(book != null && book.name.equals("火神手记"), "Handbook name/ID changed incorrectly");
            require(book.bookTexture.equals(SoulItems.id("textures/gui/forbiddenmicon.png")), "Wrong legacy page atlas");
            book.reloadContents(mc.level, false);
            require(!book.getContents().isErrored(), "Handbook failed to compile: " + book.getContents().getException());
            require(book.getContents().entries.size() == 77 && book.getContents().categories.size() == 10, "Missing revised entries/categories");
            if (!jarOnly) {
            var relics = book.getContents().categories.get(SoulItems.id("relics"));
            require(relics != null && relics.getEntries().size() == 8, "Relics are not in their own chapter");
            require(book.getContents().entries.containsKey(SoulItems.id("practical/draco_scepter")), "No separate scepter entry");
            require(!book.getContents().entries.containsKey(SoulItems.id("practical/legacy_book")), "Deleted book entry remains");
            for (var id : List.of("forbiddenmicon", "arcane_gold_ingot")) {
                require(!BuiltInRegistries.ITEM.containsKey(SoulItems.id(id)), "Deleted item still registered: " + id);
                require(mc.level.getRecipeManager().byKey(SoulItems.id(id)).isEmpty(), "Deleted recipe remains: " + id);
            }
            var frameRecipe = (ShapedRecipe) mc.level.getRecipeManager().byKey(SoulItems.id("runic_tenebris_frame")).orElseThrow().value();
            var ingredients = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
            for (int i = 0; i < 9; i++) ingredients.set(i, frameRecipe.getIngredients().get(i).getItems()[0].copy());
            require(frameRecipe.matches(CraftingInput.of(3, 3, ingredients), mc.level), "Updated frame recipe does not craft");
            for (int i : new int[]{0, 2, 6, 8}) require(ingredients.get(i).is(com.stal111.forbidden_arcanus.core.init.ModItems.DEORUM_INGOT.get()), "Frame must use Deorum in four corners");
            var guide = book.getBookItem();
            require(guide.getHoverName().getString().equals("火神手记"), "Guide item name incorrect");
            require(mc.getItemRenderer().getModel(guide, mc.level, mc.player, 0) != mc.getModelManager().getMissingModel(), "Guide model missing");
            var model = mc.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(SoulItems.id("item/guide_book")));
            require(!model.getQuads(null, null, net.minecraft.util.RandomSource.create()).isEmpty(), "Legacy three-dimensional cover missing");
            }
            book.getContents().entries.values().stream().filter(e -> !jarOnly || List.of("practical/containers", "basics/souls", "practical/obsidian_skulls", "forge_tier_4/aurealic_skull").contains(e.getId().getPath())).sorted(java.util.Comparator.comparing(e -> e.getId().toString())).forEach(entry -> {
                require(!entry.isLocked(), "Entry unexpectedly locked");
                for (int i = 0; i < entry.getPages().size(); i += 2) spreads.add(new Spread(entry, i));
            });
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                player.getInventory().setItem(0, book.getBookItem()); player.getInventory().selected = 0;
                player.inventoryMenu.broadcastChanges();
                player.getMainHandItem().use(player.serverLevel(), player, InteractionHand.MAIN_HAND);
            });
            return;
        }
        if (!ready) {
            if (!(mc.screen instanceof GuiBook)) return;
            ready = true; ticks = 0;
            if (jarOnly) { frame = 0; show(); }
            return;
        }
        if (++ticks < 3) return;
        ticks = 0;
        if (frame == -2) {
            Screenshot.grab(mc.gameDirectory, "handbook-home.png", mc.getMainRenderTarget(), c -> {});
            mc.setScreen(new GuiBookCategory(book, book.getContents().categories.get(SoulItems.id("relics"))));
            frame = -1; return;
        }
        if (frame == -1) {
            Screenshot.grab(mc.gameDirectory, "handbook-relics.png", mc.getMainRenderTarget(), c -> {});
            frame = 0; show(); return;
        }
        var current = spreads.get(frame);
        require(mc.screen instanceof GuiBookEntry, "Entry did not open: " + current.entry.getId());
        for (int i = current.offset; i < Math.min(current.offset + 2, current.entry.getPages().size()); i++) {
            var page = current.entry.getPages().get(i);
            if (page instanceof PageWithText) {
                var renderer = (BookTextRenderer) field(PageWithText.class, "textRender", page);
                if (renderer != null) require((float) field(BookTextRenderer.class, "scale", renderer) >= .999f,
                        "Text shrinks/overflows: " + current.entry.getId() + " page " + i);
            }
            if (page instanceof PageMultiblock) {
                require(((java.util.Set<?>) field(PageMultiblock.class, "erroredTiles", page)).isEmpty(), "Broken multiblock preview");
                var button = mc.screen.children().stream().filter(w -> w instanceof Button).map(w -> (Button) w)
                        .filter(b -> b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().contains("visualize"))
                        .findFirst().orElseThrow();
                button.onPress(); require(PatchouliAPI.get().getCurrentMultiblock() != null, "Broken projection button");
                projections++; PatchouliAPI.get().clearMultiblock();
            }
        }
        if (current.entry.getId().getPath().equals("practical/draco_scepter") && current.offset == 0)
            Screenshot.grab(mc.gameDirectory, "handbook-scepter.png", mc.getMainRenderTarget(), c -> {});
        if (current.entry.getId().getPath().equals("forge_setup/construction") && current.offset == 2)
            Screenshot.grab(mc.gameDirectory, "handbook-structure.png", mc.getMainRenderTarget(), c -> {});
        if (jarOnly && current.entry.getId().getPath().equals("practical/containers") && current.offset == 10)
            Screenshot.grab(mc.gameDirectory, "jar-extraction-guide.png", mc.getMainRenderTarget(), c -> {});
        if (++frame < spreads.size()) { show(); return; }
        if (jarOnly) {
            lavaScene = true; lavaTicks = 0; mc.setScreen(null);
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                var pos = player.blockPosition().offset(0, 2, 3);
                var level = player.serverLevel();
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 6; z++) {
                    level.setBlockAndUpdate(pos.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    for (int y = 0; y < 4; y++) level.setBlockAndUpdate(pos.offset(x, y, z), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
                }
                for (int y = 0; y < 3; y++) level.setBlockAndUpdate(pos.offset(0, y, 4), net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(com.stal111.forbidden_arcanus.core.init.ModBlocks.AUREALIC_OBSIDIAN_SKULL.getSkull()));
                player.teleportTo(pos.getX() + .5, pos.getY() + .2, pos.getZ() + .5);
                player.setYRot(0); player.setXRot(0); player.inventoryMenu.broadcastChanges();
            });
            return;
        }
        require(projections == 5, "Expected five preserved projections");
        finished = true;
        com.mojang.logging.LogUtils.getLogger().info("BFA_HANDBOOK_PASS: 77 entries, 10 chapters, {} spreads, original book model/atlas, actual right-click, relic chapter, scepter entry, projections and Deorum frame; no unrelated gameplay tests", spreads.size());
        mc.stop();
    }
    private static void checkLavaScene() {
        var mc = Minecraft.getInstance();
        int elapsed = ++lavaTicks;
        if (elapsed == 30) {
            var camera = mc.gameRenderer.getMainCamera();
            require(dev.betterforbiddenarcanus.client.LavaSkullVision.active(camera), "Worn skull does not enable actual submerged lava view");
            net.minecraft.client.renderer.FogRenderer.setupFog(camera, net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN, 128, false, 0);
            require(Math.abs(com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd() - 32) < .01, "Lava fog was not extended to thirty-two blocks");
            Screenshot.grab(mc.gameDirectory, "skull-lava-vision.png", mc.getMainRenderTarget(), c -> {});
        }
        if (elapsed == 45) {
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY); player.inventoryMenu.broadcastChanges();
            });
        }
        if (elapsed == 65) {
            var camera = mc.gameRenderer.getMainCamera();
            require(camera.getFluidInCamera() == net.minecraft.world.level.material.FogType.LAVA, "Comparison camera left lava");
            require(!dev.betterforbiddenarcanus.client.LavaSkullVision.active(camera), "Removing skull keeps lava vision");
            net.minecraft.client.renderer.FogRenderer.setupFog(camera, net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN, 128, false, 0);
            require(com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd() <= 5, "Ordinary lava fog not restored");
            Screenshot.grab(mc.gameDirectory, "skull-lava-without.png", mc.getMainRenderTarget(), c -> {});
            finished = true;
            com.mojang.logging.LogUtils.getLogger().info("BFA_JAR_GUIDE_PASS: four changed entries, {} spreads, native skull smithing page and actual lava vision/removal; no unrelated gameplay checks", spreads.size());
            mc.stop();
        }
    }
    private static void show() {
        var spread = spreads.get(frame);
        PatchouliAPI.get().openBookEntry(SoulItems.id("forbidden_arcanus"), spread.entry.getId(), spread.offset);
    }
}
