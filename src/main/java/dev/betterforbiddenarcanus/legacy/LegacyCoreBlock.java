package dev.betterforbiddenarcanus.legacy;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;

public final class LegacyCoreBlock extends Block implements SimpleWaterloggedBlock {
    private final boolean core;
    public LegacyCoreBlock(boolean core) {
        super(Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> core ? 7 : 0));
        this.core = core; registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.WATERLOGGED); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }
    @Override protected FluidState getFluidState(BlockState state) { return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }
    @Override protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }
    public static Item transform(ItemStack stack) {
        if (stack.is(ModItems.RUNE.get())) return LegacyContent.DARK_RUNE.get();
        if (stack.is(LegacyContent.RUNE_BAG.get())) return LegacyContent.DARK_BAG.get();
        if (stack.is(ModBlocks.RUNIC_STONE.get().asItem()) || stack.is(ModBlocks.RUNIC_DEEPSLATE.get().asItem()) || stack.is(ModBlocks.RUNIC_DARKSTONE.get().asItem()))
            return LegacyContent.DARK_ORE.get().asItem();
        return null;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!core && stack.is(ModItems.DARK_NETHER_STAR.get())) {
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlockAndUpdate(pos, LegacyContent.CORE.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)));
                level.playSound(null, pos, LegacyContent.ACTIVATE.get(), SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        Item output = core ? transform(stack) : null;
        if (output == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide()) {
            stack.consume(1, player);
            ItemStack result = new ItemStack(output);
            if (!player.getInventory().add(result)) player.drop(result, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (core && !level.isClientSide()) level.playSound(null, pos, LegacyContent.ACTIVATE.get(), SoundSource.BLOCKS, 1, 1);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!core) return;
        for (int i = 0; i < 2; i++) level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX()+random.nextDouble(), pos.getY()+random.nextDouble(), pos.getZ()+random.nextDouble(), 0, .02, 0);
        if (random.nextInt(80) == 0) level.playLocalSound(pos, LegacyContent.AMBIENT.get(), SoundSource.BLOCKS, .7F, 1, false);
    }
}
