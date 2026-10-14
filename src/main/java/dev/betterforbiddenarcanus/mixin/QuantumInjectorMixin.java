package dev.betterforbiddenarcanus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.stal111.forbidden_arcanus.common.block.entity.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import dev.betterforbiddenarcanus.*;
import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuantumInjectorBlockEntity.class, remap = false)
public abstract class QuantumInjectorMixin extends BlockEntity implements QuantumTransfer {
    @Shadow @Final private static Map<EssenceType, Holder<PoiType>> ESSENCE_TYPE_TO_POI_TYPE;
    @Shadow private HephaestusForgeBlockEntity forgeBlockEntity;
    @Shadow private EssenceUtremJarBlockEntity jarBlockEntity;
    @Shadow protected abstract void transferEssence(ServerLevel level, BlockPos pos);
    @Unique private int bfa$cost, bfa$gain;
    @Unique private final int[] bfa$credits = new int[EssenceType.values().length];
    protected QuantumInjectorMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void bfa$safeTick(Level level, BlockPos pos, BlockState state, QuantumInjectorBlockEntity entity, CallbackInfo ci) {
        QuantumInjector.tick(level, pos, state, entity); ci.cancel();
    }
    @Override public HephaestusForgeBlockEntity bfa$forge() { return forgeBlockEntity; }
    @Override public void bfa$forge(HephaestusForgeBlockEntity forge) { forgeBlockEntity = forge; }
    @Override public void bfa$jar(EssenceUtremJarBlockEntity jar) { jarBlockEntity = jar; }
    @Override public Map<EssenceType, Holder<PoiType>> bfa$poiTypes() { return ESSENCE_TYPE_TO_POI_TYPE; }
    @Override public int bfa$credit(EssenceType type) { return bfa$credits[type.ordinal()]; }
    @Override public void bfa$credit(EssenceType type, int fifths) { bfa$credits[type.ordinal()] = Math.clamp(fifths, 0, 14); }
    @Override public void bfa$transfer(ServerLevel level, BlockPos pos, int cost, int gain) {
        bfa$cost = cost; bfa$gain = gain; transferEssence(level, pos);
    }
    @Override public void bfa$stop(ServerLevel level, BlockPos pos) {
        jarBlockEntity = null; transferEssence(level, pos);
    }
    @WrapOperation(method = "transferEssence", at = @At(value = "INVOKE", target = "Lcom/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssenceManager;increaseEssence(Lcom/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssenceType;I)V"))
    private void bfa$deposit(EssenceManager manager, EssenceType type, int ignored, Operation<Void> original) { original.call(manager, type, bfa$gain); }
    @WrapOperation(method = "transferEssence", at = @At(value = "INVOKE", target = "Lcom/stal111/forbidden_arcanus/common/block/entity/EssenceUtremJarBlockEntity;addEssence(I)V"))
    private void bfa$withdraw(EssenceUtremJarBlockEntity jar, int ignored, Operation<Void> original) { original.call(jar, -bfa$cost); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putIntArray("bfa_quantum_credits", bfa$credits);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); var saved = tag.getIntArray("bfa_quantum_credits");
        for (int i = 0; i < bfa$credits.length; i++) bfa$credits[i] = i < saved.length ? Math.clamp(saved[i], 0, 14) : 0;
    }
}
