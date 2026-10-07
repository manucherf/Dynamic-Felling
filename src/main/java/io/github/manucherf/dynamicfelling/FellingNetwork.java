package io.github.manucherf.dynamicfelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
        registrar.playToClient(SavedHitsPayload.TYPE, SavedHitsPayload.STREAM_CODEC, FellingNetwork::onSavedHits);
        registrar.playToClient(ChopEffectsPayload.TYPE, ChopEffectsPayload.STREAM_CODEC, FellingNetwork::onChopEffects);
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
                FellingTiming.endServerSession(player);
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
        FellingTiming.endServerSession(event.getEntity());
    }

    //only chopping players
    static void onChopHit(ChopHitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && CHOPPING.containsKey(player.getUUID())) {
                ServerLevel level = player.serverLevel();
                BlockPos trunk = payload.trunk();

                //nearby players hear the chop
                float size = FellingTiming.trunkSize(level, trunk);
                float weight = Mth.clamp((size - 0.5F) / 2.5F, 0.0F, 1.0F);
                float pitch = Math.max(0.7F, 1.0F - 0.15F * (size - 1.0F)) * (0.95F + level.random.nextFloat() * 0.1F);
                level.playSound(player, trunk, FellingSounds.CHOP.get(), SoundSource.BLOCKS, 0.8F + 0.2F * size, pitch);
                level.playSound(player, trunk, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.15F + 0.55F * weight, 0.75F - 0.2F * weight + level.random.nextFloat() * 0.05F);

                //show chips and leaves to players who see the chopper
                HitResult pick = player.pick(FellingConfig.CHOP_REACH.get(), 1.0F, false);
                Direction face;
                Vec3 at;
                if (pick instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                    face = hit.getDirection();
                    at = hit.getLocation();
                } else {
                    //trunk already gone on last hit, use the side facing the chopper
                    face = Direction.getNearest(player.getX() - trunk.getX() - 0.5, 0.0, player.getZ() - trunk.getZ() - 0.5);
                    at = Vec3.atCenterOf(trunk).relative(face, 0.5);
                }
                PacketDistributor.sendToPlayersTrackingEntity(player, new ChopEffectsPayload(trunk, face, at.toVector3f()));

                if (isRealHit(player, trunk)) {
                    AngryBees.onHit(player, trunk);
                    //work out total
                    ChopMemory.addHit(level, trunk, FellingTiming.hitsToFell(FellingTiming.swingsToFell(player, level, trunk)));
                    FellingTiming.countServerHit(player, trunk);
                }
            }
        });
    }

    private static boolean isRealHit(ServerPlayer player, BlockPos trunk) {
        //check if real trunk
        return player.canInteractWithBlock(trunk, 1.0) && FellingTiming.swingsToFell(player, player.serverLevel(), trunk) > 0;
    }

    static void onSavedHits(SavedHitsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SavedChops.onSaved(payload));
    }


    static void onChopEffects(ChopEffectsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ChopTracker.onOtherHit(payload.trunk(), payload.face(), new Vec3(payload.at())));
    }


    static void onServerStopping(ServerStoppingEvent event) {
        CHOPPING.clear();
        FellingTiming.clearServerSessions();
        ChopMemory.clear();
    }
}