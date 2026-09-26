package io.github.manucherf.dynamicfelling;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FellingNetwork {
    private FellingNetwork() {}
    private static final Map<UUID, Float> CHOPPING = new HashMap<>();

    static void register(RegisterPayloadHandlersEvent event) {
        //packet payload version
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ChopStatePayload.TYPE, ChopStatePayload.STREAM_CODEC, FellingNetwork::onChopState);
        registrar.playToServer(ChopHitPayload.TYPE, ChopHitPayload.STREAM_CODEC, FellingNetwork::onChopHit);
        registrar.playToClient(PlayerChopPayload.TYPE, PlayerChopPayload.STREAM_CODEC, FellingNetwork::onPlayerChop);
    }

    private static void onChopState(ChopStatePayload payload, IPayloadContext context) {
        //forward to server
        float pace = Float.isFinite(payload.pace()) ? Mth.clamp(payload.pace(), 0.0F, 4.0F) : 1.0F;
        context.enqueueWork(() -> {
            Player player = context.player();
            if (payload.chopping()) {
                CHOPPING.put(player.getUUID(), pace);
            } else {
                CHOPPING.remove(player.getUUID());
            }
            PacketDistributor.sendToPlayersTrackingEntity(player,
                    new PlayerChopPayload(player.getId(), payload.chopping(), pace));
        });
    }

    private static void onPlayerChop(PlayerChopPayload payload, IPayloadContext context) {
        //hand job to chopAnimation()
        context.enqueueWork(() -> ChopAnimation.showOther(payload.entityId(), payload.chopping(), payload.pace()));
    }

    static void onStartTracking(PlayerEvent.StartTracking event) {
        //only continue if player
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer viewer) {
            Float pace = CHOPPING.get(target.getUUID());
            if (pace != null) {
                PacketDistributor.sendToPlayer(viewer, new PlayerChopPayload(target.getId(), true, pace));
            }
        }
    }

    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        //remove if quit mid chop
        CHOPPING.remove(event.getEntity().getUUID());
    }

    //only chopping players
    static void onChopHit(ChopHitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && CHOPPING.containsKey(player.getUUID()) && isRealHit(player, payload.trunk())){
                LeafPiles.onHit(player, payload.trunk());
                AngryBees.onHit(player, payload.trunk());
            }
        });
    }

    private static boolean isRealHit(ServerPlayer player, BlockPos trunk) {
        //check if real trunk
        return player.canInteractWithBlock(trunk, 1.0) && FellingTiming.swingsToFell(player, player.serverLevel(), trunk) > 0;
    }
}