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
 public record Jumped(net.minecraft.world.phys.Vec3 origin) implements CustomPacketPayload {
  public static final Type<Jumped> TYPE=new Type<>(SoulItems.id("double_jump_confirmed"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Jumped> CODEC=StreamCodec.of((b,v)->{b.writeDouble(v.origin.x);b.writeDouble(v.origin.y);b.writeDouble(v.origin.z);},b->new Jumped(new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble())));
  @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void resetFallOrigin(net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.Vec3 origin) {
  player.resetFallDistance();player.currentImpulseImpactPos=origin;player.currentExplosionCause=null;player.setIgnoreFallDamageFromCurrentImpulse(true);
 }
 public static void register(RegisterPayloadHandlersEvent event){
 event.registrar("1").playToClient(Jumped.TYPE,Jumped.CODEC,(data,ctx)->ctx.enqueueWork(()->dev.betterforbiddenarcanus.client.PrismJumpClient.confirm(data.origin())));
event.registrar("1").playToServer(Request.TYPE,Request.CODEC,(data,ctx)->ctx.enqueueWork(()->{if(ctx.player() instanceof ServerPlayer p)jump(p);}));}
 public static boolean jump(ServerPlayer p){
  if(!PrismEffects.slot(p,EquipmentSlot.LEGS,PrismEffects.WIND)||p.onGround()||p.isInWaterOrBubble()||p.onClimbable()||p.getAbilities().flying||p.isFallFlying()||p.isPassenger()||!p.isAlive()||p.getPersistentData().getBoolean("bfa_double_jump_used"))return false;
  p.getPersistentData().putBoolean("bfa_double_jump_used",true);
  var v=p.getDeltaMovement();p.setDeltaMovement(v.x,.52,v.z);resetFallOrigin(p,p.position());p.hurtMarked=true;
  p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
  net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new Jumped(p.position()));
  p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),8,.2,.1,.2,.03);
  return true;
 }
}
