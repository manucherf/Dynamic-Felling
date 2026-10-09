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
    private static final int FOLLOW_THROUGH_TICKS = 5;
    static final float HOLD_SHARE = 0.7F;

    //one pose as six numbers
    private record AxePose(float x, float y, float z, float lean, float tilt, float twist) {
        AxePose lerp(AxePose to, float t) {
            return new AxePose(Mth.lerp(t, x, to.x), Mth.lerp(t, y, to.y), Mth.lerp(t, z, to.z),
                    Mth.lerp(t, lean, to.lean), Mth.lerp(t, tilt, to.tilt), Mth.lerp(t, twist, to.twist));
        }
    }

    //swing made of two poses
    private record Swing(AxePose hit, AxePose raised) {}

    //vanilla axe as pose
    private static final AxePose HOLD = new AxePose(0.63F, -0.62F, -0.72F, 0.0F, 0.0F, 0.0F);

    //list of swings
    private static final Swing[] SWINGS = {
            // high to low
            new Swing(new AxePose(0.13F, -0.28F, -1.0F, 30.0F, -65.0F, 90.0F),
                    new AxePose(0.9F, -0.4F, -0.3F, -10.0F, 50.0F, 0.0F)),
            // mid to mid
            new Swing(new AxePose(0.05F, -0.25F, -0.85F, -90.0F, -110.0F, 0.0F),
                    new AxePose(0.55F, -0.10F, -0.45F, -90.0F, 20.0F, 0.0F)),
            // low to high
            new Swing(new AxePose(0.15F, -0.15F, -0.85F, -110.0F, -110.0F, 0.0F),
                    new AxePose(0.50F, -0.55F, -0.50F, -130.0F, 20.0F, 0.0F))
    };

    private static AxePose lastPose = HOLD;

    //get swing from list
    private static Swing swing(int index) {
        long seed = ChopTracker.chopSeed();
        //start with random swing
        int pick = Math.floorMod(seed, SWINGS.length);
        for (int i = 1; i <= index; i++) {
            pick = (pick + 1 + Math.floorMod(Mth.murmurHash3Mixer((int) seed + i), SWINGS.length - 1)) % SWINGS.length;        }
        return SWINGS[pick];
    }

    private static AxePose chopPose(float partialTick) {
        //if (true) return new AxePose(0.05F, -0.25F, -0.85F, -90.0F, -110.0F, 0.0F);//mid hit
        //if (true) return new AxePose(0.13F, -0.28F, -1.0F, 30.0F, -65.0F, 90.0F);//high to low hit
        //if (true) return new AxePose(0.15F, -0.15F, -0.85F, -110.0F, -110.0F, 00.0F);//low to high hit
        //if (true) return new AxePose(0.50F, -0.55F, -0.50F, -130.0F, 20.0F, 0.0F);//low to high raise
        float chopped = ChopTracker.chopTicks(partialTick);
        // until the first downswing, stay raised instead of joining mid pull-back
        if (chopped < FellingTiming.FIRST_HIT_TICKS - 5.0F) {
            return swing(0).raised();
        }
        float ticks = visualTicks(ChopTracker.swingTicks(partialTick), ChopTracker.pace());
        int hits = Mth.floor((chopped - FellingTiming.FIRST_HIT_TICKS) / FellingTiming.TICKS_PER_SWING) + 1;
        Swing next = swing(hits);
        // resting and pulling back start from the last swing's hit; the downswing ends at the next one's
        AxePose from = ticks >= 15.0F ? next.hit() : swing(hits - 1).hit();
        return from.lerp(next.raised(), lift(ticks));
    }

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

        //if (ChopTracker.isChopping()) {
        //    float partialTick = event.getPartialTick();
            // until first downswing, stay raised instead of joining mid pull-back
        //    if (ChopTracker.chopTicks(partialTick) < FellingTiming.FIRST_HIT_TICKS - 5.0F) {
        //        lastLift = 1.0F;
        //    } else {
        //       lastLift = lift(ChopTracker.swingTicks(partialTick));
        //    }

        if (ChopTracker.isChopping()) {
            lastPose = chopPose(event.getPartialTick());
        } else if (minecraft.level != null && ChopTracker.recentlyCut(minecraft.level.getGameTime(), FOLLOW_THROUGH_TICKS)) {
            //land exactly on final hit,break can come before last frame reaches it
            lastPose = swing(ChopTracker.cutHits() - 1).hit();
        }
        float mix = amount * amount * (3.0F - 2.0F * amount);
        AxePose p = HOLD.lerp(lastPose, mix);


        //float lift = lastLift;
        //float mix = amount * amount * (3.0F - 2.0F * amount);
        //float mix = 0.0F;
        //float lift = 0.0F;
        //float lift = 0.0F;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        //float x = Mth.lerp(mix, 0.63F, Mth.lerp(lift, 0.13F, 0.9F));
        //float y = Mth.lerp(mix, -0.62F, Mth.lerp(lift, -0.28F, -0.4F));
        //float z = Mth.lerp(mix, -0.72F, Mth.lerp(lift, -1.0F, -0.30F));
        //float lean = Mth.lerp(mix, 0.0F, Mth.lerp(lift, 30.0F, -10.0F));
        //float tilt = Mth.lerp(mix, 0.0F, Mth.lerp(lift, -65.0F, 50.0F));
        //float twist = Mth.lerp(mix, 0.0F, Mth.lerp(lift, 90.0F, 0.0F));

        // where your hands are (x right, y up, z toward you)
        pose.translate(side * p.x(), p.y() + event.getEquipProgress() * -0.6F * (1.0F - mix), p.z());
        //pose.translate(side * x, y + event.getEquipProgress() * -0.6F * (1.0F - mix), z);

        // angle of the axe around your hands
        pose.mulPose(Axis.ZP.rotationDegrees(side * p.lean()));
        pose.mulPose(Axis.XP.rotationDegrees(p.tilt()));
        pose.mulPose(Axis.YP.rotationDegrees(side * p.twist()));
        //pose.mulPose(Axis.ZP.rotationDegrees(side * lean));
        //pose.mulPose(Axis.XP.rotationDegrees(tilt));
        //pose.mulPose(Axis.YP.rotationDegrees(side * twist));

        // puts the grip of the handle on your hands
        pose.translate(side * -0.07F, 0.10F, 0.0F);
        //pose.translate(side * -0.07F, 0.10F, 0.0F);

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

     static void drawMarker(PoseStack pose, RenderHandEvent event) {
        pose.pushPose();
        pose.scale(0.1F, 0.1F, 0.1F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.REDSTONE_BLOCK), ItemDisplayContext.FIXED,
                event.getPackedLight(), OverlayTexture.NO_OVERLAY, pose, event.getMultiBufferSource(), Minecraft.getInstance().level, 0);
        pose.popPose();
    }

    //blend once a tick
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        prevBlend = blend;
        float step = 1.0F / BLEND_TICKS;
        //hold hit pose briefly after last hit
        boolean holding = ChopTracker.isChopping() || (minecraft.level != null && ChopTracker.recentlyCut(minecraft.level.getGameTime(), FOLLOW_THROUGH_TICKS));
        blend = holding ? Math.min(1.0F, blend + step) : Math.max(0.0F, blend - step);
    }

    private static float visualTicks(float ticks, float pace) {
        if (pace >= 1.0F) {
            return ticks;
        }
        float real = ticks / pace;
        float extra = (FellingTiming.TICKS_PER_SWING / pace - FellingTiming.TICKS_PER_SWING);// / 2.0F;
        float rest = 6.0F + extra * (1.0F - HOLD_SHARE);
        if (real < rest) {
            return real / rest * 6.0F;
        }
        real -= rest;
        if (real < 6.0F) {
            return 6.0F + real;
        }
        real -= 6.0F;
        float hold = 3.0F + extra * HOLD_SHARE;
        if (real < hold) {
            return 12.0F + real / hold * 3.0F;
        }
        real -= hold;
        return Math.min(20.0F, 15.0F + real);
    }

}