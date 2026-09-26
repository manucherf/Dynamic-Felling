package io.github.manucherf.dynamicfelling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

public class ChopHand {
    //change hands
    static void onRenderHand(RenderHandEvent event) {
        if (!ChopTracker.isChopping()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            drawChop(event);
        }
    }


    //lift against ticks
    private static float lift(float ticks) {
        if (ticks < 6.0F) {
            return 0.0F;
        }
        if (ticks < 12.0F) {
            float t = (ticks - 6.0F) / 6.0F;
            return t * t * (3.0F - 2.0F * t);
        }
        if (ticks < 15.0F) {
            return 1.0F;
        }
        float t = (ticks - 15.0F) / 5.0F;
        return 1.0F - t * t;
    }


    //draw axe
    private static void drawChop(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        float side = right ? 1.0F : -1.0F;
        float lift = lift(ChopTracker.swingTicks(event.getPartialTick()));
        //float lift = 0.0F;
        //float lift = 1.0F;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        // where your hands are (x right, y up, z toward you)
        pose.translate(side * Mth.lerp(lift, 0.13F, 0.9F),//left right
                                 Mth.lerp(lift, -0.28F, -0.4F) + event.getEquipProgress() * -0.6F,//up down
                                 Mth.lerp(lift, -1.0F, -0.30F));//distance
        //drawMarker(pose, event);

        // angle of the axe around your hands
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(lift, 30.0F, -10.0F)));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lift, -65.0F, 70.0F)));
        pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(lift, 90.0F, 00.0F)));

        // puts the grip of the handle on your hands
        pose.translate(side * -0.07F, 0.10F, 0.0F);

        minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, event.getItemStack(),
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                !right, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    private static void drawMarker(PoseStack pose, RenderHandEvent event) {
        pose.pushPose();
        pose.scale(0.1F, 0.1F, 0.1F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.REDSTONE_BLOCK), ItemDisplayContext.FIXED,
                event.getPackedLight(), OverlayTexture.NO_OVERLAY, pose, event.getMultiBufferSource(), Minecraft.getInstance().level, 0);
        pose.popPose();
    }

}