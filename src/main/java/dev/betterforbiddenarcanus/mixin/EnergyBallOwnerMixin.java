package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.entity.projectile.EnergyBall;
import dev.betterforbiddenarcanus.DracoCombat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value = EnergyBall.class, remap = false)
public abstract class EnergyBallOwnerMixin extends Projectile {
    @Shadow private LivingEntity shootingEntity;
    protected EnergyBallOwnerMixin(EntityType<? extends Projectile> type, Level level) { super(type, level); }
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void bfa$owner(CallbackInfo ci) {
        if (shootingEntity != null) { setOwner(shootingEntity); getPersistentData().putBoolean(DracoCombat.SCEPTER_SHOT, true); }
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bfa$saveOwner(CompoundTag tag, CallbackInfo ci) { super.addAdditionalSaveData(tag); }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bfa$loadOwner(CompoundTag tag, CallbackInfo ci) { super.readAdditionalSaveData(tag); }
    @Inject(method = "tick", at = @At("HEAD"))
    private void bfa$resolveOwner(CallbackInfo ci) { if (shootingEntity == null && getOwner() instanceof LivingEntity living) shootingEntity = living; }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void bfa$deflect(net.minecraft.world.damagesource.DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && shootingEntity != null) setOwner(shootingEntity);
    }
}
