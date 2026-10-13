package dev.betterforbiddenarcanus.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stal111.forbidden_arcanus.common.block.ModBlockPatterns;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class GuideGameTests {
    private static final String ROOT = "/assets/better_forbidden_arcanus/patchouli_books/forbidden_arcanus/en_us/entries/";

    private static JsonObject entry(String path) {
        try (var stream = dev.betterforbiddenarcanus.BetterForbiddenArcanus.class.getResourceAsStream(ROOT + path + ".json")) {
            if (stream == null) throw new IllegalStateException("Missing addon book resource: " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    private static IMultiblock structure(String path) {
        var json = entry(path).getAsJsonArray("pages").asList().stream().map(value -> value.getAsJsonObject())
                .filter(page -> page.get("type").getAsString().equals("patchouli:multiblock")).findFirst().orElseThrow().getAsJsonObject("multiblock");
        var pattern = json.getAsJsonArray("pattern").asList().stream().map(layer -> layer.getAsJsonArray().asList().stream()
                .map(value -> value.getAsString()).toArray(String[]::new)).toArray(String[][]::new);
        var mappings = new ArrayList<Object>();
        for (var pair : json.getAsJsonObject("mapping").entrySet()) {
            String predicate = pair.getValue().getAsString();
            mappings.add(pair.getKey().charAt(0));
            mappings.add(predicate.startsWith("#") ? PatchouliAPI.get().tagMatcher(TagKey.create(Registries.BLOCK, ResourceLocation.parse(predicate.substring(1))))
                    : PatchouliAPI.get().looseBlockMatcher(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(predicate))));
        }
        return PatchouliAPI.get().makeMultiblock(pattern, mappings.toArray()).setSymmetrical(json.get("symmetrical").getAsBoolean());
    }

    @GameTest(template = "guide_area", timeoutTicks = 400)
    public static void guideForgeBlueprint(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(8, 4, 8));
        var before = structure("forge_setup/construction");
        before.place(level, center, Rotation.NONE);
        helper.assertTrue(before.validate(level, center, Rotation.NONE), "Published creation blueprint must validate");
        helper.assertTrue(ModBlockPatterns.HEPHAESTUS_PATTERN.find(level, center) != null, "Blueprint must match original forge activation pattern");
        level.setBlockAndUpdate(center.offset(-4, -1, -4), Blocks.DIAMOND_BLOCK.defaultBlockState());
        helper.assertTrue(before.validate(level, center, Rotation.NONE), "Ignored outside corner must allow arbitrary blocks");
        var pedestal = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden_arcanus:darkstone_pedestal"));
        level.setBlockAndUpdate(center.offset(0, 0, -3), pedestal.defaultBlockState());
        helper.assertTrue(before.validate(level, center, Rotation.NONE), "Creation blueprint must accept preplaced pedestals");
        level.setBlockAndUpdate(center.offset(0, 0, -3), Blocks.AIR.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var dust = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden_arcanus:mundabitur_dust")));
        player.setItemInHand(InteractionHand.MAIN_HAND, dust);
        dust.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false)));
        helper.assertTrue(level.getBlockState(center).is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden_arcanus:hephaestus_forge_tier_1"))), "Blueprint must actually activate with Mundabitur dust");
        var complete = structure("forge_setup/pedestals");
        // Creation tag displays can choose magnetized pedestals. API.place only
        // fills empty cells, so reset those eight optional positions before
        // placing the completed diagram's explicitly normal pedestals.
        before.simulate(level, center, Rotation.NONE, true).getSecond().stream()
                .filter(result -> result.getCharacter() == 'H')
                .forEach(result -> level.setBlockAndUpdate(result.getWorldPosition(), Blocks.AIR.defaultBlockState()));
        complete.place(level, center, Rotation.NONE);
        for (int tier = 1; tier <= 5; tier++) {
            level.setBlockAndUpdate(center, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden_arcanus:hephaestus_forge_tier_" + tier)).defaultBlockState());
            var missing = complete.simulate(level, center, Rotation.NONE, false).getSecond().stream()
                    .filter(result -> !result.test(level, Rotation.NONE))
                    .map(result -> result.getWorldPosition() + "=" + level.getBlockState(result.getWorldPosition())).toList();
            helper.assertTrue(complete.validate(level, center, Rotation.NONE), "Completed blueprint must accept forge tier " + tier + "; missing=" + missing);
        }
        level.setBlockAndUpdate(center.offset(-4, -1, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
        helper.assertTrue(!complete.validate(level, center, Rotation.NONE), "Incorrect foundation material must fail");
        helper.succeed();
    }

    @GameTest(template = "guide_area", timeoutTicks = 400)
    public static void guideAuxiliaryBlueprints(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(8, 4, 8));
        var clibano = structure("practical/clibano");
        for (var rotation : Rotation.values()) {
            for (int x = -4; x <= 4; x++) for (int y = -3; y <= 3; y++) for (int z = -4; z <= 4; z++) level.setBlockAndUpdate(center.offset(x,y,z), Blocks.AIR.defaultBlockState());
            clibano.place(level, center, rotation);
            helper.assertTrue(clibano.validate(level, center, rotation), "Clibano book structure must validate facing " + rotation);
            helper.assertTrue(ModBlockPatterns.CLIBANO_COMBUSTION_BASE.find(level, center) != null, "Published Clibano must match original activation pattern facing " + rotation);
        }
        for (int x = -4; x <= 4; x++) for (int y = -3; y <= 3; y++) for (int z = -4; z <= 4; z++) level.setBlockAndUpdate(center.offset(x,y,z), Blocks.AIR.defaultBlockState());
        var obelisk = structure("practical/obelisk");
        obelisk.place(level, center, Rotation.NONE);
        helper.assertTrue(obelisk.validate(level, center, Rotation.NONE), "Obelisk book structure must validate");
        helper.assertTrue(ModBlockPatterns.ARCANE_CRYSTAL_OBELISK_PATTERN.find(level, center) != null, "Published obelisk must match original activation pattern");
        helper.succeed();
    }
}
