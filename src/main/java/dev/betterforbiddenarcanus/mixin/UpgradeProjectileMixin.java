package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ProjectileWeaponItem.class)
public abstract class UpgradeProjectileMixin {
    @WrapOperation(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ProjectileWeaponItem;shootProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFLnet/minecraft/world/entity/LivingEntity;)V"))
    private void bfa$launch(ProjectileWeaponItem weapon, LivingEntity shooter, Projectile projectile, int index, float speed, float spread, float angle,
                            LivingEntity target, Operation<Void> original, @Local(argsOnly = true) ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BowItem && dev.betterforbiddenarcanus.PrismEffects.has(stack, dev.betterforbiddenarcanus.PrismEffects.EARTH)) projectile.getPersistentData().putBoolean("bfa_demolishing_bow", true);
        double factor = ForgeUpgrades.ranged(stack) ? ForgeUpgrades.multiplier(stack) : 1;
        if (factor > 1) projectile.getPersistentData().putDouble("bfa_forge_projectile_multiplier", factor);
        original.call(weapon, shooter, projectile, index, (float)(speed * factor), spread, angle, target);
    }
}
