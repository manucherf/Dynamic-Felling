package io.github.manucherf.dynamicfelling;

import com.dtteam.dynamictrees.block.branch.BranchBlock;
import io.github.manucherf.dynamicfelling.compat.FallingLeavesCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.*;

public class CanopyShake {
    private static final int LEAF_SEARCHES = 512;
    private static final int LEAVES_PER_HIT = 128;
    private static final int FALLING_LEAVES_PER_HIT = 64;
    private static final int MAX_BRANCHES = 512;
    private static BlockPos cachedTrunk;
    private static List<BlockPos> cachedLeaves = List.of();
    private static final int LEAFY_RADIUS = 1;
    private static final boolean FALLING_LEAVES = ModList.get().isLoaded("fallingleaves");

    static void onHit(Minecraft minecraft, BlockPos trunk) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;

        List<BlockPos> leaves = leavesOf(level, trunk);
        if (leaves.isEmpty()) {
            return;
        }

        int maxLeaves = switch (minecraft.options.particles().get()) {
            case ALL -> LEAVES_PER_HIT;
            case DECREASED -> LEAVES_PER_HIT / 3;
            case MINIMAL -> 0;
        };

        if (FALLING_LEAVES) {
            maxLeaves = maxLeaves * FALLING_LEAVES_PER_HIT / LEAVES_PER_HIT;
        }

        BlockPos rustleAt = null;
        int spawned = 0;
        for (int i = 0; i < LEAF_SEARCHES && spawned < maxLeaves; i++) {
            BlockPos pos = leaves.get(random.nextInt(leaves.size()));
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES)) {
                continue;
            }
            for (int j = 0; j < 1 && spawned < maxLeaves; j++) {
                if (FALLING_LEAVES) {
                    FallingLeavesCompat.spawnLeaf(level, pos, state, random);
                    rustleAt = pos;
                    spawned++;
                    continue;
                }
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() - 0.05;
                double z = pos.getZ() + random.nextDouble();
                Particle particle = minecraft.particleEngine.createParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        x, y, z, (random.nextDouble() - 0.5) * 0.05, 0.0, (random.nextDouble() - 0.5) * 0.05);
                if (particle != null) {
                    // leaves drift down slowly instead of dropping like wood chips
                    particle.gravity = 0.06F;
                    particle.setLifetime(60 + random.nextInt(40));
                    particle.scale(1.5F);
                    rustleAt = pos;
                    spawned++;
                }
            }
        }
        if (rustleAt != null) {
            SoundType sound = level.getBlockState(rustleAt).getSoundType(level, rustleAt, minecraft.player);
            level.playLocalSound(rustleAt, sound.getStepSound(), SoundSource.BLOCKS, 0.6F, 0.8F + random.nextFloat() * 0.2F, false);
        }
    }


    //walk the tree's connected branches, collect leaves within 2 blocks of them
    private static List<BlockPos> findLeaves(ClientLevel level, BlockPos trunk) {
        if (!(level.getBlockState(trunk).getBlock() instanceof BranchBlock)) {
            return List.of();
        }
        Set<BlockPos> branches = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        branches.add(trunk);
        open.add(trunk);
        while (!open.isEmpty() && branches.size() < MAX_BRANCHES) {
            BlockPos branch = open.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = branch.relative(direction);
                if (!branches.contains(next) && level.getBlockState(next).getBlock() instanceof BranchBlock) {
                    branches.add(next);
                    open.add(next);
                }
            }
        }
        Set<BlockPos> leaves = new HashSet<>();
        for (BlockPos branch : branches) {
            BlockState state = level.getBlockState(branch);
            //only thin branches
            if (((BranchBlock) state.getBlock()).getRadius(state) > LEAFY_RADIUS) {
                continue;
            }
            //check 5x5x5 around each branch
            for (BlockPos pos : BlockPos.betweenClosed(branch.offset(-2, -2, -2), branch.offset(2, 2, 2))) {
                //only the underside of the canopy
                if (level.getBlockState(pos).is(BlockTags.LEAVES) && level.getBlockState(pos.below()).isAir()) {
                    leaves.add(pos.immutable());
                }
            }
        }
        return new ArrayList<>(leaves);
    }


    //leaves of a tree, found once per trunk and reused every chop
    private static List<BlockPos> leavesOf(ClientLevel level, BlockPos trunk) {
        if (!trunk.equals(cachedTrunk)) {
            cachedTrunk = trunk;
            cachedLeaves = findLeaves(level, trunk);
        }
        return cachedLeaves;
    }
}
