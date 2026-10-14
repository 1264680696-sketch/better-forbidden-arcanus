package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.block.ArcaneDragonEggBlock;
import dev.betterforbiddenarcanus.DragonEggScales;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = ArcaneDragonEggBlock.class, remap = false)
public abstract class DragonEggScalesMixin extends FallingBlock {
    protected DragonEggScalesMixin(Properties properties) { super(properties); }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel server && !old.is(state.getBlock())) DragonEggScales.placed(server, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        super.onRemove(state, level, pos, next, moved);
        if (level instanceof ServerLevel server && !next.is(state.getBlock())) DragonEggScales.removed(server, pos);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random); // Preserve falling before producing scales.
        DragonEggScales.tick(level, pos);
    }
}
