package dev.betterforbiddenarcanus.legacy;
import dev.betterforbiddenarcanus.BetterForbiddenArcanus;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class LegacySpawnWard extends SavedData {
    public static final int RADIUS = 25;
    private final Set<BlockPos> positions = new HashSet<>();
    public static LegacySpawnWard data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(LegacySpawnWard::new, LegacySpawnWard::load), "bfa_legacy_spawn_wards");
    }
    private static LegacySpawnWard load(CompoundTag tag, HolderLookup.Provider lookup) {
        var data = new LegacySpawnWard(); for (long pos : tag.getLongArray("Positions")) data.positions.add(BlockPos.of(pos)); return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putLongArray("Positions", positions.stream().mapToLong(BlockPos::asLong).toArray()); return tag;
    }
    public void add(BlockPos pos) { if (positions.add(pos.immutable())) setDirty(); }
    public void remove(BlockPos pos) { if (positions.remove(pos)) setDirty(); }
    public boolean blocks(ServerLevel level, double x, double y, double z) {
        var iterator = positions.iterator();
        while (iterator.hasNext()) {
            var pos = iterator.next();
            // Unloaded wards never force chunks into memory. Their saved entry survives unloading.
            if (!level.hasChunkAt(pos) || pos.distToLowCornerSqr(x, y, z) > RADIUS*RADIUS) continue;
            var state = level.getBlockState(pos);
            if (!state.is(LegacyContent.WARD.get()) || !state.getValue(LegacyRuneBlock.ACTIVATED)) { iterator.remove(); setDirty(); continue; }
            return true;
        }
        return false;
    }
    @SubscribeEvent public static void spawn(MobSpawnEvent.PositionCheck event) {
        var level = event.getLevel().getLevel();
        if (event.getEntity().getType().getCategory() == MobCategory.MONSTER && data(level).blocks(level, event.getX(), event.getY(), event.getZ()))
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }
}
