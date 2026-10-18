package dev.betterforbiddenarcanus.tests;
import dev.betterforbiddenarcanus.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
@JeiPlugin
public final class PrismJeiChecks implements IModPlugin{
 public static volatile IJeiRuntime runtime;
 private static Ritual skull;
 private static final RecipeType<Ritual> TYPE=RecipeType.create("forbidden_arcanus","hephaestus_smithing",Ritual.class);
 @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("bfa_tests","prism_checks");}
 @Override public void onRuntimeAvailable(IJeiRuntime value){runtime=value;}
 @Override public void onRuntimeUnavailable(){runtime=null;}
 public static void check(){
  var manager=runtime.getRecipeManager();
  skull=manager.createRecipeLookup(TYPE).get().filter(r->AurealicSkulls.consecration(r.result())).findFirst().orElseThrow(()->new IllegalStateException("Aurealic skull missing from actual JEI recipe index"));
  var category=manager.getRecipeCategory(TYPE);
  if(manager.createRecipeLayoutDrawable(category,skull,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).isEmpty())throw new IllegalStateException("JEI cannot build skull recipe layout");
 }
 public static void show(){runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(TYPE),java.util.List.of(skull),java.util.List.of());}
}
