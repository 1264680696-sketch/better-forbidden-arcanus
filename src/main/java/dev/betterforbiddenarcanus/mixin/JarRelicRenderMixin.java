package dev.betterforbiddenarcanus.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.stal111.forbidden_arcanus.client.renderer.block.EssenceUtremJarRenderer;
import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import dev.betterforbiddenarcanus.JarRelicStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EssenceUtremJarRenderer.class,remap=false)
public abstract class JarRelicRenderMixin {
    @Inject(method="render(Lcom/stal111/forbidden_arcanus/common/block/entity/EssenceUtremJarBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("RETURN"))
    private void bfa$display(EssenceUtremJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        var stack=((JarRelicStorage)jar).bfa$relic();if(stack.isEmpty())return;
        float time=jar.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,1.2+.05*Math.sin(time*.1),.5);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time*2));pose.scale(.75F,.75F,.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.GROUND,light,overlay,pose,buffers,jar.getLevel(),0);pose.popPose();
    }
}
