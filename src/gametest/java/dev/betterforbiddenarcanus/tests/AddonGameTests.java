package dev.betterforbiddenarcanus.tests;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class AddonGameTests {
    private AddonGameTests() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("forbidden_arcanus", path);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(id(path)).orElseThrow();
    }

    @GameTest(template = "empty")
    public static void stonecuttingMatrix(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING);
        try (var stream = AddonGameTests.class.getResourceAsStream("/stonecutting_expectations.json")) {
            var rows = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            helper.assertTrue(rows.size() == 39, "Expected 39 effective stonecutting conversions");
            for (var row : rows) {
                var pair = row.getAsJsonObject();
                var inputId = ResourceLocation.parse(pair.get("input").getAsString());
                var outputId = ResourceLocation.parse(pair.get("output").getAsString());
                var input = new SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.get(inputId)));
                var matches = recipes.stream().filter(holder -> holder.value().matches(input, helper.getLevel()))
                        .filter(holder -> BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(helper.getLevel().registryAccess()).getItem()).equals(outputId)).toList();
                helper.assertTrue(matches.size() == 1, "Missing or duplicate conversion: " + inputId + " -> " + outputId);
                helper.assertTrue(matches.getFirst().value().getResultItem(helper.getLevel().registryAccess()).getCount() == 1,
                        "Stonecutting output must be exactly one item");
            }
            long additions = recipes.stream().filter(holder -> holder.id().getNamespace().equals("better_forbidden_arcanus")).count();
            helper.assertTrue(additions == 36, "Expected exactly 36 added stonecutting recipes");
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideBookCrafting(GameTestHelper helper) {
        var bookId = ResourceLocation.fromNamespaceAndPath("better_forbidden_arcanus", "forbidden_arcanus");
        var expected = vazkii.patchouli.api.PatchouliAPI.get().getBookStack(bookId);
        helper.assertTrue(expected.getHoverName().getString().equals("禁忌与奥秘"), "Guide definition must load on the server");
        for (var ingredients : List.of(List.of(new ItemStack(item("arcane_crystal")), new ItemStack(Items.BOOK)),
                List.of(new ItemStack(Items.BOOK), new ItemStack(item("arcane_crystal"))))) {
            var input = CraftingInput.of(2, 1, ingredients);
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
            helper.assertTrue(recipe.id().equals(ResourceLocation.fromNamespaceAndPath("better_forbidden_arcanus", "guide_book")), "Guide recipe must be shapeless");
            var result = recipe.value().assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.getCount() == 1 && ItemStack.isSameItemSameComponents(result, expected), "Recipe must produce the exact named Patchouli book component");
        }
        var wrong = CraftingInput.of(2, 1, List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.BOOK)));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, wrong, helper.getLevel()).isEmpty(), "Amethyst must not replace arcane crystal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soupIngredients(GameTestHelper helper) {
        for (Item green : List.of(Items.GLOW_BERRIES, Items.GLOW_LICHEN)) {
            var input = CraftingInput.of(2, 2, List.of(new ItemStack(Items.RED_MUSHROOM),
                    new ItemStack(green), new ItemStack(Items.BOWL), new ItemStack(item("bat_wing"))));
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent() && recipe.get().id().equals(id("bat_soup")), "New soup ingredient must match original recipe ID");
            var output = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(output.is(item("bat_soup")) && output.getCount() == 1, "Expected one original bat soup");
        }
        var oldInput = CraftingInput.of(2, 2, List.of(new ItemStack(Items.RED_MUSHROOM), new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.BOWL), new ItemStack(item("bat_wing"))));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, oldInput, helper.getLevel()).isEmpty(),
                "Nether wart must no longer craft bat soup");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void batLootProbabilityAndLooting(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var bat = EntityType.BAT.create(level);
        var table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("minecraft", "entities/bat")));
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, bat)
                .withParameter(LootContextParams.ORIGIN, bat.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .create(LootContextParamSets.ENTITY);
        for (int looting : new int[]{0, 3}) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            if (looting > 0) {
                sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), looting);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            int successes = 0;
            boolean extraQuantity = false;
            RandomSource random = RandomSource.create(87001L);
            for (int sample = 0; sample < 10000; sample++) {
                int count = table.getRandomItems(params, random).stream()
                        .filter(stack -> stack.is(item("bat_wing"))).mapToInt(ItemStack::getCount).sum();
                helper.assertTrue(count >= 0 && count <= 1 + looting, "Duplicate or invalid bat wing drop");
                if (count > 0) successes++;
                if (count > 1) extraQuantity = true;
            }
            helper.assertTrue(successes >= 7800 && successes <= 8200,
                    "80% loot gate expected; looting=" + looting + ", successes=" + successes);
            helper.assertTrue(looting == 0 || extraQuantity, "Looting must retain quantity bonuses");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soupConsumption(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        var food = item("bat_soup").components().get(DataComponents.FOOD);
        helper.assertTrue(food.nutrition() == 7 && Math.abs(food.saturation() - 9.8F) < 0.001F,
                "Original nutrition/saturation must be preserved");
        helper.assertTrue(!food.canAlwaysEat() && food.usingConvertsTo().isEmpty(), "Original eating/remainder behavior must be preserved");
        new ItemStack(item("bat_soup")).finishUsingItem(helper.getLevel(), player);
        assertEffect(helper, player, MobEffects.NIGHT_VISION, 1800);
        assertEffect(helper, player, MobEffects.MOVEMENT_SPEED, 600);
        assertEffect(helper, player, MobEffects.GLOWING, 1800);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 17, "Soup must restore seven food points");
        new ItemStack(item("bat_soup")).finishUsingItem(helper.getLevel(), player);
        assertEffect(helper, player, MobEffects.NIGHT_VISION, 1800);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledConsumptionAndOtherFoods(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("bat_soup")));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        helper.assertTrue(player.getActiveEffects().isEmpty(), "Cancelled eating must not grant effects");
        new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "Other foods must not receive soup effects");
        helper.succeed();
    }

    private static void assertEffect(GameTestHelper helper, Player player, Holder<MobEffect> effect, int duration) {
        var actual = player.getEffect(effect);
        helper.assertTrue(actual != null && actual.getAmplifier() == 0 && actual.getDuration() == duration,
                "Incorrect effect duration or amplifier: " + effect);
    }
}
