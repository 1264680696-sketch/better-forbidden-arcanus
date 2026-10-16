package dev.betterforbiddenarcanus.mixin;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AreaEffectCloud.class)
public abstract class DracoCloudMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void bfa$debuffs(CallbackInfo ci) {
        var cloud = (AreaEffectCloud)(Object)this;
        if (cloud.level().isClientSide() || cloud.isRemoved() || cloud.isWaiting() || !cloud.getPersistentData().getBoolean("bfa_draco_breath")) return;
        for (var mob : cloud.level().getEntitiesOfClass(LivingEntity.class, cloud.getBoundingBox(), e -> !(e instanceof Player) && e.isAlive() && e.isAffectedByPotions())) {
            double x = mob.getX()-cloud.getX(), z = mob.getZ()-cloud.getZ();
            if (x*x+z*z > cloud.getRadius()*cloud.getRadius()) continue;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), cloud);
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), cloud);
        }
    }
}
