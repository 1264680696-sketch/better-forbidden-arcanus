package dev.betterforbiddenarcanus.client;
import dev.betterforbiddenarcanus.*;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ScepterRenderer {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScepterStorm.BOLT.get(),LightningBoltRenderer::new);
    }
}
