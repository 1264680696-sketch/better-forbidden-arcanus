package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoFireType;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.logic.ClibanoAccessor;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.logic.ClibanoSmeltLogic;
import com.stal111.forbidden_arcanus.common.item.crafting.ClibanoRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClibanoSmeltLogic.class, remap = false)
public abstract class ClibanoProgressMixin {
    @Shadow @Final public ClibanoAccessor clibano;
    @Shadow public int[] cookingProgress;
    @Shadow public int[] cookingDuration;

    @Inject(method = "updateCookingProgress", at = @At("HEAD"), cancellable = true)
    private void bfa$rescale(ClibanoFireType type, RecipeHolder<ClibanoRecipe> recipe, int slot, CallbackInfo ci) {
        if (recipe != null && clibano instanceof ClibanoMainBlockEntity) {
            int oldDuration = cookingDuration[slot];
            int newDuration = clibano.getCookingTime(recipe);
            cookingProgress[slot] = oldDuration > 0
                    ? Math.min(newDuration - 1, (int) ((long) cookingProgress[slot] * newDuration / oldDuration)) : 0;
            cookingDuration[slot] = newDuration;
            ci.cancel();
        }
    }
}
