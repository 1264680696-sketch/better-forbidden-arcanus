package dev.betterforbiddenarcanus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.betterforbiddenarcanus.RunicAxeProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** The axe spins end over end in its flight plane, independently of the camera. */
public final class RunicAxeRenderer extends EntityRenderer<RunicAxeProjectile> {
    private final ItemRenderer items;
    public RunicAxeRenderer(EntityRendererProvider.Context context) { super(context); items=context.getItemRenderer(); }
    public static float spin(float age) { return -36 * age - 45; }
    public static float heading(double x,double z,float fallback) {
        return x*x+z*z<1.0E-8?fallback-90:(float)Math.toDegrees(Math.atan2(x,z))-90;
    }
    @Override public void render(RunicAxeProjectile axe,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var motion=axe.getDeltaMovement();
        float pitch=(float)Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(heading(motion.x,motion.z,axe.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(pitch+spin(axe.tickCount+partial)));
        pose.scale(1.25F,1.25F,1.25F);
        items.renderStatic(axe.getItem(),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,axe.level(),axe.getId());
        pose.popPose();super.render(axe,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(RunicAxeProjectile axe) { return TextureAtlas.LOCATION_BLOCKS; }
}
