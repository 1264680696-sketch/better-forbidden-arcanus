package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoFireType;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoInputSlot;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ResiduesStorage;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.logic.ClibanoSmeltLogic;
import com.stal111.forbidden_arcanus.common.inventory.clibano.ClibanoMenu;
import com.stal111.forbidden_arcanus.common.item.crafting.ClibanoRecipe;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClibanoMainBlockEntity.class, remap = false)
public abstract class ClibanoMixin {
    @Shadow private int soulTime;
    @Shadow private int burnTime;
    @Shadow private ClibanoFireType fireType;
    @Shadow private ClibanoFireType nextFireType;
    @Shadow private ClibanoSmeltLogic logic;
    @Shadow protected abstract void changeFireType(Level level, ClibanoFireType type);
    @Unique private double bfa$lastSpeed = Double.NaN;

    @Unique
    private void bfa$syncSoul() {
        var entity = (ClibanoMainBlockEntity) (Object) this;
        var level = entity.getLevel();
        if (level == null || level.isClientSide()) return;
        var soul = entity.getStack(ClibanoMenu.SOUL_SLOT);
        var type = ClibanoFireType.fromItem(soul);
        double speed = SoulItems.speed(soul);
        nextFireType = type;
        // The original ticker decrements once. Keep a full bar and bypass consumeSoul;
        // the slot, rather than a saved countdown, is the source of fire state.
        soulTime = type == ClibanoFireType.FIRE ? 0 : ClibanoMainBlockEntity.SOUL_DURATION + 1;
        if (fireType != type) {
            changeFireType(level, type);
        } else if (speed != bfa$lastSpeed) {
            logic.onFireTypeChange(type);
        }
        bfa$lastSpeed = speed;
        if (!SoulItems.canExtractSoul(soul)) {
            if (SoulItems.strongSoulInput(entity.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()))) logic.resetCookingProgress(0);
            if (SoulItems.strongSoulInput(entity.getStack(ClibanoMenu.INPUT_SLOTS.getSecond()))) logic.resetCookingProgress(1);
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void bfa$beforeTick(Level level, BlockPos pos, BlockState state, ClibanoMainBlockEntity entity, CallbackInfo ci) {
        ((ClibanoMixin) (Object) entity).bfa$syncSoul();
    }

    @Inject(method = "onSlotChanged", at = @At("RETURN"))
    private void bfa$slotChanged(int slot, CallbackInfo ci) {
        if (slot == ClibanoMenu.SOUL_SLOT) bfa$syncSoul();
    }

    @Inject(method = "getCookingTime", at = @At("HEAD"), cancellable = true)
    private void bfa$cookingTime(RecipeHolder<ClibanoRecipe> recipe, CallbackInfoReturnable<Integer> cir) {
        var entity = (ClibanoMainBlockEntity) (Object) this;
        cir.setReturnValue(Math.max(1, (int) (recipe.value().cookingTimes().get(ClibanoFireType.FIRE)
                / SoulItems.speed(entity.getStack(ClibanoMenu.SOUL_SLOT)))));
    }

    @Inject(method = "canSmelt", at = @At("HEAD"), cancellable = true)
    private void bfa$recipeGate(RecipeHolder<ClibanoRecipe> recipe, ClibanoInputSlot slot, CallbackInfoReturnable<Boolean> cir) {
        if (recipe == null || !SoulItems.requiresStrongSoul(recipe.id())) return;
        var entity = (ClibanoMainBlockEntity) (Object) this;
        if (!SoulItems.canExtractSoul(entity.getStack(ClibanoMenu.SOUL_SLOT))
                || (recipe.id().equals(SoulItems.SOUL_SAND_RECIPE) && entity.getResiduesStorage().getTotalAmount() >= ResiduesStorage.MAX_AMOUNT)
                || (burnTime <= 0 && entity.getStack(ClibanoMenu.FUEL_SLOT).getBurnTime(RecipeType.BLASTING) <= 0)) {
            cir.setReturnValue(false);
        }
    }
}
