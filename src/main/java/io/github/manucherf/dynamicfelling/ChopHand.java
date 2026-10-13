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

    //swing made of two poses
    private record Swing(HandPose hit, HandPose raised) {}

    //vanilla axe as pose
    private static final HandPose HOLD = new HandPose(0.63F, -0.62F, -0.72F, 0.0F, 0.0F, 0.0F);

    //list of swings
    private static final Swing[] SWINGS = {
            // high to low
            new Swing(new HandPose(0.13F, -0.28F, -1.0F, 30.0F, -65.0F, 90.0F),
                    new HandPose(0.9F, -0.4F, -0.3F, -10.0F, 50.0F, 0.0F)),
            // mid to mid
            new Swing(new HandPose(0.05F, -0.25F, -0.85F, -90.0F, -110.0F, 0.0F),
                    new HandPose(0.55F, -0.10F, -0.45F, -90.0F, 20.0F, 0.0F)),
            // low to high
            new Swing(new HandPose(0.15F, -0.15F, -0.85F, -110.0F, -110.0F, 0.0F),
                    new HandPose(0.50F, -0.55F, -0.50F, -130.0F, 20.0F, 0.0F))
    };

    private static HandPose lastPose = HOLD;

    //get swing from list
    private static Swing swing(int index) {
        long seed = ChopTracker.chopSeed();
        //start with random swing
        int pick = Math.floorMod(seed, SWINGS.length);
        for (int i = 1; i <= index; i++) {
            pick = (pick + 1 + Math.floorMod(Mth.murmurHash3Mixer((int) seed + i), SWINGS.length - 1)) % SWINGS.length;        }
        return SWINGS[pick];
    }

    private static HandPose chopPose(float partialTick) {
        float chopped = ChopTracker.chopTicks(partialTick);
        // until the first downswing, stay raised instead of joining mid pull-back
        if (chopped < FellingTiming.FIRST_HIT_TICKS - (FellingTiming.TICKS_PER_SWING - FellingTiming.HOLD_END)) {
            return swing(0).raised();
        }
        float ticks = visualTicks(ChopTracker.swingTicks(partialTick), ChopTracker.pace());
        int hits = Mth.floor((chopped - FellingTiming.FIRST_HIT_TICKS) / FellingTiming.TICKS_PER_SWING) + 1;
        Swing next = swing(hits);
        // resting and pulling back start from the last swing's hit; the downswing ends at the next one's
        HandPose from = ticks >= FellingTiming.HOLD_END ? next.hit() : swing(hits - 1).hit();
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
        if (ticks < FellingTiming.REST_END) {
            return 0.0F;
        }
        if (ticks < FellingTiming.LIFT_END) {
            return io.github.manucherf.dynamicfelling.HandPose.smooth((ticks - FellingTiming.REST_END) / (FellingTiming.LIFT_END - FellingTiming.REST_END));
        }
        if (ticks < FellingTiming.HOLD_END) {
            return 1.0F;
        }
        float t = (ticks - FellingTiming.HOLD_END) / (FellingTiming.TICKS_PER_SWING - FellingTiming.HOLD_END);
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
            lastPose = chopPose(event.getPartialTick());
        } else if (minecraft.level != null && ChopTracker.recentlyCut(minecraft.level.getGameTime(), FOLLOW_THROUGH_TICKS)) {
            //land exactly on final hit,break can come before last frame reaches it
            lastPose = swing(ChopTracker.cutHits() - 1).hit();
        }
        float mix = HandPose.smooth(amount);
        HandPose p = HOLD.lerp(lastPose, mix);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        // where your hands are (x right, y up, z toward you)
        pose.translate(side * p.x(), p.y() + event.getEquipProgress() * -0.6F * (1.0F - mix), p.z());


        // angle of the axe around your hands
        pose.mulPose(Axis.ZP.rotationDegrees(side * p.lean()));
        pose.mulPose(Axis.XP.rotationDegrees(p.tilt()));
        pose.mulPose(Axis.YP.rotationDegrees(side * p.twist()));

        // puts the grip of the handle on your hands
        pose.translate(side * -0.07F, 0.10F, 0.0F);

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
        float liftLength = FellingTiming.LIFT_END - FellingTiming.REST_END;
        float holdLength = FellingTiming.HOLD_END - FellingTiming.LIFT_END;
        float real = ticks / pace;
        float extra = FellingTiming.TICKS_PER_SWING / pace - FellingTiming.TICKS_PER_SWING;
        float rest = FellingTiming.REST_END + extra * (1.0F - FellingTiming.HOLD_SHARE);
        if (real < rest) {
            return real / rest * FellingTiming.REST_END;
        }
        real -= rest;
        if (real < liftLength) {
            return FellingTiming.REST_END + real;
        }
        real -= liftLength;
        float hold = holdLength + extra * FellingTiming.HOLD_SHARE;
        if (real < hold) {
            return FellingTiming.LIFT_END + real / hold * holdLength;
        }
        real -= hold;
        return Math.min(FellingTiming.TICKS_PER_SWING, FellingTiming.HOLD_END + real);
    }

}