package dev.betterforbiddenarcanus.tests;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.entity.projectile.EnergyBall;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("bfa_tests") @PrefixGameTestTemplate(false)
public final class ScepterGameTests {
    @GameTest(template="guide_area") public static void ordinaryShotCostsOneAndKeepsCharge(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(8,1,8))));
        var stack=new ItemStack(ModItems.DRACO_ARCANUS_SCEPTER.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var energy=EssenceHelper.getEssenceProvider(p).orElseThrow();energy.setAmount(EssenceType.AUREAL,2);
        stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(stack.getUseDuration(p)==30,"Original thirty tick charge");
        stack.finishUsingItem(h.getLevel(),p);h.assertTrue(energy.getAmount(EssenceType.AUREAL)==1,"Exactly one Aureal per ordinary shot");
        h.assertTrue(h.getLevel().getEntitiesOfClass(EnergyBall.class,p.getBoundingBox().inflate(2)).size()==1,"Original energy ball spawned");
        energy.setAmount(EssenceType.AUREAL,0);p.stopUsingItem();stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(!p.isUsingItem(),"Empty energy cannot start");stack.finishUsingItem(h.getLevel(),p);
        h.assertTrue(energy.getAmount(EssenceType.AUREAL)==0&&h.getLevel().getEntitiesOfClass(EnergyBall.class,p.getBoundingBox().inflate(2)).size()==1,"Cannot force free shot or negative energy");h.succeed();
    }
    private static <T extends Mob> T spawn(GameTestHelper h,EntityType<T> type,Vec3 pos) {
        var mob=type.create(h.getLevel());mob.setPos(pos);mob.setNoAi(true);mob.setNoGravity(true);h.getLevel().addFreshEntity(mob);return mob;
    }
    @GameTest(template="guide_area") public static void sustainedStormHeightRadiusEnemiesAndSharedCost(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(8,1,8))));
        var stack=new ItemStack(ModItems.DRACO_ARCANUS_SCEPTER.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setShiftKeyDown(true);
        var energy=EssenceHelper.getEssenceProvider(p).orElseThrow();energy.setAmount(EssenceType.AUREAL,10);
        var phantom=spawn(h,EntityType.PHANTOM,p.position().add(0,3,0));float hp=phantom.getHealth();
        var low=spawn(h,EntityType.PHANTOM,p.position().add(1,-.1,0));float lowHp=low.getHealth();
        var distant=spawn(h,EntityType.PHANTOM,p.position().add(0,4.1,0));float farHp=distant.getHealth();
        var cow=spawn(h,EntityType.COW,p.position().add(1,1,0));float cowHp=cow.getHealth();
        var wolf=spawn(h,EntityType.WOLF,p.position().add(2,1,0));wolf.setTarget(p);float wolfHp=wolf.getHealth();
        var pet=spawn(h,EntityType.WOLF,p.position().add(-1,1,0));pet.setTame(true,true);pet.setOwnerUUID(p.getUUID());pet.setTarget(p);float petHp=pet.getHealth();
        var drop=new ItemEntity(h.getLevel(),phantom.getX(),phantom.getY(),phantom.getZ(),new ItemStack(Items.DIAMOND));h.getLevel().addFreshEntity(drop);
        stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.isUsingItem()&&stack.getUseDuration(p)==72000,"Sneak right click starts sustained mode");
        stack.getItem().onUseTick(h.getLevel(),p,stack,71999);
        h.assertTrue(Math.abs(phantom.getHealth()-(hp-5))<.01&&Math.abs(wolf.getHealth()-(wolfHp-5))<.01,"Above hostile and attacking neutral each take five damage");
        h.assertTrue(low.getHealth()==lowHp&&distant.getHealth()==farHp&&cow.getHealth()==cowHp&&pet.getHealth()==petHp,"Height, spherical range, neutral and pet exclusions");
        h.assertTrue(energy.getAmount(EssenceType.AUREAL)==5,"Cost is five per pulse, independent of target count");
        stack.getItem().onUseTick(h.getLevel(),p,stack,71998);h.assertTrue(energy.getAmount(EssenceType.AUREAL)==5,"One pulse per twenty ticks");
        var bolts=h.getLevel().getEntitiesOfClass(ScepterStorm.SilentBolt.class,p.getBoundingBox().inflate(5));h.assertTrue(bolts.size()==2,"Visual bolts only for selected targets");
        for(var bolt:bolts)for(int i=0;i<7;i++)bolt.tick();h.assertTrue(drop.isAlive()&&drop.getItem().is(Items.DIAMOND),"Bolt cannot destroy adjacent drops");
        h.assertTrue(phantom.getHealth()==hp-5,"Visual lightning cannot apply damage twice");
        p.setShiftKeyDown(false);stack.getItem().onUseTick(h.getLevel(),p,stack,71997);h.assertTrue(!p.isUsingItem()&&!ScepterStorm.channeling(p),"Releasing sneak stops immediately");
        p.getPersistentData().putLong("bfa_scepter_next_pulse",0);energy.setAmount(EssenceType.AUREAL,4);p.setShiftKeyDown(true);
        stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(!p.isUsingItem(),"Insufficient energy cannot restart storm");
        energy.setAmount(EssenceType.AUREAL,5);stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);energy.setAmount(EssenceType.AUREAL,0);stack.getItem().onUseTick(h.getLevel(),p,stack,71999);
        h.assertTrue(!p.isUsingItem()&&energy.getAmount(EssenceType.AUREAL)==0,"Exhaustion stops without going negative");
        phantom.discard();low.discard();distant.discard();cow.discard();wolf.discard();pet.discard();drop.discard();h.succeed();
    }
}
