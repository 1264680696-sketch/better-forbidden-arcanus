package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.item.ModTiers;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** Restore the tool-specific offsets from upstream 1.20.x with the current tier. */
final class DracoToolComponents {
    private DracoToolComponents() {}
    static void modifyDefaults(ModifyDefaultComponentsEvent event) {
        var tier = ModTiers.DRACO_ARCANUS;
        set(event, ModItems.DRACO_ARCANUS_SWORD.get(), SwordItem.createAttributes(tier, 4, -2.2F));
        set(event, ModItems.DRACO_ARCANUS_PICKAXE.get(), DiggerItem.createAttributes(tier, 2, -2.6F));
        set(event, ModItems.DRACO_ARCANUS_AXE.get(), DiggerItem.createAttributes(tier, 6, -2.8F));
        set(event, ModItems.DRACO_ARCANUS_SHOVEL.get(), DiggerItem.createAttributes(tier, 2.5F, -2.8F));
        set(event, ModItems.DRACO_ARCANUS_HOE.get(), DiggerItem.createAttributes(tier, -4, 1));
        // Scepter has no native melee bonus. Show its real 1 damage / 4 speed,
        // without changing energy-ball damage or treating the staff material as a weapon.
        set(event, ModItems.DRACO_ARCANUS_SCEPTER.get(), ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 0,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build());
    }
    private static void set(ModifyDefaultComponentsEvent event, Item item, ItemAttributeModifiers modifiers) {
        event.modify(item, patch -> patch.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers));
    }
}
