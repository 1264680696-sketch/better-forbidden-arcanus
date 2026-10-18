package dev.betterforbiddenarcanus.client;
import com.stal111.forbidden_arcanus.common.item.crafting.ApplyModifierRecipe;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.*;
public final class PrismRecipeProcessor implements IComponentProcessor {
 private ApplyModifierRecipe recipe;private ItemStack base;
 @Override public void setup(Level level,IVariableProvider vars){
  recipe=(ApplyModifierRecipe)level.getRecipeManager().byKey(ResourceLocation.parse(vars.get("recipe_id",level.registryAccess()).asString())).orElseThrow().value();
  base=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(vars.get("base_id",level.registryAccess()).asString())));
 }
 @Override public IVariable process(Level level,String key){
  var stack=switch(key){case "template"->recipe.template().getItems()[0];case "base"->base;case "addition"->recipe.addition().getItems()[0];case "result"->recipe.assemble(new SmithingRecipeInput(recipe.template().getItems()[0],base,recipe.addition().getItems()[0]),level.registryAccess());default->ItemStack.EMPTY;};
  return stack.isEmpty()?null:IVariable.from(stack,level.registryAccess());
 }
}
