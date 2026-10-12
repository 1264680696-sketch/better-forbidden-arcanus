package dev.betterforbiddenarcanus.tests;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
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
            System.out.println("BFA_CLIENT_SMOKE_PASS: title screen reached");
            Minecraft.getInstance().stop();
        }
    }
}
