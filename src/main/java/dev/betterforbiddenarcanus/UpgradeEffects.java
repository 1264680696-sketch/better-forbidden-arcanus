package dev.betterforbiddenarcanus;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class UpgradeEffects {
    private UpgradeEffects() {}
    private static double value(List<ItemAttributeModifiers.Entry> entries, Holder<Attribute> type, double base) {
        var matches = entries.stream().filter(e -> e.attribute().equals(type) && e.slot().test(EquipmentSlot.MAINHAND)).toList();
        double sum = base;
        for (var e : matches) if (e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) sum += e.modifier().amount();
        // Added grade damage/speed participates in those percentage modifiers
        // naturally. Including them here would apply them a second time.
        return sum;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attributes(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack(); double bonus = ForgeUpgrades.bonus(stack);
        if (bonus == 0 || !ForgeUpgrades.supported(stack) || ForgeUpgrades.ranged(stack)) return;
        var entries = List.copyOf(event.getModifiers());
        if (stack.getItem() instanceof ArmorItem) {
            for (var e : entries) if (List.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE).contains(e.attribute())
                    && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE && e.modifier().amount() > 0)
                event.replaceModifier(e.attribute(), new AttributeModifier(e.modifier().id(), e.modifier().amount() * (1 + bonus), e.modifier().operation()), e.slot());
        } else {
            for (var type : List.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED)) {
                if (entries.stream().noneMatch(e -> e.attribute().equals(type) && e.slot().test(EquipmentSlot.MAINHAND))) continue;
                double base = type.equals(Attributes.ATTACK_SPEED) ? 4 : 1;
                double extra = Math.max(0, value(entries, type, base)) * bonus;
                var canonical = base == 4 ? Item.BASE_ATTACK_SPEED_ID : Item.BASE_ATTACK_DAMAGE_ID;
                // Merge main-hand additive values into the native base modifier.
                // This keeps real stats and the green tooltip total identical,
                // while percentage modifiers still apply once in the normal order.
                var additive = entries.stream().filter(e -> e.attribute().equals(type)
                        && e.slot() == net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND
                        && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE).toList();
                double amount = additive.stream().mapToDouble(e -> e.modifier().amount()).sum() + extra;
                additive.forEach(e -> event.removeModifier(type, e.modifier().id()));
                event.addModifier(type, new AttributeModifier(canonical, amount, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
            }
        }
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed event) {
        var stack = event.getEntity().getMainHandItem();
        if (stack.has(net.minecraft.core.component.DataComponents.TOOL))
            event.setNewSpeed((float)(event.getNewSpeed() * ForgeUpgrades.multiplier(stack)));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack(); int tier = ForgeUpgrades.tier(stack);
        if (tier == 0 || dev.betterforbiddenarcanus.PrismEffects.axe(stack) && dev.betterforbiddenarcanus.PrismEffects.bits(stack) == 15) return;
        if (!event.getToolTip().isEmpty()) event.getToolTip().set(0, event.getToolTip().getFirst().copy().withStyle(ForgeUpgrades.color(stack)));
    }
}
