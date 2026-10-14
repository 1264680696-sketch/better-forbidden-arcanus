package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.entity.forge.input.EssenceDataInput;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.inventory.clibano.ClibanoMenu;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import dev.betterforbiddenarcanus.SoulItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class SoulGameTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final List<BlockPos> SIDES = List.of(new BlockPos(0, 1, 1), new BlockPos(2, 1, 1), new BlockPos(1, 1, 0), new BlockPos(1, 1, 2));

    private static net.minecraft.server.level.ServerPlayer ritualPlayer(GameTestHelper h) {
        // Rituals only need a UUID. Do not join a mock client without NeoForge's
        // payload handshake: the upstream aureal join event requires that handshake.
        return new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ritual-test"),
                net.minecraft.server.level.ClientInformation.createDefault());
    }

    private static ClibanoMainBlockEntity clibano(GameTestHelper h) {
        h.setBlock(CENTER, Blocks.AIR);
        h.setBlock(CENTER, ModBlocks.CLIBANO_MAIN_PART.get());
        return h.getBlockEntity(CENTER);
    }

    private static void tick(GameTestHelper h, ClibanoMainBlockEntity c, int count) {
        for (int i = 0; i < count; i++) ClibanoMainBlockEntity.serverTick(h.getLevel(), c.getBlockPos(), c.getBlockState(), c);
    }

    private static int output(ClibanoMainBlockEntity c, Item item) {
        return List.of(ClibanoMenu.RESULT_SLOTS.getFirst(), ClibanoMenu.RESULT_SLOTS.getSecond()).stream()
                .map(c::getStack).filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void threeSoulSpeedsAndNoConsumption(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(SoulItems.SOUL_SAND_RECIPE).orElseThrow();
        List<Item> souls = List.of(ModItems.SOUL.get(), ModItems.CORRUPT_SOUL.get(), ModItems.ENCHANTED_SOUL.get());
        int[] durations = {80, 66, 40};
        for (int i = 0; i < souls.size(); i++) {
            var c = clibano(h);
            c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(souls.get(i)));
            c.setStack(ClibanoMenu.FUEL_SLOT, new ItemStack(Items.COAL, 64));
            c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.RAW_IRON, 64));
            @SuppressWarnings("unchecked") var cast = (net.minecraft.world.item.crafting.RecipeHolder<com.stal111.forbidden_arcanus.common.item.crafting.ClibanoRecipe>) (Object) recipe;
            h.assertTrue(c.getCookingTime(cast) == durations[i], "Incorrect speed for " + souls.get(i));
            tick(h, c, durations[i]);
            h.assertTrue(output(c, Items.IRON_INGOT) == 1, "Actual completion time must match soul multiplier");
            tick(h, c, 2800);
            h.assertTrue(c.getStack(ClibanoMenu.SOUL_SLOT).getCount() == 1, "Soul must survive original 2700-tick timer");
            c.setStack(ClibanoMenu.SOUL_SLOT, ItemStack.EMPTY);
            h.assertTrue(c.getCookingTime(cast) == 100, "Removing soul must immediately remove speed");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void soulSandGateDualInputAndResidue(GameTestHelper h) {
        for (Item soul : List.of(ModItems.SOUL.get())) {
            var c = clibano(h);
            c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(soul));
            c.setStack(ClibanoMenu.FUEL_SLOT, new ItemStack(Items.COAL));
            c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.SOUL_SAND));
            tick(h, c, 150);
            h.assertTrue(output(c, ModItems.SOUL.get()) == 0 && c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount() == 1,
                    "Ordinary soul cannot extract souls");
        }
        for (Item soul : List.of(ModItems.CORRUPT_SOUL.get(), ModItems.ENCHANTED_SOUL.get())) {
            var c = clibano(h);
            c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(soul));
            c.setStack(ClibanoMenu.FUEL_SLOT, new ItemStack(Items.COAL));
            c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.SOUL_SAND));
            c.setStack(ClibanoMenu.INPUT_SLOTS.getSecond(), new ItemStack(Items.SOUL_SAND));
            tick(h, c, 100);
            h.assertTrue(output(c, ModItems.SOUL.get()) == 2 && output(c, ModBlocks.SOULLESS_SAND.get().asItem()) == 2,
                    "Both inputs must yield one soul and one sand each");
            h.assertTrue(c.getStack(ClibanoMenu.SOUL_SLOT).getCount() == 1 && c.getStack(ClibanoMenu.FUEL_SLOT).isEmpty(),
                    "Only fuel must be consumed");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void blockedClibanoAndRateChanges(GameTestHelper h) {
        var c = clibano(h);
        c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(ModItems.CORRUPT_SOUL.get()));
        c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.SOUL_SAND, 4));
        tick(h, c, 100);
        h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount() == 4, "No fuel must block processing");
        c.setStack(ClibanoMenu.FUEL_SLOT, new ItemStack(Items.COAL));
        c.setStack(ClibanoMenu.RESULT_SLOTS.getFirst(), new ItemStack(Items.STONE, 64));
        c.setStack(ClibanoMenu.RESULT_SLOTS.getSecond(), new ItemStack(Items.DIRT, 64));
        tick(h, c, 100);
        h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount() == 4, "Full outputs must block processing");
        c.setStack(ClibanoMenu.RESULT_SLOTS.getFirst(), new ItemStack(ModItems.SOUL.get()));
        var residue = h.getLevel().registryAccess().registryOrThrow(FARegistries.RESIDUE_TYPE)
                .getHolderOrThrow(ResourceKey.create(FARegistries.RESIDUE_TYPE, SoulItems.id("soulless_sand")));
        c.getResiduesStorage().increaseType(residue, 64);
        tick(h, c, 100);
        h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount() == 4, "Full residue must not discard sand");

        c = clibano(h);
        c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(ModItems.SOUL.get()));
        c.setStack(ClibanoMenu.FUEL_SLOT, new ItemStack(Items.COAL));
        c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.RAW_IRON));
        tick(h, c, 40); // 50% at 1.25x
        c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(ModItems.CORRUPT_SOUL.get()));
        tick(h, c, 32);
        h.assertTrue(output(c, Items.IRON_INGOT) == 0, "Rescaled progress must not complete early");
        tick(h, c, 1);
        h.assertTrue(output(c, Items.IRON_INGOT) == 1, "Ordinary-to-corrupt change must preserve completion fraction");

        c = clibano(h);
        c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(ModItems.CORRUPT_SOUL.get()));
        c.setStack(ClibanoMenu.FUEL_SLOT, new ItemStack(Items.COAL));
        c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.SOUL_SAND));
        tick(h, c, 30);
        c.setStack(ClibanoMenu.SOUL_SLOT, ItemStack.EMPTY);
        c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(ModItems.CORRUPT_SOUL.get()));
        tick(h, c, 36);
        h.assertTrue(output(c, ModItems.SOUL.get()) == 0, "Removing catalyst must clear extraction progress");
        var saved = c.saveWithoutMetadata(h.getLevel().registryAccess());
        var restored = new ClibanoMainBlockEntity(c.getBlockPos(), c.getBlockState());
        restored.setLevel(h.getLevel());
        restored.loadWithComponents(saved, h.getLevel().registryAccess());
        tick(h, restored, 100);
        h.assertTrue(restored.getStack(ClibanoMenu.SOUL_SLOT).getCount() == 1 && output(restored, ModItems.SOUL.get()) == 1,
                "Reload must retain catalyst and finish exactly once");
        c = clibano(h);
        c.setStack(ClibanoMenu.SOUL_SLOT, new ItemStack(ModItems.CORRUPT_SOUL.get()));
        c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(), new ItemStack(Items.SOUL_SAND));
        var almostExtinguished = c.saveWithoutMetadata(h.getLevel().registryAccess());
        almostExtinguished.putInt("burn_time", 1);
        c.loadWithComponents(almostExtinguished, h.getLevel().registryAccess());
        tick(h, c, 100);
        h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount() == 1 && output(c, ModItems.SOUL.get()) == 0,
                "Expired fuel must block completion even when a catalyst remains");
        h.succeed();
    }

    private static HephaestusForgeBlockEntity forge(GameTestHelper h, boolean enchanted, Item main) {
        h.setBlock(CENTER, enchanted ? ModBlocks.HEPHAESTUS_FORGE_TIER_3.get() : ModBlocks.HEPHAESTUS_FORGE_TIER_2.get());
        HephaestusForgeBlockEntity f = h.getBlockEntity(CENTER);
        f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT, new ItemStack(main));
        for (var pos : SIDES) {
            h.setBlock(pos, ModBlocks.DARKSTONE_PEDESTAL.get());
            PedestalBlockEntity p = h.getBlockEntity(pos);
            var stack = new ItemStack(enchanted ? Items.EXPERIENCE_BOTTLE : ModItems.CORRUPTI_DUST.get());
            p.setStack(stack, null, com.stal111.forbidden_arcanus.common.block.pedestal.effect.PedestalEffectTrigger.RITUAL_FINISHED);
            f.updatePedestalStack(p.getBlockPos(), stack);
        }
        var e = f.getEssenceManager();
        e.setAureal(enchanted ? 500 : 100);
        e.setSouls(enchanted ? 20 : 10);
        e.setBlood(enchanted ? 500 : 250);
        return f;
    }

    @GameTest(template = "empty")
    public static void forgeRitualLevelsCostsAndOrdinaryMain(GameTestHelper h) {
        for (boolean enchanted : List.of(false, true)) {
            h.setBlock(CENTER, Blocks.AIR);
            Item main = enchanted ? ModItems.CORRUPT_SOUL.get() : ModItems.SOUL.get();
            var f = forge(h, enchanted, main);
            var manager = f.getRitualManager();
            h.assertTrue(manager.getValidRitual().isPresent(), "Exact essence cost must allow ritual");
            f.setForgeLevel(enchanted ? HephaestusForgeLevel.TWO : HephaestusForgeLevel.ONE);
            f.updatePedestalStack(h.absolutePos(SIDES.getFirst()), ((PedestalBlockEntity) h.getBlockEntity(SIDES.getFirst())).getStack());
            h.assertTrue(manager.getValidRitual().isEmpty(), "Lower forge tier must reject ritual");
            f.setForgeLevel(HephaestusForgeLevel.FIVE);
            f.updatePedestalStack(h.absolutePos(SIDES.getFirst()), ((PedestalBlockEntity) h.getBlockEntity(SIDES.getFirst())).getStack());
            h.assertTrue(manager.startRitual(ritualPlayer(h), f.getEssenceManager().getStorage()), "Higher tier must allow ritual");
            h.assertTrue(f.getEssenceManager().getAureal() == 0 && f.getEssenceManager().getSouls() == 0 && f.getEssenceManager().getBlood() == 0,
                    "Ritual must deduct exact specified essences");
            for (int i = 0; i <= 500; i++) manager.tick().ifPresent(result -> f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT, result));
            Item resultItem = enchanted ? ModItems.ENCHANTED_SOUL.get() : ModItems.CORRUPT_SOUL.get();
            h.assertTrue(f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT).is(resultItem), "Incorrect main/result retention");
            for (var pos : SIDES) h.assertTrue(!((PedestalBlockEntity) h.getBlockEntity(pos)).hasStack(), "Four side ingredients must be consumed");
            h.killAllEntities();
        }
        h.succeed();
    }





    @GameTest(template = "empty")
    public static void creativeTabAndOriginalRitualIngredients(GameTestHelper h) {
        CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
        var tab = SoulItems.TAB.get();
        h.assertTrue(tab.getIconItem().is(ModBlocks.HEPHAESTUS_FORGE_TIER_5.get().asItem()) && tab.getDisplayItems().size() == 1, "Addon tab must use the tier-five icon and contain only the guide");
        var guide = vazkii.patchouli.api.PatchouliAPI.get().getBookStack(SoulItems.id("forbidden_arcanus"));
        h.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, guide)), "Addon tab must contain the named guide");
        var ritual = h.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL)
                .get(ResourceLocation.parse("forbidden_arcanus:soul_binding_crystal"));
        h.assertTrue(ritual.checkIngredients(List.of(new ItemStack(ModItems.SOUL.get()), new ItemStack(ModItems.CORRUPT_SOUL.get()),
                new ItemStack(ModItems.ENCHANTED_SOUL.get()), new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack(ModItems.ENDER_PEARL_FRAGMENT.get()), new ItemStack(ModItems.ENDER_PEARL_FRAGMENT.get()), new ItemStack(ModItems.ENDER_PEARL_FRAGMENT.get())),
                new ItemStack(Items.QUARTZ)), "Original soul-binding ritual must accept ordinary souls");
        h.succeed();
    }
}
