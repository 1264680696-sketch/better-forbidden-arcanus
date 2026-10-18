package dev.betterforbiddenarcanus;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
public final class RunicAxeProjectile extends ThrowableItemProjectile {
 public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>,EntityType<RunicAxeProjectile>> TYPE=ScepterStorm.ENTITIES.register("runic_axe",()->EntityType.Builder.<RunicAxeProjectile>of(RunicAxeProjectile::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build("runic_axe"));
 private boolean returning;private int flight;private float damage;private java.util.UUID returnOwner;
 public static void init(){}
 public RunicAxeProjectile(EntityType<? extends RunicAxeProjectile> type,Level level){super(type,level);}
 public RunicAxeProjectile(Level level,Player owner,ItemStack stack,float power){super(TYPE.get(),owner,level);setItem(stack);returnOwner=owner.getUUID();damage=(float)(owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*power);}
 @Override protected Item getDefaultItem(){return LegacyContent.AXE.get();}
 @Override protected boolean canHitEntity(Entity target){return !returning && super.canHitEntity(target);}
 @Override protected void onHitEntity(EntityHitResult hit){
  if(returning||level().isClientSide())return;returning=true;
  if(hit.getEntity() instanceof LivingEntity target&&getOwner() instanceof Player owner&&target!=owner&&!owner.isAlliedTo(target)&&!(target instanceof TamableAnimal pet&&pet.isTame())&&(!(target instanceof Player p)||owner.canHarmPlayer(p))){
   var source=damageSources().thrown(this,owner);
   float hitDamage=damage;
   if(level() instanceof net.minecraft.server.level.ServerLevel server)hitDamage=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(server,getItem(),target,source,hitDamage);
   if(target.hurt(source,hitDamage)&&level() instanceof net.minecraft.server.level.ServerLevel server)net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(server,target,source,getItem());
  }
 }
 @Override protected void onHitBlock(BlockHitResult hit){returning=true;}
 @Override public void tick(){
  flight++;
  if(!level().isClientSide()){
   if(flight>=30)returning=true;
   Entity owner=returnOwner==null?getOwner():level().getPlayerByUUID(returnOwner);
   if(owner==null&&getOwner()!=null&&(returnOwner==null||returnOwner.equals(getOwner().getUUID())))owner=getOwner();
   if(owner==null||!owner.isAlive()||owner.isRemoved()){
    if(flight>=40){spawnAtLocation(getItem());discard();return;}
   }else if(returning){
    setNoGravity(true);noPhysics=true;
    var delta=owner.getEyePosition().subtract(position());
    if(delta.lengthSqr()<2.25&&owner instanceof Player p){
     var stack=getItem().copy();if(!p.getInventory().add(stack))p.drop(stack,false);discard();return;
    }
    setDeltaMovement(delta.normalize().scale(1.5));
   }
   if(getY()<level().getMinBuildHeight()-8&&owner!=null){setPos(owner.getEyePosition());returning=true;}
  }
  super.tick();
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Returning",returning);tag.putInt("Flight",flight);tag.putFloat("AxeDamage",damage);if(returnOwner!=null)tag.putUUID("ReturnOwner",returnOwner);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);returning=tag.getBoolean("Returning");flight=tag.getInt("Flight");damage=tag.getFloat("AxeDamage");returnOwner=tag.hasUUID("ReturnOwner")?tag.getUUID("ReturnOwner"):null;}
}
