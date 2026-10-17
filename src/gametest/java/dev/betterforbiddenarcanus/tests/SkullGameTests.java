package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.forge.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import com.stal111.forbidden_arcanus.common.block.skull.ObsidianSkullType;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.core.init.*;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.mixin.EssenceCapacityAccessor;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_jar_tests") @PrefixGameTestTemplate(false)
public final class SkullGameTests {
    @GameTest(template = "guide_area")
    public static void eternalSkullSmithingRecipeAndComponents(GameTestHelper h) {
        var recipe = (SmithingRecipe) h.getLevel().getRecipeManager().byKey(SoulItems.id("eternal_obsidian_skull")).orElseThrow().value();
        var base = new ItemStack(ModBlocks.OBSIDIAN_SKULL.getSkull());
        base.set(DataComponents.CUSTOM_NAME, Component.literal("护火之骨"));
        var input = new SmithingRecipeInput(new ItemStack(ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()), base, new ItemStack(ModItems.ETERNAL_STELLA.get()));
        h.assertTrue(recipe.matches(input, h.getLevel()), "Template + obsidian skull + eternal stella match");
        var result = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(ModBlocks.ETERNAL_OBSIDIAN_SKULL.getSkull().asItem()) && result.getCount() == 1, "One original eternal skull");
        h.assertTrue(result.getHoverName().getString().equals("护火之骨") && result.get(ModDataComponents.OBSIDIAN_SKULL_TYPE) == ObsidianSkullType.ETERNAL, "Name preserved and correct skull type");
        h.assertTrue(!recipe.matches(new SmithingRecipeInput(ItemStack.EMPTY, base, input.addition()), h.getLevel()), "Template required");
        h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void fullAurealConsecrationAtTierFourAndFive(GameTestHelper h) {
        var center = new BlockPos(8, 1, 8);
        for (int tier : new int[]{3, 4, 5}) {
            h.setBlock(center, Blocks.AIR); h.setBlock(center, ModBlocks.HEPHAESTUS_FORGE_TIER_4.get());
            HephaestusForgeBlockEntity forge = h.getBlockEntity(center);
            forge.getRitualManager().setup(h.getLevel(), forge.getBlockPos());
            forge.setForgeLevel(HephaestusForgeLevel.values()[tier - 1]);
            var main = new ItemStack(ModBlocks.ETERNAL_OBSIDIAN_SKULL.getSkull()); main.set(DataComponents.CUSTOM_NAME, Component.literal("被祝福的骨"));
            forge.setStack(HephaestusForgeBlockEntity.MAIN_SLOT, main);
            var manager = forge.getEssenceManager();
            int capacity = ((EssenceCapacityAccessor) manager).bfa$capacity().get(EssenceType.AUREAL);
            manager.setSouls(7); manager.setBlood(9); manager.setExperience(11); manager.setAureal(capacity - 1);
            var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "skull-test"), ClientInformation.createDefault());
            h.assertTrue(!forge.getRitualManager().startRitual(player, manager.getStorage()), "Incomplete Aureal cannot start");
            manager.setAureal(capacity);
            if (tier == 3) {
                h.assertTrue(!forge.getRitualManager().startRitual(player, manager.getStorage()), "Tier three cannot consecrate"); continue;
            }
            h.assertTrue(forge.getRitualManager().getValidRitual().orElseThrow().value().inputs().isEmpty(), "No invented pedestal ingredients");
            manager.getStorage().put(EssenceType.AUREAL, capacity - 1);
            h.assertTrue(!forge.getRitualManager().startRitual(player, manager.getStorage()), "Start rechecks stale cached full-stock condition");
            manager.setAureal(capacity);
            h.assertTrue(forge.getRitualManager().startRitual(player, manager.getStorage()), "Full stock starts at tier " + tier);
            h.assertTrue(manager.getAureal() == 0 && manager.getSouls() == 7 && manager.getBlood() == 9 && manager.getExperience() == 11, "Spends all Aureal and no other essence");
            for (int i = 0; i <= 500; i++) forge.getRitualManager().tick().ifPresent(result -> forge.setStack(HephaestusForgeBlockEntity.MAIN_SLOT, result));
            var result = forge.getStack(HephaestusForgeBlockEntity.MAIN_SLOT);
            h.assertTrue(result.is(ModBlocks.AUREALIC_OBSIDIAN_SKULL.getSkull().asItem()) && result.getCount() == 1, "One original aurealic skull produced");
            h.assertTrue(result.getHoverName().getString().equals("被祝福的骨") && result.get(ModDataComponents.OBSIDIAN_SKULL_TYPE) == ObsidianSkullType.AUREALIC, "Consecration retains name and replaces eternal type");
        }
        h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void wornSkullProtectionVisionRegenAndRemoval(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModBlocks.ETERNAL_OBSIDIAN_SKULL.getSkull()));
        p.hurt(h.getLevel().damageSources().lava(), 4);
        h.assertTrue(p.getHealth() == 20, "Eternal skull blocks fire-tag damage");
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModBlocks.AUREALIC_OBSIDIAN_SKULL.getSkull()));
        var provider = EssenceHelper.getEssenceProvider(p).orElseThrow(); provider.setLimit(EssenceType.AUREAL, 1000); provider.setAmount(EssenceType.AUREAL, 100);
        ObsidianSkullType.AUREALIC.tick(p.getItemBySlot(EquipmentSlot.HEAD), p);
        h.assertTrue(provider.getAmount(EssenceType.AUREAL) == 100, "Old burning drain is removed");
        h.runAfterDelay((int) (20 - Math.floorMod(h.getLevel().getGameTime(), 20)), () -> {
        RelicEffects.afterPlayerTick(new PlayerTickEvent.Post(p));
        h.assertTrue(provider.getAmount(EssenceType.AUREAL) == 102 && p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "Wearing grants two Aureal per second and night vision: amount=" + provider.getAmount(EssenceType.AUREAL) + " night=" + p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION) + " time=" + h.getLevel().getGameTime());
        p.hurt(h.getLevel().damageSources().onFire(), 4); h.assertTrue(p.getHealth() == 20, "Aurealic retains eternal fire protection");
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); RelicEffects.afterPlayerTick(new PlayerTickEvent.Post(p));
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "Own night vision clears when removed");
        p.hurt(h.getLevel().damageSources().magic(), 4); h.assertTrue(p.getHealth() < 20, "Non-fire damage unaffected"); h.succeed();
        });
    }
}
