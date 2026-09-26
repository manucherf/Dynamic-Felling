package io.github.manucherf.dynamicfelling;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SavedHitsPayload(BlockPos center, int hits, long lastHit, int total) implements CustomPacketPayload {
    public static final Type<SavedHitsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "saved_hits"));
    public static final StreamCodec<ByteBuf, SavedHitsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SavedHitsPayload::center,
            ByteBufCodecs.VAR_INT, SavedHitsPayload::hits,
            //world time of latest hit
            ByteBufCodecs.VAR_LONG, SavedHitsPayload::lastHit,
            ByteBufCodecs.VAR_INT, SavedHitsPayload::total,
            SavedHitsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

