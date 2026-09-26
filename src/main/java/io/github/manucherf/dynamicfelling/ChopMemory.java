package io.github.manucherf.dynamicfelling;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class ChopMemory {
    //forgotten after 1 min
    static final long FORGET_TICKS = 1200;

    private record Entry(int hits, long lastHit) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Entry>> TRUNKS = new HashMap<>();

    static int saved(ServerLevel level, BlockPos center) {
        Entry entry = TRUNKS.getOrDefault(level.dimension(), Map.of()).get(center);
        return entry == null || level.getGameTime() - entry.lastHit() > FORGET_TICKS ? 0 : entry.hits();
    }

    static void addHit(ServerLevel level, BlockPos center, int total) {
        long now = level.getGameTime();
        int hits = saved(level, center) + 1;
        Map<BlockPos, Entry> trunks = TRUNKS.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        // drop trunks nobody has touched in a minute, so the map doesn't grow forever
        trunks.values().removeIf(entry -> now - entry.lastHit() > FORGET_TICKS);
        trunks.put(center.immutable(), new Entry(hits, now));
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(center), new SavedHitsPayload(center, hits, now, total));
    }
}