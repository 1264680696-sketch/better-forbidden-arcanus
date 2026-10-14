package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class ArtisanEffects {
    private static final Map<Player, Boolean> PREVIOUS = new WeakHashMap<>();
    private ArtisanEffects() {}
    public static boolean equipped(Player player) { return RelicCurio.equipped(player, ModItems.ARTISAN_RELIC.get()); }
    public static int discountedCost(int cost) { return cost / 2 + cost % 2; }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void repair(AnvilRepairEvent event) {
        if (equipped(event.getEntity())) event.setBreakChance(event.getBreakChance() * .75F);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        var player = event.getEntity(); boolean active = equipped(player);
        Boolean old = PREVIOUS.put(player, active);
        if (old == null || old != active) {
            if (player.containerMenu instanceof AnvilMenu anvil) { anvil.createResult(); anvil.broadcastChanges(); }
        }
    }
}
