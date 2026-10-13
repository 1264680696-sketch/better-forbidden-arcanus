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
                var expected = SoulItems.id("item/" + name);
                var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(expected).contents();
                if (!sprite.name().equals(expected) || sprite.width() != 16 || sprite.height() != 16
                        || sprite.getUniqueFrames().count() != 5) {
                    throw new IllegalStateException("Invalid eternal soul texture or animation: " + name);
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("BFA_CLIENT_SMOKE_PASS: title screen and three animated textures loaded");
            Minecraft.getInstance().stop();
        }
    }
}
