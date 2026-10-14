package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = EssenceUtremJarBlockEntity.class, remap = false)
public abstract class JarStockMixin {
    @Shadow private int amount;
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void bfa$repairNegativeStock(CompoundTag tag, HolderLookup.Provider lookup, CallbackInfo ci) { amount = Math.max(0, amount); }
}
