package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_relic_acquisition_tests") @PrefixGameTestTemplate(false)
public final class RelicAcquisitionGameTests {
    private static LootTable table(GameTestHelper h,String entity) {
        return h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("entities/"+entity)));
    }
    private static LootParams params(GameTestHelper h,Entity victim,Player player,boolean ranged) {
        var level=h.getLevel();var b=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,victim).withParameter(LootContextParams.ORIGIN,victim.position());
        if(player!=null){
            var arrow=EntityType.ARROW.create(level);arrow.setOwner(player);
            b.withParameter(LootContextParams.DAMAGE_SOURCE,ranged?level.damageSources().arrow(arrow,player):level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.ATTACKING_ENTITY,player).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,ranged?arrow:player).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player);
        }else b.withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().fall());
        return b.create(LootContextParamSets.ENTITY);
    }
    private static int moons(List<ItemStack> loot){return loot.stream().filter(s->s.is(ModItems.CRESCENT_MOON.get())).mapToInt(ItemStack::getCount).sum();}

    @GameTest(template="guide_area",timeoutTicks=200)
    public static void actualPhantomTableHasTenPercentWithAndWithoutLooting(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);var phantom=EntityType.PHANTOM.create(h.getLevel());
        for(int looting:new int[]{0,3}){
            var sword=new ItemStack(Items.DIAMOND_SWORD);
            if(looting>0)sword.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),looting);
            player.setItemInHand(InteractionHand.MAIN_HAND,sword);var params=params(h,phantom,player,false);var random=RandomSource.create(931755);
            int count=0,membranes=0;
            for(int i=0;i<10000;i++){
                var loot=table(h,"phantom").getRandomItems(params,random);int n=moons(loot);h.assertTrue(n<=1,"At most one crescent per phantom");count+=n;
                membranes+=loot.stream().filter(s->s.is(Items.PHANTOM_MEMBRANE)).mapToInt(ItemStack::getCount).sum();
            }
            h.assertTrue(count>=900&&count<=1100,"10% at looting "+looting+", got "+count+"/10000");h.assertTrue(membranes>0,"Original membrane loot preserved");
            com.mojang.logging.LogUtils.getLogger().info("Crescent actual phantom table looting {}: {}/10000",looting,count);
        }
        h.succeed();
    }
    @GameTest(template="guide_area",timeoutTicks=200)
    public static void requiresPlayerKillAndOnlyPhantomsButAllowsArrows(GameTestHelper h){
        var phantom=EntityType.PHANTOM.create(h.getLevel());var player=h.makeMockPlayer(GameType.SURVIVAL);var random=RandomSource.create(44573);
        var natural=params(h,phantom,null,false);var zombie=params(h,EntityType.ZOMBIE.create(h.getLevel()),player,false);var shot=params(h,phantom,player,true);int rangedDrops=0;
        for(int i=0;i<1000;i++){
            h.assertTrue(moons(table(h,"phantom").getRandomItems(natural,random))==0,"Environment kill has no crescent");
            h.assertTrue(moons(table(h,"zombie").getRandomItems(zombie,random))==0,"Other mobs do not drop crescent");
            rangedDrops+=moons(table(h,"phantom").getRandomItems(shot,random));
        }
        h.assertTrue(rangedDrops>50&&rangedDrops<150,"Player-owned arrows qualify");h.succeed();
    }
    @GameTest(template="guide_area")
    public static void actualClericPoolAndGeneratedApprenticeOffers(GameTestHelper h){
        var merchant=EntityType.VILLAGER.create(h.getLevel());var random=RandomSource.create(3);int listings=0;
        for(var profession:VillagerTrades.TRADES.keySet())for(var level:VillagerTrades.TRADES.get(profession).int2ObjectEntrySet())for(var listing:level.getValue()){
            var offer=listing.getOffer(merchant,random);
            if(offer==null||!offer.getResult().is(ModItems.DIVINE_PACT.get()))continue;
            listings++;h.assertTrue(profession==VillagerProfession.CLERIC&&level.getIntKey()==2,"Only apprentice cleric pool contains pact");
            h.assertTrue(offer.getBaseCostA().is(Items.EMERALD)&&offer.getBaseCostA().getCount()==16&&offer.getCostB().isEmpty()&&offer.getResult().getCount()==1,"16 emeralds buy one pact");
            h.assertTrue(offer.getMaxUses()==4&&offer.getXp()==10&&Math.abs(offer.getPriceMultiplier()-.05F)<.0001,"Restock and pricing metadata");
        }
        h.assertTrue(listings==1,"Exactly one pact listing after actual trade reload");
        int found=0;
        for(int i=0;i<40;i++){
            var v=EntityType.VILLAGER.create(h.getLevel());v.getRandom().setSeed(i);v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.CLERIC).setLevel(2));
            h.assertTrue(v.getOffers().size()==2,"Vanilla still picks two apprentice offers");
            if(v.getOffers().stream().anyMatch(o->o.getResult().is(ModItems.DIVINE_PACT.get())))found++;
        }
        h.assertTrue(found>0&&found<40,"New level-2 clerics actually select pact from random pool");
        var novice=EntityType.VILLAGER.create(h.getLevel());novice.setVillagerData(novice.getVillagerData().setProfession(VillagerProfession.CLERIC));
        h.assertTrue(novice.getOffers().stream().noneMatch(o->o.getResult().is(ModItems.DIVINE_PACT.get())),"Novices cannot sell the pact");h.succeed();
    }
}
