package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.betterforbiddenarcanus.SoulBinding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ServerPlayer.class)
public abstract class SoulCrystalPlayerMixin {
    @WrapMethod(method="die")
    private void death(DamageSource source,Operation<Void> original){SoulBinding.withRules((ServerPlayer)(Object)this,()->original.call(source));}
    @WrapOperation(method="die",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/common/CommonHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean accepted(LivingEntity entity,DamageSource source,Operation<Boolean> original){
        boolean canceled=original.call(entity,source);if(!canceled)SoulBinding.activate((ServerPlayer)entity);return canceled;
    }
    @WrapMethod(method="restoreFrom")
    private void restore(ServerPlayer previous,boolean keepEverything,Operation<Void> original){
        SoulBinding.withRules(previous,()->original.call(previous,keepEverything));
        ((ServerPlayer)(Object)this).getPersistentData().remove(SoulBinding.PROTECTED);
        previous.getPersistentData().remove(SoulBinding.PROTECTED);
    }
}
