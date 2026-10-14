package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.ForgeDataCache;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssencesStorage;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.*;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Only addon equipment rituals get cancellation without consuming ingredients. */
@Mixin(value = RitualManager.class, remap = false)
public abstract class RitualMixin {
    @Shadow private ForgeDataCache dataCache;
    @Shadow private int forgeTier;
    @Shadow private ActiveRitualData activeRitualData;
    @Shadow private net.minecraft.server.level.ServerLevel level;
    @Shadow private net.minecraft.core.Holder<Ritual> validRitual;
    @Shadow protected abstract void reset();
    @Unique private boolean bfa$cancelling;
    @Unique private ItemStack bfa$main = ItemStack.EMPTY;
    @Unique private final Map<BlockPos, ItemStack> bfa$sides = new HashMap<>();
    @Unique private boolean bfa$isUpgrade() {
        return activeRitualData != null && activeRitualData.getRitual().result() instanceof ForgeUpgrades.UpgradeResult;
    }
    @Unique private void bfa$cancel() {
        if (level == null) {
            activeRitualData = null; validRitual = null;
            bfa$main = ItemStack.EMPTY; bfa$sides.clear(); return;
        }
        dataCache.cachedIngredients().forEach(entry -> {
            if (level.getBlockEntity(entry.pos()) instanceof com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity pedestal)
                pedestal.setItemHeightTarget(com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity.DEFAULT_ITEM_HEIGHT);
        });
        bfa$cancelling = true;
        try { reset(); } finally { bfa$cancelling = false; }
        bfa$main = ItemStack.EMPTY; bfa$sides.clear();
    }
    @Unique private boolean bfa$unchanged() {
        if (bfa$main.isEmpty() || !ItemStack.matches(bfa$main, dataCache.mainIngredient()) || bfa$sides.size() != dataCache.cachedIngredients().size()) return false;
        return dataCache.cachedIngredients().stream().allMatch(e -> ItemStack.matches(e.stack(), bfa$sides.getOrDefault(e.pos(), ItemStack.EMPTY))
                && level.getBlockEntity(e.pos()) instanceof com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity pedestal
                && ItemStack.matches(pedestal.getStack(), bfa$sides.get(e.pos())));
    }
    @Inject(method = "canStartRitual", at = @At("HEAD"), cancellable = true)
    private void bfa$singleMaterials(Ritual ritual, com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssencesDefinition definition, CallbackInfoReturnable<Boolean> cir) {
        if (ritual.result() instanceof ForgeUpgrades.UpgradeResult && dataCache.cachedIngredients().stream().anyMatch(e -> e.stack().getCount() != 1)) cir.setReturnValue(false);
    }
    @Inject(method = "startRitual", at = @At("RETURN"))
    private void bfa$snapshot(ServerPlayer player, EssencesStorage storage, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && bfa$isUpgrade()) {
            bfa$main = dataCache.mainIngredient().copy(); bfa$sides.clear();
            dataCache.cachedIngredients().forEach(e -> bfa$sides.put(e.pos(), e.stack().copy()));
        }
    }
    @Inject(method = "startRitual", at = @At("HEAD"), cancellable = true)
    private void bfa$validateStart(ServerPlayer player, EssencesStorage storage, CallbackInfoReturnable<Boolean> cir) {
        if (validRitual != null && validRitual.value().result() instanceof ForgeUpgrades.UpgradeResult
                && (!validRitual.value().canStart(dataCache, forgeTier) || dataCache.cachedIngredients().stream().anyMatch(e -> e.stack().getCount() != 1
                || !(level.getBlockEntity(e.pos()) instanceof com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity pedestal)
                || !ItemStack.matches(e.stack(), pedestal.getStack())))) cir.setReturnValue(false);
    }
    @Inject(method = "onDataChanged", at = @At("HEAD"))
    private void bfa$inputsChanged(ForgeDataCache cache, com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssencesDefinition definition, HolderLookup.Provider lookup, CallbackInfo ci) {
        if (bfa$isUpgrade() && (!activeRitualData.getRitual().canStart(cache, forgeTier) || !ItemStack.matches(bfa$main, cache.mainIngredient())
                || cache.cachedIngredients().size() != bfa$sides.size()
                || cache.cachedIngredients().stream().anyMatch(e -> !ItemStack.matches(e.stack(), bfa$sides.getOrDefault(e.pos(), ItemStack.EMPTY))))) bfa$cancel();
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bfa$checkActive(CallbackInfoReturnable<Optional<ItemStack>> cir) {
        if (bfa$isUpgrade() && (!activeRitualData.getRitual().canStart(dataCache, forgeTier) || !bfa$unchanged())) {
            bfa$cancel(); cir.setReturnValue(Optional.empty());
        }
    }
    @Inject(method = "failRitual", at = @At("HEAD"), cancellable = true)
    private void bfa$keepCancelledInputs(CallbackInfoReturnable<ItemStack> cir) {
        if (bfa$isUpgrade()) { bfa$cancel(); cir.setReturnValue(ItemStack.EMPTY); }
    }
    @Inject(method = "clearPedestals", at = @At("HEAD"), cancellable = true)
    private void bfa$keepMaterials(CallbackInfo ci) { if (bfa$cancelling) ci.cancel(); }
    @Inject(method = "save", at = @At("RETURN"))
    private void bfa$save(CompoundTag tag, HolderLookup.Provider lookup, CallbackInfoReturnable<CompoundTag> cir) {
        if (!bfa$isUpgrade() || bfa$main.isEmpty()) return;
        tag.put("bfa_upgrade_main", bfa$main.save(lookup));
        var sides = new ListTag();
        bfa$sides.forEach((pos, stack) -> {
            var entry = new CompoundTag(); entry.putLong("pos", pos.asLong()); entry.put("stack", stack.save(lookup)); sides.add(entry);
        });
        tag.put("bfa_upgrade_sides", sides);
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void bfa$load(CompoundTag tag, HolderLookup.Provider lookup, CallbackInfo ci) {
        bfa$main = ItemStack.parseOptional(lookup, tag.getCompound("bfa_upgrade_main")); bfa$sides.clear();
        for (var value : tag.getList("bfa_upgrade_sides", 10)) {
            var entry = (CompoundTag)value;
            bfa$sides.put(BlockPos.of(entry.getLong("pos")), ItemStack.parseOptional(lookup, entry.getCompound("stack")));
        }
    }
}
