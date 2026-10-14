package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.RelicCurio;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class ArmorCombatGameTests {
    private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.002, message + ": " + actual + " != " + expected);
    }

    private static float hitDragon(EnderDragon dragon, Player player, boolean body, boolean arrow) {
        dragon.setHealth(dragon.getMaxHealth()); dragon.invulnerableTime = 0;
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        var source = arrow ? dragon.level().damageSources().arrow(EntityType.ARROW.create(dragon.level()), player)
                : dragon.level().damageSources().playerAttack(player);
        if (body) dragon.hurt(source, 8);
        else dragon.head.hurt(source, 8);
        return dragon.getMaxHealth() - dragon.getHealth();
    }

    @GameTest(template = "empty")
    public static void dracoDragonPartsAndProjectiles(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var dragon = EntityType.ENDER_DRAGON.create(h.getLevel());
        var armor = List.of(ModItems.DRACO_ARCANUS_HELMET.get(), ModItems.DRACO_ARCANUS_CHESTPLATE.get(),
                ModItems.DRACO_ARCANUS_LEGGINGS.get(), ModItems.DRACO_ARCANUS_BOOTS.get());
        close(h, hitDragon(dragon, player, false, false), 8, "Unarmored dragon damage must be unchanged");
        for (int i = 0; i < 4; i++) {
            player.setItemSlot(SLOTS.get(i), new ItemStack(armor.get(i)));
            double multiplier = 1 + (i + 1) * 0.25;
            close(h, hitDragon(dragon, player, false, false), 8 * multiplier, "Each Draco piece must add 25% head damage");
            close(h, hitDragon(dragon, player, true, false), 3 * multiplier, "Body damage must retain the dragon's normal part scaling");
            close(h, hitDragon(dragon, player, false, true), 8 * multiplier, "Player-owned arrows must receive the bonus once");
        }
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(RelicCurio.SLOT, 0, new ItemStack(ModItems.MALEDICTUS_PACT.get()));
        close(h, hitDragon(dragon, player, false, false), 22.4, "Draco and cursed pact must multiply independently");
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(RelicCurio.SLOT, 0, ItemStack.EMPTY);
        for (var slot : SLOTS) player.setItemSlot(slot, ItemStack.EMPTY);
        close(h, hitDragon(dragon, player, false, false), 8, "Unequipping must immediately remove dragon damage bonus");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(armor.getFirst()));
        close(h, hitDragon(dragon, player, false, false), 8, "Holding armor must not count as wearing it");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dracoDoesNotBoostOtherTargets(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DRACO_ARCANUS_CHESTPLATE.get()));
        var cow = EntityType.COW.create(h.getLevel());
        cow.hurt(h.getLevel().damageSources().playerAttack(player), 4);
        close(h, cow.getHealth(), 6, "Draco must not boost damage to other targets");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mortemProjectileReductionAndRemoval(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var armor = List.of(ModItems.MORTEM_HELMET.get(), ModItems.MORTEM_CHESTPLATE.get(), ModItems.MORTEM_LEGGINGS.get(), ModItems.MORTEM_BOOTS.get());
        var shooter = EntityType.SKELETON.create(h.getLevel());
        var arrow = EntityType.ARROW.create(h.getLevel());
        for (int pieces = 0; pieces <= 4; pieces++) {
            if (pieces > 0) player.setItemSlot(SLOTS.get(pieces - 1), new ItemStack(armor.get(pieces - 1)));
            // Isolate the new reduction from native armour values.
            player.getAttribute(Attributes.ARMOR).removeModifiers();
            player.getAttribute(Attributes.ARMOR).setBaseValue(0);
            player.setHealth(20); player.invulnerableTime = 0;
            player.hurt(h.getLevel().damageSources().arrow(arrow, shooter), 8);
            close(h, 20 - player.getHealth(), 8 * (1 - 0.2 * pieces), "Each Mortem piece must add 20% projectile reduction");
        }
        player.setHealth(20); player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().generic(), 8);
        close(h, player.getHealth(), 12, "Non-projectile damage must remain unchanged");
        player.setHealth(20); player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().trident(EntityType.TRIDENT.create(h.getLevel()), shooter), 8);
        close(h, 20 - player.getHealth(), 1.6, "Tridents must use the projectile damage tag");
        for (var slot : SLOTS) player.setItemSlot(slot, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(armor.getFirst()));
        player.setHealth(20); player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().arrow(arrow, null), 8);
        close(h, player.getHealth(), 12, "Unequipping must remove bonus; offhand armor and ownerless arrows must retain baseline damage");
        h.succeed();
    }
}
