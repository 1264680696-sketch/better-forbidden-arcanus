package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceManager;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import dev.betterforbiddenarcanus.RelicEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EssenceManager.class, remap = false)
public abstract class CrimsonBloodMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssenceManager;increaseEssence(Lcom/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssenceType;I)V"))
    private void bfa$moreAttackBlood(EssenceManager manager, EssenceType type, int amount, @Local LivingEntity victim) {
        var source = victim.getLastDamageSource();
        if (type == EssenceType.BLOOD && source != null && source.getEntity() instanceof Player player && RelicEffects.hasCrimson(player)) {
            amount += amount / 2;
        }
        manager.increaseEssence(type, amount);
    }
}
