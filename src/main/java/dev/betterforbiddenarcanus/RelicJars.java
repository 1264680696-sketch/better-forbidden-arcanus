package dev.betterforbiddenarcanus;
import com.stal111.forbidden_arcanus.common.block.UtremJarBlock;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.core.init.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID)
public final class RelicJars {
    public static final int PERIOD=100;
    private RelicJars(){}
    public static EssenceType type(ItemStack stack) {
        if(stack.is(ModItems.CRESCENT_MOON.get()))return EssenceType.AUREAL;
        if(stack.is(ModItems.CRIMSON_STONE.get()))return EssenceType.BLOOD;
        return null;
    }
    public static void sync(EssenceUtremJarBlockEntity jar) {
        jar.setChanged();var level=jar.getLevel();if(level!=null)level.sendBlockUpdated(jar.getBlockPos(),jar.getBlockState(),jar.getBlockState(),3);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        var level=event.getLevel();var pos=event.getPos();var state=level.getBlockState(pos);var p=event.getEntity();
        if(!(state.getBlock() instanceof UtremJarBlock)||p.isSpectator())return;
        var jar=level.getBlockEntity(pos) instanceof EssenceUtremJarBlockEntity entity?entity:null;
        if(event.getItemStack().isEmpty()&&p.isShiftKeyDown()&&jar!=null&&!((JarRelicStorage)jar).bfa$relic().isEmpty()) {
            event.setCanceled(true);
            if(!level.isClientSide()) {var storage=(JarRelicStorage)jar;p.setItemInHand(event.getHand(),storage.bfa$relic());storage.bfa$relic(ItemStack.EMPTY);storage.bfa$relicTicks(0);sync(jar);}
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));return;
        }
        var type=type(event.getItemStack());if(type==null)return;
        event.setCanceled(true);
        if(jar!=null&&(!((JarRelicStorage)jar).bfa$relic().isEmpty()||jar.getAmount()>0&&state.getValue(ModBlockStateProperties.ESSENCE_TYPE)!=type)) {
            event.setCancellationResult(InteractionResult.FAIL);return;
        }
        if(!level.isClientSide()) {
            var desired=ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,type)
                    .setValue(UtremJarBlock.WATERLOGGED,state.getValue(UtremJarBlock.WATERLOGGED));
            level.setBlockAndUpdate(pos,desired);jar=(EssenceUtremJarBlockEntity)level.getBlockEntity(pos);
            ((JarRelicStorage)jar).bfa$relic(event.getItemStack().copyWithCount(1));((JarRelicStorage)jar).bfa$relicTicks(0);
            event.getItemStack().consume(1,p);sync(jar);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
    public static void tick(EssenceUtremJarBlockEntity jar) {
        var storage=(JarRelicStorage)jar;var type=type(storage.bfa$relic());
        if(type==null||type!=jar.getBlockState().getValue(ModBlockStateProperties.ESSENCE_TYPE)||jar.getAmount()>=jar.getLimit())return;
        int ticks=storage.bfa$relicTicks()+1;
        storage.bfa$relicTicks(ticks>=PERIOD?0:ticks);jar.setChanged();
        if(ticks>=PERIOD){jar.addEssence(1);sync(jar);}
    }
    public static void drop(Level level,BlockPos pos,EssenceUtremJarBlockEntity jar) {
        var storage=(JarRelicStorage)jar;var stack=storage.bfa$relic();storage.bfa$relic(ItemStack.EMPTY);
        if(!level.isClientSide()&&!stack.isEmpty())Block.popResource(level,pos,stack);
    }
}
