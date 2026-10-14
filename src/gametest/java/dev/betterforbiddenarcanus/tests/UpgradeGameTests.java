package dev.betterforbiddenarcanus.tests;

import com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.pedestal.effect.PedestalEffectTrigger;
import com.stal111.forbidden_arcanus.common.inventory.HephaestusForgeMenu;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import dev.betterforbiddenarcanus.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("bfa_tests")
@PrefixGameTestTemplate(false)
public final class UpgradeGameTests {
    private static final BlockPos CENTER = new BlockPos(8, 1, 8);
    private static final List<BlockPos> SIDES = List.of(CENTER.offset(-3,0,0), CENTER.offset(3,0,0), CENTER.offset(0,0,-3), CENTER.offset(0,0,3),
            CENTER.offset(-2,0,-2), CENTER.offset(2,0,-2), CENTER.offset(-2,0,2), CENTER.offset(2,0,2));
    private static ItemStack upgraded(Item item, int tier) { var s = new ItemStack(item); s.set(ForgeUpgrades.TIER, tier); return s; }
    private static void close(GameTestHelper h, double value, double expected, String why) { h.assertTrue(Math.abs(value - expected) < .003, why + ": " + value + " != " + expected); }
    private static Player player(GameTestHelper h, boolean artisan) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,1,1))));
        if (artisan) CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("relic", 0, new ItemStack(ModItems.ARTISAN_RELIC.get()));
        return p;
    }
    private static ServerPlayer starter(GameTestHelper h) {
        return new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "upgrade-test"), net.minecraft.server.level.ClientInformation.createDefault());
    }
    private static List<Item> materials(int tier) {
        return switch (tier) {
            case 1 -> List.of(Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,Items.IRON_INGOT,Items.COPPER_INGOT,Items.COPPER_INGOT,ModItems.ARCANE_CRYSTAL.get(),ModItems.ARCANE_CRYSTAL.get());
            case 2 -> List.of(ModItems.DEORUM_INGOT.get(),ModItems.DEORUM_INGOT.get(),ModItems.DEORUM_INGOT.get(),ModItems.DEORUM_INGOT.get(),Items.DIAMOND,Items.DIAMOND,ModBlocks.ARCANE_CRYSTAL_BLOCK.get().asItem(),ModBlocks.ARCANE_CRYSTAL_BLOCK.get().asItem());
            case 3 -> List.of(ModItems.OBSIDIANSTEEL_INGOT.get(),ModItems.OBSIDIANSTEEL_INGOT.get(),ModItems.OBSIDIANSTEEL_INGOT.get(),ModItems.OBSIDIANSTEEL_INGOT.get(),ModItems.DRAGON_SCALE.get(),ModItems.DRAGON_SCALE.get(),Items.NETHERITE_INGOT,Items.NETHERITE_INGOT);
            case 4 -> List.of(Items.NETHERITE_INGOT,Items.NETHERITE_INGOT,Items.NETHERITE_INGOT,Items.NETHERITE_INGOT,ModItems.DARK_NETHER_STAR.get(),ModItems.DARK_NETHER_STAR.get(),ModItems.STELLARITE_PIECE.get(),ModItems.STELLARITE_PIECE.get());
            default -> List.of(Items.NETHERITE_BLOCK,Items.NETHERITE_BLOCK,ModBlocks.STELLARITE_BLOCK.get().asItem(),ModBlocks.STELLARITE_BLOCK.get().asItem(),ModItems.DARK_NETHER_STAR.get(),ModItems.DARK_NETHER_STAR.get(),ModItems.ETERNAL_STELLA.get(),ModItems.ETERNAL_STELLA.get());
        };
    }
    private static void fill(GameTestHelper h, HephaestusForgeBlockEntity forge, int tier) {
        var materials = materials(tier);
        for (int i = 0; i < 8; i++) {
            h.setBlock(SIDES.get(i), ModBlocks.DARKSTONE_PEDESTAL.get());
            PedestalBlockEntity pedestal = h.getBlockEntity(SIDES.get(i));
            var s = new ItemStack(materials.get(i));
            pedestal.setStack(s, null, PedestalEffectTrigger.RITUAL_FINISHED); forge.updatePedestalStack(pedestal.getBlockPos(), s);
        }
    }
    private static HephaestusForgeBlockEntity forge(GameTestHelper h, int tier, ItemStack main) {
        h.setBlock(CENTER, Blocks.AIR);
        h.setBlock(CENTER, List.of(ModBlocks.HEPHAESTUS_FORGE_TIER_1.get(),ModBlocks.HEPHAESTUS_FORGE_TIER_2.get(),ModBlocks.HEPHAESTUS_FORGE_TIER_3.get(),ModBlocks.HEPHAESTUS_FORGE_TIER_4.get(),ModBlocks.HEPHAESTUS_FORGE_TIER_5.get()).get(tier-1));
        HephaestusForgeBlockEntity f = h.getBlockEntity(CENTER);
        f.setForgeLevel(HephaestusForgeLevel.values()[tier-1]);
        f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT, main);
        fill(h,f,tier);
        f.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0), new ItemStack(ModItems.ARTISAN_RELIC.get()));
        return f;
    }
    private static void finish(HephaestusForgeBlockEntity f) {
        for (int i = 0; i <= 500; i++) f.getRitualManager().tick().ifPresent(s -> f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT,s));
    }
    @GameTest(template = "guide_area")
    public static void allGradesPreserveComponentsAndRejectDowngrades(GameTestHelper h) {
        for (int tier = 1; tier <= 5; tier++) {
            var input = new ItemStack(Items.DIAMOND_PICKAXE); input.setDamageValue(123);
            input.set(DataComponents.CUSTOM_NAME,Component.literal("原名字").withStyle(net.minecraft.ChatFormatting.ITALIC));
            input.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.EFFICIENCY),3);
            if (tier > 1) input.set(ForgeUpgrades.TIER,1);
            var before = input.copy(); var f = forge(h,tier,input);
            h.assertTrue(f.getRitualManager().startRitual(starter(h),f.getEssenceManager().getStorage()),"Must start target grade " + tier);
            finish(f); var result = f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT);
            h.assertTrue(ForgeUpgrades.tier(result)==tier && result.getCount()==1,"Grade must replace, not accumulate");
            var stripped=result.copy(); stripped.remove(ForgeUpgrades.TIER); before.remove(ForgeUpgrades.TIER);
            h.assertTrue(ItemStack.matches(stripped,before),"All original components must survive");
            h.assertTrue(f.getStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0)).getCount()==1,"Relic must be retained");
            for (var pos:SIDES) h.assertTrue(!((PedestalBlockEntity)h.getBlockEntity(pos)).hasStack(),"Each side material must be consumed exactly once");
            fill(h,f,tier); h.assertTrue(f.getRitualManager().getValidRitual().isEmpty(),"Same grade must be rejected");
            var saved = result.save(h.getLevel().registryAccess()); var loaded=ItemStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)saved);
            h.assertTrue(ItemStack.matches(result,loaded),"Upgrade component must persist through save/load");
        }
        h.assertTrue(!ForgeUpgrades.canUpgrade(upgraded(Items.DIAMOND_SWORD,5),4),"Downgrade forbidden");
        for (var item:List.of(Items.ELYTRA,Items.SHIELD,Items.STONE)) h.assertTrue(!ForgeUpgrades.canUpgrade(new ItemStack(item),5),"Unsupported input must be rejected");
        h.succeed();
    }
    @GameTest(template = "guide_area")
    public static void upgradeCancellationAndReload(GameTestHelper h) {
        var f=forge(h,3,new ItemStack(Items.DIAMOND_SWORD));
        f.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0),ItemStack.EMPTY);
        h.assertTrue(f.getRitualManager().getValidRitual().isEmpty(),"Missing relic must not start");
        f.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0),new ItemStack(ModItems.ARTISAN_RELIC.get()));
        h.assertTrue(f.getRitualManager().startRitual(starter(h),f.getEssenceManager().getStorage()),"Setup start");
        for(int i=0;i<50;i++)f.getRitualManager().tick();
        f.setStack(HephaestusForgeMenu.ENHANCERS_SLOTS.getInt(0),ItemStack.EMPTY);
        finish(f);
        h.assertTrue(ForgeUpgrades.tier(f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT))==0,"Removing relic must cancel");
        for(var pos:SIDES)h.assertTrue(((PedestalBlockEntity)h.getBlockEntity(pos)).getStack().getCount()==1,"Cancelled materials must survive");
        f=forge(h,2,new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(f.getRitualManager().startRitual(starter(h),f.getEssenceManager().getStorage()),"Reload start");
        for(int i=0;i<100;i++)f.getRitualManager().tick();
        var tag=f.saveWithoutMetadata(h.getLevel().registryAccess());
        var restored=new HephaestusForgeBlockEntity(f.getBlockPos(),f.getBlockState());
        restored.setLevel(h.getLevel());restored.onLoad();restored.loadWithComponents(tag,h.getLevel().registryAccess());
        f=restored;finish(f);
        h.assertTrue(ForgeUpgrades.tier(f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT))==2,"Active ritual must resume with its snapshot");
        f=forge(h,1,new ItemStack(Items.DIAMOND_SWORD));
        h.assertTrue(f.getRitualManager().startRitual(starter(h),f.getEssenceManager().getStorage()),"Input change start");
        f.setStack(HephaestusForgeBlockEntity.MAIN_SLOT,new ItemStack(Items.IRON_SWORD)); finish(f);
        h.assertTrue(ForgeUpgrades.tier(f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT))==0,"Changing valid input must also cancel");
        f=forge(h,1,new ItemStack(Items.DIAMOND_SWORD));
        h.assertTrue(f.getRitualManager().startRitual(starter(h),f.getEssenceManager().getStorage()),"Pedestal break start");
        h.setBlock(SIDES.getFirst(),Blocks.AIR);finish(f);
        h.assertTrue(ForgeUpgrades.tier(f.getStack(HephaestusForgeBlockEntity.MAIN_SLOT))==0,"Broken pedestal must cancel despite any stale cached ingredients");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void itemStatsSpeedAndUpgradeNames(GameTestHelper h) {
        var p=player(h,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.tick();p.tick();
        double attack=p.getAttributeValue(Attributes.ATTACK_DAMAGE),speed=p.getAttributeValue(Attributes.ATTACK_SPEED);
        for(int tier=1;tier<=5;tier++) {
            var s=upgraded(Items.DIAMOND_SWORD,tier);p.setItemInHand(InteractionHand.MAIN_HAND,s);p.tick();p.tick();
            close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),attack*ForgeUpgrades.multiplier(s),"Attack grade");
            close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),speed*ForgeUpgrades.multiplier(s),"Negative weapon speed must become faster");
            h.assertTrue(s.getHoverName().getStyle().getColor().getValue()==ForgeUpgrades.color(s).getColor(),"Grade name color");
        }
        p.setItemSlot(EquipmentSlot.CHEST,upgraded(Items.NETHERITE_CHESTPLATE,5));p.tick();p.tick();
        close(h,p.getAttributeValue(Attributes.ARMOR),11.2,"Armor must scale its own value");
        close(h,p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),4.2,"Toughness grade");
        close(h,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),.14,"Knockback grade");
        p.setItemInHand(InteractionHand.MAIN_HAND,upgraded(Items.DIAMOND_PICKAXE,5));
        var event=new PlayerEvent.BreakSpeed(p,Blocks.STONE.defaultBlockState(),10,BlockPos.ZERO);UpgradeEffects.mining(event);close(h,event.getNewSpeed(),14,"Mining grade");
        p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.tick();p.tick();
        close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),speed,"Removal must restore speed");close(h,p.getAttributeValue(Attributes.ARMOR),0,"Removal must restore armor");
        var modified = new ItemStack(Items.DIAMOND_SWORD);
        modified.set(DataComponents.ATTRIBUTE_MODIFIERS,modified.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY)
                .withModifierAdded(Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(SoulItems.id("test_other_mod"),.2,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND));
        p.setItemInHand(InteractionHand.MAIN_HAND,modified);p.tick();p.tick();var customBaseline=p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        var improved=modified.copy();improved.set(ForgeUpgrades.TIER,5);p.setItemInHand(InteractionHand.MAIN_HAND,improved);p.tick();p.tick();
        close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),customBaseline*1.4,"Other mod percentage modifiers must not be counted twice");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void anvilDiscountCostGateAndActualCharge(GameTestHelper h) {
        var p=player(h,true);p.experienceLevel=100;
        for(int repairCost:List.of(4,40,78)) {
            var menu=new AnvilMenu(1,p.getInventory());var input=upgraded(Items.DIAMOND_PICKAXE,2);input.setDamageValue(100);input.set(DataComponents.REPAIR_COST,repairCost);
            menu.getSlot(0).set(input);menu.getSlot(1).set(new ItemStack(Items.DIAMOND,2));menu.createResult();
            int expected=ArtisanEffects.discountedCost(repairCost+1);
            h.assertTrue(menu.getCost()==expected && menu.getSlot(2).hasItem()==(expected<40),"Discount before 40-level gate " + repairCost);
            if(expected<40) {
                var output=menu.getSlot(2).getItem().copy();int before=p.experienceLevel;menu.getSlot(2).onTake(p,output);
                h.assertTrue(p.experienceLevel==before-expected && menu.getSlot(1).getItem().getCount()==1,"Charge displayed level cost and consume one repair material");
                h.assertTrue(ForgeUpgrades.tier(output)==2 && output.getOrDefault(DataComponents.REPAIR_COST,0)==repairCost*2+1,"Grade and original work penalty must survive");
            }
        }
        close(h,CommonHooks.onAnvilRepair(p,new ItemStack(Items.IRON_SWORD),ItemStack.EMPTY,ItemStack.EMPTY),.09,"Anvil damage probability");
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("relic",0,ItemStack.EMPTY);
        close(h,CommonHooks.onAnvilRepair(p,new ItemStack(Items.IRON_SWORD),ItemStack.EMPTY,ItemStack.EMPTY),.12,"Unequip restores native probability");
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("relic",0,new ItemStack(ModItems.ARTISAN_RELIC.get()));
        var pos=h.absolutePos(new BlockPos(1,1,1));p.getRandom().setSeed(951123);int damaged=0;
        for(int i=0;i<1000;i++) {
            h.getLevel().setBlockAndUpdate(pos,Blocks.ANVIL.defaultBlockState());p.experienceLevel=10;
            var actual=new AnvilMenu(1,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));
            var input=new ItemStack(Items.IRON_SWORD);input.setDamageValue(20);
            actual.getSlot(0).set(input);actual.getSlot(1).set(new ItemStack(Items.IRON_INGOT));actual.createResult();
            actual.getSlot(2).onTake(p,actual.getSlot(2).getItem().copy());
            if(!h.getLevel().getBlockState(pos).is(Blocks.ANVIL))damaged++;
        }
        h.assertTrue(damaged>=60&&damaged<=120,"Actual fixed-seed anvil damage near 9%: "+damaged);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void smithingTemplatesUseOneServerRollAndPreserveGrade(GameTestHelper h) {
        var p=player(h,true);p.getRandom().setSeed(831482);
        var menu=new SmithingMenu(1,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),h.absolutePos(new BlockPos(1,1,1))));
        int retained=0;
        for(int i=0;i<1200;i++) {
            menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            menu.getSlot(1).set(upgraded(Items.DIAMOND_SWORD,3));menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));menu.createResult();
            var out=menu.getSlot(3).getItem().copy();h.assertTrue(out.is(Items.NETHERITE_SWORD)&&ForgeUpgrades.tier(out)==3,"Standard smithing must retain grade");
            menu.getSlot(3).onTake(p,out);
            if(!menu.getSlot(0).getItem().isEmpty())retained++;
            h.assertTrue(menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().isEmpty(),"Base and addition consumed regardless of template roll");
        }
        h.assertTrue(retained>260&&retained<340,"Actual fixed-seed smithing retention near 25%: "+retained);
        p.getInventory().clearContent();menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,4));
        menu.getSlot(1).set(upgraded(Items.DIAMOND_SWORD,4));menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT,4));menu.createResult();
        h.assertTrue(!menu.quickMoveStack(p,3).isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().getCount()==3,"Quick take consumes base exactly once");
        h.assertTrue(p.getInventory().items.stream().filter(s->s.is(Items.NETHERITE_SWORD)).mapToInt(ItemStack::getCount).sum()==1,"Quick take cannot duplicate result");
        menu.getSlot(0).set(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE));menu.getSlot(1).set(upgraded(Items.IRON_CHESTPLATE,3));
        menu.getSlot(2).set(new ItemStack(Items.GOLD_INGOT));menu.createResult();
        var trim=menu.getSlot(3).getItem().copy();h.assertTrue(trim.has(DataComponents.TRIM)&&ForgeUpgrades.tier(trim)==3,"Armor trim must preserve upgrade");
        menu.getSlot(3).onTake(p,trim);h.assertTrue(menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().isEmpty(),"Trim consumes base and addition");
        h.succeed();
    }
    private static void hit(AbstractArrow arrow, net.minecraft.world.entity.Entity victim) {
        try { var method=AbstractArrow.class.getDeclaredMethod("onHitEntity",EntityHitResult.class);method.setAccessible(true);method.invoke(arrow,new EntityHitResult(victim)); }
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    @GameTest(template = "empty")
    public static void realBowAndCrossbowVelocityDamageAndCharge(GameTestHelper h) {
        var p=player(h,false);var bow=upgraded(Items.BOW,5);p.setItemInHand(InteractionHand.MAIN_HAND,bow);p.getInventory().add(new ItemStack(Items.ARROW,32));p.startUsingItem(InteractionHand.MAIN_HAND);
        Items.BOW.releaseUsing(bow,h.getLevel(),p,72000-15);
        var arrows=h.getEntities(EntityType.ARROW);h.assertTrue(arrows.size()==1,"Fast draw must shoot once");var arrow=arrows.getFirst();
        h.assertTrue(Math.abs(arrow.getDeltaMovement().length()-4.2)<.15,"Bow velocity must be 4.2 with vanilla launch spread");
        arrow.setCritArrow(false);arrow.setBaseDamage(2);arrow.setDeltaMovement(0,0,4.2);
        var cow=EntityType.COW.create(h.getLevel());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);hit(arrow,cow);
        close(h,cow.getMaxHealth()-cow.getHealth(),8.4,"Final damage +40%, not +96%; changing hand does not remove it");
        h.killAllEntities();
        var cross=upgraded(Items.CROSSBOW,5);p.setItemInHand(InteractionHand.MAIN_HAND,cross);
        h.assertTrue(CrossbowItem.getChargeDuration(cross,p)==18,"Base crossbow charge reduced from 25 to 18 ticks");
        cross.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.QUICK_CHARGE),3);
        h.assertTrue(CrossbowItem.getChargeDuration(cross,p)==8,"Quick Charge must combine with grade");
        cross.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.of(new ItemStack(Items.ARROW)));
        Items.CROSSBOW.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        var bolt=h.getEntities(EntityType.ARROW).getFirst();h.assertTrue(Math.abs(bolt.getDeltaMovement().length()-3.15*1.4)<.15,"Crossbow projectile speed with native spread");
        var saved=new net.minecraft.nbt.CompoundTag();bolt.saveWithoutId(saved);var loaded=EntityType.ARROW.create(h.getLevel());loaded.load(saved);
        close(h,loaded.getPersistentData().getDouble("bfa_forge_projectile_multiplier"),1.4,"Projectile grade snapshot persists");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void crossbowFireworksKeepIndependentDamageBonus(GameTestHelper h) {
        var p=player(h,false);var cross=upgraded(Items.CROSSBOW,5);p.setItemInHand(InteractionHand.MAIN_HAND,cross);
        var rocketStack=new ItemStack(Items.FIREWORK_ROCKET);
        rocketStack.set(DataComponents.FIREWORKS,new net.minecraft.world.item.component.Fireworks(1,List.of(
                new net.minecraft.world.item.component.FireworkExplosion(net.minecraft.world.item.component.FireworkExplosion.Shape.SMALL_BALL,
                        it.unimi.dsi.fastutil.ints.IntList.of(0xff0000),it.unimi.dsi.fastutil.ints.IntList.of(),false,false))));
        cross.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.of(rocketStack));Items.CROSSBOW.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        var rocket=h.getEntities(EntityType.FIREWORK_ROCKET).getFirst();
        close(h,rocket.getPersistentData().getDouble("bfa_forge_projectile_multiplier"),1.4,"Firework must snapshot grade");
        var cow=h.spawn(EntityType.COW,new BlockPos(1,1,1));cow.setPos(rocket.position());
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        try {var method=net.minecraft.world.entity.projectile.FireworkRocketEntity.class.getDeclaredMethod("dealExplosionDamage");method.setAccessible(true);method.invoke(rocket);}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        close(h,cow.getMaxHealth()-cow.getHealth(),9.8,"Firework damage must receive one 40% bonus");h.succeed();
    }
}
