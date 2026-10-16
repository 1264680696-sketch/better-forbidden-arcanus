package dev.betterforbiddenarcanus.client;

import dev.betterforbiddenarcanus.BetterForbiddenArcanus;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyRenderLayers {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(LegacyContent.CORE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(LegacyContent.FRAME.get(), RenderType.cutout());
        });
    }
}
