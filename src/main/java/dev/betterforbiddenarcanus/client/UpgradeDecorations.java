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
            int color = 0xff000000 | ForgeUpgrades.color(stack).getColor();
            // 18x18 slot outline, not a silhouette around the item sprite.
            graphics.fill(x - 1, y - 1, x + 17, y, color);
            graphics.fill(x - 1, y + 16, x + 17, y + 17, color);
            graphics.fill(x - 1, y, x, y + 16, color);
            graphics.fill(x + 16, y, x + 17, y + 16, color);
            return false;
        }));
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof net.minecraft.world.item.BowItem)
                net.minecraft.client.renderer.item.ItemProperties.register(item, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                        entity == null || !entity.isUsingItem() || entity.getUseItem() != stack ? 0 : (float)((stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) * ForgeUpgrades.multiplier(stack) / 20));
        }));
    }
}
