package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.betterforbiddenarcanus.client.BowVisuals;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(AbstractClientPlayer.class)
public abstract class BowFovMixin {
    @ModifyExpressionValue(method="getFieldOfViewModifier",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/AbstractClientPlayer;getTicksUsingItem()I"))
    private int time(int time) {
        var stack=((AbstractClientPlayer)(Object)this).getUseItem();
        return stack.getItem() instanceof BowItem?BowVisuals.elapsed(stack,time):time;
    }
}
