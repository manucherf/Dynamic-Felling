package io.github.manucherf.dynamicfelling;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChopStatePayload(boolean chopping, float pace) implements CustomPacketPayload {
    public static final Type<ChopStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop_state"));
    public static final StreamCodec<ByteBuf, ChopStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ChopStatePayload::chopping,
            ByteBufCodecs.FLOAT, ChopStatePayload::pace,
            ChopStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}