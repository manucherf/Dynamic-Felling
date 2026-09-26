package io.github.manucherf.dynamicfelling;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

//send trunk location
public record ChopHitPayload(BlockPos trunk) implements CustomPacketPayload {
    public static final Type<ChopHitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop_hit"));
    public static final StreamCodec<ByteBuf, ChopHitPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ChopHitPayload::trunk,
            ChopHitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}