package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.block.ArcaneCrystalObeliskBlock;
import com.stal111.forbidden_arcanus.common.block.pattern.UpwardsBlockPattern;
import com.stal111.forbidden_arcanus.common.block.properties.ObeliskPart;
import com.stal111.forbidden_arcanus.common.item.mundabitur.MundabiturInteraction;
import com.stal111.forbidden_arcanus.common.item.mundabitur.TransformPatternInteraction;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CorruptedObelisk {
    public static final BlockPattern PATTERN = UpwardsBlockPattern.of(BlockPatternBuilder.start()
            .aisle("#", "#", "X")
            .where('#', BlockInWorld.hasState(state -> state.is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get())))
            .where('X', BlockInWorld.hasState(state -> state.is(ModBlocks.ARCANE_POLISHED_DARKSTONE.get()))));
    public static final DeferredRegister<MundabiturInteraction<?>> INTERACTIONS = DeferredRegister.create(FARegistries.MUNDABITUR_INTERACTION, BetterForbiddenArcanus.MOD_ID);

    static { INTERACTIONS.register("create_corrupted_obelisk", Creation::new); }
    private CorruptedObelisk() {}

    private static final class Creation extends TransformPatternInteraction {
        Creation() {
            super(state -> state.is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get())
                    || state.is(ModBlocks.ARCANE_POLISHED_DARKSTONE.get()), PATTERN);
        }

        @Override
        public void interact(TransformPatternContext context) {
            var level = context.level();
            var base = context.pos();
            while (level.getBlockState(base).is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get())) base = base.below();
            var block = ModBlocks.CORRUPTED_ARCANE_CRYSTAL_OBELISK.get().defaultBlockState();
            placeBlock(level, base, block.setValue(ArcaneCrystalObeliskBlock.PART, ObeliskPart.LOWER)
                    .setValue(ArcaneCrystalObeliskBlock.ACTIVE, ArcaneCrystalObeliskBlock.shouldActivate(level, base)));
            placeBlock(level, base.above(), block.setValue(ArcaneCrystalObeliskBlock.PART, ObeliskPart.MIDDLE));
            placeBlock(level, base.above(2), block.setValue(ArcaneCrystalObeliskBlock.PART, ObeliskPart.UPPER));
        }
    }
}
