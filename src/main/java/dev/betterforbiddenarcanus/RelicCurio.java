package dev.betterforbiddenarcanus;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class RelicCurio implements ICurioItem {
    public static final String SLOT = "relic";

    public static List<Item> items() {
        return List.of(ModItems.DIVINE_PACT.get(), ModItems.MALEDICTUS_PACT.get(), ModItems.CRESCENT_MOON.get(),
                ModItems.ELEMENTARIUM.get(), ModItems.ARTISAN_RELIC.get(), ModItems.CRIMSON_STONE.get(), ModItems.SOUL_CRIMSON_STONE.get());
    }

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> items().forEach(item -> CuriosApi.registerCurio(item, new RelicCurio())));
    }

    public static boolean equipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler(SLOT).map(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (inventory.isSlotActive(SLOT, i) && stacks.getStackInSlot(i).is(item)) return true;
            }
            return false;
        }).orElse(false)).orElse(false);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        if (!context.identifier().equals(SLOT) || context.cosmetic() || !(context.entity() instanceof Player player)) return false;
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler(SLOT).map(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (i != context.index() && stacks.getStackInSlot(i).is(stack.getItem())) return false;
            }
            return true;
        // Curios validates saved items while rebuilding its inventory, before
        // publishing the new stacks handler. A missing handler here is that
        // initialization phase, not a failed duplicate check.
        }).orElse(true)).orElse(false);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext context, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = LinkedHashMultimap.create();
        if (!context.identifier().equals(SLOT) || context.cosmetic()) return modifiers;
        if (stack.is(ModItems.ELEMENTARIUM.get())) {
            CuriosApi.addSlotModifier(modifiers, SLOT, id, 2, AttributeModifier.Operation.ADD_VALUE);
        }
        // This modifier uses a fixed ID as well as the equip rule, so even forced
        // duplicate inventory entries cannot multiply the health penalty.
        if (stack.is(ModItems.MALEDICTUS_PACT.get())) {
            modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(SoulItems.id("cursed_health"), -0.2,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        return modifiers;
    }
}
