package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(AbstractArrow.class)
public abstract class UpgradeArrowMixin {
    @Unique private double bfa$factor() {
        return Math.clamp(((AbstractArrow)(Object)this).getPersistentData().getDouble("bfa_forge_projectile_multiplier"), 1, 1.4);
    }
    // Undo the upgraded velocity in vanilla's kinetic damage term, then apply
    // the grade once to final damage, including the native critical bonus.
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;length()D"))
    private double bfa$kinetic(Vec3 velocity, Operation<Double> original) { return original.call(velocity) / bfa$factor(); }
    @ModifyArg(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float bfa$damage(float damage) { return (float)(damage * bfa$factor()); }
}
