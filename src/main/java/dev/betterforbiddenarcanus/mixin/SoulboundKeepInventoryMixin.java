package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.event.EntityEvents;
import com.stal111.forbidden_arcanus.common.item.modifier.SoulboundInventory;
import com.stal111.forbidden_arcanus.core.init.other.ModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EntityEvents.class,remap=false)
public abstract class SoulboundKeepInventoryMixin {
    @Inject(method="onLivingDeath",at=@At("HEAD"),cancellable=true)
    private static void keep(LivingDeathEvent event,CallbackInfo ci) {
        if(event.getEntity() instanceof ServerPlayer p&&(p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)||p.isSpectator())) {
            p.setData(ModAttachmentTypes.SOULBOUND_INVENTORY,SoulboundInventory.create());ci.cancel();
        }
    }
}
