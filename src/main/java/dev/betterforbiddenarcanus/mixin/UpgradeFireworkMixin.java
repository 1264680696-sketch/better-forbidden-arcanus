package dev.betterforbiddenarcanus.mixin;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(FireworkRocketEntity.class)
public abstract class UpgradeFireworkMixin {
    @ModifyArg(method = "dealExplosionDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float bfa$damage(float damage) {
        return (float)(damage * Math.clamp(((FireworkRocketEntity)(Object)this).getPersistentData().getDouble("bfa_forge_projectile_multiplier"), 1, 1.4));
    }
}
