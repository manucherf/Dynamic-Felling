package io.github.manucherf.dynamicfelling;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public record ChopEffectsPayload(BlockPos trunk, Direction face, Vector3f at) implements CustomPacketPayload {
    public static final Type<ChopEffectsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "chop_effects"));
    public static final StreamCodec<ByteBuf, ChopEffectsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ChopEffectsPayload::trunk,
            Direction.STREAM_CODEC, ChopEffectsPayload::face,
            ByteBufCodecs.VECTOR3F, ChopEffectsPayload::at,
            ChopEffectsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}