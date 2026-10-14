package dev.betterforbiddenarcanus.mixin;

import com.stal111.forbidden_arcanus.common.block.ArcaneCrystalObeliskBlock;
import com.stal111.forbidden_arcanus.common.block.entity.ArcaneCrystalObeliskBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ObeliskPart;
import com.stal111.forbidden_arcanus.core.init.ModBlockEntities;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.other.ModPOITypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArcaneCrystalObeliskBlockEntity.class, remap = false)
public abstract class CorruptedObeliskMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void bfa$experience(Level level, BlockPos pos, BlockState state, ArcaneCrystalObeliskBlockEntity entity, CallbackInfo ci) {
        if (!state.is(ModBlocks.CORRUPTED_ARCANE_CRYSTAL_OBELISK.get())) return;
        ci.cancel();
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 100 != 0
                || !state.getValue(ArcaneCrystalObeliskBlock.ACTIVE)
                || state.getValue(ArcaneCrystalObeliskBlock.PART) != ObeliskPart.LOWER) return;
        server.getPoiManager().findClosest(holder -> holder.value() == ModPOITypes.HEPHAESTUS_FORGE.get(), pos, 4, PoiManager.Occupancy.ANY)
                .flatMap(target -> server.getBlockEntity(target, ModBlockEntities.HEPHAESTUS_FORGE.get()))
                .ifPresent(forge -> forge.getEssenceManager().increaseEssence(EssenceType.EXPERIENCE, 1));
    }
}
