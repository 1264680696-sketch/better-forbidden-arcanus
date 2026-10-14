package dev.betterforbiddenarcanus.tests;
import com.stal111.forbidden_arcanus.common.block.entity.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import com.stal111.forbidden_arcanus.common.block.pedestal.effect.PedestalEffectTrigger;
import com.stal111.forbidden_arcanus.core.init.*;
import dev.betterforbiddenarcanus.SoulItems;
import dev.betterforbiddenarcanus.mixin.EssenceCapacityAccessor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("bfa_tests") @PrefixGameTestTemplate(false)
public final class SoulCrimsonGameTests {
    @GameTest(template="guide_area") public static void exactTierFiveHalfCapacityAndEightMaterials(GameTestHelper h) {
        var center=new BlockPos(8,1,8);h.setBlock(center,ModBlocks.HEPHAESTUS_FORGE_TIER_5.get());HephaestusForgeBlockEntity f=h.getBlockEntity(center);
        f.setForgeLevel(HephaestusForgeLevel.values()[4]);f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT,new ItemStack(ModItems.CRIMSON_STONE.get()));
        var items=List.of(ModItems.DIVINE_PACT.get(),ModItems.MALEDICTUS_PACT.get(),ModItems.CRESCENT_MOON.get(),ModItems.ELEMENTARIUM.get(),ModItems.ARTISAN_RELIC.get(),ModItems.SOUL_BINDING_CRYSTAL.get(),ModItems.DARK_NETHER_STAR.get(),ModItems.ETERNAL_STELLA.get());
        var sides=List.of(center.offset(-3,0,0),center.offset(3,0,0),center.offset(0,0,-3),center.offset(0,0,3),center.offset(-2,0,-2),center.offset(2,0,-2),center.offset(-2,0,2),center.offset(2,0,2));
        for(int i=0;i<8;i++){h.setBlock(sides.get(i),ModBlocks.DARKSTONE_PEDESTAL.get());PedestalBlockEntity pedestal=h.getBlockEntity(sides.get(i));var stack=new ItemStack(items.get(i));pedestal.setStack(stack,null,PedestalEffectTrigger.RITUAL_FINISHED);f.updatePedestalStack(pedestal.getBlockPos(),stack);}
        var capacities=((EssenceCapacityAccessor)f.getEssenceManager()).bfa$capacity();
        h.assertTrue(capacities.get(EssenceType.AUREAL)==20000&&capacities.get(EssenceType.SOULS)==1000&&capacities.get(EssenceType.BLOOD)==100000&&capacities.get(EssenceType.EXPERIENCE)==7500,"Documented native fifth-tier capacities");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"soul-crimson-test"),ClientInformation.createDefault());
        f.setForgeLevel(HephaestusForgeLevel.values()[3]);h.assertTrue(f.getRitualManager().getValidRitual().isEmpty(),"Tier four cannot match");f.setForgeLevel(HephaestusForgeLevel.values()[4]);
        for(var type:EssenceType.values())f.getEssenceManager().setEssence(type,capacities.get(type)/2);
        f.getEssenceManager().setExperience(3749);h.assertTrue(!f.getRitualManager().startRitual(p,f.getEssenceManager().getStorage()),"One missing XP rejects without spending materials");f.getEssenceManager().setExperience(3750);
        h.assertTrue(f.getRitualManager().startRitual(p,f.getEssenceManager().getStorage()),"Complete tier-five ritual starts");
        for(int i=0;i<=500;i++)f.getRitualManager().tick().ifPresent(stack->f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT,stack));
        h.assertTrue(f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT).is(ModItems.SOUL_CRIMSON_STONE.get())&&f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT).getCount()==1,"One original soul crimson stone");
        for(var type:EssenceType.values())h.assertTrue(f.getEssenceManager().getEssence(type)==0,"Exactly half native capacity spent for "+type);
        for(var pos:sides)h.assertTrue(!((PedestalBlockEntity)h.getBlockEntity(pos)).hasStack(),"All eight pedestal materials consumed");h.succeed();
    }
}
