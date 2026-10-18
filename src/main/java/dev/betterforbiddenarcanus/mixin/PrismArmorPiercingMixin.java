package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.betterforbiddenarcanus.PrismEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
public abstract class PrismArmorPiercingMixin{
 @WrapOperation(method="getDamageAfterArmorAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
 private float armor(LivingEntity entity,float amount,DamageSource source,float armor,float toughness,Operation<Float> original){return original.call(entity,amount,source,PrismEffects.pierces(source)?armor*.5F:armor,toughness);}
}
