package dev.betterforbiddenarcanus;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class TyrCooldown {
    private static final Set<ServerPlayer> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
    public record Payload(int ticks) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(SoulItems.id("tyr_cooldown"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buffer, data) -> buffer.writeVarInt(data.ticks), buffer -> new Payload(buffer.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private TyrCooldown() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE, Payload.CODEC, (data, context) -> context.enqueueWork(
                () -> dev.betterforbiddenarcanus.client.TyrCooldownClient.receive(data.ticks)));
    }
    public static int remaining(Player player) {
        return (int)Math.clamp(player.getPersistentData().getLong(TyrCombat.COOLDOWN) - TyrCombat.clock(player), 0, 2400);
    }
    public static void sync(Player player) {
        if (!(player instanceof ServerPlayer remote) || remote.connection == null) return;
        int ticks = remaining(player);
        if (ticks > 0) ACTIVE.add(remote); else ACTIVE.remove(remote);
        PacketDistributor.sendToPlayer(remote, new Payload(ticks));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p
                && ((p.level().getGameTime() % 20 == 0 && remaining(p) > 0) || (ACTIVE.contains(p) && remaining(p) == 0))) sync(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event.getEntity()); }
}
