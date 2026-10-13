package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.essence.EssenceValue;
import com.stal111.forbidden_arcanus.common.item.DarkSoulItem;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoulItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<Item, Item> ETERNAL_SOUL = ITEMS.register("eternal_soul", () -> new Item(properties(1)));
    public static final DeferredHolder<Item, DarkSoulItem> ETERNAL_CORRUPT_SOUL = ITEMS.register("eternal_corrupt_soul", () -> new DarkSoulItem(properties(1)));
    public static final DeferredHolder<Item, Item> ETERNAL_ENCHANTED_SOUL = ITEMS.register("eternal_enchanted_soul", () -> new Item(properties(10)));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.better_forbidden_arcanus"))
            .icon(() -> new ItemStack(ETERNAL_SOUL.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());
    public static final ResourceLocation SOUL_SAND_RECIPE = id("clibano_combustion/soul_from_soul_sand");

    private SoulItems() {}

    private static Item.Properties properties(int essence) {
        return new Item.Properties().component(ModDataComponents.ESSENCE_VALUE, EssenceValue.of(EssenceType.SOULS, essence));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterForbiddenArcanus.MOD_ID, path);
    }

    public static boolean isEternal(ItemStack stack) {
        return stack.is(ETERNAL_SOUL.get()) || stack.is(ETERNAL_CORRUPT_SOUL.get()) || stack.is(ETERNAL_ENCHANTED_SOUL.get());
    }

    public static double speed(ItemStack stack) {
        if (stack.is(ModItems.ENCHANTED_SOUL.get()) || stack.is(ETERNAL_ENCHANTED_SOUL.get())) return 2.5;
        if (stack.is(ModItems.CORRUPT_SOUL.get()) || stack.is(ETERNAL_CORRUPT_SOUL.get())) return 1.5;
        if (stack.is(ModItems.SOUL.get()) || stack.is(ETERNAL_SOUL.get())) return 1.25;
        return 1.0;
    }

    public static boolean canExtractSoul(ItemStack stack) {
        return speed(stack) >= 1.5;
    }
}
