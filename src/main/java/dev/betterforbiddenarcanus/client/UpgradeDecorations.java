package dev.betterforbiddenarcanus.client;
import dev.betterforbiddenarcanus.BetterForbiddenArcanus;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UpgradeDecorations {
    private UpgradeDecorations() {}
    @SubscribeEvent public static void register(RegisterItemDecorationsEvent event) {
        BuiltInRegistries.ITEM.forEach(item -> event.register(item, (graphics, font, stack, x, y) -> {
            if (ForgeUpgrades.tier(stack) == 0) return false;
            graphics.blit(dev.betterforbiddenarcanus.SoulItems.id("textures/gui/upgrade_border_" + ForgeUpgrades.tier(stack) + ".png"),
                    x - 1, y - 1, 0, 0, 18, 18, 18, 18);
            return false;
        }));
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    com.stal111.forbidden_arcanus.core.init.ModBlocks.WHIRLWIND.get(), net.minecraft.client.renderer.RenderType.cutout());
            BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof net.minecraft.world.item.BowItem)
                net.minecraft.client.renderer.item.ItemProperties.register(item, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                        entity == null || !entity.isUsingItem() || entity.getUseItem() != stack ? 0 : (float)((stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) * ForgeUpgrades.multiplier(stack) / 20));
            });
        });
    }
}
