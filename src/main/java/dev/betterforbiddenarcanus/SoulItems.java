package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
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
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.better_forbidden_arcanus"))
            .icon(() -> new ItemStack(ModBlocks.HEPHAESTUS_FORGE_TIER_5.get()))
             .displayItems((parameters, output) -> {
                ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                output.accept(PrismEffects.applyAxe(new ItemStack(dev.betterforbiddenarcanus.legacy.LegacyContent.AXE.get()),15));
            })
            .build());
    public static final ResourceLocation SOUL_SAND_RECIPE = id("clibano_combustion/soul_from_soul_sand");
    public static final ResourceLocation GHAST_TEAR_RECIPE = id("clibano_combustion/ghast_tear_from_aureal_bottle");
    public static boolean requiresStrongSoul(ResourceLocation recipe){return SOUL_SAND_RECIPE.equals(recipe)||GHAST_TEAR_RECIPE.equals(recipe);}
    public static boolean strongSoulInput(ItemStack stack){return stack.is(net.minecraft.world.item.Items.SOUL_SAND)||stack.is(ModItems.AUREAL_BOTTLE.get());}

    private SoulItems() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterForbiddenArcanus.MOD_ID, path);
    }

    public static double speed(ItemStack stack) {
        if (stack.is(ModItems.ENCHANTED_SOUL.get())) return 2.5;
        if (stack.is(ModItems.CORRUPT_SOUL.get())) return 1.5;
        if (stack.is(ModItems.SOUL.get())) return 1.25;
        return 1.0;
    }

    public static boolean canExtractSoul(ItemStack stack) {
        return speed(stack) >= 1.5;
    }
}
