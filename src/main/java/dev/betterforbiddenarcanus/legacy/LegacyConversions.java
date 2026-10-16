package dev.betterforbiddenarcanus.legacy;
import dev.betterforbiddenarcanus.BetterForbiddenArcanus;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class LegacyConversions {
    private LegacyConversions() {}
    public static Mob convert(Mob target) {
        if (!(target.level() instanceof ServerLevel level)) return null;
        EntityType<? extends Mob> type = target.getType() == EntityType.HORSE ? EntityType.ZOMBIE_HORSE : target instanceof Villager ? EntityType.ZOMBIE_VILLAGER : null;
        if (type == null || !EventHooks.canLivingConvert(target, type, timer -> {})) return null;
        Mob result = target.convertTo(type, false);
        if (result == null) return null;
        result.finalizeSpawn(level, level.getCurrentDifficultyAt(result.blockPosition()), MobSpawnType.CONVERSION,
                result instanceof ZombieVillager ? new Zombie.ZombieGroupData(false, true) : null);
        if (target instanceof Villager villager && result instanceof ZombieVillager zombie) {
            zombie.setVillagerData(villager.getVillagerData());
            zombie.setGossips(villager.getGossips().store(NbtOps.INSTANCE));
            zombie.setTradeOffers(villager.getOffers().copy()); zombie.setVillagerXp(villager.getVillagerXp());
        }
        EventHooks.onLivingConvert(target, result);
        return result;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player
                || !(player.getMainHandItem().getItem() instanceof ZombieArmItem) || !(event.getEntity() instanceof Mob mob)) return;
        // Convert before normal death loot; one victim becomes exactly one replacement.
        if (convert(mob) != null) event.setCanceled(true);
    }
}
