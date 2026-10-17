package dev.betterforbiddenarcanus.tests;
import com.stal111.forbidden_arcanus.common.block.UtremJarBlock;
import com.stal111.forbidden_arcanus.common.block.entity.*;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.common.inventory.clibano.ClibanoMenu;
import com.stal111.forbidden_arcanus.core.init.*;
import dev.betterforbiddenarcanus.*;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_tests") @PrefixGameTestTemplate(false)
public final class MagicAndJarGameTests {
    private static final BlockPos POS=new BlockPos(8,1,8);
    private static Player player(GameTestHelper h) {var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());p.setPos(Vec3.atCenterOf(h.absolutePos(POS)));return p;}
    private static void click(GameTestHelper h,Player p,BlockPos pos) {var absolute=h.absolutePos(pos);CommonHooks.onRightClickBlock(p,InteractionHand.MAIN_HAND,absolute,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false));}
    private static CraftingRecipe recipe(GameTestHelper h,String id){return (CraftingRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus",id)).orElseThrow().value();}
    @GameTest(template="empty") public static void wandAndEyeRecipeReplacements(GameTestHelper h) {
        var wand=recipe(h,"magic_wand");var grid=NonNullList.withSize(9,ItemStack.EMPTY);
        grid.set(2,new ItemStack(ModItems.EDELWOOD_STICK.get()));grid.set(4,new ItemStack(ModItems.DEORUM_INGOT.get()));grid.set(6,new ItemStack(ModItems.ARCANE_CRYSTAL.get()));
        h.assertTrue(!wand.matches(CraftingInput.of(3,3,grid),h.getLevel()),"Old wand recipe no longer matches");
        grid.set(5,new ItemStack(ModItems.STELLARITE_PIECE.get()));var input=CraftingInput.of(3,3,grid);
        h.assertTrue(wand.matches(input,h.getLevel())&&wand.assemble(input,h.getLevel().registryAccess()).is(ModItems.MAGIC_WAND.get()),"Meteor fragment directly below Edelwood stick");
        grid.set(5,ItemStack.EMPTY);grid.set(8,new ItemStack(ModItems.STELLARITE_PIECE.get()));h.assertTrue(!wand.matches(CraftingInput.of(3,3,grid),h.getLevel()),"Wrong fragment cell rejected");
        var eye=recipe(h,"spectral_eye_amulet");grid=NonNullList.withSize(9,ItemStack.EMPTY);
        for(int i:new int[]{1,3,5})grid.set(i,new ItemStack(Items.STRING));for(int i:new int[]{6,8})grid.set(i,new ItemStack(ModItems.DEORUM_INGOT.get()));grid.set(7,new ItemStack(Items.ENDER_EYE));
        for(var item:List.of(Items.NETHER_STAR,ModItems.CORRUPT_SOUL.get(),ModItems.ENCHANTED_SOUL.get())) {
            grid.set(4,new ItemStack(item));h.assertTrue(eye.matches(CraftingInput.of(3,3,grid),h.getLevel())==(item==ModItems.ENCHANTED_SOUL.get()),"Only enchanted soul replaces nether star");
        }
        h.succeed();
    }
    @GameTest(template="guide_area") public static void meteorSphereAndOrdinaryWandPreserved(GameTestHelper h) {
        var center=h.absolutePos(POS);var near=POS.offset(2,1,0);var boundary=POS.above(64);var outside=POS.above(65);
        var side=POS.east(64);var diagonal=POS.offset(46,0,46);
        for(var pos:List.of(near,boundary,outside,side,diagonal))h.setBlock(pos,ModBlocks.STELLA_ARCANUM.get());
        h.setBlock(POS.above(),Blocks.STONE);
        var found=MeteorScan.find(h.getLevel(),Vec3.atCenterOf(center));
        h.assertTrue(found.contains(h.absolutePos(near))&&found.contains(h.absolutePos(boundary))&&!found.contains(h.absolutePos(outside)),"Hidden ore and exact sixty-four-block sphere boundary found, outside rejected");
        h.assertTrue(found.contains(h.absolutePos(side))&&!found.contains(h.absolutePos(diagonal)),"Horizontal sixty-four-block boundary included; diagonal outside sphere rejected");
        h.setBlock(near,Blocks.AIR);h.assertTrue(!MeteorScan.find(h.getLevel(),Vec3.atCenterOf(center)).contains(h.absolutePos(near)),"Destroyed ore removed on new scan");
        var p=player(h);var stack=new ItemStack(ModItems.MAGIC_WAND.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var provider=EssenceHelper.getEssenceProvider(p).orElseThrow();provider.setAmount(EssenceType.AUREAL,500);
        stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.isUsingItem(),"Ordinary wand still charges");
        stack.getItem().releaseUsing(stack,h.getLevel(),p,71970);p.stopUsingItem();
        h.assertTrue(!h.getLevel().getEntitiesOfClass(com.stal111.forbidden_arcanus.common.entity.projectile.AurealMissile.class,p.getBoundingBox().inflate(3)).isEmpty(),"Ordinary projectile unchanged");
        int before=provider.getAmount(EssenceType.AUREAL);p.setShiftKeyDown(true);stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(!p.isUsingItem()&&provider.getAmount(EssenceType.AUREAL)==before,"Sneak scan does not charge a projectile or consume Aureal");
        for(var pos:List.of(near,boundary,outside,side,diagonal))h.setBlock(pos,Blocks.AIR);h.succeed();
    }
    @GameTest(template="guide_area") public static void bottleExtractionStockAndStackHandling(GameTestHelper h) {
        var p=player(h);h.setBlock(POS,ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,EssenceType.AUREAL));
        EssenceUtremJarBlockEntity jar=h.getBlockEntity(POS);h.assertTrue(EssenceJarInteractions.aurealAmount()==35,"Native bottle contains thirty-five Aureal");
        jar.addEssence(70);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));click(h,p,POS);
        h.assertTrue(jar.getAmount()==35&&p.getMainHandItem().is(Items.GLASS_BOTTLE)&&p.getMainHandItem().getCount()==1,"One glass bottle and thirty-five essence consumed");
        h.assertTrue(p.getInventory().items.stream().anyMatch(s->s.is(ModItems.AUREAL_BOTTLE.get())&&s.getCount()==1),"Filled bottle delivered to inventory");
        click(h,p,POS);h.assertTrue(jar.getAmount()==0&&p.getMainHandItem().is(ModItems.AUREAL_BOTTLE.get()),"Last glass becomes filled bottle in hand");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));jar.addEssence(34);click(h,p,POS);
        h.assertTrue(jar.getAmount()==34&&p.getMainHandItem().is(Items.GLASS_BOTTLE),"Insufficient stock neither consumed nor made negative");h.succeed();
    }
    @GameTest(template="guide_area") public static void jarRelicsProductionReloadCapacityRetrievalAndDrop(GameTestHelper h) {
        var p=player(h);
        for(var relic:List.of(ModItems.CRESCENT_MOON.get(),ModItems.CRIMSON_STONE.get())) {
            h.setBlock(POS,Blocks.AIR);h.setBlock(POS,ModBlocks.UTREM_JAR.get());p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(relic));click(h,p,POS);
            EssenceUtremJarBlockEntity jar=h.getBlockEntity(POS);var storage=(JarRelicStorage)jar;
            h.assertTrue(storage.bfa$relic().is(relic)&&p.getMainHandItem().isEmpty()&&jar.getBlockState().getValue(ModBlockStateProperties.ESSENCE_TYPE)==RelicJars.type(new ItemStack(relic)),"Relic mounted and empty jar converted to its own type");
            for(int i=0;i<99;i++)RelicJars.tick(jar);h.assertTrue(jar.getAmount()==0,"No early generation");
            var saved=jar.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new EssenceUtremJarBlockEntity(jar.getBlockPos(),jar.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);jar=restored;storage=(JarRelicStorage)jar;
            RelicJars.tick(jar);h.assertTrue(jar.getAmount()==3&&storage.bfa$relic().is(relic),"Progress and displayed item persist through reload");
            jar.addEssence(jar.getLimit()-jar.getAmount());for(int i=0;i<200;i++)RelicJars.tick(jar);h.assertTrue(jar.getAmount()==jar.getLimit(),"Full jar never overflows");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);click(h,p,POS);
            h.assertTrue(p.getMainHandItem().is(relic)&&storage.bfa$relic().isEmpty(),"Empty hand sneak returns the mounted relic");
            jar.addEssence(-jar.getAmount());p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ELEMENTARIUM.get()));click(h,p,POS);
            h.assertTrue(storage.bfa$relic().isEmpty(),"Other relics cannot mount");
        }
        h.setBlock(POS,Blocks.AIR);h.setBlock(POS,ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,EssenceType.AUREAL));EssenceUtremJarBlockEntity jar=h.getBlockEntity(POS);jar.addEssence(7);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CRIMSON_STONE.get()));click(h,p,POS);
        h.assertTrue(jar.getAmount()==7&&((JarRelicStorage)jar).bfa$relic().isEmpty()&&p.getMainHandItem().getCount()==1,"Different stock cannot be overwritten by relic placement");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CRESCENT_MOON.get()));click(h,p,POS);h.setBlock(POS,Blocks.AIR);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2),e->e.getItem().is(ModItems.CRESCENT_MOON.get()));
        h.assertTrue(drops.size()==1&&drops.getFirst().getItem().getCount()==1,"Breaking jar returns exactly one mounted relic");h.succeed();
    }
    @GameTest(template="guide_area",timeoutTicks=150) public static void mountedJarHasRealServerTicker(GameTestHelper h) {
        var p=player(h);h.setBlock(POS,ModBlocks.UTREM_JAR.get());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CRESCENT_MOON.get()));click(h,p,POS);
        h.runAtTickTime(120,()->{EssenceUtremJarBlockEntity jar=h.getBlockEntity(POS);h.assertTrue(jar.getAmount()==3,"Actual block ticker produces three points in five seconds");h.succeed();});
    }
    @GameTest(template="empty") public static void aurealBottleGhastRecipeSoulGateAndCancellation(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);
        for(var soul:List.of(ItemStack.EMPTY,new ItemStack(ModItems.SOUL.get()),new ItemStack(ModItems.CORRUPT_SOUL.get()),new ItemStack(ModItems.ENCHANTED_SOUL.get()))) {
            h.setBlock(pos,Blocks.AIR);h.setBlock(pos,ModBlocks.CLIBANO_MAIN_PART.get());ClibanoMainBlockEntity c=h.getBlockEntity(pos);
            c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(),new ItemStack(ModItems.AUREAL_BOTTLE.get()));c.setStack(ClibanoMenu.FUEL_SLOT,new ItemStack(Items.COAL));c.setStack(ClibanoMenu.SOUL_SLOT,soul);
            for(int i=0;i<100;i++)ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
            int output=0;for(int slot:List.of(ClibanoMenu.RESULT_SLOTS.getFirst(),ClibanoMenu.RESULT_SLOTS.getSecond()))if(c.getStack(slot).is(Items.GHAST_TEAR))output+=c.getStack(slot).getCount();
            h.assertTrue(output==(SoulItems.canExtractSoul(soul)?1:0),"Only corrupt and enchanted soul can make one ghast tear");
            h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).isEmpty()==SoulItems.canExtractSoul(soul),"Rejected input bottle retained");
        }
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,ModBlocks.CLIBANO_MAIN_PART.get());ClibanoMainBlockEntity c=h.getBlockEntity(pos);
        c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(),new ItemStack(ModItems.AUREAL_BOTTLE.get()));c.setStack(ClibanoMenu.SOUL_SLOT,new ItemStack(ModItems.CORRUPT_SOUL.get()));
        for(int i=0;i<100;i++)ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
        h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount()==1,"Fuel still required");
        c.setStack(ClibanoMenu.FUEL_SLOT,new ItemStack(Items.COAL));for(int i=0;i<30;i++)ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
        c.setStack(ClibanoMenu.SOUL_SLOT,ItemStack.EMPTY);c.setStack(ClibanoMenu.SOUL_SLOT,new ItemStack(ModItems.CORRUPT_SOUL.get()));
        for(int i=0;i<35;i++)ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
        h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount()==1,"Losing qualifying soul clears partial progress");h.succeed();
    }
}
