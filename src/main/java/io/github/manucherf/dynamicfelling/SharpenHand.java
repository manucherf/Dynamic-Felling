package io.github.manucherf.dynamicfelling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class SharpenHand {
    private static final float AXE_PIVOT_X = -0.05F;
    private static final float AXE_PIVOT_Y = -0.25F;
    private static final float AXE_PIVOT_Z = 0.0F;
    private static final float STONE_PIVOT_X = 0.0F;
    private static final float STONE_PIVOT_Y = -0.10F;
    private static final float STONE_PIVOT_Z = 0.0F;

    private static final boolean SHOW_PIVOTS = false;
    private static final float FREEZE_TICK = -1F;

    private static final float OUT_TICKS = 6.0F;

    private static Pose lastAxe;
    private static Pose lastStone;
    private static float releasedAt = -1.0F;

    private static float shrink;
    private static float lastShrink;



    private record Pose(float x, float y, float z, float lean, float tilt, float twist) {
        Pose lerp(Pose to, float t) {
            return new Pose(Mth.lerp(t, x, to.x), Mth.lerp(t, y, to.y), Mth.lerp(t, z, to.z),
                    Mth.lerp(t, lean, to.lean), Mth.lerp(t, tilt, to.tilt), Mth.lerp(t, twist, to.twist));
        }
    }

    //vanilla held spots
    private static Pose axeHold() {
        return new Pose(0.56F - AXE_PIVOT_X, -0.52F - AXE_PIVOT_Y, -0.72F - AXE_PIVOT_Z, 0.0F, 0.0F, 0.0F);
    }

    private static Pose stoneHold() {
        return new Pose(-0.56F - STONE_PIVOT_X, -0.52F - STONE_PIVOT_Y, -0.72F - STONE_PIVOT_Z, 0.0F, 0.0F, 0.0F);
    }

    //x right, y up, z towards
    private static Pose axeSideA() {
        return new Pose(-0.01F, -0.12F, -0.95F, 10.0F, 0.0F, 26.0F);
    }

    private static Pose axeSideB() {
        return new Pose(-0.0F, -0.12F, -0.95F, -10.0F, 0.0F, 220.0F);
    }

    private static Pose stoneStart() {
        return new Pose(-0.12F, -0.14F, -1.2F, 0.0F, 0.0F, 30.0F);
    }

    private static Pose stoneEnd() {
        return new Pose(-0.02F, -0.20F, -0.70F, 0.0F, 0.0F, 30.0F);
    }

    private static Pose stoneStartB() {
        return new Pose(-0.05F, -0.14F, -1.0F, 0.0F, 0.0F, 40.0F);
    }

    private static Pose stoneEndB() {
        return new Pose(0.05F, -0.14F, -0.8F, 0.0F, 0.0F, 40.0F);
    }

    private static Pose stoneAway() {
        return new Pose(-0.18F, -0.14F, -1.2F, 0.0F, 0.0F, 11.0F);
    }

    private SharpenHand() {}

    private static boolean isSharpening(LocalPlayer player) {
        return (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND
                && player.getUseItem().is(FellingItems.WHETSTONE.get())) || Grinding.isGrinding(player);
    }

    static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        float side = right ? 1.0F : -1.0F;
        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        Pose axe;
        Pose stone;

        if (isSharpening(player)) {
            float used = FREEZE_TICK >= 0.0F ? FREEZE_TICK : WhetstoneItem.usedTicks(player) + event.getPartialTick();
            float ticks = WhetstoneItem.cycleTick(used);

            axe = axePose(ticks);
            stone = stonePose(ticks);
            lastAxe = axe;
            lastStone = stone;
            shrink = smooth(ticks / WhetstoneItem.IN_END);
            lastShrink = shrink;
            releasedAt = -1.0F;
        } else {
            if (lastAxe == null || lastStone == null) {
                return;
            }
            //ease back
            float now = player.tickCount + event.getPartialTick();
            if (releasedAt < 0.0F) {
                releasedAt = now;
            }
            float out = (now - releasedAt) / OUT_TICKS;
            if (out >= 1.0F) {
                lastAxe = null;
                lastStone = null;
                return;
            }
            axe = lastAxe.lerp(axeHold(), smooth(out));
            stone = lastStone.lerp(stoneHold(), smooth(out));
            shrink = Mth.lerp(smooth(out), lastShrink, 0.0F);
        }

        event.setCanceled(true);
        if (mainHand) {
            draw(event, player, player.getMainHandItem(), axe, side, !right, AXE_PIVOT_X, AXE_PIVOT_Y, AXE_PIVOT_Z);
        } else {
            draw(event, player, player.getOffhandItem(), stone, side, right, STONE_PIVOT_X, STONE_PIVOT_Y, STONE_PIVOT_Z);
        }
    }

    private static Pose axePose(float t) {
        if (t < WhetstoneItem.IN_END) {
            return axeHold().lerp(axeSideA(), smooth(t / WhetstoneItem.IN_END));
        }
        if (t < WhetstoneItem.SIDE_A_END) {
            return axeSideA();
        }
        if (t < WhetstoneItem.FLIP_END) {
            return axeFlip(flipForward(t));
        }
        if (t < WhetstoneItem.SIDE_B_END) {
            return axeSideB();
        }
        return axeFlip(flipBack(t));
    }

    private static Pose stonePose(float t) {
        if (t < WhetstoneItem.IN_END) {
            return stoneHold().lerp(stoneStart(), smooth(t / WhetstoneItem.IN_END));
        }
        if (t < WhetstoneItem.SIDE_A_END) {
            return stroke(stoneStart(), stoneEnd(),
                    (t - WhetstoneItem.IN_END) / (WhetstoneItem.SIDE_A_END - WhetstoneItem.IN_END));
        }
        if (t < WhetstoneItem.FLIP_END) {
            return stoneFlip(flipForward(t));
        }
        if (t < WhetstoneItem.SIDE_B_END) {
            return stroke(stoneStartB(), stoneEndB(),
                    (t - WhetstoneItem.FLIP_END) / (WhetstoneItem.SIDE_B_END - WhetstoneItem.FLIP_END));
        }
        return stoneFlip(flipBack(t));
    }

    //0 = side A, 1 = side B
    private static float flipForward(float t) {
        return (t - WhetstoneItem.SIDE_A_END) / (WhetstoneItem.FLIP_END - WhetstoneItem.SIDE_A_END);
    }

    private static float flipBack(float t) {
        return 1.0F - (t - WhetstoneItem.SIDE_B_END) / (WhetstoneItem.CYCLE_END - WhetstoneItem.SIDE_B_END);
    }

    private static Pose axeFlip(float f) {
        return axeSideA().lerp(axeSideB(), smooth(f));
    }

    private static Pose stoneFlip(float f) {
        return f < 0.5F ? stoneStart().lerp(stoneAway(), smooth(f * 2.0F))
                : stoneAway().lerp(stoneStartB(), smooth(f * 2.0F - 1.0F));
    }

    //back and forth
    private static Pose stroke(Pose start, Pose end, float t) {
        float wave = 0.5F - 0.5F * Mth.cos(t * WhetstoneItem.STROKES * 2.0F * Mth.PI);
        return start.lerp(end, wave);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void draw(RenderHandEvent event, LocalPlayer player, ItemStack stack, Pose p, float side,
                             boolean leftHand, float pivotX, float pivotY, float pivotZ) {
        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        //where pivot sits
        pose.translate(side * p.x(), p.y(), p.z());
        //turn around the pivot
        pose.mulPose(Axis.ZP.rotationDegrees(side * p.lean()));
        pose.mulPose(Axis.XP.rotationDegrees(p.tilt()));
        pose.mulPose(Axis.YP.rotationDegrees(side * p.twist()));
        if (SHOW_PIVOTS) {
            ChopHand.drawMarker(pose, event);
        }
        //from pivot to where the item is drawn
        pose.translate(side * pivotX, pivotY, pivotZ);

        if (stack.getItem() instanceof BlockItem) {
            float s = Grinding.blockScale(shrink);
            pose.scale(s, s, s);
        }

        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                leftHand, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    /*static void onClientTick(ClientTickEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || lastAxe == null) return;
        ItemInHandRenderer hands = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (ItemStack.isSameItem(hands.mainHandItem, main)) hands.mainHandItem = main;
        if (ItemStack.isSameItem(hands.offHandItem, off)) hands.offHandItem = off;
    }*/
}