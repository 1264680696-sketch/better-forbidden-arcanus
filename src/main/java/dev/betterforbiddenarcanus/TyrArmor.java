package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class TyrArmor {
    private TyrArmor() {}

    public static boolean isTyr(ItemStack stack) {
        return stack.is(ModItems.TYR_HELMET.get()) || stack.is(ModItems.TYR_CHESTPLATE.get())
                || stack.is(ModItems.TYR_LEGGINGS.get()) || stack.is(ModItems.TYR_BOOTS.get());
    }

    public static int count(LivingEntity entity) {
        int count = 0;
        for (var stack : entity.getArmorSlots()) if (isTyr(stack)) count++;
        return count;
    }

    public static double swimMultiplier(Player player, int pieces) {
        if (pieces == 0) return 1;
        // Dolphins' Grace changes horizontal water drag to 0.96, rather than
        // supplying a fixed speed attribute. Match its steady-state speed,
        // including sprinting and Depth Strider, and distribute the bonus equally.
        float drag = player.isSprinting() ? 0.9F : 0.8F;
        float efficiency = (float) player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (!player.onGround()) efficiency *= 0.5F;
        drag += (0.54600006F - drag) * efficiency;
        double fullSet = player.hasEffect(MobEffects.DOLPHINS_GRACE) ? 1.25 : 1.25 * (1 - drag) / (1 - 0.96F);
        return 1 + (fullSet - 1) * pieces / 4.0;
    }

    public static void update(Player player) {
        var attribute = player.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (attribute == null) return;
        var id = SoulItems.id("tyr_swimming");
        int pieces = count(player);
        double bonus = swimMultiplier(player, pieces) - 1;
        var previous = attribute.getModifier(id);
        if (pieces == 0) {
            if (previous != null) attribute.removeModifier(id);
        } else if (previous == null || Math.abs(previous.amount() - bonus) > 0.000001) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
