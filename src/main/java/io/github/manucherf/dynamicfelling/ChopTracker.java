package io.github.manucherf.dynamicfelling;

import com.dtteam.dynamictrees.block.branch.BranchBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private static final int WOOSH_LEAD_TICKS = 8;
    static final int SWING_LEAD_TICKS = 3;
    private static final float CHOP_MOVE_SPEED = 0.15F;
    private static final float KICK_DEGREES = 1.2F;
    private static final float KICK_TICKS = 4.0F;
    private static long kickAt = -100L;
    private static float kickSize = 1.0F;
    private static int saved;
    private static BlockPos lastCut;
    private static long lastCutAt;
    private static ResourceLocation breakSound;
    private static int cutHits;
    private static long chopSeed;
    private static BlockPos lastCutCenter;

    private ChopTracker() {}

    //client, counts ticks spent chopping trunk and detects when each hit lands
    static void onClientTick(ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        //if (minecraft.player != null && minecraft.level != null && center != null) {
        //    minecraft.player.displayClientMessage(Component.literal("saved=" + SavedChops.saved(minecraft.level, center)), true);
        //}
        BlockPos pos = chopTarget(minecraft);
        //stopped chopping or switched blocks: start over
        if (pos == null || !pos.equals(target)) {
            if (target != null && minecraft.gameMode != null && minecraft.gameMode.destroyDelay > 0 && FellingTiming.hitsLanded(time) < FellingTiming.hitsToFell(Math.max(1.0F, swings - saved))) {
                //play final hit before letting go
                onHit(minecraft);
            }

            if (target != null && minecraft.gameMode != null && minecraft.gameMode.destroyDelay > 0) {
                //break sound arrives from server later
                lastCut = target.immutable();
                lastCutCenter = center;
                lastCutAt = minecraft.level.getGameTime();
                cutHits = FellingTiming.hitsToFell(Math.max(1.0F, swings - saved));
            }

            clearCenterCrack(minecraft);
            target = pos == null ? null : pos.immutable();
            center = pos == null ? null : FellingTiming.trunkCenter(minecraft.level, pos).immutable();            //start client chopping session
            saved = target == null ? 0 : SavedChops.saved(minecraft.level, center);
            FellingTiming.startClientSession(center, saved);

            time = 0.0F;
            if (target != null) {
                //fix crack flash
                updateCracks(minecraft);
                //save break sound
                breakSound = minecraft.level.getBlockState(center).getSoundType(minecraft.level, center, minecraft.player).getBreakSound().getLocation();                //new swing order for each chop
                chopSeed = minecraft.level.random.nextLong();
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
        BlockPos pos = trunkInSight(minecraft);
        return pos == null ? null : stickyTarget(pos);
    }

    //am I mining it?
    private static BlockPos trunkInSight(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        swings = FellingTiming.swingsToFell(player, minecraft.level, hit.getBlockPos());
        return swings > 0.0F && FellingTiming.inChopReach(player, hit.getBlockPos()) ? hit.getBlockPos() : null;
    }

    //choppable trunk out of chop reach
    private static boolean trunkTooFar(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        return FellingTiming.swingsToFell(player, minecraft.level, pos) > 0.0F && !FellingTiming.inChopReach(player, pos);
    }

    private static void updateCracks(Minecraft minecraft) {
        //crack updates
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        int hits = saved + FellingTiming.hitsLanded(time);
        int stage = hits == 0 ? -1 : Math.min(9, Mth.ceil(hits * 10.0F / FellingTiming.hitsToFell(swings)) - 1);
        level.destroyBlockProgress(player.getId(), target, stage);
        //draw crack on center
        if (!center.equals(target)) {
            level.destroyBlockProgress(CENTER_CRACK_ID, center, stage);
        }
    }

    //placeholder, later replace this with cracks, sound and particles.
    private static void onHit(Minecraft minecraft) {

        float size = trunkSize(minecraft);
        //minecraft.player.displayClientMessage(Component.literal("size=" + size), true);
        //float pitch = (1.1F - 0.2F * (size - 1.0F)) * (0.95F + minecraft.level.random.nextFloat() * 0.1F);
        //float volume = 0.8F + 0.2F * size;

        float pitch = Math.max(0.7F, 1.0F - 0.15F * (size - 1.0F)) * (0.95F + minecraft.level.random.nextFloat() * 0.1F);
        float volume = 0.8F + 0.2F * size;

        minecraft.level.playLocalSound(target, FellingSounds.CHOP.get(), SoundSource.BLOCKS, volume, pitch, false);
        //low wooden layer under every chop, heavier on thicker trunks
        float weight = Mth.clamp((size - 0.5F) / 2.5F, 0.0F, 1.0F);
        minecraft.level.playLocalSound(target, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS,
                0.15F + 0.55F * weight, 0.75F - 0.2F * weight + minecraft.level.random.nextFloat() * 0.05F, false);

        //String text = String.format(Locale.ROOT, "Hit %d of %d", FellingTiming.hitsLanded(time), FellingTiming.hitsToFell(swings));
        //minecraft.player.displayClientMessage(Component.literal(text), true);
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        //level.playLocalSound(target, FellingSounds.CHOP.get(), SoundSource.BLOCKS,
        //        1.0F, 0.9F + level.random.nextFloat() * 0.2F, false);

        if (minecraft.hitResult instanceof BlockHitResult hit) {
            spawnChips(level, center, hit.getDirection(), hit.getLocation(), size);
        }

        CanopyShake.onHit(minecraft, center);
        PacketDistributor.sendToServer(new ChopHitPayload(center));
        kickAt = minecraft.level.getGameTime();
        kickSize = size;
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
        if ((pos.equals(lastCut)  || pos.equals(lastCutCenter)) && minecraft.level.getGameTime() - lastCutAt < 20 && sound.getLocation().equals(breakSound)) {
            event.setSound(null);
            return;
        }
        //System.out.println(sound.getLocation() + " at " + pos + " target=" + target + " chop=" + chopTarget(minecraft));
        //compare with chopping  not crosshair
        if (!pos.equals(target) && !pos.equals(chopTarget(minecraft))) {
            return;
        }
        SoundType type = minecraft.level.getBlockState(pos).getSoundType(minecraft.level, pos, minecraft.player);
        if (sound.getLocation().equals(type.getHitSound().getLocation())) {
            event.setSound(null);
        }
    }

    //only left click
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (trunkInSight(minecraft) != null) {
            event.setSwingHand(false);
        } else if (trunkTooFar(minecraft)) {
            //too far to chop, do nothing instead of mining
            event.setCanceled(true);
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
        //heavier swing on thicker trunks
        float weight = Mth.clamp((trunkSize(minecraft) - 0.5F) / 2.5F, 0.0F, 1.0F);
        level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), FellingSounds.WOOSH.get(), SoundSource.PLAYERS, 0.5F + 0.4F * weight, (1.1F - 0.3F * weight) * (0.95F + level.random.nextFloat() * 0.1F), false);
    }


    //wood chips
    private static void spawnChips(ClientLevel level, BlockPos center, Direction face, Vec3 at, float size) {
        BlockState state = level.getBlockState(center);
        if (state.isAir()) {
            return;
        }
        RandomSource random = level.random;
        BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, state);
        int count = Math.round(8 * size);
        for (int i = 0; i < count; i++) {
            // spray out of struck face toward player
            double speed = 0.1 + random.nextDouble() * 0.15;
            double vx = face.getStepX() * speed + (random.nextDouble() - 0.5) * 0.15;
            double vy = 0.05 + random.nextDouble() * 0.15;
            double vz = face.getStepZ() * speed + (random.nextDouble() - 0.5) * 0.15;
            level.addParticle(chip, at.x + face.getStepX() * 0.05, at.y, at.z + face.getStepZ() * 0.05, vx, vy, vz);
        }
    }

    //did I pass a hit since last tick
    private static boolean crossed(float before, float after, int lead) {
        return FellingTiming.hitsLanded(after + lead) > FellingTiming.hitsLanded(before + lead);
    }

    static boolean isChopping() {
        return chopping;
    }

    //let other classes ask about the cut
    static boolean recentlyCut(long gameTime, int ticks) {
        return lastCut != null && gameTime - lastCutAt < ticks;
    }

    static int cutHits() {
        return cutHits;
    }

    static long chopSeed() {
        return chopSeed;
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

    private static float trunkSize(Minecraft minecraft) {
        return FellingTiming.trunkSize(minecraft.level, center);
    }


    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        //ticks since hit
        float since = minecraft.level.getGameTime() - kickAt + (float) event.getPartialTick();

        float length = KICK_TICKS + kickSize;
        if (since < length) {
            // sharp dip on impact, bigger and longer on thick trunks
            float left = 1.0F - since / length;
            event.setPitch(event.getPitch() + KICK_DEGREES * kickSize * kickSize * left * left);
        }
    }


    //chips and canopy for hit by another player
    static void onOtherHit(BlockPos center, Direction face, Vec3 at) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        spawnChips(minecraft.level, center, face, at, FellingTiming.trunkSize(minecraft.level, center));
        CanopyShake.onHit(minecraft, center);
    }


    //keep mining the same block if crosshair moves
    public static BlockPos stickyTarget(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (target == null || center == null || minecraft.level == null || minecraft.player == null || pos.equals(target)) {
            return pos;
        }
        if (!center.equals(FellingTiming.trunkCenter(minecraft.level, pos)) || !FellingTiming.inChopReach(minecraft.player, target)) {
            return pos;
        }
        return target;
    }

}
