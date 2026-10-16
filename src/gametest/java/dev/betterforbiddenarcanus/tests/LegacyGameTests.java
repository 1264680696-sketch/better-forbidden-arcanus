package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.common.entity.projectile.*;
import dev.betterforbiddenarcanus.*;
import dev.betterforbiddenarcanus.legacy.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_tests") @PrefixGameTestTemplate(false)
public final class LegacyGameTests {
    private static void close(GameTestHelper h, double actual, double expected, String message) { h.assertTrue(Math.abs(actual-expected)<.01, message+": "+actual+" != "+expected); }
    private static void click(GameTestHelper h, net.minecraft.world.entity.player.Player p, BlockPos pos) {
        var at=h.absolutePos(pos); h.getLevel().getBlockState(at).useItemOn(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
    }
    @GameTest(template="empty") public static void legacyRecipesResolveWithoutMissingItems(GameTestHelper h) {
        var names=List.of("skull","runic_chiseled_polished_darkstone","runic_tenebris_frame","runic_battleaxe","rune_bag","rune_bag_unpacking","dark_rune_bag","dark_rune_bag_unpacking","dark_rune_smelting");
        for(var name:names) {
            var recipe=h.getLevel().getRecipeManager().byKey(SoulItems.id(name)).orElseThrow().value();
            h.assertTrue(!recipe.getResultItem(h.getLevel().registryAccess()).isEmpty(),"Empty restored recipe "+name);
            for(var ingredient:recipe.getIngredients())if(!ingredient.isEmpty())h.assertTrue(ingredient.getItems().length>0,"Missing ingredient in "+name);
            if(recipe instanceof ShapedRecipe shaped) {
                var items=NonNullList.withSize(shaped.getWidth()*shaped.getHeight(),ItemStack.EMPTY);
                for(int i=0;i<items.size();i++)if(!shaped.getIngredients().get(i).isEmpty())items.set(i,shaped.getIngredients().get(i).getItems()[0].copy());
                var input=CraftingInput.of(shaped.getWidth(),shaped.getHeight(),items);
                h.assertTrue(shaped.matches(input,h.getLevel()),"Original layout must match "+name);
                items.set(0,ItemStack.EMPTY);
                if(!shaped.getIngredients().get(0).isEmpty())h.assertTrue(!shaped.matches(CraftingInput.of(shaped.getWidth(),shaped.getHeight(),items),h.getLevel()),"Missing material rejected "+name);
            }
        }
        var axe=new ItemStack(LegacyContent.AXE.get());var p=h.makeMockPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,axe);p.tick();p.tick();
        close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),5,"Legacy axe damage");close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),1.2,"Legacy axe speed");h.assertTrue(axe.getMaxDamage()==1561,"Diamond durability restored");
        h.succeed();
    }
    @GameTest(template="empty") public static void coreActivationAllChainsAndWaterlogging(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(1,1,1);
        h.setBlock(pos,LegacyContent.FRAME.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.DARK_NETHER_STAR.get()));click(h,p,pos);
        h.assertTrue(h.getBlockState(pos).is(LegacyContent.CORE.get())&&h.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED)&&p.getMainHandItem().isEmpty(),"One star activates the frame and preserves water");
        var inputs=List.of(ModItems.RUNE.get(),LegacyContent.RUNE_BAG.get(),ModBlocks.RUNIC_STONE.get().asItem(),ModBlocks.RUNIC_DEEPSLATE.get().asItem(),ModBlocks.RUNIC_DARKSTONE.get().asItem());
        for(var input:inputs) {
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(input,2));var expected=LegacyCoreBlock.transform(p.getMainHandItem());click(h,p,pos);
            h.assertTrue(p.getMainHandItem().getCount()==1&&p.getInventory().items.stream().filter(s->s.is(expected)).mapToInt(ItemStack::getCount).sum()==1,"One input gives exactly one output: "+input);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));click(h,p,pos);h.assertTrue(p.getMainHandItem().getCount()==1,"Unsupported inputs not consumed");
        h.succeed();
    }
    @GameTest(template="empty") public static void wardActivationBoundaryAndRemoval(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(1,1,1);var at=h.absolutePos(pos);var level=h.getLevel();
        h.setBlock(pos,LegacyContent.WARD.get());var data=LegacySpawnWard.data(level);
        h.assertTrue(!data.blocks(level,at.getX(),at.getY(),at.getZ()),"Inactive ward does nothing");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ARCANE_CRYSTAL.get(),2));click(h,p,pos);
        h.assertTrue(p.getMainHandItem().getCount()==1&&h.getBlockState(pos).getValue(LegacyRuneBlock.ACTIVATED),"One crystal activates");
        h.assertTrue(data.blocks(level,at.getX()+25,at.getY(),at.getZ()),"Exact boundary included");h.assertTrue(!data.blocks(level,at.getX()+25.01,at.getY(),at.getZ()),"Outside radius excluded");
        var saved=data.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess());h.assertTrue(saved.getLongArray("Positions").length>0,"Ward positions serialize");
        h.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);h.assertTrue(!data.blocks(level,at.getX(),at.getY(),at.getZ()),"Breaking immediately removes ward");h.succeed();
    }
    @GameTest(template="empty") public static void zombieArmsConvertOnRealLethalHit(GameTestHelper h) {
        var level=h.getLevel();var p=h.makeMockPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LegacyContent.SHINY_ARM.get()));
        var villager=EntityType.VILLAGER.create(level);villager.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,1,1))));villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(3));villager.setVillagerXp(79);
        var offers=new net.minecraft.world.item.trading.MerchantOffers();offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD,2),new ItemStack(Items.APPLE),12,3,.05F));villager.setOffers(offers);level.addFreshEntity(villager);villager.setHealth(1);
        villager.hurt(level.damageSources().playerAttack(p),10);
        var zombies=level.getEntitiesOfClass(ZombieVillager.class,new AABB(villager.blockPosition()).inflate(2));
        h.assertTrue(villager.isRemoved()&&zombies.size()==1,"One lethal arm hit converts exactly once");var zombie=zombies.getFirst();
        h.assertTrue(zombie.getVillagerData().getProfession()==VillagerProfession.LIBRARIAN&&zombie.getVillagerData().getLevel()==3&&zombie.getVillagerXp()==79,"Profession level and XP preserved");
        var tag=zombie.saveWithoutId(new net.minecraft.nbt.CompoundTag());h.assertTrue(tag.getCompound("Offers").getList("Recipes",10).size()==1,"Offers preserved");
        var horse=EntityType.HORSE.create(level);horse.setPos(villager.position());level.addFreshEntity(horse);horse.setHealth(1);horse.hurt(level.damageSources().playerAttack(p),10);
        h.assertTrue(horse.isRemoved()&&level.getEntitiesOfClass(net.minecraft.world.entity.animal.horse.ZombieHorse.class,new AABB(horse.blockPosition()).inflate(2)).size()==1,"Horse converts");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void zombieArmLootUsesOneLootingRoll(GameTestHelper h) {
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var type:List.of(EntityType.ZOMBIE,EntityType.DROWNED)) {
            var victim=type.create(level);var expected=type==EntityType.ZOMBIE?LegacyContent.ZOMBIE_ARM.get():LegacyContent.SHINY_ARM.get();
            var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("entities/"+BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath())));
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,victim).withParameter(LootContextParams.ORIGIN,victim.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(player)).withParameter(LootContextParams.ATTACKING_ENTITY,player).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,player).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).create(LootContextParamSets.ENTITY);
            for(int looting:new int[]{0,3}) {
                var sword=new ItemStack(Items.DIAMOND_SWORD);if(looting>0)sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),looting);player.setItemInHand(InteractionHand.MAIN_HAND,sword);
                var random=net.minecraft.util.RandomSource.create(73123);int count=0;
                for(int i=0;i<10000;i++){int n=table.getRandomItems(params,random).stream().filter(s->s.is(expected)).mapToInt(ItemStack::getCount).sum();h.assertTrue(n<=1,"Never duplicate a rare arm");count+=n;}
                h.assertTrue(looting==0?count>=8&&count<=38:count>=30&&count<=75,"Expected 0.2%/0.5% arm chance, got "+count);
            }
        }h.succeed();
    }
    @GameTest(template="empty") public static void dragonWeaponArmorAdditionAndProjectileOrigin(GameTestHelper h) {
        var level=h.getLevel();var p=h.makeMockPlayer(GameType.SURVIVAL);var dragon=EntityType.ENDER_DRAGON.create(level);dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.DRACO_ARCANUS_SWORD.get()));dragon.head.hurt(level.damageSources().playerAttack(p),8);close(h,dragon.getMaxHealth()-dragon.getHealth(),16,"Sword doubles dragon damage");
        var items=List.of(ModItems.DRACO_ARCANUS_HELMET.get(),ModItems.DRACO_ARCANUS_CHESTPLATE.get(),ModItems.DRACO_ARCANUS_LEGGINGS.get(),ModItems.DRACO_ARCANUS_BOOTS.get());var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
        for(int i=0;i<4;i++)p.setItemSlot(slots.get(i),new ItemStack(items.get(i)));dragon.setHealth(dragon.getMaxHealth());dragon.invulnerableTime=0;dragon.head.hurt(level.damageSources().playerAttack(p),8);close(h,dragon.getMaxHealth()-dragon.getHealth(),24,"Weapon and armor add to three times");
        var ball=new EnergyBall(level,p,1,0,0);level.addFreshEntity(ball);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.assertTrue(DracoCombat.weaponBonus(level.damageSources().indirectMagic(ball,p),p)==1,"Scepter shot remembers origin after swap");
        var arrow=EntityType.ARROW.create(level);arrow.setOwner(p);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.DRACO_ARCANUS_SCEPTER.get()));
        h.assertTrue(DracoCombat.weaponBonus(level.damageSources().arrow(arrow,p),p)==0,"Holding scepter cannot buff an ordinary arrow");h.succeed();
    }
    @GameTest(template="empty") public static void dracoArrowCloudAndEffects(GameTestHelper h) {
        var level=h.getLevel();var shooter=h.makeMockPlayer(GameType.SURVIVAL);var at=Vec3.atCenterOf(h.absolutePos(new BlockPos(1,1,1)));
        var arrow=new DracoArcanusArrow(level,shooter,new ItemStack(ModItems.DRACO_ARCANUS_ARROW.get()),new ItemStack(Items.BOW));close(h,arrow.getBaseDamage(),5,"Full-speed normal arrow has fifteen base kinetic damage");
        var target=EntityType.COW.create(level);target.setPos(at);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);level.addFreshEntity(target);
        arrow.setPos(at);arrow.setDeltaMovement(3,0,0);arrow.setCritArrow(false);
        try {var method=net.minecraft.world.entity.projectile.AbstractArrow.class.getDeclaredMethod("onHitEntity",EntityHitResult.class);method.setAccessible(true);method.invoke(arrow,new EntityHitResult(target));}catch(Exception error){throw new IllegalStateException(error);}
        close(h,100-target.getHealth(),15,"Actual full-speed unenchanted hit deals fifteen");
        var cloud=level.getEntitiesOfClass(AreaEffectCloud.class,new AABB(target.blockPosition()).inflate(3)).getFirst();close(h,cloud.getRadius(),2.5,"Larger initial cloud");close(h,cloud.getRadiusPerTick(),.015625,"Twenty-five percent more growth");h.assertTrue(cloud.getDuration()==400,"Duration unchanged");
        shooter.setPos(at);level.addFreshEntity(shooter);target.invulnerableTime=0;target.setHealth(100);cloud.setWaitTime(0);cloud.tickCount=5;cloud.tick();
        close(h,100-target.getHealth(),7.5,"Cloud damage increases exactly twenty-five percent");
        h.assertTrue(target.getEffect(MobEffects.WEAKNESS).getAmplifier()==1&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==100,"Two level-II effects refresh to five seconds");
        h.assertTrue(!shooter.hasEffect(MobEffects.WEAKNESS)&&!shooter.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Players excluded only from extra debuffs");
        target.setPos(at.add(10,0,0));for(int i=0;i<100;i++)target.tick();h.assertTrue(!target.hasEffect(MobEffects.WEAKNESS),"Debuff expires five seconds after leaving");
        h.succeed();
    }
}
