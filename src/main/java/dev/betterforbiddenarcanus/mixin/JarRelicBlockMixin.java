package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.block.*;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import dev.betterforbiddenarcanus.RelicJars;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=EssenceUtremJarBlock.class,remap=false)
public abstract class JarRelicBlockMixin extends UtremJarBlock {
    protected JarRelicBlockMixin(Properties properties){super(properties);}
    @Inject(method="getTicker",at=@At("HEAD"),cancellable=true)
    private <T extends BlockEntity> void bfa$ticker(Level level,BlockState state,BlockEntityType<T> type,CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        if(!level.isClientSide())cir.setReturnValue((world,pos,s,entity)->{if(entity instanceof EssenceUtremJarBlockEntity jar)RelicJars.tick(jar);});
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved) {
        if(!next.is(state.getBlock())&&level.getBlockEntity(pos) instanceof EssenceUtremJarBlockEntity jar)RelicJars.drop(level,pos,jar);
        super.onRemove(state,level,pos,next,moved);
    }
}
