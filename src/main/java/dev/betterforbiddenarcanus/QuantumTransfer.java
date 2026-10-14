package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.block.entity.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiType;

/** Bridge to the native transfer animation, with separately budgeted amounts. */
public interface QuantumTransfer {
    HephaestusForgeBlockEntity bfa$forge();
    void bfa$forge(HephaestusForgeBlockEntity forge);
    void bfa$jar(EssenceUtremJarBlockEntity jar);
    Map<EssenceType, Holder<PoiType>> bfa$poiTypes();
    int bfa$credit(EssenceType type);
    void bfa$credit(EssenceType type, int fifths);
    void bfa$transfer(ServerLevel level, BlockPos pos, int cost, int gain);
    void bfa$stop(ServerLevel level, BlockPos pos);
}
