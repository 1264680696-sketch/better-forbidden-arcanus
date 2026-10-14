package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.core.init.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Wrap the complete native result, including early returns from spectral vision. */
@Mixin(value=Minecraft.class,priority=900)
public abstract class SpectralRangeMixin {
    @WrapMethod(method="shouldEntityAppearGlowing")
    private boolean bfa$range(Entity entity,Operation<Boolean> original) {
        var mc=(Minecraft)(Object)this;
        boolean glowing=original.call(entity);
        if(mc.player==null||!mc.player.hasEffect(ModMobEffects.SPECTRAL_VISION)||mc.player.distanceToSqr(entity)<=1600)return glowing;
        boolean ordinary=entity.isCurrentlyGlowing()||mc.player.isSpectator()&&mc.options.keySpectatorOutlines.isDown()&&entity.getType()==EntityType.PLAYER;
        return ordinary;
    }
}
