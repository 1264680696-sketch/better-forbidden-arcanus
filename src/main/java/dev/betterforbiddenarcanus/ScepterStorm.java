package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.registries.*;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class ScepterStorm {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<LightningBolt>> BOLT = ENTITIES.register("scepter_lightning",
            () -> EntityType.Builder.<LightningBolt>of(SilentBolt::new, MobCategory.MISC).sized(0,0).clientTrackingRange(16).updateInterval(20).build("scepter_lightning"));
    private static final Set<Player> CHANNELS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String NEXT = "bfa_scepter_next_pulse";
    private ScepterStorm() {}
    /** A rendered lightning bolt with no world, item, thunder or fire interactions. */
    public static final class SilentBolt extends LightningBolt {
        public SilentBolt(EntityType<? extends LightningBolt> type, Level level) { super(type,level);setVisualOnly(true);setSilent(true); }
        @Override public void tick() { if (++tickCount >= 6) discard(); }
    }
    public static boolean channeling(Player player) { return CHANNELS.contains(player); }
    public static void start(Player player, boolean storm) { if (storm) CHANNELS.add(player); else CHANNELS.remove(player); }
    public static boolean enough(Player player,int amount) {
        return player.getAbilities().instabuild || EssenceHelper.getEssenceProvider(player).map(p->p.getAmount(EssenceType.AUREAL)>=amount).orElse(false);
    }
    public static boolean pay(Player player,int amount) {
        if (!enough(player,amount)) return false;
        if (!player.level().isClientSide() && !player.getAbilities().instabuild)
            EssenceHelper.getEssenceProvider(player).orElseThrow().updateAmount(EssenceType.AUREAL,value->value-amount);
        return true;
    }
    public static boolean target(Player player,LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof Player) && entity.getY()>player.getY() && player.distanceToSqr(entity)<=16
                && !player.isAlliedTo(entity) && !entity.isAlliedTo(player) && !(entity instanceof TamableAnimal pet && pet.isTame())
                && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget()==player);
    }
    public static void pulse(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        long now=TyrCombat.clock(player);
        if(now<player.getPersistentData().getLong(NEXT))return;
        if(!pay(player,5)){player.stopUsingItem();CHANNELS.remove(player);return;}
        player.getPersistentData().putLong(NEXT,now+20);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(4),target->target(player,target))) {
            var bolt=BOLT.get().create(level);bolt.setPos(entity.position());level.addFreshEntity(bolt);
            entity.hurt(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.LIGHTNING_BOLT),player,player),5);
        }
    }
    @SubscribeEvent public static void stop(LivingEntityUseItemEvent.Stop event) { if(event.getEntity() instanceof Player p)CHANNELS.remove(p); }
    @SubscribeEvent public static void finish(LivingEntityUseItemEvent.Finish event) { if(event.getEntity() instanceof Player p)CHANNELS.remove(p); }
    @SubscribeEvent public static void death(LivingDeathEvent event) { if(event.getEntity() instanceof Player p)CHANNELS.remove(p); }
}
