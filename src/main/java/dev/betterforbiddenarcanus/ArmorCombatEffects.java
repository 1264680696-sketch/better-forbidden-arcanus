package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class ArmorCombatEffects {
    private ArmorCombatEffects() {}

    private static int count(Player player, Item head, Item chest, Item legs, Item feet) {
        return (player.getItemBySlot(EquipmentSlot.HEAD).is(head) ? 1 : 0)
                + (player.getItemBySlot(EquipmentSlot.CHEST).is(chest) ? 1 : 0)
                + (player.getItemBySlot(EquipmentSlot.LEGS).is(legs) ? 1 : 0)
                + (player.getItemBySlot(EquipmentSlot.FEET).is(feet) ? 1 : 0);
    }

    @SubscribeEvent
    public static void dragonDamage(LivingIncomingDamageEvent event) {
        // Dragon parts forward their damage to the parent living entity.
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof EnderDragon)
                || !(event.getSource().getEntity() instanceof Player player)) return;
        int pieces = count(player, ModItems.DRACO_ARCANUS_HELMET.get(), ModItems.DRACO_ARCANUS_CHESTPLATE.get(),
                ModItems.DRACO_ARCANUS_LEGGINGS.get(), ModItems.DRACO_ARCANUS_BOOTS.get());
        pieces += TyrArmor.count(player);
        float weapon = DracoCombat.weaponBonus(event.getSource(), player);
        if (pieces > 0 || weapon > 0) event.setAmount(event.getAmount() * (1 + weapon + 0.25F * pieces));
    }

    @SubscribeEvent
    public static void projectileProtection(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player player)
                || !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) return;
        int pieces = count(player, ModItems.MORTEM_HELMET.get(), ModItems.MORTEM_CHESTPLATE.get(),
                ModItems.MORTEM_LEGGINGS.get(), ModItems.MORTEM_BOOTS.get());
        // Apply to damage after normal armour/enchantment reductions, so the
        // additional reduction remains proportional to otherwise received damage.
        if (pieces > 0) event.setNewDamage(event.getNewDamage() * (1 - 0.125F * pieces));
    }
}
