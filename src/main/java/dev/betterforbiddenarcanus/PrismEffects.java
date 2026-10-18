package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.item.modifier.ItemModifier;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import dev.betterforbiddenarcanus.legacy.LegacyContent;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.*;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID)
public final class PrismEffects {
    public static final int FIRE=1, SEA=2, WIND=4, EARTH=8;
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> PRISMS=
        ForgeUpgrades.COMPONENTS.register("runic_prisms",()->DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.intRange(0,15)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
    private record Critical(Entity target,long tick) {}
    private static final Map<Player,Critical> CRITICAL=new WeakHashMap<>();
    private static final ThreadLocal<Boolean> SECONDARY=ThreadLocal.withInitial(()->false);
    public static void init() {}
    public static int bit(ItemModifier modifier) {
        var name=modifier.displaySettings().name();
        if(!(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key))return 0;
        return switch(key.getKey()) {
            case "modifier.forbidden_arcanus.fiery" -> FIRE;
            case "modifier.forbidden_arcanus.aquatic" -> SEA;
            case "modifier.forbidden_arcanus.demolishing" -> EARTH;
            // Only gale_legs uses this translated title as an equipment prism.
            case "modifier.better_forbidden_arcanus.gale_legs" -> WIND;
            default -> 0;
        };
    }
    public static int bit(Holder<ItemModifier> modifier) {
        if(modifier.is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus","fiery")))return FIRE;
        if(modifier.is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus","aquatic")))return SEA;
        if(modifier.is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus","demolishing")))return EARTH;
        return modifier.is(SoulItems.id("gale_legs"))?WIND:0;
    }
    public static int bits(ItemStack stack) {
        int bits=stack.getOrDefault(PRISMS,0);
        var nativeModifier=stack.get(ModDataComponents.ITEM_MODIFIER);
        return bits|(nativeModifier==null?0:bit(nativeModifier));
    }
    public static boolean axe(ItemStack stack){return stack.is(LegacyContent.AXE.get());}
    public static boolean has(ItemStack stack,int bit){return (bits(stack)&bit)!=0;}
    public static boolean slot(Player p,EquipmentSlot slot,int bit){return has(p.getItemBySlot(slot),bit)&&p.getItemBySlot(slot).getItem() instanceof ArmorItem a&&a.getEquipmentSlot()==slot;}
    public static boolean extraBase(ItemStack stack,int bit){
        return switch(bit){case EARTH->stack.getItem() instanceof SwordItem||stack.is(ItemTags.SWORDS)||stack.getItem() instanceof BowItem;
            case SEA->stack.getItem() instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.CHEST;
            case WIND->stack.getItem() instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.LEGS;default->false;};
    }
    public static ItemStack applyAxe(ItemStack input,int bit){
        var out=input.copyWithCount(1);int mask=bits(out)|bit;
        out.set(PRISMS,mask);out.remove(ModDataComponents.ITEM_MODIFIER);out.remove(DataComponents.ITEM_NAME);
        if(mask==15)out.set(DataComponents.UNBREAKABLE,new Unbreakable(true));
        return out;
    }
    public static Component axeName(ItemStack stack){
        if(!axe(stack)||bits(stack)==0)return null;
        if(bits(stack)==15){
            var result=Component.empty();String[] chars={"符","文","战","斧"};int[] colors={0xFF8F00,0x5A82F3,0xBBD8D1,0x7C6E67};
            for(int i=0;i<4;i++)result.append(Component.literal(chars[i]).withStyle(Style.EMPTY.withColor(colors[i])));
            return result;
        }
        if(stack.has(DataComponents.CUSTOM_NAME))return null;
        var name=Component.empty();String[] labels={"熔炉","海洋","旋风","撼地"};
        for(int i=0;i<4;i++)if((bits(stack)&(1<<i))!=0)name.append(labels[i]+" ");
        return name.append(stack.getItem().getName(stack));
    }
    public static double bowDraw(ItemStack stack){return ForgeUpgrades.multiplier(stack)/(has(stack,EARTH)?1.4:1);}
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void attributes(ItemAttributeModifierEvent event){
        var stack=event.getItemStack();boolean axe=axe(stack);
        double speed=axe?(has(stack,WIND)?1.5:1):(extraBase(stack,EARTH)&&has(stack,EARTH)?.6:1);
        double damage=!axe&&extraBase(stack,EARTH)&&!(stack.getItem() instanceof BowItem)&&has(stack,EARTH)?2:1;
        var entries=List.copyOf(event.getModifiers());
        for(var attr:List.of(Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED)){
            double factor=attr==Attributes.ATTACK_DAMAGE?damage:speed;
            double flat=axe&&attr==Attributes.ATTACK_DAMAGE?3*Integer.bitCount(bits(stack)):0;
            if(factor==1&&flat==0)continue;
            double base=attr==Attributes.ATTACK_DAMAGE?1:4;
            var matches=entries.stream().filter(e->e.attribute().equals(attr)&&e.slot()==EquipmentSlotGroup.MAINHAND&&e.modifier().operation()==AttributeModifier.Operation.ADD_VALUE).toList();
            if(matches.isEmpty())continue;
            double value=matches.stream().mapToDouble(e->e.modifier().amount()).sum();
            matches.forEach(e->event.removeModifier(attr,e.modifier().id()));
            event.addModifier(attr,new AttributeModifier(attr==Attributes.ATTACK_DAMAGE?Item.BASE_ATTACK_DAMAGE_ID:Item.BASE_ATTACK_SPEED_ID,(base+value)*factor+flat-base,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        }
        if(axe&&has(stack,SEA))event.addModifier(Attributes.ENTITY_INTERACTION_RANGE,new AttributeModifier(SoulItems.id("runic_reach"),2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void critical(CriticalHitEvent event){if(event.isCriticalHit())CRITICAL.put(event.getEntity(),new Critical(event.getTarget(),event.getEntity().level().getGameTime()));}
    public static ItemStack weapon(DamageSource source){
        if(source.getDirectEntity() instanceof RunicAxeProjectile axe)return axe.getItem();
        if(source.getDirectEntity()==source.getEntity()&&source.getEntity() instanceof Player p&&source.is(DamageTypes.PLAYER_ATTACK))return p.getMainHandItem();
        return ItemStack.EMPTY;
    }
    public static boolean pierces(DamageSource source){var s=weapon(source);return axe(s)&&has(s,EARTH);}
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void incoming(LivingIncomingDamageEvent event){
        if(SECONDARY.get()||event.getEntity().level().isClientSide())return;
        var source=event.getSource();var stack=weapon(source);
        if(axe(stack)&&has(stack,FIRE)&&source.getEntity() instanceof Player p&&source.getDirectEntity()==p){
            var crit=CRITICAL.remove(p);if(crit!=null&&crit.target==event.getEntity()&&crit.tick==p.level().getGameTime())event.setAmount(event.getAmount()+event.getEntity().getMaxHealth()*.06F);
        }
        if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile shot&&shot.getPersistentData().getBoolean("bfa_demolishing_bow"))event.setAmount(event.getAmount()*2);
    }
    private static boolean allowed(Player owner,LivingEntity target){return target!=owner&&target.isAlive()&&!owner.isAlliedTo(target)&&!(target instanceof TamableAnimal p&&p.isTame())&&(!(target instanceof Player p)||owner.canHarmPlayer(p));}
    @SubscribeEvent public static void hit(LivingDamageEvent.Post event){
        if(SECONDARY.get()||event.getEntity().level().isClientSide()||event.getNewDamage()<=0||!(event.getSource().getEntity() instanceof Player owner))return;
        var stack=weapon(event.getSource());if(!axe(stack))return;
        SECONDARY.set(true);
        try{
            if(has(stack,EARTH))for(var other:owner.level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(event.getEntity().position().add(0,event.getEntity().getBbHeight()/2,0),3,3,3),e->e!=event.getEntity()&&allowed(owner,e)))
                other.hurt(event.getSource(),event.getOriginalDamage());
            if(bits(stack)==15&&event.getEntity().isAlive())event.getEntity().hurt(new RunicVoidSource(owner),10);
        }finally{SECONDARY.set(false);}
    }
    /** Flat void damage ignores armor; it respects invulnerability and protection hooks. */
    public static final class RunicVoidSource extends DamageSource{
        public RunicVoidSource(Player owner){super(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,SoulItems.id("runic_void"))),owner,owner);}
        @Override public boolean is(TagKey<DamageType> tag){return tag.equals(DamageTypeTags.BYPASSES_ARMOR)||tag.equals(DamageTypeTags.BYPASSES_COOLDOWN)||super.is(tag);}
    }
    @SubscribeEvent public static void guard(LivingDamageEvent.Pre event){
        if(event.getEntity() instanceof Player p&&slot(p,EquipmentSlot.CHEST,SEA)&&event.getSource().getEntity() instanceof Guardian)event.setNewDamage(event.getNewDamage()*.7F);
    }
    @SubscribeEvent public static void fatigue(MobEffectEvent.Applicable event){if(event.getEntity() instanceof Player p&&slot(p,EquipmentSlot.CHEST,SEA)&&event.getEffectInstance().getEffect().equals(MobEffects.DIG_SLOWDOWN))event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        var p=event.getEntity();if(p.level().isClientSide())return;
        if(p.onGround()||p.isInWaterOrBubble()||p.onClimbable())p.getPersistentData().remove("bfa_double_jump_used");
        if(slot(p,EquipmentSlot.CHEST,SEA)&&p.hasEffect(MobEffects.DIG_SLOWDOWN))p.removeEffect(MobEffects.DIG_SLOWDOWN);
    }
}
