package dev.betterforbiddenarcanus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class PrismJump {
 public record Request() implements CustomPacketPayload {
  public static final Type<Request> TYPE=new Type<>(SoulItems.id("double_jump"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.unit(new Request());
  @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(Request.TYPE,Request.CODEC,(data,ctx)->ctx.enqueueWork(()->{if(ctx.player() instanceof ServerPlayer p)jump(p);}));}
 public static boolean jump(ServerPlayer p){
  if(!PrismEffects.slot(p,EquipmentSlot.LEGS,PrismEffects.WIND)||p.onGround()||p.isInWaterOrBubble()||p.onClimbable()||p.getAbilities().flying||p.isFallFlying()||p.isPassenger()||!p.isAlive()||p.getPersistentData().getBoolean("bfa_double_jump_used"))return false;
  p.getPersistentData().putBoolean("bfa_double_jump_used",true);
  var v=p.getDeltaMovement();p.setDeltaMovement(v.x,.52,v.z);p.fallDistance=0;p.hurtMarked=true;
  p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
  p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),8,.2,.1,.2,.03);
  return true;
 }
}
