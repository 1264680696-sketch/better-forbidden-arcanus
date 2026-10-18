package dev.betterforbiddenarcanus.mixin;
import dev.betterforbiddenarcanus.SoulBinding;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameRules.class)
public abstract class SoulCrystalRulesMixin {
    @Inject(method="getBoolean",at=@At("HEAD"),cancellable=true)
    private void personalKeep(GameRules.Key<GameRules.BooleanValue> key,CallbackInfoReturnable<Boolean> cir){
        if(key==GameRules.RULE_KEEPINVENTORY&&SoulBinding.keepingInScope())cir.setReturnValue(true);
    }
}
