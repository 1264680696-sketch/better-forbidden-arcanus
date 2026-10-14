package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.block.QuantumInjectorBlock;
import com.stal111.forbidden_arcanus.common.block.entity.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.core.init.other.ModPOITypes;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import dev.betterforbiddenarcanus.mixin.EssenceCapacityAccessor;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class QuantumInjector {
    private record Connection(BlockPos forge, long tick) {}
    private static final Map<ServerLevel, Map<BlockPos, Connection>> CONNECTIONS = new WeakHashMap<>();
    private QuantumInjector() {}
    public static void tick(Level world, BlockPos pos, BlockState state, QuantumInjectorBlockEntity injector) {
        if (!(world instanceof ServerLevel level) || level.getGameTime() % 5 != 0) return;
        var bridge = (QuantumTransfer)injector;
        var sources = CONNECTIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        if (!state.getValue(QuantumInjectorBlock.ENABLED)) {
            bridge.bfa$forge(null); sources.remove(pos); bridge.bfa$stop(level, pos); return;
        }
        var forge = level.getPoiManager().findClosest(holder -> holder.value() == ModPOITypes.HEPHAESTUS_FORGE.get(), pos, 4, PoiManager.Occupancy.ANY)
                .filter(level::hasChunkAt).map(level::getBlockEntity).filter(HephaestusForgeBlockEntity.class::isInstance)
                .map(HephaestusForgeBlockEntity.class::cast).filter(entity -> !entity.isRemoved()).orElse(null);
        bridge.bfa$forge(forge);
        if (forge == null) { sources.remove(pos); bridge.bfa$stop(level, pos); return; }
        sources.put(pos.immutable(), new Connection(forge.getBlockPos(), level.getGameTime()));
        var manager = forge.getEssenceManager();
        var capacities = ((EssenceCapacityAccessor)manager).bfa$capacity();
        for (var type : EssenceType.values()) {
            int room = Math.max(0, capacities.get(type) - manager.getEssence(type));
            if (room == 0) continue;
            int credit = bridge.bfa$credit(type);
            // Whole points owed by a previous capacity-limited transfer are not new input.
            int owed = Math.min(room, credit / 5);
            if (owed > 0) {
                manager.increaseEssence(type, owed); bridge.bfa$credit(type, credit - owed * 5);
                injector.setChanged(); bridge.bfa$stop(level, pos); return;
            }
            var poi = bridge.bfa$poiTypes().get(type);
            var jar = level.getPoiManager().getInRange(holder -> holder.value() == poi.value(), pos, 8, PoiManager.Occupancy.ANY)
                    .map(record -> record.getPos()).filter(level::hasChunkAt).map(level::getBlockEntity)
                    .filter(EssenceUtremJarBlockEntity.class::isInstance).map(EssenceUtremJarBlockEntity.class::cast)
                    .filter(entity -> !entity.isRemoved() && entity.getAmount() > 0
                            && entity.getBlockState().getValue(ModBlockStateProperties.ESSENCE_TYPE) == type)
                    .min(Comparator.comparingDouble(entity -> entity.getBlockPos().distSqr(pos))).orElse(null);
            if (jar == null) continue;
            int cost = Math.min(10, Math.min(jar.getAmount(), room));
            int fifths = cost * 6 + credit;
            int gain = Math.min(room, fifths / 5);
            if (cost <= 0 || gain <= 0) continue;
            bridge.bfa$jar(jar); bridge.bfa$transfer(level, pos, cost, gain);
            bridge.bfa$credit(type, fifths - gain * 5); injector.setChanged(); return;
        }
        bridge.bfa$stop(level, pos);
    }
    public static boolean boosts(ServerLevel level, BlockPos forgePos) {
        var sources = CONNECTIONS.get(level); if (sources == null) return false;
        boolean result = false; var iterator = sources.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); var pos = entry.getKey();
            if (level.getGameTime() - entry.getValue().tick > 5 || !level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof QuantumInjectorBlockEntity injector)
                    || injector.isRemoved() || !injector.getBlockState().getValue(QuantumInjectorBlock.ENABLED)) {
                iterator.remove(); continue;
            }
            if (entry.getValue().forge.equals(forgePos) && level.hasChunkAt(forgePos)
                    && level.getBlockEntity(forgePos) instanceof HephaestusForgeBlockEntity) result = true;
        }
        return result;
    }
}
