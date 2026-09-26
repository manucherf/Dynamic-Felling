package io.github.manucherf.dynamicfelling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;


public class ChopHand {
    private static final float BLEND_TICKS = 6.0F;
    private static float blend;
    private static float prevBlend;
    private static float lastLift;

    //change hands
    static void onRenderHand(RenderHandEvent event) {
        float amount = Mth.lerp(event.getPartialTick(), prevBlend, blend);
        if (amount <= 0.0F) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            drawChop(event, amount);
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
    private static void drawChop(RenderHandEvent event, float amount) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        float side = right ? 1.0F : -1.0F;

        if (ChopTracker.isChopping()) {
            float partialTick = event.getPartialTick();
            // until first downswing, stay raised instead of joining mid pull-back
            if (ChopTracker.chopTicks(partialTick) < FellingTiming.FIRST_HIT_TICKS - 5.0F) {
                lastLift = 1.0F;
            } else {
                lastLift = lift(ChopTracker.swingTicks(partialTick));
            }
        }
        float lift = lastLift;
        float mix = amount * amount * (3.0F - 2.0F * amount);
        //float mix = 0.0F;
        //float lift = 0.0F;
        //float lift = 0.0F;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        float x = Mth.lerp(mix, 0.63F, Mth.lerp(lift, 0.13F, 0.9F));
        float y = Mth.lerp(mix, -0.62F, Mth.lerp(lift, -0.28F, -0.4F));
        float z = Mth.lerp(mix, -0.72F, Mth.lerp(lift, -1.0F, -0.30F));
        float lean = Mth.lerp(mix, 0.0F, Mth.lerp(lift, 30.0F, -10.0F));
        float tilt = Mth.lerp(mix, 0.0F, Mth.lerp(lift, -65.0F, 50.0F));
        float twist = Mth.lerp(mix, 0.0F, Mth.lerp(lift, 90.0F, 0.0F));

        // where your hands are (x right, y up, z toward you)
        pose.translate(side * x, y + event.getEquipProgress() * -0.6F * (1.0F - mix), z);

        // angle of the axe around your hands
        pose.mulPose(Axis.ZP.rotationDegrees(side * lean));
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.mulPose(Axis.YP.rotationDegrees(side * twist));

        // puts the grip of the handle on your hands
        pose.translate(side * -0.07F, 0.10F, 0.0F);

        /*pose.translate(side * Mth.lerp(lift, 0.13F, 0.9F),//left right
                                 Mth.lerp(lift, -0.28F, -0.4F) + event.getEquipProgress() * -0.6F,//up down
                                 Mth.lerp(lift, -1.0F, -0.30F));//distance
        //drawMarker(pose, event);

        // angle of the axe around your hands
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(lift, 30.0F, -10.0F)));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lift, -65.0F, 70.0F)));
        pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(lift, 90.0F, 00.0F)));

        // puts the grip of the handle on your hands
        pose.translate(side * -0.07F, 0.10F, 0.0F);*/

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

    //blend once a tick
    static void onClientTick(ClientTickEvent.Post event) {
        prevBlend = blend;
        float step = 1.0F / BLEND_TICKS;
        blend = ChopTracker.isChopping() ? Math.min(1.0F, blend + step) : Math.max(0.0F, blend - step);
    }

}