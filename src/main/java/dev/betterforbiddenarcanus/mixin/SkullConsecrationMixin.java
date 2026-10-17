package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ForgeDataCache;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.*;
import dev.betterforbiddenarcanus.AurealicSkulls;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RitualManager.class, remap = false)
public abstract class SkullConsecrationMixin {
    @Shadow private ForgeDataCache dataCache;
    @Shadow private int forgeTier;
    @Shadow private ServerLevel level;
    @Shadow private BlockPos pos;
    @Shadow private Holder<Ritual> validRitual;
    @Shadow private ActiveRitualData activeRitualData;

    @Unique private boolean bfa$full(EssencesDefinition definition) {
        if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge)) return false;
        int capacity = ((EssenceCapacityAccessor) forge.getEssenceManager()).bfa$capacity().get(EssenceType.AUREAL);
        return capacity > 0 && definition.get(EssenceType.AUREAL) >= capacity;
    }
    @Inject(method = "canStartRitual", at = @At("HEAD"), cancellable = true)
    private void bfa$matchFullAureal(Ritual ritual, EssencesDefinition definition, CallbackInfoReturnable<Boolean> cir) {
        if (ritual.result() instanceof AurealicSkulls.ConsecrateResult)
            cir.setReturnValue(ritual.canStart(dataCache, forgeTier) && bfa$full(definition));
    }
    @Inject(method = "startRitual", at = @At("HEAD"), cancellable = true)
    private void bfa$recheck(ServerPlayer player, EssencesStorage storage, CallbackInfoReturnable<Boolean> cir) {
        if (validRitual != null && validRitual.value().result() instanceof AurealicSkulls.ConsecrateResult
                && (!bfa$full(storage.immutable()) || !validRitual.value().canStart(dataCache, forgeTier))) cir.setReturnValue(false);
    }
    @WrapOperation(method = "lambda$startRitual$5", at = @At(value = "INVOKE", target = "Lcom/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssencesStorage;reduce(Lcom/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssencesDefinition;)V"))
    private void bfa$spendAllAureal(EssencesStorage storage, EssencesDefinition definition, Operation<Void> original) {
        if (activeRitualData != null && activeRitualData.getRitual().result() instanceof AurealicSkulls.ConsecrateResult)
            storage.put(EssenceType.AUREAL, 0);
        else original.call(storage, definition);
    }
}
