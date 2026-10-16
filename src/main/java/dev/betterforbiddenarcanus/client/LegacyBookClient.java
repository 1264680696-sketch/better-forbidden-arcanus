package dev.betterforbiddenarcanus.client;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID, value = Dist.CLIENT)
public final class LegacyBookClient {
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && event.getItemStack().is(LegacyContent.BOOK.get())) {
            Minecraft.getInstance().setScreen(new LegacyBookScreen());
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(LegacyContent.BOOK.get(), net.minecraft.resources.ResourceLocation.withDefaultNamespace("open"),
                    (stack, level, entity, seed) -> Minecraft.getInstance().screen instanceof LegacyBookScreen ? 1 : 0);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(LegacyContent.CORE.get(), net.minecraft.client.renderer.RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(LegacyContent.FRAME.get(), net.minecraft.client.renderer.RenderType.cutout());
        });
    }
}
