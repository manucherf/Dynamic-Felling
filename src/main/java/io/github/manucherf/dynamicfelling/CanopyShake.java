package io.github.manucherf.dynamicfelling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class CanopyShake {
    private static final int LEAF_SEARCHES = 500;
    private static final int LEAVES_PER_HIT = 100;

    static void onHit(Minecraft minecraft, BlockPos trunk) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;

        int maxLeaves = switch (minecraft.options.particles().get()) {
            case ALL -> LEAVES_PER_HIT;
            case DECREASED -> LEAVES_PER_HIT / 3;
            case MINIMAL -> 0;
        };

        BlockPos rustleAt = null;
        int spawned = 0;
        for (int i = 0; i < LEAF_SEARCHES && spawned < maxLeaves; i++) {
            BlockPos pos = trunk.offset(random.nextInt(7) - 3, 2 + random.nextInt(8), random.nextInt(7) - 3);
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES)) {
                continue;
            }
            for (int j = 0; j < 5 && spawned < maxLeaves; j++) {
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
}
