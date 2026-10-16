package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.entity.projectile.DracoArcanusArrow;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = DracoArcanusArrow.class, remap = false)
public abstract class DracoArrowMixin {
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void bfa$base(CallbackInfo ci) { ((AbstractArrow)(Object)this).setBaseDamage(5); }
    @WrapOperation(method = "doPostHurtEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean bfa$cloud(Level level, Entity entity, Operation<Boolean> original) {
        if (entity instanceof AreaEffectCloud cloud) {
            cloud.setRadius(2.5F); cloud.setRadiusPerTick((7F-2F)/400F*1.25F);
            cloud.getPersistentData().putBoolean("bfa_draco_breath", true);
        }
        return original.call(level, entity);
    }
}
