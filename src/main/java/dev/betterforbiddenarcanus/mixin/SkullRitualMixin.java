package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import dev.betterforbiddenarcanus.AurealicSkulls;
import net.minecraft.world.item.ItemStack;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only consecration accepts no pedestal ingredients; other rituals retain the old constraint. */
@Mixin(value = Ritual.class, remap = false)
public abstract class SkullRitualMixin {
    @ModifyConstant(method = "lambda$static$6", constant = @Constant(intValue = 1), require = 1)
    private static int bfa$allowEmptyPedestals(int minimum) { return 0; }

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;", ordinal = 0))
    private static Codec<Ritual> bfa$validateEmptyPedestals(Codec<Ritual> codec) {
        return codec.validate(ritual -> ritual.inputs().isEmpty() && !(AurealicSkulls.consecration(ritual.result()))
                ? DataResult.error(() -> "Only skull consecration may omit pedestal ingredients") : DataResult.success(ritual));
    }

    @Inject(method = "checkIngredients", at = @At("HEAD"), cancellable = true)
    private void bfa$singleSkull(Collection<ItemStack> inputs, ItemStack main, CallbackInfoReturnable<Boolean> cir) {
        if (AurealicSkulls.consecration(((Ritual) (Object) this).result()))
            cir.setReturnValue(inputs.isEmpty() && main.getCount() == 1 && main.is(ModBlocks.ETERNAL_OBSIDIAN_SKULL.getSkull().asItem()));
    }
}
