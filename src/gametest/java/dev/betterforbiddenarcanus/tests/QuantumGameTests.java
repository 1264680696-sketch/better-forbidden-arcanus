package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.QuantumInjectorBlock;
import com.stal111.forbidden_arcanus.common.block.entity.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import com.stal111.forbidden_arcanus.common.block.pedestal.effect.PedestalEffectTrigger;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.inventory.HephaestusForgeMenu;
import com.stal111.forbidden_arcanus.core.init.*;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.mixin.EssenceCapacityAccessor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_tests") @PrefixGameTestTemplate(false)
public final class QuantumGameTests {
    private static final BlockPos CENTER = new BlockPos(8,1,8), INJECTOR = CENTER.offset(-2,0,0), JAR = CENTER.offset(-2,0,2);
    private record Setup(HephaestusForgeBlockEntity forge, QuantumInjectorBlockEntity injector, EssenceUtremJarBlockEntity jar) {}
    private static Setup setup(GameTestHelper h, EssenceType type) {
        String[] floor={"___PPP___","_PPPAPPP_","_PAPPPAP_","PPPPCPPPP","PAPCACPAP","PPPPCPPPP","_PAPPPAP_","_PPPAPPP_","___PPP___"};
        for(int x=0;x<9;x++)for(int z=0;z<9;z++) {
            var block=switch(floor[x].charAt(z)){case 'P'->ModBlocks.POLISHED_DARKSTONE.get();case 'A'->ModBlocks.GILDED_CHISELED_POLISHED_DARKSTONE.get();case 'C'->ModBlocks.CHISELED_ARCANE_POLISHED_DARKSTONE.get();default->null;};
            if(block!=null)h.setBlock(CENTER.offset(x-4,-1,z-4),block);
        }
        h.setBlock(CENTER,ModBlocks.HEPHAESTUS_FORGE_TIER_1.get().defaultBlockState().setValue(ModBlockStateProperties.ACTIVATED,true));
        HephaestusForgeBlockEntity forge=h.getBlockEntity(CENTER);forge.setForgeLevel(HephaestusForgeLevel.values()[0]);
        h.setBlock(INJECTOR,ModBlocks.QUANTUM_INJECTOR.get().defaultBlockState().setValue(QuantumInjectorBlock.ENABLED,false));
        h.setBlock(JAR,ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,type));
        return new Setup(forge,h.getBlockEntity(INJECTOR),h.getBlockEntity(JAR));
    }
    private static void enabled(GameTestHelper h,BlockPos pos,boolean value) {
        h.getLevel().setBlockAndUpdate(h.absolutePos(pos),h.getBlockState(pos).setValue(QuantumInjectorBlock.ENABLED,value));
    }
    private static void pulse(GameTestHelper h,Runnable action) {
        if(h.getLevel().getGameTime()%5==0)action.run();else h.runAfterDelay(1,()->pulse(h,action));
    }
    private static void transfer(GameTestHelper h,QuantumInjectorBlockEntity injector) {
        QuantumInjectorBlockEntity.serverTick(h.getLevel(),injector.getBlockPos(),injector.getBlockState(),injector);
    }
    private static void eq(GameTestHelper h,int actual,int expected,String why) {h.assertTrue(actual==expected,why+": "+actual+" != "+expected);}
    @GameTest(template="guide_area") public static void simultaneousTypesAndIndependentCapacity(GameTestHelper h) {
        var s=setup(h,EssenceType.values()[0]);s.forge.setForgeLevel(HephaestusForgeLevel.values()[4]);
        var jars=new java.util.ArrayList<EssenceUtremJarBlockEntity>();
        for(var type:EssenceType.values()) {
            var pos=CENTER.offset(-3+type.ordinal()*2,0,2);
            h.setBlock(pos,ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,type));
            EssenceUtremJarBlockEntity jar=h.getBlockEntity(pos);jar.addEssence(30);jars.add(jar);
        }
        h.runAfterDelay(10,()->pulse(h,()->{
            enabled(h,INJECTOR,true);transfer(h,s.injector);
            for(var type:EssenceType.values()) {
                eq(h,jars.get(type.ordinal()).getAmount(),20,"All four types withdrawn on the same pulse");
                eq(h,s.forge.getEssenceManager().getEssence(type),12,"All four types receive their own 20 percent bonus");
            }
            var full=EssenceType.values()[0];int cap=((EssenceCapacityAccessor)s.forge.getEssenceManager()).bfa$capacity().get(full);
            s.forge.getEssenceManager().setEssence(full,cap);transfer(h,s.injector);
            eq(h,jars.get(0).getAmount(),20,"Full type consumes nothing");
            for(int i=1;i<jars.size();i++) {
                eq(h,jars.get(i).getAmount(),10,"Other types continue even when the first is full");
                eq(h,s.forge.getEssenceManager().getEssence(EssenceType.values()[i]),24,"No type starved by another");
            }
            h.succeed();
        }));
    }
    @GameTest(template="guide_area") public static void doubleRateAllEssencesEmptyAndDisabled(GameTestHelper h) {
        var s=setup(h,EssenceType.values()[0]);
        h.runAfterDelay(10,()->pulse(h,()->{
            s.forge.setForgeLevel(HephaestusForgeLevel.values()[4]);
            for(var type:EssenceType.values()) {
                h.setBlock(JAR,ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,type));
                EssenceUtremJarBlockEntity jar=h.getBlockEntity(JAR);jar.addEssence(-jar.getAmount());jar.addEssence(10);
                // Register changed POI type before inspecting the real native search.
                h.getLevel().getPoiManager().remove(jar.getBlockPos());
                h.getLevel().getPoiManager().add(jar.getBlockPos(),((QuantumTransfer)s.injector).bfa$poiTypes().get(type));
                enabled(h,INJECTOR,true);transfer(h,s.injector);
                eq(h,jar.getAmount(),0,"Input throughput is 10 per pulse");eq(h,s.forge.getEssenceManager().getEssence(type),12,"20 percent yield for every type");
                for(int i=0;i<20;i++)transfer(h,s.injector);
                eq(h,jar.getAmount(),0,"Empty jar never becomes negative");eq(h,s.forge.getEssenceManager().getEssence(type),12,"Empty source cannot create essence");
                enabled(h,INJECTOR,false);jar.addEssence(10);transfer(h,s.injector);eq(h,jar.getAmount(),10,"Disabled injector stops immediately");
                h.assertTrue(!QuantumInjector.boosts(h.getLevel(),s.forge.getBlockPos()),"Disabled source stops ritual boost");
            }
            h.succeed();
        }));
    }
    @GameTest(template="guide_area") public static void partialInputCreditsReloadAndFullCapacity(GameTestHelper h) {
        var type=EssenceType.values()[0];var s=setup(h,type);
        h.runAfterDelay(10,()->pulse(h,()->{
            enabled(h,INJECTOR,true);var injector=s.injector;
            for(int i=0;i<5;i++) {
                s.jar.addEssence(1);transfer(h,injector);eq(h,s.jar.getAmount(),0,"One point source consumed once");
                if(i==1) {
                    var tag=injector.saveWithoutMetadata(h.getLevel().registryAccess());
                    var restored=new QuantumInjectorBlockEntity(injector.getBlockPos(),injector.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(tag,h.getLevel().registryAccess());
                    eq(h,((QuantumTransfer)restored).bfa$credit(type),2,"Fractional credit persisted");h.getLevel().setBlockEntity(restored);injector=restored;
                }
            }
            eq(h,s.forge.getEssenceManager().getEssence(type),6,"Five individual inputs yield six without rounding loss");
            int cap=((EssenceCapacityAccessor)s.forge.getEssenceManager()).bfa$capacity().get(type);
            s.forge.getEssenceManager().setEssence(type,cap);s.jar.addEssence(10);transfer(h,injector);eq(h,s.jar.getAmount(),10,"Full forge costs zero");
            s.forge.getEssenceManager().setEssence(type,cap-1);transfer(h,injector);eq(h,s.jar.getAmount(),9,"Single free space cannot drain ten points");eq(h,s.forge.getEssenceManager().getEssence(type),cap,"Never exceeds capacity");
            s.forge.getEssenceManager().setEssence(type,0);
            for(int i=0;i<4;i++){s.jar.addEssence(-s.jar.getAmount());s.jar.addEssence(1);transfer(h,injector);}
            eq(h,s.forge.getEssenceManager().getEssence(type),5,"Capacity-limited bonus remains credited");
            var tag=s.jar.saveWithoutMetadata(h.getLevel().registryAccess());tag.putInt(EssenceUtremJarBlockEntity.TAG_AMOUNT,-90);
            var restored=new EssenceUtremJarBlockEntity(s.jar.getBlockPos(),s.jar.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());eq(h,restored.getAmount(),0,"Old negative save repaired");
            h.succeed();
        }));
    }
    @GameTest(template="guide_area") public static void skipEmptyJarAndSharedStock(GameTestHelper h) {
        var type=EssenceType.values()[0];var s=setup(h,type);var second=INJECTOR.offset(0,0,-1);var far=JAR.offset(0,0,2);
        h.setBlock(second,ModBlocks.QUANTUM_INJECTOR.get().defaultBlockState().setValue(QuantumInjectorBlock.ENABLED,false));
        h.setBlock(far,ModBlocks.ESSENCE_UTREM_JAR.get().defaultBlockState().setValue(ModBlockStateProperties.ESSENCE_TYPE,type));
        EssenceUtremJarBlockEntity stock=h.getBlockEntity(far);stock.addEssence(15);
        h.runAfterDelay(10,()->pulse(h,()->{
            enabled(h,INJECTOR,true);enabled(h,second,true);transfer(h,s.injector);transfer(h,h.getBlockEntity(second));
            eq(h,s.jar.getAmount(),0,"Near empty jar skipped");eq(h,stock.getAmount(),0,"Two injectors share only available fifteen");eq(h,s.forge.getEssenceManager().getEssence(type),18,"No repeated stock or lost bonus");
            h.assertTrue(QuantumInjector.boosts(h.getLevel(),s.forge.getBlockPos()),"Empty but enabled connected injector boosts");
            h.setBlock(INJECTOR,net.minecraft.world.level.block.Blocks.AIR);enabled(h,second,false);
            h.assertTrue(!QuantumInjector.boosts(h.getLevel(),s.forge.getBlockPos()),"Removed and disabled sources immediately invalid");h.succeed();
        }));
    }
    private static void prepare(GameTestHelper h,HephaestusForgeBlockEntity forge) {
        forge.setStack(HephaestusForgeBlockEntity.MAIN_SLOT,new ItemStack(Items.IRON_PICKAXE));
        forge.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0),new ItemStack(ModItems.ARTISAN_RELIC.get()));
        var sides=List.of(CENTER.offset(-3,0,0),CENTER.offset(3,0,0),CENTER.offset(0,0,-3),CENTER.offset(0,0,3),CENTER.offset(-2,0,-2),CENTER.offset(2,0,-2),CENTER.offset(-2,0,2),CENTER.offset(2,0,2));
        var materials=List.of(Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,Items.COPPER_INGOT,Items.COPPER_INGOT,ModItems.ARCANE_CRYSTAL.get(),ModItems.ARCANE_CRYSTAL.get());
        for(int i=0;i<8;i++) {h.setBlock(sides.get(i),ModBlocks.DARKSTONE_PEDESTAL.get());PedestalBlockEntity pedestal=h.getBlockEntity(sides.get(i));var stack=new ItemStack(materials.get(i));pedestal.setStack(stack,null,PedestalEffectTrigger.RITUAL_FINISHED);forge.updatePedestalStack(pedestal.getBlockPos(),stack);}
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"quantum-test"),ClientInformation.createDefault());
        h.assertTrue(forge.getRitualManager().startRitual(p,forge.getEssenceManager().getStorage()),"Actual native ritual starts");
    }
    @GameTest(template="guide_area") public static void ritualTimeDiscountReloadAndNoStacking(GameTestHelper h) {
        var s=setup(h,EssenceType.values()[0]);var second=INJECTOR.offset(0,0,-1);
        h.setBlock(second,ModBlocks.QUANTUM_INJECTOR.get().defaultBlockState().setValue(QuantumInjectorBlock.ENABLED,false));
        h.runAfterDelay(10,()->pulse(h,()->{
            prepare(h,s.forge);int baseline=0;
            while(baseline++<510)if(s.forge.getRitualManager().tick().isPresent())break;
            eq(h,baseline,501,"Unconnected base duration");
            enabled(h,INJECTOR,true);enabled(h,second,true);transfer(h,s.injector);transfer(h,h.getBlockEntity(second));
            prepare(h,s.forge);var forge=s.forge;int accelerated=0;
            while(accelerated++<510) {
                var result=forge.getRitualManager().tick();
                if(accelerated==103) {
                    var tag=forge.saveWithoutMetadata(h.getLevel().registryAccess());
                    var restored=new HephaestusForgeBlockEntity(forge.getBlockPos(),forge.getBlockState());restored.setLevel(h.getLevel());restored.onLoad();restored.loadWithComponents(tag,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);forge=restored;
                }
                if(result.isPresent()){h.assertTrue(ForgeUpgrades.tier(result.get())==1,"Real upgraded result");break;}
            }
            eq(h,accelerated,401,"500 processing ticks become 400, including reload and multiple injectors");
            enabled(h,INJECTOR,false);enabled(h,second,false);prepare(h,forge);int reverted=0;
            while(reverted++<510)if(forge.getRitualManager().tick().isPresent())break;
            eq(h,reverted,501,"Turning injectors off restores original duration");h.succeed();
        }));
    }
}
