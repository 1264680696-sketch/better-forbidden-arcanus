package dev.betterforbiddenarcanus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResultType;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** A single grade component is the source of truth; upgrades never rewrite base stats. */
public final class ForgeUpgrades {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIER = COMPONENTS.register("forge_upgrade_tier",
            () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(1, 5)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<IngredientType<?>, IngredientType<EquipmentIngredient>> EQUIPMENT = INGREDIENTS.register("forgeable_equipment", () -> new IngredientType<>(EquipmentIngredient.CODEC));
    public static final DeferredRegister<RitualResultType<?>> RESULTS = DeferredRegister.create(FARegistries.RITUAL_RESULT_TYPE, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<RitualResultType<?>, RitualResultType<UpgradeResult>> RESULT = RESULTS.register("upgrade_equipment", () -> new RitualResultType<>(UpgradeResult.CODEC));
    private static final double[] BONUSES = {0, .05, .10, .20, .30, .40};
    private static final ChatFormatting[] COLORS = {ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.BLUE, ChatFormatting.DARK_PURPLE, ChatFormatting.RED};
    private ForgeUpgrades() {}
    public static int tier(ItemStack stack) { return Math.clamp(stack.getOrDefault(TIER, 0), 0, 5); }
    public static double bonus(ItemStack stack) { return BONUSES[tier(stack)]; }
    public static double multiplier(ItemStack stack) { return 1 + bonus(stack); }
    public static ChatFormatting color(ItemStack stack) { return COLORS[tier(stack)]; }
    public static boolean ranged(ItemStack stack) { return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem; }
    public static boolean supported(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof AnimalArmorItem) return false;
        return !stack.isEmpty() && (item instanceof ArmorItem || ranged(stack) || stack.has(DataComponents.TOOL)
                || stack.is(ItemTags.SWORDS) || item instanceof SwordItem || item instanceof TridentItem || item instanceof MaceItem);
    }
    public static boolean canUpgrade(ItemStack stack, int target) {
        return target >= 1 && target <= 5 && stack.getCount() == 1 && supported(stack) && tier(stack) < target;
    }
    public record EquipmentIngredient(int targetTier) implements ICustomIngredient {
        public static final MapCodec<EquipmentIngredient> CODEC = Codec.intRange(1, 5).fieldOf("target_tier").xmap(EquipmentIngredient::new, EquipmentIngredient::targetTier);
        @Override public boolean test(ItemStack stack) { return canUpgrade(stack, targetTier); }
        @Override public Stream<ItemStack> getItems() { return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(ForgeUpgrades::supported); }
        @Override public boolean isSimple() { return false; }
        @Override public IngredientType<?> getType() { return EQUIPMENT.get(); }
    }
    public record UpgradeResult(int tier) implements RitualResult {
        public static final MapCodec<UpgradeResult> CODEC = Codec.intRange(1, 5).fieldOf("tier").xmap(UpgradeResult::new, UpgradeResult::tier);
        @Override public ItemStack getResultItem(ItemStack input) {
            if (!canUpgrade(input, tier)) return input.copy();
            var result = input.copy(); result.set(TIER, tier); return result;
        }
        @Override public RitualResultType<?> getType() { return RESULT.get(); }
    }
}
