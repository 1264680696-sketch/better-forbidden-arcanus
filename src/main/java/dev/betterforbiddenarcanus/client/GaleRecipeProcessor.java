package dev.betterforbiddenarcanus.client;

import com.stal111.forbidden_arcanus.common.item.crafting.ApplyModifierRecipe;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.*;

/** Native smithing pages only support vanilla recipe implementations. */
public final class GaleRecipeProcessor implements IComponentProcessor {
    private ApplyModifierRecipe recipe;
    @Override public void setup(Level level, IVariableProvider variables) {
        recipe = (ApplyModifierRecipe)level.getRecipeManager().byKey(SoulItems.id("smithing/apply_gale_modifier")).orElseThrow().value();
    }
    @Override public IVariable process(Level level, String key) {
        ItemStack stack = switch (key) {
            case "template" -> recipe.template().getItems()[0];
            case "base" -> new ItemStack(Items.MACE);
            case "addition" -> recipe.addition().getItems()[0];
            case "result" -> recipe.assemble(new SmithingRecipeInput(recipe.template().getItems()[0], new ItemStack(Items.MACE), recipe.addition().getItems()[0]), level.registryAccess());
            default -> ItemStack.EMPTY;
        };
        return stack.isEmpty() ? null : IVariable.from(stack, level.registryAccess());
    }
}
