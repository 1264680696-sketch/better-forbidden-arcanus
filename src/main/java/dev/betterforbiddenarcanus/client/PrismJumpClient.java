package dev.betterforbiddenarcanus.client;
import dev.betterforbiddenarcanus.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID,value=Dist.CLIENT)
public final class PrismJumpClient{
 private static boolean down,wasGround;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();if(mc.player==null){down=false;wasGround=false;return;}
  boolean pressed=mc.options.keyJump.isDown();
  if(mc.screen==null&&pressed&&!down&&!mc.player.onGround()&&!wasGround&&PrismEffects.slot(mc.player,net.minecraft.world.entity.EquipmentSlot.LEGS,PrismEffects.WIND))PacketDistributor.sendToServer(new PrismJump.Request());
  down=pressed;wasGround=mc.player.onGround();
 }
}
