package dev.betterforbiddenarcanus.legacy;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class LegacyRuneBlock extends Block {
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public LegacyRuneBlock() {
        super(Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS));
        registerDefaultState(stateDefinition.any().setValue(ACTIVATED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVATED); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(ACTIVATED) && stack.is(ModItems.ARCANE_CRYSTAL.get())) {
            if (level instanceof ServerLevel server) {
                stack.consume(1, player); level.setBlockAndUpdate(pos, state.setValue(ACTIVATED, true));
                LegacySpawnWard.data(server).add(pos);
                server.sendParticles(ParticleTypes.END_ROD, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, 15, .5, .5, .5, .1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (level instanceof ServerLevel server && state.getValue(ACTIVATED)) LegacySpawnWard.data(server).add(pos);
        super.onPlace(state, level, pos, old, moving);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (level instanceof ServerLevel server && (!next.is(this) || !next.getValue(ACTIVATED))) LegacySpawnWard.data(server).remove(pos);
        super.onRemove(state, level, pos, next, moving);
    }
}
