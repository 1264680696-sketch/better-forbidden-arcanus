package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceManager;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.RelicCurio;
import dev.betterforbiddenarcanus.RelicEffects;
import dev.betterforbiddenarcanus.TyrArmor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.event.CuriosEventHandler;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class RelicGameTests {
    private static ICuriosItemHandler inventory(Player player) { return CuriosApi.getCuriosInventory(player).orElseThrow(); }

    private static Player player(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 1, 1))));
        return p;
    }

    private static void curiosTick(Player player) {
        for (int i = 0; i < 3; i++) {
            player.tickCount++;
            new CuriosEventHandler().tick(new EntityTickEvent.Post(player));
        }
    }

    private static void equip(Player player, int slot, Item item) {
        inventory(player).setEquippedCurio(RelicCurio.SLOT, slot, new ItemStack(item));
        curiosTick(player);
    }

    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.002, message + ": " + actual + " != " + expected);
    }

    @GameTest(template = "empty")
    public static void relicSlotsExpansionPersistenceAndOverflow(GameTestHelper h) {
        var p = player(h);
        var inv = inventory(p);
        h.assertTrue(inv.getStacksHandler("relic").orElseThrow().getSlots() == 1, "New players need one relic slot");
        var context = new SlotContext("relic", p, 0, false, true);
        for (var item : RelicCurio.items()) h.assertTrue(CuriosApi.isStackValid(context, new ItemStack(item)), "All seven enhancers must fit relic slot");
        h.assertTrue(!CuriosApi.isStackValid(context, new ItemStack(Items.STONE)), "Unrelated items must not fit relic slot");
        equip(p, 0, ModItems.ELEMENTARIUM.get());
        h.assertTrue(inv.getStacksHandler("relic").orElseThrow().getSlots() == 3, "Elementarium must add exactly two slots");
        equip(p, 1, ModItems.DIVINE_PACT.get());
        equip(p, 2, ModItems.MALEDICTUS_PACT.get());
        h.assertTrue(!new RelicCurio().canEquip(new SlotContext("relic", p, 2, false, true), new ItemStack(ModItems.DIVINE_PACT.get())), "Duplicate relics must be rejected");
        close(h, p.getMaxHealth(), 16, "Cursed pact must reduce max health by 20%");
        var restored = player(h);
        inventory(restored).readTag(inv.writeTag());
        curiosTick(restored);
        h.assertTrue(inventory(restored).getStacksHandler("relic").orElseThrow().getSlots() == 3
                && RelicCurio.equipped(restored, ModItems.DIVINE_PACT.get()), "Curios save/load must preserve slots and effects without doubling; slots="
                + inventory(restored).getStacksHandler("relic").orElseThrow().getSlots() + "; contents=" + inventory(restored).findCurios("relic"));
        inv.setEquippedCurio("relic", 0, ItemStack.EMPTY);
        curiosTick(p);
        inv.handleInvalidStacks();
        h.assertTrue(inv.getStacksHandler("relic").orElseThrow().getSlots() == 1, "Removing Elementarium must restore one slot");
        h.assertTrue(p.getInventory().countItem(ModItems.DIVINE_PACT.get()) == 1 && p.getInventory().countItem(ModItems.MALEDICTUS_PACT.get()) == 1,
                "Overflow relics must return to inventory");
        close(h, p.getMaxHealth(), 20, "Health penalty must be removed after overflow unequip");
        h.succeed();
    }

    private static Player swimmer(GameTestHelper h) {
        Player p = new Player(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), 0,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "swim-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
            @Override public boolean isInWater() { return true; }
            @Override public void move(MoverType type, Vec3 movement) { setPos(position().add(movement)); }
        };
        p.setNoGravity(true);
        return p;
    }

    private static double swim(Player p, boolean update) {
        p.setDeltaMovement(Vec3.ZERO);
        double distance = 0;
        for (int i = 0; i < 350; i++) {
            if (update) TyrArmor.update(p);
            Vec3 before = p.position();
            p.travel(new Vec3(0, 0, 1));
            distance = p.position().subtract(before).horizontalDistance();
        }
        return distance;
    }

    @GameTest(template = "empty")
    public static void tyrPiglinAndMeasuredSwimming(GameTestHelper h) {
        var p = swimmer(h);
        h.assertTrue(!PiglinAi.isWearingGold(p), "Unarmored player must retain original piglin rules");
        var slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        var armor = List.of(ModItems.TYR_HELMET.get(), ModItems.TYR_CHESTPLATE.get(), ModItems.TYR_LEGGINGS.get(), ModItems.TYR_BOOTS.get());
        double last = swim(p, true);
        for (int i = 0; i < 4; i++) {
            p.setItemSlot(slots.get(i), new ItemStack(armor.get(i)));
            h.assertTrue(PiglinAi.isWearingGold(p), "Any Tyr piece must make piglins neutral as gold does");
            double speed = swim(p, true);
            h.assertTrue(speed > last, "Every piece must increase actual swimming speed");
            last = speed;
        }
        var reference = swimmer(h);
        reference.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1200));
        close(h, last / swim(reference, false), 1.25, "Full set must reach 1.25x Dolphins' Grace actual speed");
        p.setSprinting(true);
        reference.setSprinting(true);
        p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(1);
        reference.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(1);
        close(h, swim(p, true) / swim(reference, false), 1.25, "Speed target must hold with sprinting and Depth Strider");
        p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1200));
        close(h, swim(p, true) / swim(reference, false), 1.25, "Real Dolphins' Grace must not multiply the full set twice");
        for (var slot : slots) p.setItemSlot(slot, ItemStack.EMPTY);
        TyrArmor.update(p);
        h.assertTrue(!PiglinAi.isWearingGold(p), "Removing all Tyr armor must restore piglin rules");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void relicDamageLifeStealAndBlood(GameTestHelper h) {
        var p = player(h);
        equip(p, 0, ModItems.ELEMENTARIUM.get());
        equip(p, 1, ModItems.DIVINE_PACT.get());
        var undead = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        undead.getAttribute(Attributes.ARMOR).setBaseValue(0);
        undead.hurt(h.getLevel().damageSources().playerAttack(p), 4);
        close(h, undead.getHealth(), 15, "Divine pact must add 25% undead damage");
        var cow = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        cow.hurt(h.getLevel().damageSources().playerAttack(p), 4);
        close(h, cow.getHealth(), 6, "Divine pact must not boost non-undead damage");
        equip(p, 2, ModItems.MALEDICTUS_PACT.get());
        undead.invulnerableTime = 0;
        undead.setHealth(20);
        undead.hurt(h.getLevel().damageSources().playerAttack(p), 4);
        close(h, undead.getHealth(), 13, "Independent relic damage bonuses must multiply once");
        var arrow = EntityType.ARROW.create(h.getLevel());
        cow.invulnerableTime = 0;
        cow.setHealth(10);
        cow.hurt(h.getLevel().damageSources().arrow(arrow, p), 4);
        close(h, cow.getHealth(), 4.4, "Cursed pact must boost player-attributed projectile damage");
        var vampire = player(h);
        equip(vampire, 0, ModItems.CRIMSON_STONE.get());
        vampire.setHealth(5);
        vampire.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TEST_TUBE.get()));
        var victim = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        victim.hurt(h.getLevel().damageSources().playerAttack(vampire), 4);
        close(h, vampire.getHealth(), 5.4, "Life steal must use actual damage");
        h.assertTrue(EssenceHelper.getEssenceStorage(vampire.getOffhandItem()).orElseThrow().value().amount() == 120, "Blood tube must gain base 80 plus 50%");
        var overkill = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        overkill.hurt(h.getLevel().damageSources().playerAttack(vampire), 100);
        close(h, vampire.getHealth(), 6.4, "Overkill must not give life steal beyond victim's health");
        var protectedCow = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        protectedCow.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4));
        protectedCow.hurt(h.getLevel().damageSources().playerAttack(vampire), 4);
        close(h, vampire.getHealth(), 6.4, "Prevented health damage must not heal attacker");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nearbyForgeBloodBonus(GameTestHelper h) {
        var p = player(h);
        equip(p, 0, ModItems.CRIMSON_STONE.get());
        var cow = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        var manager = new EssenceManager(HephaestusForgeLevel.FIVE.getMaxEssences(), essences -> {});
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        manager.tick(h.getLevel(), pos);
        cow.hurt(h.getLevel().damageSources().playerAttack(p), 4);
        manager.tick(h.getLevel(), pos);
        h.assertTrue(manager.getBlood() == 120, "Nearby forge must receive 50% more player-generated blood");
        cow.invulnerableTime = 0;
        cow.hurt(h.getLevel().damageSources().generic(), 2);
        manager.tick(h.getLevel(), pos);
        h.assertTrue(manager.getBlood() == 160, "Unattributed environmental damage must keep original blood amount");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void conditionalEffectsAndExternalPotions(GameTestHelper h) {
        var p = player(h);
        equip(p, 0, ModItems.DIVINE_PACT.get());
        p.setHealth(4);
        RelicEffects.afterPlayerTick(new PlayerTickEvent.Post(p));
        h.assertTrue(!p.hasEffect(MobEffects.REGENERATION), "Exactly 20% health must not trigger below-20% condition");
        p.setHealth(3);
        RelicEffects.afterPlayerTick(new PlayerTickEvent.Post(p));
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier() == 0, "Low health must grant Regeneration I");
        equip(p, 0, ModItems.CRESCENT_MOON.get());
        long day = h.getLevel().getDayTime(), time = h.getLevel().getGameTime();
        try {
            h.getLevel().setDayTime(18000);
            ((net.minecraft.world.level.storage.ServerLevelData) h.getLevel().getLevelData()).setGameTime(time - time % 20);
            var provider = EssenceHelper.getEssenceProvider(p).orElseThrow();
            provider.setAmount(EssenceType.AUREAL, 0);
            RelicEffects.afterPlayerTick(new PlayerTickEvent.Post(p));
            for (var effect : List.of(MobEffects.NIGHT_VISION, MobEffects.MOVEMENT_SPEED, MobEffects.JUMP))
                h.assertTrue(p.getEffect(effect) != null && p.getEffect(effect).getAmplifier() == 0, "Night relic effects must be level I");
            h.assertTrue(provider.getAmount(EssenceType.AUREAL) == 2, "Moon relic must add two Aureal per second");
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200));
            h.getLevel().setDayTime(6000);
            RelicEffects.afterPlayerTick(new PlayerTickEvent.Post(p));
            h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION) && p.getEffect(MobEffects.NIGHT_VISION).getDuration() == 1200,
                    "Disabling relic must preserve longer external potions");
            h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SPEED) && !p.hasEffect(MobEffects.JUMP), "Daytime must remove relic-owned effects");
        } finally { h.getLevel().setDayTime(day); ((net.minecraft.world.level.storage.ServerLevelData) h.getLevel().getLevelData()).setGameTime(time); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void artisanStatsAndTooltipScope(GameTestHelper h) {
        var p = player(h);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        p.setItemInHand(InteractionHand.MAIN_HAND, sword);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.TYR_CHESTPLATE.get()));
        p.tick(); p.tick();
        var attributes = List.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE);
        var baseline = attributes.stream().map(p::getAttributeValue).toList();
        equip(p, 0, ModItems.ARTISAN_RELIC.get());
        RelicEffects.updateAttributes(p);
        for (int i = 0; i < attributes.size(); i++) close(h, p.getAttributeValue(attributes.get(i)), baseline.get(i), "Artisan must no longer boost equipment stats");
        var breakEvent = new PlayerEvent.BreakSpeed(p, Blocks.STONE.defaultBlockState(), 10, BlockPos.ZERO);
        dev.betterforbiddenarcanus.UpgradeEffects.mining(breakEvent);
        close(h, breakEvent.getNewSpeed(), 10, "Unupgraded tool must not gain mining speed from Artisan");
        h.assertTrue(sword.getMaxDamage() == Items.DIAMOND_SWORD.getDefaultInstance().getMaxDamage(), "Artisan must not change durability");
        for (var item : RelicCurio.items()) {
            var lines = new ItemStack(item).getTooltipLines(Item.TooltipContext.of(h.getLevel()), p, TooltipFlag.NORMAL);
            int label = -1;
            for (int i = 0; i < lines.size(); i++) if (lines.get(i).getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("item.forbidden_arcanus.enhancer")) label = i;
            h.assertTrue(label >= 0 && label == lines.size()-1, "Relic tooltip must end at the enhancer label");
        }
        var armorTooltip = new ItemStack(ModItems.TYR_CHESTPLATE.get()).getTooltipLines(Item.TooltipContext.of(h.getLevel()), p, TooltipFlag.NORMAL);
        h.assertTrue(armorTooltip.stream().noneMatch(line -> line.getContents() instanceof TranslatableContents contents
                && contents.getKey().startsWith("tooltip.better_forbidden_arcanus")), "Tyr armor must not get new effect descriptions");
        inventory(p).setEquippedCurio("relic", 0, ItemStack.EMPTY);
        curiosTick(p); RelicEffects.updateAttributes(p);
        for (int i = 0; i < attributes.size(); i++) close(h, p.getAttributeValue(attributes.get(i)), baseline.get(i), "Removing artisan must restore stats");
        h.succeed();
    }
}
