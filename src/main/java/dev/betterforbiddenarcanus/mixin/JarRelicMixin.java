package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import dev.betterforbiddenarcanus.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EssenceUtremJarBlockEntity.class,remap=false)
public abstract class JarRelicMixin implements JarRelicStorage {
    @Unique private ItemStack bfa$relic=ItemStack.EMPTY;
    @Unique private int bfa$progress;
    @Override public ItemStack bfa$relic(){return bfa$relic;}
    @Override public void bfa$relic(ItemStack stack){bfa$relic=RelicJars.type(stack)==null?ItemStack.EMPTY:stack.copyWithCount(1);}
    @Override public int bfa$relicTicks(){return bfa$progress;}
    @Override public void bfa$relicTicks(int ticks){bfa$progress=Math.clamp(ticks,0,99);}
    @Inject(method="saveAdditional",at=@At("RETURN"))
    private void bfa$save(CompoundTag tag,HolderLookup.Provider lookup,CallbackInfo ci) {
        if(!bfa$relic.isEmpty())tag.put("bfa_jar_relic",bfa$relic.save(lookup));tag.putInt("bfa_relic_progress",bfa$progress);
    }
    @Inject(method="loadAdditional",at=@At("RETURN"))
    private void bfa$load(CompoundTag tag,HolderLookup.Provider lookup,CallbackInfo ci) {
        bfa$relic(ItemStack.parseOptional(lookup,tag.getCompound("bfa_jar_relic")));bfa$relicTicks(tag.getInt("bfa_relic_progress"));
    }
}
