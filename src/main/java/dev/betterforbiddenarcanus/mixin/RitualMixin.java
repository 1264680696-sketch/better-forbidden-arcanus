package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.ForgeDataCache;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.ActiveRitualData;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.RitualManager;
import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RitualManager.class, remap = false)
public abstract class RitualMixin {
    @Shadow private ForgeDataCache dataCache;
    @Shadow private ServerLevel level;
    @Shadow private BlockPos pos;
    @Unique private java.util.List<ForgeDataCache.IngredientEntry> bfa$retained = java.util.List.of();

    @Inject(method = "finishRitual", at = @At("RETURN"), cancellable = true)
    private void bfa$retainMain(ActiveRitualData data, CallbackInfoReturnable<ItemStack> cir) {
        var main = dataCache.mainIngredient();
        if (SoulItems.isEternal(main)) {
            var result = cir.getReturnValue();
            if (!result.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, result.copy()));
            cir.setReturnValue(main);
        }
    }

    @Inject(method = "clearPedestals", at = @At("HEAD"))
    private void bfa$rememberRetainedInputs(CallbackInfo ci) {
        bfa$retained = dataCache.cachedIngredients().stream().filter(entry -> SoulItems.isEternal(entry.stack())).toList();
    }

    @Inject(method = "clearPedestals", at = @At("RETURN"))
    private void bfa$rebuildRetainedInputs(CallbackInfo ci) {
        // Original reset clears its ingredient cache even for retained pedestals.
        // Rebuild after completion so the next ritual sees them without reinsertion.
        for (var entry : bfa$retained) dataCache.setIngredient(entry.pos(), entry.stack());
        bfa$retained = java.util.List.of();
    }
}
