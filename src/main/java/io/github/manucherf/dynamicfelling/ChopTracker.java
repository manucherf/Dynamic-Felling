package io.github.manucherf.dynamicfelling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public class ChopTracker {
    private static BlockPos target;
    private static float swings;
    private static float time;
    private static boolean chopping;
    private static float sentPace = 1.0F;
    private static int heldStrength;
    private static float currentPace = 1.0F;
    private static final int CENTER_CRACK_ID = -1712;
    private static BlockPos center;
    private static final int WOOSH_LEAD_TICKS = 6;
    static final int SWING_LEAD_TICKS = 3;
    private static final float CHOP_MOVE_SPEED = 0.15F;

    private ChopTracker() {}

    //client, counts ticks spent chopping trunk and detects when each hit lands
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = chopTarget(minecraft);
        //stopped chopping or switched blocks: start over
        if (pos == null || !pos.equals(target)) {
            clearCenterCrack(minecraft);
            target = pos;
            center = pos == null ? null : FellingTiming.trunkCenter(minecraft.level, pos);
            time = 0.0F;
            if (target != null) {
                //fix crack flash
                updateCracks(minecraft);
            }

            //chop animation
            if (minecraft.player != null) {
                if (target != null) {
                    ChopAnimation.start(minecraft.player);
                } else {
                    ChopAnimation.stop(minecraft.player);
                }
            }

            if ((target != null) != chopping) {
                if (chopping && minecraft.player != null && !minecraft.options.keyAttack.isDown()) {
                    // release attack key ending mining, which resets the attack cooldown
                    minecraft.player.attackStrengthTicker = heldStrength + 1;
                }

                if (chopping && minecraft.gameMode != null && !minecraft.gameMode.isDestroying()) {
                    // fix start mining skip
                    minecraft.gameMode.destroyBlockPos = new BlockPos(-1, -1, -1);
                }
                //run every tick when not chopping
                chopping = target != null;
                sentPace = 1.0F;
                PacketDistributor.sendToServer(new ChopStatePayload(chopping, sentPace));
            }
            return;
        }

        float before = time;
        float pace = FellingTiming.pace(minecraft.player);
        currentPace = pace;
        time += pace;
        ChopAnimation.setPace(minecraft.player, pace);

        //only update when pace modified
        if (pace != sentPace) {
            sentPace = pace;
            PacketDistributor.sendToServer(new ChopStatePayload(true, pace));
        }

        updateCracks(minecraft);
        if (crossed(before, time, 0)) {
            onHit(minecraft);
        }

        //only 1 swing
        if (crossed(before, time, SWING_LEAD_TICKS) && minecraft.player != null) {
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }

        if (crossed(before, time, WOOSH_LEAD_TICKS)) {
            playWoosh(minecraft);
        }

        if (minecraft.player != null) {
            heldStrength = minecraft.player.attackStrengthTicker;
        }
    }


    //is crosshair on mineable trunk?
    private static BlockPos chopTarget(Minecraft minecraft) {
        if (minecraft.gameMode == null || !minecraft.gameMode.isDestroying()) {
            return null;
        }
        return trunkInSight(minecraft);
    }

    //am I mining it?
    private static BlockPos trunkInSight(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
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
        int hits = FellingTiming.hitsLanded(time);
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
                FellingTiming.hitsLanded(time), FellingTiming.hitsToFell(swings));
        minecraft.player.displayClientMessage(Component.literal(text), true);
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        level.playLocalSound(target, FellingSounds.CHOP.get(), SoundSource.BLOCKS,
                1.0F, 0.9F + level.random.nextFloat() * 0.2F, false);

        spawnChips(minecraft, level);
        CanopyShake.onHit(minecraft, center);
        PacketDistributor.sendToServer(new ChopHitPayload(center));
    }

    //clean up when crack stops
    private static void clearCenterCrack(Minecraft minecraft) {
        if (center != null && minecraft.level != null) {
            minecraft.level.destroyBlockProgress(CENTER_CRACK_ID, center, -1);
        }
    }

    //mute default chop
    static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        Minecraft minecraft = Minecraft.getInstance();
        if (sound == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        BlockPos pos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        if (!pos.equals(chopTarget(minecraft))) {
            return;
        }
        SoundType type = minecraft.level.getBlockState(pos).getSoundType(minecraft.level, pos, minecraft.player);
        if (sound.getLocation().equals(type.getHitSound().getLocation())) {
            event.setSound(null);
        }
    }

    //only left click
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && trunkInSight(Minecraft.getInstance()) != null) {
            event.setSwingHand(false);
        }
    }

    //play woosh
    private static void playWoosh(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FellingSounds.WOOSH.get(),
                SoundSource.PLAYERS, 0.6F, 0.9F + level.random.nextFloat() * 0.2F, false);
    }


    //wood chips
    private static void spawnChips(Minecraft minecraft, ClientLevel level) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(center));
        Direction face = hit.getDirection();
        Vec3 point = hit.getLocation().add(face.getStepX() * 0.05, face.getStepY() * 0.05, face.getStepZ() * 0.05);
        for (int i = 0; i < 8; i++) {
            level.addParticle(chip, point.x, point.y, point.z, face.getStepX(), 0.5, face.getStepZ());
        }
    }

    //did I pass a hit since last tick
    private static boolean crossed(float before, float after, int lead) {
        return FellingTiming.hitsLanded(after + lead) > FellingTiming.hitsLanded(before + lead);
    }

    static boolean isChopping() {
        return chopping;
    }

    //return 0 when hit lands, then count to 20
    static float swingTicks(float partialTick) {
        return Mth.positiveModulo(time + partialTick * currentPace - FellingTiming.FIRST_HIT_TICKS, FellingTiming.TICKS_PER_SWING);
    }

    static float chopTicks(float partialTick) {
        return time + partialTick * currentPace;
    }

    //restrict movement speed
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!chopping) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse *= CHOP_MOVE_SPEED;
        input.leftImpulse *= CHOP_MOVE_SPEED;
        event.getEntity().setSprinting(false);
    }
}
