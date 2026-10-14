package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class MeteorScan {
    public static final int RADIUS=32, DURATION=200, MAX_RESULTS=4096;
    private static final String NEXT="bfa_meteor_scan_next";
    private MeteorScan() {}
    public record Payload(ResourceLocation dimension,List<BlockPos> positions) implements CustomPacketPayload {
        public static final Type<Payload> TYPE=new Type<>(SoulItems.id("meteor_scan"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Payload> CODEC=StreamCodec.of((buffer,data)->{
            buffer.writeResourceLocation(data.dimension);buffer.writeVarInt(data.positions.size());data.positions.forEach(buffer::writeBlockPos);
        },buffer->{
            var dimension=buffer.readResourceLocation();int count=buffer.readVarInt();
            if(count<0||count>MAX_RESULTS)throw new IllegalArgumentException("Invalid meteor scan result count");
            var positions=new ArrayList<BlockPos>(count);for(int i=0;i<count;i++)positions.add(buffer.readBlockPos());
            return new Payload(dimension,List.copyOf(positions));
        });
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE,Payload.CODEC,(data,context)->context.enqueueWork(
                ()->dev.betterforbiddenarcanus.client.MeteorHighlights.receive(data)));
    }
    /** Examine only loaded chunk palettes, then exact ore positions within the sphere. */
    public static List<BlockPos> find(ServerLevel level,Vec3 center) {
        var result=new ArrayList<BlockPos>();
        int minX=(int)Math.floor(center.x-RADIUS),maxX=(int)Math.floor(center.x+RADIUS);
        int minY=Math.max(level.getMinBuildHeight(),(int)Math.floor(center.y-RADIUS));
        int maxY=Math.min(level.getMaxBuildHeight()-1,(int)Math.floor(center.y+RADIUS));
        int minZ=(int)Math.floor(center.z-RADIUS),maxZ=(int)Math.floor(center.z+RADIUS);
        for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++) {
            var chunk=level.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
            var sections=chunk.getSections();
            for(int i=level.getSectionIndex(minY);i<=level.getSectionIndex(maxY);i++) {
                var section=sections[i];
                if(!section.getStates().maybeHas(state->state.is(ModBlocks.STELLA_ARCANUM.get())))continue;
                int baseY=level.getMinBuildHeight()+i*16;
                for(int x=Math.max(minX,cx*16);x<=Math.min(maxX,cx*16+15);x++)
                    for(int z=Math.max(minZ,cz*16);z<=Math.min(maxZ,cz*16+15);z++)
                        for(int y=Math.max(minY,baseY);y<=Math.min(maxY,baseY+15);y++) {
                            if(!section.getBlockState(x&15,y&15,z&15).is(ModBlocks.STELLA_ARCANUM.get()))continue;
                            var pos=new BlockPos(x,y,z);
                            if(pos.distToCenterSqr(center)<=RADIUS*RADIUS) {
                                result.add(pos);if(result.size()==MAX_RESULTS)return List.copyOf(result);
                            }
                        }
            }
        }
        return List.copyOf(result);
    }
    public static void use(ServerPlayer player) {
        long now=TyrCombat.clock(player);if(now<player.getPersistentData().getLong(NEXT))return;
        player.getPersistentData().putLong(NEXT,now+20);
        var positions=find(player.serverLevel(),player.position());
        PacketDistributor.sendToPlayer(player,new Payload(player.level().dimension().location(),positions));
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.better_forbidden_arcanus.meteor_scan",positions.size()),true);
    }
}
