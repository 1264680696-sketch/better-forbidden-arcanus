package dev.betterforbiddenarcanus.mixin;

import dev.betterforbiddenarcanus.TyrArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public abstract class TyrPiglinMixin {
    @Inject(method = "isWearingGold", at = @At("HEAD"), cancellable = true)
    private static void bfa$tyrCountsAsGold(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (TyrArmor.count(entity) > 0) cir.setReturnValue(true);
    }
}
