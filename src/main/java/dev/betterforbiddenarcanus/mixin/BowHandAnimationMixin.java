package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.betterforbiddenarcanus.client.BowVisuals;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ItemInHandRenderer.class)
public abstract class BowHandAnimationMixin {
    @WrapOperation(method="renderArmWithItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int duration(ItemStack stack,LivingEntity user,Operation<Integer> original) {
        int duration=original.call(stack,user);
        return stack.getItem() instanceof BowItem?user.getUseItemRemainingTicks()+BowVisuals.elapsed(stack,duration-user.getUseItemRemainingTicks()):duration;
    }
}
