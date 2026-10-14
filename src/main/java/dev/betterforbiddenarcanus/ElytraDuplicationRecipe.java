package dev.betterforbiddenarcanus;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Normal shaped crafting, with the original elytra returned as a catalyst. */
public final class ElytraDuplicationRecipe extends ShapedRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, Serializer> SERIALIZER = SERIALIZERS.register("elytra_duplication", Serializer::new);

    private ElytraDuplicationRecipe(ShapedRecipe original) {
        // Vanilla ShapedRecipe's result getter does not use the lookup argument.
        super(original.getGroup(), original.category(), original.pattern, original.getResultItem(null), original.showNotification());
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).is(Items.ELYTRA)) remaining.set(slot, input.getItem(slot).copyWithCount(1));
        }
        return remaining;
    }

    public static final class Serializer implements RecipeSerializer<ElytraDuplicationRecipe> {
        private static final MapCodec<ElytraDuplicationRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(ElytraDuplicationRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, ElytraDuplicationRecipe> STREAM_CODEC = ShapedRecipe.Serializer.STREAM_CODEC.map(ElytraDuplicationRecipe::new, recipe -> recipe);
        @Override public MapCodec<ElytraDuplicationRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ElytraDuplicationRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
