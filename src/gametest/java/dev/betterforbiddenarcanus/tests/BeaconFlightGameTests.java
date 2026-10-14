package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import dev.betterforbiddenarcanus.BeaconFlight;
import dev.betterforbiddenarcanus.SoulItems;
import dev.betterforbiddenarcanus.mixin.BeaconAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class BeaconFlightGameTests {
    private static void base(GameTestHelper h, BlockPos pos, int tier) {
        for (int depth = 1; depth <= 4; depth++) for (int x = -depth; x <= depth; x++) for (int z = -depth; z <= depth; z++)
            h.getLevel().setBlockAndUpdate(pos.offset(x, -depth, z), (depth <= tier ? Blocks.IRON_BLOCK : Blocks.AIR).defaultBlockState());
    }

    private static void checkRange(GameTestHelper h, Player player, BlockPos pos, int tier) {
        var beacon = (BeaconBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(((BeaconAccessor)beacon).bfa$levels() == tier && !beacon.getBeamSections().isEmpty(), "Real beacon must be active at tier " + tier);
        int radius = tier * 10 + 10;
        player.setPos(pos.getX() + radius + 0.5, pos.getY() + 10, pos.getZ() + 0.5);
        BeaconFlight.update(player);
        h.assertTrue(player.mayFly(), "Player inside vanilla tier " + tier + " horizontal range must be able to fly");
        player.getAbilities().flying = true;
        player.setPos(pos.getX() + radius + 2, pos.getY() + 10, pos.getZ() + 0.5);
        BeaconFlight.update(player);
        h.assertTrue(!player.mayFly() && !player.getAbilities().flying, "Leaving tier " + tier + " range must revoke flight immediately");
        player.setPos(pos.getX() + 0.5, pos.getY() + h.getLevel().getHeight() - 1, pos.getZ() + 0.5);
        BeaconFlight.update(player);
        h.assertTrue(player.mayFly(), "Vertical coverage must follow vanilla's upward-expanded range");
        player.setPos(pos.getX() + 0.5, pos.getY() - radius - 2, pos.getZ() + 0.5);
        BeaconFlight.update(player);
        h.assertTrue(!player.mayFly(), "Player below vanilla vertical range must lose flight");
    }

    @GameTest(template = "guide_area", timeoutTicks = 1250)
    public static void beaconFlightTiersLifecycleAndOtherPermissions(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(8, 4, 8));
        base(h, pos, 4);
        level.setBlockAndUpdate(pos, Blocks.BEACON.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState());
        var beacon = (BeaconBlockEntity)level.getBlockEntity(pos);
        var data = new CompoundTag(); data.putString("primary_effect", "minecraft:speed");
        beacon.loadWithComponents(data, level.registryAccess());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.runAtTickTime(180, () -> { checkRange(h, player, pos, 4); base(h, pos, 3); });
        h.runAtTickTime(360, () -> { checkRange(h, player, pos, 3); base(h, pos, 2); });
        h.runAtTickTime(540, () -> { checkRange(h, player, pos, 2); base(h, pos, 1); });
        h.runAtTickTime(720, () -> {
            checkRange(h, player, pos, 1);
            player.setPos(Vec3.atCenterOf(pos.above(2))); BeaconFlight.update(player);
            h.assertTrue(player.mayFly(), "Valid structure must grant flight after returning");
            var saved = player.saveWithoutId(new CompoundTag());
            var restored = h.makeMockPlayer(GameType.SURVIVAL); restored.load(saved);
            BeaconFlight.update(restored);
            h.assertTrue(restored.mayFly(), "Reloaded player in range must regain flight");
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            restored.getAbilities().flying = true; BeaconFlight.update(restored);
            h.assertTrue(!restored.mayFly() && !restored.getAbilities().flying, "Egg removal must revoke reloaded player's flight immediately");
            level.setBlockAndUpdate(pos.above(), ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState());
        });
        h.runAtTickTime(740, () -> {
            player.setPos(Vec3.atCenterOf(pos.above(2))); BeaconFlight.update(player);
            var other = SoulItems.id("test_other_flight");
            player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addTransientModifier(new AttributeModifier(other, 1, AttributeModifier.Operation.ADD_VALUE));
            player.getAbilities().flying = true;
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState()); BeaconFlight.update(player);
            h.assertTrue(player.mayFly() && player.getAbilities().flying && player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).getModifier(BeaconFlight.MODIFIER) == null,
                    "Revoking beacon modifier must preserve other mods' flight");
            player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).removeModifier(other);
            player.getAbilities().mayfly = true; BeaconFlight.update(player);
            h.assertTrue(player.getAbilities().flying, "Creative/spectator flight permission must be preserved");
            player.getAbilities().mayfly = false;
            level.setBlockAndUpdate(pos.above(), ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState());
            level.setBlockAndUpdate(pos.above(3), Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(920, () -> {
            BeaconFlight.update(player);
            h.assertTrue(!player.mayFly() && !player.getAbilities().flying, "Blocked real beacon beam must stop flight");
            level.setBlockAndUpdate(pos.above(3), Blocks.AIR.defaultBlockState());
            var second = pos.offset(4, 0, 0);
            base(h, second, 1);
            level.setBlockAndUpdate(second, Blocks.BEACON.defaultBlockState());
            level.setBlockAndUpdate(second.above(), ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState());
            ((BeaconBlockEntity)level.getBlockEntity(second)).loadWithComponents(data, level.registryAccess());
        });
        h.runAtTickTime(1100, () -> {
            player.setPos(Vec3.atCenterOf(pos.above(2))); BeaconFlight.update(player);
            h.assertTrue(player.mayFly(), "Restored beam must reactivate flight");
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState()); BeaconFlight.update(player);
            h.assertTrue(player.mayFly(), "Overlapping second beacon must preserve flight when first egg is removed");
            level.setBlockAndUpdate(pos.offset(4, 1, 0), Blocks.AIR.defaultBlockState()); BeaconFlight.update(player);
            h.assertTrue(!player.mayFly(), "Removing both overlapping sources must revoke flight");
            level.setBlockAndUpdate(pos.above(), ModBlocks.ARCANE_DRAGON_EGG.get().defaultBlockState());
        });
        h.runAtTickTime(1140, () -> {
            BeaconFlight.update(player);
            h.assertTrue(player.mayFly(), "Native beacon tick must register restored source again");
            beacon.loadWithComponents(new CompoundTag(), level.registryAccess()); BeaconFlight.update(player);
            h.assertTrue(!player.mayFly(), "A beacon without a selected primary effect must not grant flight");
            beacon.loadWithComponents(data, level.registryAccess());
            var otherLevel = level.getServer().getLevel(Level.NETHER);
            var traveler = new Player(otherLevel, pos, 0, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "flight-dimension")) {
                @Override public boolean isCreative() { return false; }
                @Override public boolean isSpectator() { return false; }
            };
            traveler.setPos(Vec3.atCenterOf(pos.above(2)));
            traveler.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addTransientModifier(new AttributeModifier(BeaconFlight.MODIFIER, 1, AttributeModifier.Operation.ADD_VALUE));
            traveler.getAbilities().flying = true; BeaconFlight.update(traveler);
            h.assertTrue(!traveler.mayFly() && !traveler.getAbilities().flying, "Changing dimension must not retain old source's flight at identical coordinates");
        });
        h.runAtTickTime(1180, () -> {
            BeaconFlight.update(player);
            h.assertTrue(player.mayFly(), "Restored primary power must resume flight");
            BeaconFlight.unload(new net.neoforged.neoforge.event.level.LevelEvent.Unload(level)); BeaconFlight.update(player);
            h.assertTrue(!player.mayFly(), "Level unload must clear cached sources");
            BeaconFlight.track(level, pos, beacon); BeaconFlight.update(player);
            h.assertTrue(player.mayFly(), "Loaded native beacon can re-register after a cache clear");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); BeaconFlight.update(player);
            h.assertTrue(!player.mayFly(), "Removed beacon must cease being a flight source immediately");
            h.succeed();
        });
    }
}
