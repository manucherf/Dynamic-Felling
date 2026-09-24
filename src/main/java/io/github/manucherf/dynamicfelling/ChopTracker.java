package io.github.manucherf.dynamicfelling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Locale;

public class ChopTracker {
    private static BlockPos target;
    private static float swings;
    private static int ticks;
    private static final int CENTER_CRACK_ID = -1712;
    private static BlockPos center;

    private ChopTracker() {}

    //client, counts ticks spent chopping trunk and detects when each hit lands
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = chopTarget(minecraft);
        //topped chopping or switched blocks: start over
        if (pos == null || !pos.equals(target)) {
            clearCenterCrack(minecraft);
            target = pos;
            center = pos == null ? null : FellingTiming.trunkCenter(minecraft.level, pos);
            ticks = 0;
            if (target != null) {
                //fix crack flash
                updateCracks(minecraft);
            }
            return;
        }
        ticks++;
        updateCracks(minecraft);
        if (FellingTiming.isHitTick(ticks)) {
            onHit(minecraft);
        }
    }

    //trunk block being chopped right now, or null if the player isn't chopping one.
    private static BlockPos chopTarget(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null
                || !minecraft.gameMode.isDestroying()
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        swings = FellingTiming.swingsToFell(player, minecraft.level, hit.getBlockPos());
        return swings > 0.0F ? hit.getBlockPos() : null;
    }

    private static void updateCracks(Minecraft minecraft) {
        //crack updates
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        int hits = FellingTiming.hitsLanded(ticks);
        int stage = hits == 0 ? -1 : Math.min(9, Mth.ceil(hits * 10.0F / FellingTiming.hitsToFell(swings)) - 1);
        level.destroyBlockProgress(player.getId(), target, stage);
        //draw crack on center
        if (!center.equals(target)) {
            level.destroyBlockProgress(CENTER_CRACK_ID, center, stage);
        }
    }

    //placeholder, later replace this with cracks, sound and particles.
    private static void onHit(Minecraft minecraft) {
        String text = String.format(Locale.ROOT, "Hit %d of %d",
                FellingTiming.hitsLanded(ticks), FellingTiming.hitsToFell(swings));
        minecraft.player.displayClientMessage(Component.literal(text), true);
    }

    //clean up when crack stops
    private static void clearCenterCrack(Minecraft minecraft) {
        if (center != null && minecraft.level != null) {
            minecraft.level.destroyBlockProgress(CENTER_CRACK_ID, center, -1);
        }
    }



}
