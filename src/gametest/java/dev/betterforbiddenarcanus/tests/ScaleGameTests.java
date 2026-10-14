package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.inventory.clibano.ClibanoMenu;
import com.stal111.forbidden_arcanus.core.init.*;
import dev.betterforbiddenarcanus.*;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_tests") @PrefixGameTestTemplate(false)
public final class ScaleGameTests {
    @GameTest(template="empty") public static void existingEggBootstrapAfterChunkLoad(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,ModBlocks.ARCANE_DRAGON_EGG.get());
        var absolute=h.absolutePos(pos);DragonEggScales.removed(h.getLevel(),absolute);
        DragonEggScales.loadChunk(new net.neoforged.neoforge.event.level.ChunkEvent.Load(h.getLevel().getChunkAt(absolute),false));
        h.runAfterDelay(2,()->{
            var tag=DragonEggScales.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
            h.assertTrue(tag.getList("eggs",net.minecraft.nbt.Tag.TAG_COMPOUND).stream().map(net.minecraft.nbt.CompoundTag.class::cast).anyMatch(row->row.getLong("pos")==absolute.asLong()),"Existing egg receives timer after chunk is ready");h.succeed();
        });
    }
    @GameTest(template="empty") public static void enchantedAppleActualCraftingAndVariants(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(SoulItems.id("enchanted_golden_apple")).orElseThrow().value();
        var items=NonNullList.withSize(9,ItemStack.EMPTY);for(int i=0;i<9;i++)items.set(i,new ItemStack(ModItems.GOLDEN_DRAGON_SCALE.get(),2));items.set(4,new ItemStack(Items.GOLDEN_APPLE,2));
        var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),3,3,items);
        var input=CraftingInput.of(3,3,grid.getItems());h.assertTrue(recipe.matches(input,h.getLevel()),"Golden scales around golden apple match");
        var result=recipe.assemble(input,h.getLevel().registryAccess());h.assertTrue(result.is(Items.ENCHANTED_GOLDEN_APPLE)&&result.getCount()==1,"Exactly one original enchanted apple");
        var container=new ResultContainer();container.setItem(0,result);new ResultSlot(player,grid,container,0,0,0).onTake(player,result);
        for(int i=0;i<9;i++)h.assertTrue(grid.getItem(i).getCount()==1,"Every ingredient consumed normally");
        for(var other:List.of(ModItems.DRAGON_SCALE.get(),ModItems.SILVER_DRAGON_SCALE.get(),ModItems.AQUATIC_DRAGON_SCALE.get())) {
            grid.setItem(0,new ItemStack(other));h.assertTrue(!recipe.matches(CraftingInput.of(3,3,grid.getItems()),h.getLevel()),"Only golden variant allowed");
        }
        h.assertTrue(!recipe.canCraftInDimensions(2,2),"Full crafting table required");h.succeed();
    }
    @GameTest(template="empty") public static void clibanoScaleRecyclingFuelAndOutputs(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);
        for(var scale:List.of(ModItems.GOLDEN_DRAGON_SCALE.get(),ModItems.SILVER_DRAGON_SCALE.get(),ModItems.AQUATIC_DRAGON_SCALE.get())) {
            h.setBlock(pos,Blocks.AIR);h.setBlock(pos,ModBlocks.CLIBANO_MAIN_PART.get());ClibanoMainBlockEntity c=h.getBlockEntity(pos);
            c.setStack(ClibanoMenu.INPUT_SLOTS.getFirst(),new ItemStack(scale));
            for(int i=0;i<110;i++)ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
            h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount()==1,"No fuel cannot recycle");
            c.setStack(ClibanoMenu.FUEL_SLOT,new ItemStack(Items.COAL));c.setStack(ClibanoMenu.INPUT_SLOTS.getSecond(),new ItemStack(scale));
            for(int i=0;i<99;i++)ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
            h.assertTrue(c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).getCount()==1,"Base time is 100 ticks");
            ClibanoMainBlockEntity.serverTick(h.getLevel(),c.getBlockPos(),c.getBlockState(),c);
            int count=0;for(int slot:List.of(ClibanoMenu.RESULT_SLOTS.getFirst(),ClibanoMenu.RESULT_SLOTS.getSecond()))if(c.getStack(slot).is(ModItems.DRAGON_SCALE.get()))count+=c.getStack(slot).getCount();
            h.assertTrue(count==2&&c.getStack(ClibanoMenu.INPUT_SLOTS.getFirst()).isEmpty()&&c.getStack(ClibanoMenu.INPUT_SLOTS.getSecond()).isEmpty(),"Both variant scales recycled one for one");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void equalScalesAndDeadlinePersistence(GameTestHelper h) {
        var random=RandomSource.create(99321);var types=List.of(ModItems.DRAGON_SCALE.get(),ModItems.GOLDEN_DRAGON_SCALE.get(),ModItems.SILVER_DRAGON_SCALE.get(),ModItems.AQUATIC_DRAGON_SCALE.get());int[] counts=new int[4];
        for(int i=0;i<40000;i++){var stack=DragonEggScales.choose(random);h.assertTrue(stack.getCount()==1,"One scale per selection");counts[types.indexOf(stack.getItem())]++;}
        for(int count:counts)h.assertTrue(Math.abs(count-10000)<350,"Four equal probabilities: "+java.util.Arrays.toString(counts));
        var pos=h.absolutePos(new BlockPos(1,1,1));h.setBlock(new BlockPos(1,0,1),Blocks.STONE);h.setBlock(new BlockPos(1,1,1),ModBlocks.ARCANE_DRAGON_EGG.get());
        var tag=DragonEggScales.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        var entry=tag.getList("eggs",net.minecraft.nbt.Tag.TAG_COMPOUND).stream().map(net.minecraft.nbt.CompoundTag.class::cast).filter(row->row.getLong("pos")==pos.asLong()).findFirst().orElseThrow();
        h.assertTrue(entry.getLong("next")==h.getLevel().getGameTime()+2400,"Full two minute delay after placement");
        var restored=DragonEggScales.load(tag,h.getLevel().registryAccess());h.assertTrue(restored.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess()).equals(tag),"Saved deadlines reload without loss");
        h.setBlock(new BlockPos(1,1,1),Blocks.AIR);var cleared=DragonEggScales.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(cleared.getList("eggs",net.minecraft.nbt.Tag.TAG_COMPOUND).stream().map(net.minecraft.nbt.CompoundTag.class::cast).noneMatch(row->row.getLong("pos")==pos.asLong()),"Removed egg removes timer");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=2470) public static void realTwoMinuteDropAndFalling(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,ModBlocks.ARCANE_DRAGON_EGG.get());
        h.runAtTickTime(2390,()->h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(2)).isEmpty(),"No premature scale"));
        h.runAtTickTime(2420,()->{
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(2));
            h.assertTrue(drops.size()==1&&drops.getFirst().getItem().getCount()==1,"One actual scheduled scale after two minutes");
            h.setBlock(pos.below(),Blocks.AIR);
        });
        h.runAtTickTime(2440,()->{h.assertTrue(!h.getBlockState(pos).is(ModBlocks.ARCANE_DRAGON_EGG.get()),"Original falling behavior retained");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=300) public static void actualSkullLootChanceLootingAndWeaponScope(GameTestHelper h) {
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);var skeleton=EntityType.WITHER_SKELETON.create(level);
        var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("entities/wither_skeleton")));
        for(int mode=0;mode<4;mode++) {
            var weapon=new ItemStack(mode==2?Items.DIAMOND_AXE:ModItems.DRACO_ARCANUS_AXE.get());
            if(mode==1)weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),3);
            player.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            var arrow=EntityType.ARROW.create(level);var source=mode==3?level.damageSources().arrow(arrow,player):level.damageSources().playerAttack(player);
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,skeleton).withParameter(LootContextParams.ORIGIN,skeleton.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE,source).withParameter(LootContextParams.ATTACKING_ENTITY,player)
                    .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,mode==3?arrow:player).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).create(LootContextParamSets.ENTITY);
            int successes=0;for(int i=0;i<12000;i++) {
                var loot=table.getRandomItems(params,771234L+i);int heads=loot.stream().filter(s->s.is(Items.WITHER_SKELETON_SKULL)).mapToInt(ItemStack::getCount).sum();
                h.assertTrue(heads<=1,"Never a second skull from the same kill");successes+=heads;
            }
            double expected=mode==0?.325:mode==1?.355:.025,actual=successes/12000.0;
            h.assertTrue(Math.abs(actual-expected)<.015,"Actual skull table mode "+mode+": "+actual+" != "+expected);
        }
        h.succeed();
    }
}
