package io.github.manucherf.dynamicfelling;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerChopPayload(int entityId, boolean chopping, float pace) implements CustomPacketPayload {
    //payload name
    public static final Type<PlayerChopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DynamicFelling.MODID, "player_chop"));
    //payload conversion
    public static final StreamCodec<ByteBuf, PlayerChopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerChopPayload::entityId,
            ByteBufCodecs.BOOL, PlayerChopPayload::chopping,
            ByteBufCodecs.FLOAT, PlayerChopPayload::pace,
            PlayerChopPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}