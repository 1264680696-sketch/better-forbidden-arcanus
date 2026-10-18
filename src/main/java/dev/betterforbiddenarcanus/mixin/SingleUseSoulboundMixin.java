package dev.betterforbiddenarcanus.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.stal111.forbidden_arcanus.common.event.PlayerEvents;
import com.stal111.forbidden_arcanus.common.item.modifier.SoulboundInventory;
import com.stal111.forbidden_arcanus.core.init.other.ModAttachmentTypes;
import dev.betterforbiddenarcanus.SoulBinding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PlayerEvents.class,remap=false)
public abstract class SingleUseSoulboundMixin {
    @Inject(method="onPlayerRespawn",at=@At("HEAD"),cancellable=true)
    private void actualDeath(PlayerEvent.PlayerRespawnEvent event,CallbackInfo ci) { if(event.isEndConquered())ci.cancel(); }
    @WrapOperation(method="onPlayerRespawn",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void consume(Inventory inventory,int slot,ItemStack retained,Operation<Void> original) { original.call(inventory,slot,SoulBinding.consume(retained)); }
    @Inject(method="onPlayerRespawn",at=@At("TAIL"))
    private void clear(PlayerEvent.PlayerRespawnEvent event,CallbackInfo ci) {
        if(event.getEntity() instanceof ServerPlayer player)player.setData(ModAttachmentTypes.SOULBOUND_INVENTORY,SoulboundInventory.create());
    }
}
