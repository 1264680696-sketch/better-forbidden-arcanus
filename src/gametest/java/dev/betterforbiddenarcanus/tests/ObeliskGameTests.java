package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.ArcaneCrystalObeliskBlock;
import com.stal111.forbidden_arcanus.common.block.entity.ArcaneCrystalObeliskBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ObeliskPart;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class ObeliskGameTests {
    private static void forge(GameTestHelper h, BlockPos center) {
        // Same 9x9 foundation as the native activation pattern and published guide.
        String[] floor = {"___PPP___", "_PPPAPPP_", "_PAPPPAP_", "PPPPCPPPP", "PAPCACPAP", "PPPPCPPPP", "_PAPPPAP_", "_PPPAPPP_", "___PPP___"};
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) {
            var block = switch (floor[x].charAt(z)) {
                case 'P' -> ModBlocks.POLISHED_DARKSTONE.get();
                case 'A' -> ModBlocks.GILDED_CHISELED_POLISHED_DARKSTONE.get();
                case 'C' -> ModBlocks.CHISELED_ARCANE_POLISHED_DARKSTONE.get();
                default -> null;
            };
            if (block != null) h.getLevel().setBlockAndUpdate(center.offset(x - 4, -1, z - 4), block.defaultBlockState());
        }
        h.getLevel().setBlockAndUpdate(center, ModBlocks.HEPHAESTUS_FORGE_TIER_1.get().defaultBlockState()
                .setValue(com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties.ACTIVATED, true));
    }
    private static void build(GameTestHelper h, BlockPos base, boolean corrupt) {
        h.getLevel().setBlockAndUpdate(base.below(), ModBlocks.GILDED_CHISELED_POLISHED_DARKSTONE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(base, ModBlocks.ARCANE_POLISHED_DARKSTONE.get().defaultBlockState());
        var crystal = (corrupt ? ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK : ModBlocks.ARCANE_CRYSTAL_BLOCK).get();
        h.getLevel().setBlockAndUpdate(base.above(), crystal.defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.above(2), crystal.defaultBlockState());
    }

    private static ItemStack dust(GameTestHelper h, BlockPos target) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var dust = new ItemStack(ModItems.MUNDABITUR_DUST.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, dust);
        dust.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        return player.getMainHandItem();
    }

    @GameTest(template = "obelisk_area", timeoutTicks = 650)
    public static void corruptTransformationProductionAndReload(GameTestHelper h) {
        var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(8, 4, 5));
        var normal = h.absolutePos(new BlockPos(8, 8, 8));
        var nearPos = h.absolutePos(new BlockPos(8, 4, 8));
        var farPos = h.absolutePos(new BlockPos(8, 8, 5));
        forge(h, nearPos); forge(h, farPos);
        build(h, base, true);
        level.setBlockAndUpdate(base.above(2), ModBlocks.ARCANE_CRYSTAL_BLOCK.get().defaultBlockState());
        h.assertTrue(dust(h, base).getCount() == 2, "Mixed crystal structure must not consume Mundabitur dust");
        level.setBlockAndUpdate(base.above(2), ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get().defaultBlockState());
        h.assertTrue(dust(h, base.above(2)).getCount() == 1, "Corrupt structure must transform through original dust flow, consuming one");
        for (int i = 0; i < 3; i++) {
            var state = level.getBlockState(base.above(i));
            h.assertTrue(state.is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_OBELISK.get()), "All three segments must use original corrupt block");
        }
        h.assertTrue(level.getBlockState(base).getValue(ArcaneCrystalObeliskBlock.PART) == ObeliskPart.LOWER
                && level.getBlockState(base).getValue(ArcaneCrystalObeliskBlock.ACTIVE), "Lower segment must activate on gilded stone");
        var inactive = h.absolutePos(new BlockPos(14, 4, 5));
        build(h, inactive, true); level.setBlockAndUpdate(inactive.below(), Blocks.STONE.defaultBlockState());
        dust(h, inactive);
        h.assertTrue(!level.getBlockState(inactive).getValue(ArcaneCrystalObeliskBlock.ACTIVE)
                && level.getBlockEntity(inactive) == null, "Ordinary support must not activate production");
        build(h, normal, false); dust(h, normal);
        var near = (HephaestusForgeBlockEntity)level.getBlockEntity(nearPos);
        var far = (HephaestusForgeBlockEntity)level.getBlockEntity(farPos);
        long start = level.getGameTime();
        h.runAtTickTime(240, () -> {
            int expected = (int)(level.getGameTime() / 100 - start / 100);
            var closest = level.getPoiManager().findClosest(holder -> holder.value() == com.stal111.forbidden_arcanus.core.init.other.ModPOITypes.HEPHAESTUS_FORGE.get(), base, 4, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
            h.assertTrue(near.getEssenceManager().getExperience() == expected, "Nearest forge must gain exactly one XP per 100 ticks; actual="
                    + near.getEssenceManager().getExperience() + "; expected=" + expected + "; closest=" + closest
                    + "; obelisk=" + level.getBlockState(base) + "; entity=" + level.getBlockEntity(base)
                    + "; normalAureal=" + far.getEssenceManager().getAureal());
            h.assertTrue(near.getEssenceManager().getAureal() == 0 && far.getEssenceManager().getExperience() == 0,
                    "Corrupt obelisk must not supply Aureal or multiple forges");
            h.assertTrue(far.getEssenceManager().getAureal() == expected, "Normal obelisk must retain original Aureal production");
            near.getEssenceManager().setExperience(900);
        });
        h.runAtTickTime(360, () -> {
            h.assertTrue(near.getEssenceManager().getExperience() == 900 && far.getEssenceManager().getExperience() == 0,
                    "Full nearest forge must not overflow or redirect to another forge");
            near.getEssenceManager().setExperience(0);
            level.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(480, () -> {
            h.assertTrue(near.getEssenceManager().getExperience() == 0, "Removing activation support must stop XP production");
            level.setBlockAndUpdate(base.below(), ModBlocks.GILDED_CHISELED_POLISHED_DARKSTONE.get().defaultBlockState());
            var entity = (ArcaneCrystalObeliskBlockEntity)level.getBlockEntity(base);
            var saved = entity.saveWithFullMetadata(level.registryAccess());
            var restored = new ArcaneCrystalObeliskBlockEntity(base, level.getBlockState(base));
            restored.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(restored);
        });
        h.runAtTickTime(600, () -> {
            h.assertTrue(near.getEssenceManager().getExperience() > 0 && near.getEssenceManager().getAureal() == 0,
                    "Reloaded original block entity must retain corrupt XP behavior");
            h.succeed();
        });
    }
}
