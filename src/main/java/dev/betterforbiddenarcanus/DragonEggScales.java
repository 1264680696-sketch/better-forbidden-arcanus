package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Scheduled block ticks do the work; saved deadlines survive chunk/world reloads. */
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class DragonEggScales extends SavedData {
    public static final int PERIOD = 2400;
    private final Map<Long, Long> deadlines = new HashMap<>();
    public static DragonEggScales get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(DragonEggScales::new, DragonEggScales::load), "bfa_dragon_egg_scales");
    }
    public static DragonEggScales load(CompoundTag tag, HolderLookup.Provider lookup) {
        var data = new DragonEggScales();
        for (var entry : tag.getList("eggs", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)entry; data.deadlines.put(row.getLong("pos"), row.getLong("next"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        var rows = new ListTag(); deadlines.forEach((pos, next) -> {
            var row = new CompoundTag(); row.putLong("pos", pos); row.putLong("next", next); rows.add(row);
        }); tag.put("eggs", rows); return tag;
    }
    public static void placed(ServerLevel level, BlockPos pos) {
        var data = get(level);
        if (data.deadlines.putIfAbsent(pos.asLong(), level.getGameTime() + PERIOD) == null) data.setDirty();
        int delay = (int)Math.clamp(data.deadlines.get(pos.asLong()) - level.getGameTime(), 1, 20);
        level.scheduleTick(pos, ModBlocks.ARCANE_DRAGON_EGG.get(), delay);
    }
    public static void removed(ServerLevel level, BlockPos pos) {
        var data = get(level); if (data.deadlines.remove(pos.asLong()) != null) data.setDirty();
    }
    public static ItemStack choose(net.minecraft.util.RandomSource random) {
        return new ItemStack(switch (random.nextInt(4)) {
            case 0 -> ModItems.DRAGON_SCALE.get();
            case 1 -> ModItems.GOLDEN_DRAGON_SCALE.get();
            case 2 -> ModItems.SILVER_DRAGON_SCALE.get();
            default -> ModItems.AQUATIC_DRAGON_SCALE.get();
        });
    }
    public static void tick(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.ARCANE_DRAGON_EGG.get())) return;
        var data = get(level); placed(level, pos);
        if (level.getGameTime() >= data.deadlines.get(pos.asLong())) {
            Block.popResource(level, pos, choose(level.random));
            // Unloaded time never creates a backlog of drops.
            data.deadlines.put(pos.asLong(), level.getGameTime() + PERIOD); data.setDirty();
        }
    }
    @SubscribeEvent public static void loadChunk(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        Runnable bootstrap = () -> {
            if (!level.hasChunkAt(chunk.getPos().getWorldPosition())) return;
            var sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                var section = sections[i];
                if (!section.getStates().maybeHas(state -> state.is(ModBlocks.ARCANE_DRAGON_EGG.get()))) continue;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
                    if (section.getBlockState(x, y, z).is(ModBlocks.ARCANE_DRAGON_EGG.get()))
                        placed(level, new BlockPos(chunk.getPos().getMinBlockX() + x, chunk.getMinBuildHeight() + i * 16 + y, chunk.getPos().getMinBlockZ() + z));
            }
        };
        // ChunkEvent.Load precedes completion of the chunk's FULL future. Queue
        // even on the server thread so existing eggs are discovered after promotion.
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount(), bootstrap));
    }
}
