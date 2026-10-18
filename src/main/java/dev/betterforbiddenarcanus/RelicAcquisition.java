package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Add to the apprentice cleric's pool; existing saved offers follow vanilla rules. */
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class RelicAcquisition {
    private RelicAcquisition() {}

    @SubscribeEvent
    public static void trades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.CLERIC) {
            event.getTrades().get(2).add(new BasicItemListing(
                    16, new ItemStack(ModItems.DIVINE_PACT.get()), 4, 10, .05F));
        }
    }
}
