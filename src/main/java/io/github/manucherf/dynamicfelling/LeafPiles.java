package io.github.manucherf.dynamicfelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class LeafPiles {
    private static final ResourceLocation FALLEN_LEAVES = ResourceLocation.fromNamespaceAndPath("projectvibrantjourneys", "oak_fallen_leaves");

    static void onHit(ServerPlayer player, BlockPos trunk) {
        ServerLevel level = player.serverLevel();

        Optional<Block> pile = BuiltInRegistries.BLOCK.getOptional(FALLEN_LEAVES);
        if (pile.isEmpty() || level.random.nextDouble() >= FellingConfig.LEAF_PILE_CHANCE.get()) {
            return;
        }
        BlockState state = pile.get().defaultBlockState();
        //8 random columns within 3 blocks of the trunk
        //1 block above the hit down to 4 below for an empty spot
        for (int i = 0; i < 8; i++) {
            BlockPos column = trunk.offset(level.random.nextInt(7) - 3, 0, level.random.nextInt(7) - 3);
            for (int dy = 1; dy >= -4; dy--) {
                BlockPos spot = column.above(dy);
                if (level.getBlockState(spot).isAir() && state.canSurvive(level, spot)
                        && !level.getBlockState(spot.below()).is(BlockTags.LEAVES)) {
                    level.setBlockAndUpdate(spot, state);
                    return;
                }
            }
        }
    }
}
