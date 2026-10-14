package dev.betterforbiddenarcanus;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID)
public final class AurealBottles {
    private AurealBottles(){}
    public static int amount(){return EssenceHelper.getEssenceAmount(new ItemStack(ModItems.AUREAL_BOTTLE.get()),EssenceType.AUREAL);}
    @SubscribeEvent public static void fill(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getItemStack().is(Items.GLASS_BOTTLE)||!(event.getLevel().getBlockEntity(event.getPos()) instanceof EssenceUtremJarBlockEntity jar)
                ||jar.getBlockState().getValue(ModBlockStateProperties.ESSENCE_TYPE)!=EssenceType.AUREAL||event.getEntity().isSpectator())return;
        event.setCanceled(true);int cost=amount();
        if(cost<=0||jar.getAmount()<cost){event.setCancellationResult(InteractionResult.FAIL);return;}
        if(!event.getLevel().isClientSide()) {
            jar.addEssence(-cost);RelicJars.sync(jar);
            event.getEntity().setItemInHand(event.getHand(),ItemUtils.createFilledResult(event.getItemStack(),event.getEntity(),new ItemStack(ModItems.AUREAL_BOTTLE.get())));
            event.getLevel().playSound(null,event.getPos(),net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
    }
}
