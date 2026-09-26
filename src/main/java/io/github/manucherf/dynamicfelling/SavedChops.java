package io.github.manucherf.dynamicfelling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SavedChops {
    private static final Map<BlockPos, SavedHitsPayload> TRUNKS = new HashMap<>();

    static void onSaved(SavedHitsPayload payload) {
        TRUNKS.put(payload.center(), payload);
    }

    static int saved(ClientLevel level, BlockPos center) {
        SavedHitsPayload entry = TRUNKS.get(center);
        return entry == null || level.getGameTime() - entry.lastHit() > ChopMemory.FORGET_TICKS ? 0 : entry.hits();
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TRUNKS.clear();
    }

    //draw saved cracks
    static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Iterator<SavedHitsPayload> entries = TRUNKS.values().iterator();
        while (entries.hasNext()) {
            SavedHitsPayload entry = entries.next();
            int id = crackId(entry.center());
            boolean forgotten = level.getGameTime() - entry.lastHit() > ChopMemory.FORGET_TICKS;
            if (forgotten || level.getBlockState(entry.center()).isAir()) {
                //when progress forgotten or the tree comes down take the cracks off
                level.destroyBlockProgress(id, entry.center(), -1);
                entries.remove();
            } else {
                int stage = Math.min(9, Mth.ceil(entry.hits() * 10.0F / entry.total()) - 1);
                level.destroyBlockProgress(id, entry.center(), stage);
            }
        }
    }

    private static int crackId(BlockPos center) {
        //give saved trunk negative ID
        return center.hashCode() | Integer.MIN_VALUE;
    }
}