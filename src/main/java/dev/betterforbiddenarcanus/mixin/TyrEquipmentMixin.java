package dev.betterforbiddenarcanus.mixin;

import dev.betterforbiddenarcanus.TyrCombat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TyrEquipmentMixin {
    @Inject(method = "onEquipItem", at = @At("TAIL"))
    private void bfa$refreshArmor(EquipmentSlot slot, ItemStack before, ItemStack after, CallbackInfo ci) {
        if ((Object)this instanceof Player player && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) TyrCombat.update(player);
    }
}
