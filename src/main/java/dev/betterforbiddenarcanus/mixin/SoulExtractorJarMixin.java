package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import com.stal111.forbidden_arcanus.common.item.SoulExtractorItem;
import dev.betterforbiddenarcanus.EssenceJarInteractions;
import dev.betterforbiddenarcanus.RelicJars;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the native charging, completion validation, particles, wear and dropped soul. */
@Mixin(value = SoulExtractorItem.class, remap = false)
public abstract class SoulExtractorJarMixin {
    @Inject(method = "isValidBlock", at = @At("RETURN"), cancellable = true)
    private void bfa$acceptJar(Level level, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && level.mayInteract(player, pos) && !player.isSpectator()
                && level.getBlockEntity(pos) instanceof EssenceUtremJarBlockEntity jar
                && EssenceJarInteractions.hasSoul(jar)) cir.setReturnValue(true);
    }

    @WrapOperation(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean bfa$withdrawInsteadOfReplacing(Level level, BlockPos pos, BlockState sand, Operation<Boolean> original) {
        if (level.getBlockEntity(pos) instanceof EssenceUtremJarBlockEntity jar && EssenceJarInteractions.hasSoul(jar)) {
            jar.addEssence(-1);
            RelicJars.sync(jar);
            return true;
        }
        return original.call(level, pos, sand);
    }
}
