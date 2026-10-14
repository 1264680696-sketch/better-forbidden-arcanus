package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import dev.betterforbiddenarcanus.mixin.BeaconAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class BeaconFlight {
    private record Source(AABB range, long tick) {}
    private static final Map<ServerLevel, Map<BlockPos, Source>> SOURCES = new WeakHashMap<>();
    public static final ResourceLocation MODIFIER = SoulItems.id("beacon_flight");
    private BeaconFlight() {}

    private static boolean valid(ServerLevel level, BlockPos pos, BeaconBlockEntity beacon) {
        var data = (BeaconAccessor)beacon;
        return data.bfa$levels() > 0 && data.bfa$primaryPower() != null && !beacon.getBeamSections().isEmpty()
                && level.getBlockState(pos.above()).is(ModBlocks.ARCANE_DRAGON_EGG.get());
    }

    public static void track(Level level, BlockPos pos, BeaconBlockEntity beacon) {
        if (!(level instanceof ServerLevel server)) return;
        if (!valid(server, pos, beacon)) {
            var sources = SOURCES.get(server);
            if (sources != null) sources.remove(pos);
            return;
        }
        double radius = ((BeaconAccessor)beacon).bfa$levels() * 10 + 10;
        var range = new AABB(pos).inflate(radius).expandTowards(0, server.getHeight(), 0);
        SOURCES.computeIfAbsent(server, key -> new HashMap<>()).put(pos.immutable(), new Source(range, server.getGameTime()));
    }

    public static void update(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        boolean active = false;
        var sources = SOURCES.get(level);
        if (sources != null) {
            var iterator = sources.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                // Beacons tick every server tick. Never load a chunk to maintain
                // a stale source, and recheck the egg before granting flight.
                if (level.getGameTime() - entry.getValue().tick() > 1 || !level.hasChunkAt(entry.getKey())
                        || !(level.getBlockEntity(entry.getKey()) instanceof BeaconBlockEntity beacon)
                        || !valid(level, entry.getKey(), beacon)) {
                    iterator.remove(); continue;
                }
                if (entry.getValue().range().intersects(player.getBoundingBox())) active = true;
            }
        }
        var attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (attribute == null) return;
        if (active && attribute.getModifier(MODIFIER) == null)
            attribute.addTransientModifier(new AttributeModifier(MODIFIER, 1, AttributeModifier.Operation.ADD_VALUE));
        else if (!active) attribute.removeModifier(MODIFIER);
        if (!player.mayFly() && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) { update(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { update(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { update(event.getEntity()); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) SOURCES.remove(level);
    }
}
