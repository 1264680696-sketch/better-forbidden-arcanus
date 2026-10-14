package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class TyrGaleGameTests {
    private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final List<Item> ARMOR = List.of(ModItems.TYR_HELMET.get(),ModItems.TYR_CHESTPLATE.get(),ModItems.TYR_LEGGINGS.get(),ModItems.TYR_BOOTS.get());
    private static Player player(GameTestHelper h, int pieces) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(8,1,8))));equip(p,pieces);return p;
    }
    private static void equip(Player p,int pieces) { for (int i=0;i<4;i++) p.setItemSlot(SLOTS.get(i),i<pieces?new ItemStack(ARMOR.get(i)):ItemStack.EMPTY); }
    private static void close(GameTestHelper h,double actual,double expected,String why) { h.assertTrue(Math.abs(actual-expected)<.003,why+": "+actual+" != "+expected); }
    private static LivingEntity target(GameTestHelper h,double health) {
        var mob=EntityType.COW.create(h.getLevel());mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth((float)health);return mob;
    }
    private static double hit(LivingEntity target,DamageSource source,float damage) {
        target.invulnerableTime=0;float before=target.getHealth();target.hurt(source,damage);return before-target.getHealth();
    }
    @GameTest(template="guide_area") public static void valorSourcesCapsAndAttributes(GameTestHelper h) {
        var p=player(h,1);
        double speed=p.getAttributeValue(Attributes.ATTACK_SPEED),walk=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        p.getAttribute(Attributes.ARMOR).setBaseValue(10);p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(4);
        var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setPos(p.position().add(2,0,0));h.getLevel().addFreshEntity(zombie);
        var wolf=EntityType.WOLF.create(h.getLevel());wolf.setPos(p.position().add(3,0,0));wolf.setTarget(p);h.getLevel().addFreshEntity(wolf);
        var pet=EntityType.WOLF.create(h.getLevel());pet.setPos(p.position().add(4,0,0));pet.setTame(true,true);pet.setOwnerUUID(p.getUUID());pet.setTarget(p);h.getLevel().addFreshEntity(pet);
        var neutral=EntityType.PIG.create(h.getLevel());neutral.setPos(p.position().add(5,0,0));h.getLevel().addFreshEntity(neutral);
        var distant=EntityType.ZOMBIE.create(h.getLevel());distant.setPos(p.position().add(16.1,0,0));h.getLevel().addFreshEntity(distant);
        p.addEffect(new MobEffectInstance(MobEffects.POISON,200,2));p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,100));
        close(h,TyrCombat.enemies(p,true),2,"Only hostile mob and attacking neutral count");
        TyrCombat.update(p);close(h,TyrCombat.valor(p),1.32,"Status and enemies multiply");
        close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),speed*1.32,"Attack speed");
        close(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),walk*1.32,"Movement speed");
        close(h,p.getAttributeValue(Attributes.ARMOR),13.2,"Armor");close(h,p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),5.28,"Toughness");
        p.addEffect(new MobEffectInstance(MobEffects.POISON,400,5));close(h,TyrCombat.harmfulEffects(p),2,"Amplifier does not add a type");
        for(var effect:List.of(MobEffects.WITHER,MobEffects.HUNGER,MobEffects.WEAKNESS,MobEffects.UNLUCK))p.addEffect(new MobEffectInstance(effect,200));
        for(int i=0;i<10;i++){var m=EntityType.ZOMBIE.create(h.getLevel());m.setPos(p.position().add(2,i*.1,2));h.getLevel().addFreshEntity(m);}
        TyrCombat.enemies(p,true);TyrCombat.update(p);close(h,TyrCombat.valor(p),2.25,"Independent 50% caps");
        p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);TyrCombat.update(p);close(h,p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),0,"Zero remains zero");
        equip(p,0);TyrCombat.update(p);close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),speed,"Unequip removes modifiers");
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.TYR_BOOTS.get()));close(h,TyrArmor.count(p),0,"Wrong armor slot must not count");
        zombie.discard();wolf.discard();pet.discard();neutral.discard();distant.discard();h.succeed();
    }
    @GameTest(template="guide_area") public static void justiceAndAllAttributedRangedDamage(GameTestHelper h) {
        var p=player(h,2);p.addEffect(new MobEffectInstance(MobEffects.POISON,200));
        var level=h.getLevel();var mob=target(h,40);
        var arrow=EntityType.ARROW.create(level);arrow.setOwner(p);
        var trident=EntityType.TRIDENT.create(level);trident.setOwner(p);
        var ball=EntityType.SNOWBALL.create(level);ball.setOwner(p);
        var rocket=new FireworkRocketEntity(level,new ItemStack(Items.FIREWORK_ROCKET),p,0,0,0,true);
        var sources=List.of(level.damageSources().playerAttack(p),level.damageSources().arrow(arrow,p),
                level.damageSources().trident(trident,p),level.damageSources().thrown(ball,p),level.damageSources().fireworks(rocket,p));
        for(var source:sources){mob.setHealth(40);close(h,hit(mob,source,4),6.6,"Ranged and melee share valor and capped justice once");}
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);mob.setHealth(20);close(h,hit(mob,sources.getFirst(),4),4.4,"Equal max health");
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10);mob.setHealth(10);close(h,hit(mob,sources.get(1),4),2.2,"Half health lowers outgoing damage by 50%");
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);mob.setHealth(5);close(h,TyrCombat.justice(p,mob),1.5,"Current health does not affect justice");
        equip(p,1);mob.setHealth(30);close(h,hit(mob,sources.getFirst(),4),4.4,"One piece has valor but no justice");
        equip(p,2);CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("relic",0,new ItemStack(ModItems.MALEDICTUS_PACT.get()));
        mob.setHealth(30);close(h,hit(mob,sources.get(2),4),9.24,"Cursed pact independently multiplies");
        mob.setHealth(30);close(h,hit(mob,level.damageSources().arrow(arrow,null),4),4,"Unattributed damage remains unchanged");
        h.succeed();
    }
    @GameTest(template="guide_area") public static void avatarProjectileAndShieldRestrictions(GameTestHelper h) {
        var p=player(h,4);var arrow=EntityType.ARROW.create(h.getLevel());
        close(h,hit(p,h.getLevel().damageSources().arrow(arrow,null),8),3.2,"Full set projectile reduction");
        p.setHealth(20);close(h,hit(p,h.getLevel().damageSources().generic(),8),8,"No unrelated damage reduction");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        p.startUsingItem(InteractionHand.OFF_HAND);h.assertTrue(!p.isUsingItem(),"Cannot start shield blocking");
        TyrCombat.update(p);h.assertTrue(p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty(),"Both shields stowed");
        close(h,p.getInventory().items.stream().filter(s->s.is(Items.SHIELD)).mapToInt(ItemStack::getCount).sum(),2,"Neither shield deleted");
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));TyrCombat.update(p);
        h.assertTrue(p.getOffhandItem().isEmpty(),"Full backpack must remove shield from hand");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(2),e->e.getItem().is(Items.SHIELD)).isEmpty(),"Full backpack drops shield intact");
        equip(p,3);p.setHealth(20);close(h,hit(p,h.getLevel().damageSources().arrow(arrow,null),8),8,"Three pieces have no avatar protection");h.succeed();
    }
    @GameTest(template="guide_area") public static void avatarHalfHeartAbsorptionSoundAndCounter(GameTestHelper h) {
        var p=player(h,4);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);p.setAbsorptionAmount(30);
        hit(p,h.getLevel().damageSources().generic(),25);
        h.assertTrue(!p.getPersistentData().contains(TyrCombat.COOLDOWN)&&p.getHealth()==20,"Absorption prevents premature guard");
        p.setAbsorptionAmount(3);p.invulnerableTime=0;
        var sounds=new AtomicInteger();Consumer<PlayLevelSoundEvent.AtPosition> listener=e->{if(e.getLevel()==h.getLevel()&&e.getSound()!=null&&e.getSound().value()==net.minecraft.sounds.SoundEvents.TOTEM_USE)sounds.incrementAndGet();};
        NeoForge.EVENT_BUS.addListener(listener);
        try{hit(p,h.getLevel().damageSources().generic(),100);}finally{NeoForge.EVENT_BUS.unregister(listener);}
        close(h,p.getHealth(),1,"Lethal attack leaves half a heart");close(h,p.getAbsorptionAmount(),0,"Absorption consumed normally");
        close(h,sounds.get(),1,"One totem-use sound");
        close(h,p.getPersistentData().getLong(TyrCombat.COOLDOWN)-TyrCombat.clock(p),2400,"Exact persisted cooldown");
        h.assertTrue(p.getPersistentData().getBoolean(TyrCombat.COUNTER)&&p.getActiveEffects().isEmpty(),"Pending counter without potion buffs");
        var mob=target(h,20);mob.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);mob.setAbsorptionAmount(20);
        var arrow=EntityType.ARROW.create(h.getLevel());arrow.setOwner(p);var source=h.getLevel().damageSources().arrow(arrow,p);
        close(h,hit(mob,source,2),0,"Fully absorbed counter causes no health damage");h.assertTrue(p.getPersistentData().getBoolean(TyrCombat.COUNTER),"Fully absorbed hit retains counter");
        mob.setAbsorptionAmount(0);close(h,hit(mob,source,2),10,"First actual ranged health hit has five times final damage");
        h.assertTrue(!p.getPersistentData().getBoolean(TyrCombat.COUNTER),"Counter consumed once");close(h,hit(mob,source,2),2,"Later piercing/area hits are ordinary damage");
        p.setHealth(20);hit(p,h.getLevel().damageSources().generic(),100);h.assertTrue(p.getHealth()<=0,"Cooldown must permit the next lethal damage");h.succeed();
    }
    @GameTest(template="guide_area") public static void avatarCooldownCloneExpiryAndBypass(GameTestHelper h) {
        var old=player(h,4);old.getPersistentData().putLong(TyrCombat.COOLDOWN,TyrCombat.clock(old)+2);old.getPersistentData().putBoolean(TyrCombat.COUNTER,true);
        var copy=player(h,4);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(copy,old,true));
        h.assertTrue(copy.getPersistentData().getLong(TyrCombat.COOLDOWN)==old.getPersistentData().getLong(TyrCombat.COOLDOWN)&&!copy.getPersistentData().getBoolean(TyrCombat.COUNTER),"Death clone retains cooldown, clears counter");
        copy.getPersistentData().putBoolean(TyrCombat.COUNTER,true);equip(copy,3);TyrCombat.update(copy);h.assertTrue(!copy.getPersistentData().getBoolean(TyrCombat.COUNTER),"Losing full set clears pending counter");equip(copy,4);
        var bypass=player(h,4);hit(bypass,h.getLevel().damageSources().fellOutOfWorld(),1000);h.assertTrue(!bypass.getPersistentData().contains(TyrCombat.COOLDOWN),"Void must bypass avatar");
        h.runAfterDelay(2,()->{hit(copy,h.getLevel().damageSources().generic(),100);close(h,copy.getHealth(),1,"Ready at exact expiry tick");h.succeed();});
    }
    @GameTest(template="guide_area") public static void avatarSameTickUnequipAndBlockedCounter(GameTestHelper h) {
        var p=player(h,4);p.getPersistentData().putBoolean(TyrCombat.COUNTER,true);
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);equip(p,4);
        h.assertTrue(!p.getPersistentData().getBoolean(TyrCombat.COUNTER),"Same-tick direct unequip/re-equip cannot retain counter");
        p.getPersistentData().putBoolean(TyrCombat.COUNTER,true);
        p.getInventory().setItem(36,ItemStack.EMPTY);p.getInventory().setItem(36,new ItemStack(ModItems.TYR_BOOTS.get()));
        h.assertTrue(!p.getPersistentData().getBoolean(TyrCombat.COUNTER),"Same-tick menu armor changes clear counter before restore");
        p.getPersistentData().putBoolean(TyrCombat.COUNTER,true);
        var defender=player(h,0);defender.setPos(p.position().add(0,0,2));defender.setYRot(180);
        defender.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));defender.startUsingItem(InteractionHand.OFF_HAND);
        for(int i=0;i<6;i++)defender.tick();
        var arrow=EntityType.ARROW.create(h.getLevel());arrow.setPos(p.position());arrow.setOwner(p);
        close(h,hit(defender,h.getLevel().damageSources().arrow(arrow,p),2),0,"Actual shield-blocked arrow causes no life damage");
        h.assertTrue(p.getPersistentData().getBoolean(TyrCombat.COUNTER),"Actual shield block retains pending counter");h.succeed();
    }
    private static ItemStack gale(GameTestHelper h,ItemStack base) {
        var recipe=(SmithingRecipe)h.getLevel().getRecipeManager().byKey(SoulItems.id("smithing/apply_gale_modifier")).orElseThrow().value();
        var input=new SmithingRecipeInput(new ItemStack(ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()),base,new ItemStack(ModItems.WHIRLWIND_PRISM.get()));
        h.assertTrue(recipe.matches(input,h.getLevel()),"Gale recipe matches original mace");return recipe.assemble(input,h.getLevel().registryAccess());
    }
    @GameTest(template="guide_area") public static void galeSmithingRestrictionsComponentsAndConsumption(GameTestHelper h) {
        var base=new ItemStack(Items.MACE);base.setDamageValue(45);base.set(ForgeUpgrades.TIER,5);
        base.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.DENSITY),3);
        var upgraded=gale(h,base);h.assertTrue(GaleMace.isGale(upgraded)&&upgraded.getDamageValue()==45&&ForgeUpgrades.tier(upgraded)==5&&upgraded.getEnchantments().equals(base.getEnchantments()),"Preserve all equipment components");
        h.assertTrue(upgraded.getHoverName().getString().contains("Gale")||upgraded.getHoverName().getString().contains("疾风"),"Modifier gives name prefix");
        var custom=base.copy();custom.set(DataComponents.CUSTOM_NAME,Component.literal("原重锤"));h.assertTrue(gale(h,custom).getHoverName().getString().equals("原重锤"),"Custom name preserved by native rules");
        var recipe=(SmithingRecipe)h.getLevel().getRecipeManager().byKey(SoulItems.id("smithing/apply_gale_modifier")).orElseThrow().value();
        h.assertTrue(!recipe.matches(new SmithingRecipeInput(new ItemStack(ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()),upgraded,new ItemStack(ModItems.WHIRLWIND_PRISM.get())),h.getLevel()),"No repeated or overwritten modifier");
        h.assertTrue(!recipe.isBaseIngredient(new ItemStack(Items.DIAMOND_SWORD)),"Only vanilla mace supported");
        var p=player(h,0);h.setBlock(new BlockPos(8,0,8),Blocks.SMITHING_TABLE);
        var menu=new SmithingMenu(1,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),h.absolutePos(new BlockPos(8,0,8))));
        menu.getSlot(0).set(new ItemStack(ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()));menu.getSlot(1).set(base.copy());menu.getSlot(2).set(new ItemStack(ModItems.WHIRLWIND_PRISM.get()));menu.createResult();
        h.assertTrue(GaleMace.isGale(menu.getSlot(3).getItem()),"Real menu result is gale mace");var result=menu.getSlot(3).remove(1);menu.getSlot(3).onTake(p,result);
        h.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().isEmpty(),"Real taking consumes all three inputs once");
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("relic",0,new ItemStack(ModItems.ARTISAN_RELIC.get()));p.getRandom().setSeed(127);
        // The existing fixed-seed smithing tests already verify the generic 25% branch;
        // this integration confirms this mod recipe goes through that same menu hook.
        menu.getSlot(0).set(new ItemStack(ModItems.APPLY_MODIFIER_SMITHING_TEMPLATE.get()));menu.getSlot(1).set(base.copy());menu.getSlot(2).set(new ItemStack(ModItems.WHIRLWIND_PRISM.get()));menu.createResult();
        result=menu.getSlot(3).remove(1);menu.getSlot(3).onTake(p,result);
        h.assertTrue(menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().isEmpty(),"Artisan never preserves mace or prism");
        var saved=upgraded.save(h.getLevel().registryAccess());h.assertTrue(ItemStack.matches(upgraded,ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)saved)),"Gale holder and tier survive serialization");h.succeed();
    }
    private static void click(GameTestHelper h,Player p,InteractionHand hand,BlockPos pos,Direction face) {
        var absolute=h.absolutePos(pos);CommonHooks.onRightClickBlock(p,hand,absolute,new BlockHitResult(Vec3.atCenterOf(absolute),face,absolute,false));
    }
    @GameTest(template="guide_area") public static void galeImpulseSharedCooldownAndLanding(GameTestHelper h) {
        var p=player(h,0);var pos=new BlockPos(8,0,8);h.setBlock(pos,Blocks.STONE);
        var stack=gale(h,new ItemStack(Items.MACE));stack.setDamageValue(25);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.setDeltaMovement(.2,-.3,.4);click(h,p,InteractionHand.MAIN_HAND,pos,Direction.UP);
        close(h,p.getDeltaMovement().y,1.2,"Exact launch speed");close(h,p.getDeltaMovement().x,.2,"Horizontal preserved");close(h,stack.getDamageValue(),25,"No durability cost");
        h.assertTrue(p.isIgnoringFallDamageFromCurrentImpulse()&&h.getBlockState(pos).is(Blocks.STONE),"Wind landing context and unchanged ground");
        p.setDeltaMovement(0,0,0);p.setItemInHand(InteractionHand.OFF_HAND,stack.copy());click(h,p,InteractionHand.OFF_HAND,pos,Direction.UP);close(h,p.getDeltaMovement().y,0,"Shared cooldown after changing hand");
        p.getPersistentData().putLong(GaleMace.COOLDOWN,TyrCombat.clock(p));click(h,p,InteractionHand.OFF_HAND,pos,Direction.NORTH);close(h,p.getDeltaMovement().y,0,"No wall launch");
        click(h,p,InteractionHand.OFF_HAND,pos,Direction.UP);close(h,p.getDeltaMovement().y,1.2,"Offhand launch after cooldown");
        p.causeFallDamage(8,1,h.getLevel().damageSources().fall());close(h,p.getHealth(),20,"Return to launch height has no extra fall damage");
        p.setPos(p.position().add(0,-5,0));p.invulnerableTime=0;p.causeFallDamage(8,1,h.getLevel().damageSources().fall());close(h,p.getHealth(),18,"Lower landing counts only five block height difference");
        p.fallDistance=6;p.setDeltaMovement(0,-.3,0);h.assertTrue(MaceItem.canSmashAttack(p),"Vanilla mace smash eligibility remains");h.succeed();
    }
}
