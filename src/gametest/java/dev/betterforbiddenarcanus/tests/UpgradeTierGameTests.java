package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.inventory.HephaestusForgeMenu;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import dev.betterforbiddenarcanus.ForgeUpgrades;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_upgrade_tier_tests") @PrefixGameTestTemplate(false)
public final class UpgradeTierGameTests {
    private static ItemStack main(HephaestusForgeBlockEntity f){return f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT);}
    private static void sides(GameTestHelper h,boolean consumed){
        for(var pos:UpgradeGameTests.SIDES){var stack=((PedestalBlockEntity)h.getBlockEntity(pos)).getStack();h.assertTrue(consumed?stack.isEmpty():stack.getCount()==1,"Pedestal consumption matches outcome");}
    }
    private static boolean start(GameTestHelper h,HephaestusForgeBlockEntity f){return f.getRitualManager().startRitual(UpgradeGameTests.starter(h),f.getEssenceManager().getStorage());}

    @GameTest(template="guide_area",timeoutTicks=200)
    public static void allTwentyFiveForgeAndRecipeCombinations(GameTestHelper h){
        for(int forgeTier=1;forgeTier<=5;forgeTier++)for(int recipeTier=1;recipeTier<=5;recipeTier++){
            var input=new ItemStack(Items.DIAMOND_PICKAXE);input.setDamageValue(97);input.set(DataComponents.CUSTOM_NAME,Component.literal("保留原装备"));var before=input.copy();
            var f=UpgradeGameTests.forge(h,forgeTier,input);UpgradeGameTests.fill(h,f,recipeTier);
            boolean allowed=forgeTier>=recipeTier;
            h.assertTrue(f.getRitualManager().getValidRitual().isPresent()==allowed,"Recipe selection: forge "+forgeTier+", recipe "+recipeTier);
            h.assertTrue(start(h,f)==allowed,"Start: forge "+forgeTier+", recipe "+recipeTier);
            if(allowed){
                UpgradeGameTests.finish(f);var result=main(f);h.assertTrue(result.getCount()==1&&ForgeUpgrades.tier(result)==recipeTier,"Result follows chosen recipe, not higher forge tier");
                var stripped=result.copy();stripped.remove(ForgeUpgrades.TIER);h.assertTrue(ItemStack.matches(stripped,before),"No other equipment component changes");
            }else h.assertTrue(ItemStack.matches(main(f),before),"Insufficient forge preserves input");
            sides(h,allowed);h.assertTrue(f.getStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0)).getCount()==1,"Artisan enhancer retained");
        }
        h.succeed();
    }
    @GameTest(template="guide_area")
    public static void higherForgeStillRejectsSameGradeAndDowngrades(GameTestHelper h){
        var input=new ItemStack(Items.DIAMOND_SWORD);input.set(ForgeUpgrades.TIER,1);var f=UpgradeGameTests.forge(h,5,input);UpgradeGameTests.fill(h,f,3);
        h.assertTrue(start(h,f),"Tier-five forge accepts a tier-one item for tier-three recipe");UpgradeGameTests.finish(f);h.assertTrue(ForgeUpgrades.tier(main(f))==3,"Cross-grade upgrade reaches three");
        for(int rejected:new int[]{2,3}){
            UpgradeGameTests.fill(h,f,rejected);h.assertTrue(f.getRitualManager().getValidRitual().isEmpty()&&!start(h,f),"Same-grade and downgrade rejected");sides(h,false);h.assertTrue(ForgeUpgrades.tier(main(f))==3,"Rejected operation preserves grade");
        }
        UpgradeGameTests.fill(h,f,4);h.assertTrue(start(h,f),"Same higher forge accepts subsequent tier-four upgrade");UpgradeGameTests.finish(f);h.assertTrue(ForgeUpgrades.tier(main(f))==4,"Subsequent upgrade replaces grade");sides(h,true);h.succeed();
    }
    @GameTest(template="guide_area")
    public static void lowerRecipeCancellationReloadAndNativeTierRules(GameTestHelper h){
        var f=UpgradeGameTests.forge(h,5,new ItemStack(Items.IRON_PICKAXE));UpgradeGameTests.fill(h,f,2);h.assertTrue(start(h,f),"Higher-forge lower recipe starts");
        for(int i=0;i<100;i++)f.getRitualManager().tick();
        f.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0),ItemStack.EMPTY);UpgradeGameTests.finish(f);h.assertTrue(ForgeUpgrades.tier(main(f))==0,"Removing relic cancels lower recipe");sides(h,false);
        f.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0),new ItemStack(ModItems.ARTISAN_RELIC.get()));h.assertTrue(start(h,f),"Unconsumed ingredients can restart");
        for(int i=0;i<100;i++)f.getRitualManager().tick();var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());
        var restored=new HephaestusForgeBlockEntity(f.getBlockPos(),f.getBlockState());restored.setLevel(h.getLevel());restored.onLoad();restored.loadWithComponents(saved,h.getLevel().registryAccess());
        UpgradeGameTests.finish(restored);h.assertTrue(ForgeUpgrades.tier(main(restored))==2,"Higher forge reload completes original lower recipe");sides(h,true);
        var rituals=h.getLevel().registryAccess().registryOrThrow(FARegistries.RITUAL);
        for(int target=2;target<=5;target++){
            var tier=rituals.get(ResourceLocation.parse("forbidden_arcanus:upgrade_tier_"+target)).requirements().tier();
            h.assertTrue(tier.test(target-1)&&!tier.test(target),"Native forge-level upgrades retain exact previous-tier requirements");
        }
        h.succeed();
    }
}
