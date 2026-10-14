package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.init.ModSounds;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class DustAndDracoGameTests {
    private static Stream<Component> nodes(Component component) { return Stream.concat(Stream.of(component), component.getSiblings().stream().flatMap(DustAndDracoGameTests::nodes)); }
    private static double number(Object value) {
        if (value instanceof Component c && c.getContents() instanceof TranslatableContents t) return number(t.getArgs()[0]);
        return Double.parseDouble(value.toString());
    }
    private static void click(GameTestHelper h, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        var absolute=h.absolutePos(pos);
        net.neoforged.neoforge.common.CommonHooks.onRightClickBlock(player,InteractionHand.MAIN_HAND,absolute,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),net.minecraft.core.Direction.UP,absolute,false));
    }
    @GameTest(template = "empty")
    public static void dracoStatsAndSingleGreenUpgradedTotals(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var items = List.of(ModItems.DRACO_ARCANUS_SWORD.get(), ModItems.DRACO_ARCANUS_PICKAXE.get(), ModItems.DRACO_ARCANUS_AXE.get(),
                ModItems.DRACO_ARCANUS_SHOVEL.get(), ModItems.DRACO_ARCANUS_HOE.get(), ModItems.DRACO_ARCANUS_SCEPTER.get(), Items.DIAMOND_SWORD);
        double[] attacks = {12,10,14,10.5,4,1,7}, speeds = {1.8,1.4,1.2,1.2,5,4,1.6};
        for (int i = 0; i < items.size(); i++) for (int tier : List.of(0,5)) {
            var stack = new ItemStack(items.get(i)); if (tier > 0) stack.set(ForgeUpgrades.TIER,tier);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.tick();player.tick();
            double multiplier = tier == 0 || i == 5 ? 1 : 1.4;
            h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE)-attacks[i]*multiplier)<.003,"Real damage " + items.get(i));
            h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED)-speeds[i]*multiplier)<.003,"Real speed " + items.get(i));
            var lines=stack.getTooltipLines(Item.TooltipContext.of(h.getLevel()),player,TooltipFlag.NORMAL);
            var green=lines.stream().filter(c -> c.getStyle().getColor()!=null && c.getStyle().getColor().getValue()==ChatFormatting.DARK_GREEN.getColor()).toList();
            h.assertTrue(green.size()==2,"Exactly two native green attack rows, including unupgraded tools");
            var numbers=green.stream().flatMap(DustAndDracoGameTests::nodes).filter(c->c.getContents() instanceof TranslatableContents t && t.getKey().equals("attribute.modifier.equals.0"))
                    .map(c->number(((TranslatableContents)c.getContents()).getArgs()[0])).toList();
            h.assertTrue(numbers.size()==2 && Math.abs(numbers.get(0)-attacks[i]*multiplier)<.01 && Math.abs(numbers.get(1)-speeds[i]*multiplier)<.01,"Displayed totals must equal real weapon stats");
            h.assertTrue(lines.stream().flatMap(DustAndDracoGameTests::nodes).noneMatch(c->c.getContents() instanceof TranslatableContents t
                    && (t.getKey().startsWith("attribute.modifier.plus") || t.getKey().startsWith("attribute.modifier.take") || t.getKey().equals("tooltip.better_forbidden_arcanus.forge_upgrade"))),"No separate blue bonus or upgrade description rows");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void mundabiturGoldRecipeAndRejectedOldMaterials(GameTestHelper h) {
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("forbidden_arcanus:mundabitur_dust")).orElseThrow().value();
        var stacks=List.of(new ItemStack(ModItems.ARCANE_CRYSTAL_DUST.get()),new ItemStack(Items.REDSTONE),new ItemStack(Items.BONE_MEAL),new ItemStack(Items.GUNPOWDER),new ItemStack(Items.GOLD_INGOT));
        var input=CraftingInput.of(3,3,List.of(stacks.get(4),ItemStack.EMPTY,stacks.get(0),stacks.get(2),stacks.get(1),ItemStack.EMPTY,ItemStack.EMPTY,stacks.get(3),ItemStack.EMPTY));
        h.assertTrue(recipe.matches(input,h.getLevel()),"Five materials must match in arbitrary positions");
        var result=recipe.assemble(input,h.getLevel().registryAccess());h.assertTrue(result.is(ModItems.MUNDABITUR_DUST.get())&&result.getCount()==4,"Yield remains four original dust");
        var old=CraftingInput.of(3,2,List.of(stacks.get(0),stacks.get(1),new ItemStack(Items.BLAZE_POWDER),stacks.get(2),new ItemStack(Items.PHANTOM_MEMBRANE),stacks.get(3)));
        h.assertTrue(!recipe.matches(old,h.getLevel()),"Old blaze/membrane recipe must be replaced");h.succeed();
    }
    @GameTest(template = "empty")
    public static void corruptCrystalUseConsumptionAndNativeSound(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);var player=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CORRUPTI_DUST.get(),3));
        var sounds=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<PlayLevelSoundEvent.AtPosition> listener=e->{
            if(e.getLevel()==h.getLevel()&&e.getSound()!=null&&e.getSound().value()==ModSounds.MUNDABITUR_DUST_USE.get()) sounds.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.setBlock(pos,ModBlocks.ARCANE_CRYSTAL_BLOCK.get());click(h,pos,player);
            h.assertTrue(h.getBlockState(pos).is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get())&&player.getMainHandItem().getCount()==2,"One dust transforms exactly one block");
            h.assertTrue(sounds.get()==1,"Exactly one native obelisk-conversion sound");
            click(h,pos,player);h.setBlock(pos,Blocks.STONE);click(h,pos,player);
            h.assertTrue(player.getMainHandItem().getCount()==2&&sounds.get()==1,"Wrong or already-corrupted block must not consume or emit sound");
            var creative=h.makeMockPlayer(GameType.CREATIVE);GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());creative.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CORRUPTI_DUST.get()));
            h.setBlock(pos,ModBlocks.ARCANE_CRYSTAL_BLOCK.get());click(h,pos,creative);
            h.assertTrue(creative.getMainHandItem().getCount()==1&&h.getBlockState(pos).is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get()),"Creative conversion retains dust");
            var adventure=h.makeMockPlayer(GameType.ADVENTURE);GameType.ADVENTURE.updatePlayerAbilities(adventure.getAbilities());adventure.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CORRUPTI_DUST.get()));
            h.setBlock(pos,ModBlocks.ARCANE_CRYSTAL_BLOCK.get());click(h,pos,adventure);
            h.assertTrue(h.getBlockState(pos).is(ModBlocks.ARCANE_CRYSTAL_BLOCK.get()),"Do not bypass Adventure build permissions");
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.succeed();
    }
}
