package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.EssenceJarInteractions;
import dev.betterforbiddenarcanus.JarRelicStorage;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_jar_tests")
@PrefixGameTestTemplate(false)
public final class JarExtractionGameTests {
    private static final BlockPos POS = new BlockPos(8, 1, 8);
    private static EssenceUtremJarBlockEntity jar(GameTestHelper h, EssenceType type, int amount) {
        h.setBlock(POS, Blocks.AIR);
        h.setBlock(POS, ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE, type));
        EssenceUtremJarBlockEntity jar = h.getBlockEntity(POS); jar.addEssence(amount); return jar;
    }
    private static Player player(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
        p.setNoGravity(true);
        var at = h.absolutePos(POS);
        p.setPos(at.getX() + .5, at.getY(), at.getZ() - 2);
        var aim = Vec3.atCenterOf(at).subtract(p.getEyePosition());
        p.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
        p.setXRot((float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
        return p;
    }
    private static net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock click(GameTestHelper h, Player p, InteractionHand hand) {
        var pos = h.absolutePos(POS);
        return CommonHooks.onRightClickBlock(p, hand, pos, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }
    private static int blood(ItemStack stack) { return EssenceHelper.getEssenceStorage(stack).map(s -> s.value().amount()).orElse(0); }
    private static int souls(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(POS)).inflate(2), e -> e.getItem().is(ModItems.SOUL.get()))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }
    private static ItemStack charge(GameTestHelper h, Player p, InteractionHand hand) {
        var extractor = new ItemStack(ModItems.SOUL_EXTRACTOR.get()); p.setItemInHand(hand, extractor);
        var pos = h.absolutePos(POS);
        var result = extractor.useOn(new UseOnContext(p, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
        h.assertTrue(result.consumesAction() && p.isUsingItem(), "Native extractor begins charging on a stocked soul jar");
        h.assertTrue(extractor.getUseDuration(p) == 35, "Native thirty-five tick duration retained");
        return extractor;
    }
    @GameTest(template = "guide_area")
    public static void bloodStockCapacityAndPartialTube(GameTestHelper h) {
        var jar = jar(h, EssenceType.BLOOD, 3500); var p = player(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TEST_TUBE.get(), 2)); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 500 && p.getMainHandItem().getCount() == 1, "Only one empty tube consumed");
        h.assertTrue(p.getInventory().items.stream().anyMatch(s -> s.is(ModItems.BLOOD_TEST_TUBE.get()) && blood(s) == 3000), "Tube fills to native three-thousand capacity: " + p.getInventory().items);
        click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 0 && blood(p.getMainHandItem()) == 500, "Remainder fills a partially stocked tube");
        jar.addEssence(900); p.getMainHandItem().set(DataComponents.CUSTOM_NAME, Component.literal("采血样本"));
        click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 0 && blood(p.getMainHandItem()) == 1400 && p.getMainHandItem().getHoverName().getString().equals("采血样本"), "Refill preserves existing blood and custom components");
        jar.addEssence(4000); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(blood(p.getMainHandItem()) == 3000 && jar.getAmount() == 2400, "Refill stops exactly at capacity");
        click(h, p, InteractionHand.MAIN_HAND); h.assertTrue(jar.getAmount() == 2400 && blood(p.getMainHandItem()) == 3000, "Full tube neither extracts nor accidentally deposits");
        p.setShiftKeyDown(true); var event = click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(!event.isCanceled(), "Sneak with filled tube still permits native deposit");
        var at = h.absolutePos(POS);
        jar.getBlockState().useItemOn(p.getMainHandItem(), h.getLevel(), p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
        h.assertTrue(jar.getAmount() == 5400, "Native deposit returns stored blood one-to-one"); h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void experienceCostsAndEmptyStock(GameTestHelper h) {
        var jar = jar(h, EssenceType.EXPERIENCE, 30); var p = player(h);
        h.assertTrue(EssenceJarInteractions.experienceAmount() == 15, "Native experience bottle stores fifteen essence");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE, 2)); click(h, p, InteractionHand.OFF_HAND);
        h.assertTrue(jar.getAmount() == 15 && p.getOffhandItem().getCount() == 1 && p.getOffhandItem().is(Items.GLASS_BOTTLE), "Offhand bottle stack splits once");
        h.assertTrue(p.getInventory().items.stream().anyMatch(s -> s.is(Items.EXPERIENCE_BOTTLE)), "Experience bottle reaches inventory");
        click(h, p, InteractionHand.OFF_HAND);
        h.assertTrue(jar.getAmount() == 0 && p.getOffhandItem().is(Items.EXPERIENCE_BOTTLE), "Last glass bottle becomes native experience bottle");
        jar.addEssence(14); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE)); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 14 && p.getMainHandItem().is(Items.GLASS_BOTTLE), "Insufficient stock does not consume or overdraw");
        jar.addEssence(-14); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 0 && p.getMainHandItem().is(Items.GLASS_BOTTLE), "Empty jar cannot create bottles"); h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void fullInventoryAndWrongContents(GameTestHelper h) {
        var jar = jar(h, EssenceType.EXPERIENCE, 15); var p = player(h);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2)); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 0 && p.getMainHandItem().getCount() == 1, "Full inventory consumes exactly one bottle");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(3), e -> e.getItem().is(Items.EXPERIENCE_BOTTLE)).size() == 1, "Full inventory drops exactly one result");
        jar = jar(h, EssenceType.SOULS, 2); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        h.assertTrue(!click(h, p, InteractionHand.MAIN_HAND).isCanceled() && jar.getAmount() == 2, "Glass cannot extract soul essence");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TEST_TUBE.get()));
        h.assertTrue(!click(h, p, InteractionHand.MAIN_HAND).isCanceled() && jar.getAmount() == 2, "Tube cannot extract other essences");
        jar = jar(h, EssenceType.BLOOD, 0); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 0 && p.getMainHandItem().is(ModItems.TEST_TUBE.get()), "Empty blood jar does not consume tube"); h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void nativeSoulChargeWearAndJarRetention(GameTestHelper h) {
        var jar = jar(h, EssenceType.SOULS, 2); var p = player(h); var stack = charge(h, p, InteractionHand.MAIN_HAND);
        for (int i = 0; i < 34; i++) p.tick();
        h.assertTrue(jar.getAmount() == 2 && souls(h) == 0 && stack.getDamageValue() == 0, "No premature deduction/drop/wear");
        p.tick();
        h.assertTrue(jar.getAmount() == 1 && souls(h) == 1 && stack.getDamageValue() == 1, "Native completion deducts one essence, drops one soul and wears extractor: jar=" + jar.getAmount() + " souls=" + souls(h) + " wear=" + stack.getDamageValue() + " using=" + p.isUsingItem());
        h.assertTrue(h.getBlockState(POS).is(ModBlocks.ESSENCE_UTREM_JAR.get()), "Jar is never replaced with soulless sand");
        stack = charge(h, p, InteractionHand.OFF_HAND); for (int i = 0; i < 35; i++) p.tick();
        h.assertTrue(jar.getAmount() == 0 && souls(h) == 2 && stack.getDamageValue() == 1, "Offhand completion also withdraws exactly one soul");
        stack.finishUsingItem(h.getLevel(), p);
        h.assertTrue(jar.getAmount() == 0 && souls(h) == 2 && stack.getDamageValue() == 1, "Repeated completion on empty stock is harmless"); h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void soulCancellationAndConcurrentDrain(GameTestHelper h) {
        var jar = jar(h, EssenceType.SOULS, 1); var p = player(h); var stack = charge(h, p, InteractionHand.MAIN_HAND);
        for (int i = 0; i < 20; i++) p.tick(); p.releaseUsingItem();
        h.assertTrue(jar.getAmount() == 1 && souls(h) == 0 && stack.getDamageValue() == 0, "Releasing early costs nothing");
        stack = charge(h, p, InteractionHand.MAIN_HAND); for (int i = 0; i < 34; i++) p.tick();
        jar.addEssence(-1); p.tick();
        h.assertTrue(jar.getAmount() == 0 && souls(h) == 0 && stack.getDamageValue() == 0, "Another consumer emptying the jar prevents completion");
        jar.addEssence(1); stack = charge(h, p, InteractionHand.MAIN_HAND); for (int i = 0; i < 34; i++) p.tick();
        h.setBlock(POS, Blocks.STONE); p.tick();
        h.assertTrue(h.getBlockState(POS).is(Blocks.STONE) && souls(h) == 0 && stack.getDamageValue() == 0, "Removed target is neither replaced nor paid out"); h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void mountedBloodRelicAndExistingAureal(GameTestHelper h) {
        var jar = jar(h, EssenceType.BLOOD, 400); var p = player(h);
        ((JarRelicStorage) jar).bfa$relic(new ItemStack(ModItems.CRIMSON_STONE.get()));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TEST_TUBE.get())); click(h, p, InteractionHand.OFF_HAND);
        h.assertTrue(blood(p.getOffhandItem()) == 400 && jar.getAmount() == 0 && ((JarRelicStorage) jar).bfa$relic().is(ModItems.CRIMSON_STONE.get()), "Withdrawal preserves mounted relic and one-to-one amount: " + p.getOffhandItem() + " stored=" + blood(p.getOffhandItem()) + " jar=" + jar.getAmount());
        jar = jar(h, EssenceType.AUREAL, 35); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE)); click(h, p, InteractionHand.MAIN_HAND);
        h.assertTrue(jar.getAmount() == 0 && p.getMainHandItem().is(ModItems.AUREAL_BOTTLE.get()), "Existing Aureal bottle route still works through shared handler"); h.succeed();
    }
}
