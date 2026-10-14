package dev.betterforbiddenarcanus.client;
import com.mojang.blaze3d.vertex.*;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import dev.betterforbiddenarcanus.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=BetterForbiddenArcanus.MOD_ID,value=Dist.CLIENT)
public final class MeteorHighlights extends RenderType {
    // The vanilla NO_DEPTH_TEST state is a no-op and can inherit enabled world depth testing.
    private static final DepthTestStateShard THROUGH_WALLS=new DepthTestStateShard("bfa_through_walls",519) {
        @Override public void setupRenderState(){com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();}
        @Override public void clearRenderState(){com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();}
    };
    private static final RenderType OUTLINE=create("bfa_meteor_highlight",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,256,
            CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(OptionalDouble.of(2)))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(THROUGH_WALLS).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static List<BlockPos> positions=List.of();
    private static ResourceLocation dimension;
    private static long expires;
    private MeteorHighlights(){super("unused",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,256,false,false,()->{},()->{});}
    public static void receive(MeteorScan.Payload data) {
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(data.dimension()))return;
        positions=data.positions();dimension=data.dimension();expires=level.getGameTime()+MeteorScan.DURATION;
    }
    public static List<BlockPos> visible() {
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(dimension)||level.getGameTime()>=expires){positions=List.of();return positions;}
        return positions.stream().filter(level::hasChunkAt).filter(pos->level.getBlockState(pos).is(ModBlocks.STELLA_ARCANUM.get())).toList();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var visible=visible();if(visible.isEmpty())return;
        var camera=event.getCamera().getPosition();var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var buffer=buffers.getBuffer(OUTLINE);
        var pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());
        for(var pos:visible)LevelRenderer.renderLineBox(pose,buffer,new AABB(pos).inflate(.005).move(-camera.x,-camera.y,-camera.z),1F,.85F,.2F,1F);
        buffers.endBatch(OUTLINE);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){positions=List.of();dimension=null;expires=0;}
}
