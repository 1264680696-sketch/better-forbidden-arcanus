package dev.betterforbiddenarcanus.tests;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Verifies client startup, then exits; only active in runClientSmoke. */
@EventBusSubscriber(modid = "bfa_tests", value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean finished;

    @SubscribeEvent
    public static void afterTick(ClientTickEvent.Post event) {
        if (!finished && Boolean.getBoolean("bfa.clientSmokeTest")
                && Minecraft.getInstance().screen instanceof TitleScreen) {
            finished = true;
            for (String name : java.util.List.of("eternal_soul", "eternal_corrupt_soul", "eternal_enchanted_soul")) {
                if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(SoulItems.id(name)))
                    throw new IllegalStateException("Removed soul is still registered: " + name);
            }
            var language = net.minecraft.locale.Language.getInstance();
            if (!language.has("curios.identifier.relic")) throw new IllegalStateException("Missing relic slot name");
            if (!language.has("curios.modifiers.relic")) throw new IllegalStateException("Missing relic attributes heading");
            var guideId = SoulItems.id("forbidden_arcanus");
            var guide = vazkii.patchouli.common.book.BookRegistry.INSTANCE.books.get(guideId);
            if (guide == null || guide.name.isBlank()
                    || !guide.model.toString().equals("better_forbidden_arcanus:guide_book")
                    || !guide.bookTexture.toString().equals("better_forbidden_arcanus:textures/gui/forbiddenmicon.png")
                    || !guide.creativeTab.equals(SoulItems.id("main"))) {
                throw new IllegalStateException("Guide must use default brown book and addon creative tab");
            }
            var resource = SoulItems.id("patchouli_books/forbidden_arcanus/en_us/entries/practical/legacy_core.json");
            if (Minecraft.getInstance().getResourceManager().getResource(resource).isEmpty()) {
                throw new IllegalStateException("Missing guide content");
            }
            for (var item : dev.betterforbiddenarcanus.RelicCurio.items()) {
                String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
                if (!language.has("tooltip.better_forbidden_arcanus.relic." + path)) throw new IllegalStateException("Missing relic description: " + path);
            }
            com.mojang.logging.LogUtils.getLogger().info("BFA_CLIENT_SMOKE_PASS: title screen, removed souls, guide and relic descriptions loaded");
            Minecraft.getInstance().stop();
        }
    }
}
