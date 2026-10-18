package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

/** A carried crystal buys one vanilla-style inventory/experience-preserving death. */
@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID)
public final class SoulBinding {
    public static final String PROTECTED="bfa_soul_crystal_death";
    private static final ThreadLocal<ServerPlayer> OWNER=new ThreadLocal<>();
    private SoulBinding() {}
    public static boolean protectedDeath(Player player){return player.getPersistentData().getBoolean(PROTECTED);}
    public static boolean keepingInScope(){var p=OWNER.get();return p!=null&&protectedDeath(p);}
    /** Scope a rule query to the dying/respawning player; never modify the world's game rule. */
    public static void withRules(ServerPlayer owner,Runnable action){
        var previous=OWNER.get();OWNER.set(owner);
        try{action.run();}finally{if(previous==null)OWNER.remove();else OWNER.set(previous);}
    }
    /** Called only after the cancellable death event has accepted an actual death. */
    public static boolean activate(ServerPlayer player){
        if(protectedDeath(player))return true;
        if(player.isSpectator()||player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))return false;
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);
            if(stack.is(ModItems.SOUL_BINDING_CRYSTAL.get())){
                stack.shrink(1);inventory.setChanged();player.getPersistentData().putBoolean(PROTECTED,true);return true;
            }
        }
        return false;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void curios(DropRulesEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&protectedDeath(p))event.addOverride(stack->true,ICurio.DropRule.ALWAYS_KEEP);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void experience(LivingExperienceDropEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&protectedDeath(p))event.setDroppedExperience(0);
    }
    /** Legacy equipment bindings no longer grant a second, independent keep effect. */
    @SubscribeEvent public static void clearOldBindings(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0||!p.isAlive())return;
        var inventory=p.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);var modifier=stack.get(ModDataComponents.ITEM_MODIFIER);
            if(modifier!=null&&modifier.is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus","soulbound")))inventory.setItem(i,consume(stack));
        }
    }
    /** Compatibility for legacy retained-item receipts and their generated name prefix. */
    public static ItemStack consume(ItemStack input){
        var modifier=input.get(ModDataComponents.ITEM_MODIFIER);
        if(modifier==null||!modifier.is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus","soulbound")))return input;
        var output=input.copy();output.remove(ModDataComponents.ITEM_MODIFIER);
        var name=output.get(DataComponents.ITEM_NAME);
        if(name!=null&&name.getContents() instanceof TranslatableContents key&&key.getKey().equals("modifier.forbidden_arcanus.soulbound")&&name.getSiblings().size()>=2)
            output.set(DataComponents.ITEM_NAME,name.getSiblings().getLast().copy());
        return output;
    }
}
