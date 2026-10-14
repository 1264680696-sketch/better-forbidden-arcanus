package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.ElytraDuplicationRecipe;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class ElytraDuplicationGameTests {
    @GameTest(template = "empty")
    public static void elytraCraftingKeepsOriginalAndConsumesScales(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var recipe = h.getLevel().getRecipeManager().byKey(SoulItems.id("elytra_duplication")).orElseThrow().value();
        h.assertTrue(recipe instanceof ElytraDuplicationRecipe, "Published recipe must use registered catalyst serializer");
        var shaped = (ElytraDuplicationRecipe)recipe;
        var original = new ItemStack(Items.ELYTRA);
        original.setDamageValue(100);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Original wing"));
        original.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
        var saved = original.copy();
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) items.set(i, new ItemStack(ModItems.DRAGON_SCALE.get(), 3));
        items.set(4, original);
        var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3, items);
        for (int craft = 0; craft < 3; craft++) {
            var input = CraftingInput.of(3, 3, grid.getItems());
            h.assertTrue(shaped.matches(input, h.getLevel()), "Eight ordinary scales around an elytra must match");
            var output = shaped.assemble(input, h.getLevel().registryAccess());
            h.assertTrue(output.getCount() == 1 && ItemStack.isSameItemSameComponents(output, new ItemStack(Items.ELYTRA)), "Each craft must make exactly one fresh unenchanted elytra");
            var result = new ResultContainer(); result.setItem(0, output);
            new ResultSlot(player, grid, result, 0, 0, 0).onTake(player, result.removeItem(0, 1));
            h.assertTrue(grid.getItem(4).getCount() == 1 && ItemStack.isSameItemSameComponents(grid.getItem(4), saved), "Actual crafting slot must keep original enchantment, damage and name");
            for (int slot = 0; slot < 9; slot++) if (slot != 4)
                h.assertTrue(grid.getItem(slot).getCount() == 2 - craft, "Every craft must consume one scale in each outer slot");
        }
        h.assertTrue(!shaped.matches(CraftingInput.of(3, 3, grid.getItems()), h.getLevel()), "No scales means no extra free elytra");
        for (int i = 0; i < 9; i++) if (i != 4) grid.setItem(i, new ItemStack(ModItems.GOLDEN_DRAGON_SCALE.get()));
        h.assertTrue(!shaped.matches(CraftingInput.of(3, 3, grid.getItems()), h.getLevel()), "Other dragon scale variants must not substitute");
        h.assertTrue(!shaped.canCraftInDimensions(2, 2), "Recipe must require a crafting table");
        h.succeed();
    }
}
