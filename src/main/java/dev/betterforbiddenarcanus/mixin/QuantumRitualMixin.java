package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.*;
import dev.betterforbiddenarcanus.QuantumInjector;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value = RitualManager.class, remap = false)
public abstract class QuantumRitualMixin {
    @Shadow private ServerLevel level;
    @Shadow private BlockPos pos;
    @Unique private int bfa$quarterTicks;
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/stal111/forbidden_arcanus/common/block/entity/forge/ritual/ActiveRitualData;incrementCounter()V"))
    private void bfa$faster(ActiveRitualData data, Operation<Void> original) {
        original.call(data);
        if (QuantumInjector.boosts(level, pos)) bfa$quarterTicks++;
        // Do not skip the native 50% failure check when the injector is toggled mid-ritual.
        if (bfa$quarterTicks >= 4 && data.getCounter() != 250) { bfa$quarterTicks -= 4; data.incrementCounter(); }
    }
    @Inject(method = "startRitual", at = @At("RETURN"))
    private void bfa$newRitual(ServerPlayer player, com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssencesStorage storage, CallbackInfoReturnable<Boolean> cir) { if (cir.getReturnValue()) bfa$quarterTicks = 0; }
    @Inject(method = "save", at = @At("RETURN"))
    private void bfa$save(CompoundTag tag, HolderLookup.Provider lookup, CallbackInfoReturnable<CompoundTag> cir) { tag.putInt("bfa_quantum_phase", bfa$quarterTicks); }
    @Inject(method = "load", at = @At("RETURN"))
    private void bfa$load(CompoundTag tag, HolderLookup.Provider lookup, CallbackInfo ci) { bfa$quarterTicks = Math.clamp(tag.getInt("bfa_quantum_phase"), 0, 4); }
}
