package dev.betterforbiddenarcanus.client;

import dev.betterforbiddenarcanus.AurealicSkulls;
import dev.betterforbiddenarcanus.BetterForbiddenArcanus;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID, value = Dist.CLIENT)
public final class LavaSkullVision {
    public static boolean active(Camera camera) {
        return camera.getFluidInCamera() == FogType.LAVA && camera.getEntity() instanceof Player player && AurealicSkulls.wearing(player);
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        if (!active(event.getCamera())) return;
        event.setNearPlaneDistance(.25F); event.setFarPlaneDistance(32); event.setCanceled(true);
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event) {
        if (!active(event.getCamera())) return;
        event.setRed(.45F); event.setGreen(.28F); event.setBlue(.12F);
    }
    @SubscribeEvent public static void overlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.FIRE
                && event.getPlayer().isInLava() && AurealicSkulls.wearing(event.getPlayer())) event.setCanceled(true);
    }
}
